package com.homestay.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homestay.calendar.entity.RoomCalendar;
import com.homestay.calendar.mapper.RoomCalendarMapper;
import com.homestay.common.BizException;
import com.homestay.common.ErrorCode;
import com.homestay.common.PageResult;
import com.homestay.order.dto.CreateOrderRequest;
import com.homestay.order.dto.OrderDetailVO;
import com.homestay.order.dto.OrderLogVO;
import com.homestay.order.dto.OrderVO;
import com.homestay.order.entity.Order;
import com.homestay.order.entity.OrderLog;
import com.homestay.order.entity.OrderRoom;
import com.homestay.order.entity.OrderStay;
import com.homestay.order.enums.OrderStatus;
import com.homestay.order.mapper.OrderLogMapper;
import com.homestay.order.mapper.OrderMapper;
import com.homestay.order.mapper.OrderRoomMapper;
import com.homestay.order.mapper.OrderStayMapper;
import com.homestay.room.entity.Room;
import com.homestay.room.mapper.RoomMapper;
import com.homestay.roomtype.entity.RoomType;
import com.homestay.roomtype.mapper.RoomTypeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final int MAX_NIGHTS = 30;
    private static final DateTimeFormatter ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderMapper orderMapper;
    private final OrderStayMapper orderStayMapper;
    private final OrderLogMapper orderLogMapper;
    private final OrderRoomMapper orderRoomMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final RoomMapper roomMapper;
    private final RoomCalendarMapper calendarMapper;

    /** 构造注入所需的 Mapper */
    public OrderService(OrderMapper orderMapper,
                        OrderStayMapper orderStayMapper,
                        OrderLogMapper orderLogMapper,
                        OrderRoomMapper orderRoomMapper,
                        RoomTypeMapper roomTypeMapper,
                        RoomMapper roomMapper,
                        RoomCalendarMapper calendarMapper) {
        this.orderMapper = orderMapper;
        this.orderStayMapper = orderStayMapper;
        this.orderLogMapper = orderLogMapper;
        this.orderRoomMapper = orderRoomMapper;
        this.roomTypeMapper = roomTypeMapper;
        this.roomMapper = roomMapper;
        this.calendarMapper = calendarMapper;
    }

    /**
     * 下单：整区间校验库存 → 逐日原子扣减 → 自动分配房间 → 落订单明细。
     * 任一步失败都会回滚，库存和房间分配一起撤销。
     */
    @Transactional
    public OrderVO create(Long userId, CreateOrderRequest request) {
        LocalDate checkIn = request.checkInDate();
        LocalDate checkOut = request.checkOutDate();
        if (checkIn.isBefore(LocalDate.now())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "入住日期不能早于今天");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "离店日期必须晚于入住日期");
        }
        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights > MAX_NIGHTS) {
            throw new BizException(ErrorCode.PARAM_INVALID, "单次最多预订 " + MAX_NIGHTS + " 晚");
        }

        RoomType roomType = roomTypeMapper.selectById(request.roomTypeId());
        if (roomType == null || !Objects.equals(roomType.getStatus(), 1)) {
            throw new BizException(ErrorCode.NOT_FOUND, "房型不存在或已下架");
        }

        // 每晚都必须有已开放且库存充足的日历记录，同时累计单间房的住宿总价
        List<LocalDate> dates = checkIn.datesUntil(checkOut).toList();
        Map<LocalDate, RoomCalendar> calendarMap = calendarMapper.selectList(
                        new LambdaQueryWrapper<RoomCalendar>()
                                .eq(RoomCalendar::getRoomTypeId, request.roomTypeId())
                                .between(RoomCalendar::getStayDate, checkIn, checkOut.minusDays(1)))
                .stream()
                .collect(Collectors.toMap(RoomCalendar::getStayDate, Function.identity()));

        BigDecimal nightTotal = BigDecimal.ZERO;
        for (LocalDate date : dates) {
            RoomCalendar calendar = calendarMap.get(date);
            if (calendar == null) {
                throw new BizException(ErrorCode.CONFLICT, date + " 未开放预订");
            }
            if (!Objects.equals(calendar.getStatus(), 1)) {
                throw new BizException(ErrorCode.CONFLICT, date + " 已关房，请选择其他日期");
            }
            if (calendar.getStock() < request.roomCount()) {
                throw new BizException(ErrorCode.CONFLICT, date + " 库存不足，仅剩 " + calendar.getStock() + " 间");
            }
            nightTotal = nightTotal.add(calendar.getPrice());
        }

        // 通过带库存条件的 UPDATE 原子扣减；影响行数为 0 表示并发下库存已被其它订单抢走
        for (LocalDate date : dates) {
            if (calendarMapper.decreaseStock(request.roomTypeId(), date, request.roomCount()) != 1) {
                throw new BizException(ErrorCode.CONFLICT, "库存不足，请重新选择日期");
            }
        }

        List<Room> assignedRooms = allocateRooms(request.roomTypeId(), checkIn, checkOut, request.roomCount());

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setRoomTypeId(request.roomTypeId());
        order.setCheckInDate(checkIn);
        order.setCheckOutDate(checkOut);
        order.setNights(nights);
        order.setRoomCount(request.roomCount());
        order.setTotalAmount(nightTotal.multiply(BigDecimal.valueOf(request.roomCount())));
        order.setStatus(OrderStatus.PENDING);
        order.setContactName(request.contactName());
        order.setContactPhone(request.contactPhone());
        order.setRemark(request.remark());
        orderMapper.insert(order);
        // 数据库默认值不会回填到实体，这里补上创建时间供响应使用
        order.setCreatedAt(LocalDateTime.now());

        for (Room room : assignedRooms) {
            OrderRoom link = new OrderRoom();
            link.setOrderId(order.getId());
            link.setRoomId(room.getId());
            orderRoomMapper.insert(link);
        }

        for (LocalDate date : dates) {
            OrderStay stay = new OrderStay();
            stay.setOrderId(order.getId());
            stay.setStayDate(date);
            orderStayMapper.insert(stay);
        }
        addLog(order.getId(), null, OrderStatus.PENDING, userId, "房客提交订单");

        List<String> roomNos = assignedRooms.stream().map(Room::getRoomNo).toList();
        return OrderVO.from(order, roomType.getName(), roomNos);
    }

    /** 分页查询当前用户的订单，可按状态过滤 */
    public PageResult<OrderVO> myOrders(Long userId, String status, long page, long size) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(StringUtils.hasText(status), Order::getStatus, status)
                .orderByDesc(Order::getId);
        return toPage(orderMapper.selectPage(new Page<>(page, size), wrapper));
    }

    /** 管理员分页查询全部订单，可按状态与订单号/手机号关键字过滤 */
    public PageResult<OrderVO> page(String status, String keyword, long page, long size) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(StringUtils.hasText(status), Order::getStatus, status)
                .and(StringUtils.hasText(keyword), query -> query
                        .like(Order::getOrderNo, keyword)
                        .or()
                        .like(Order::getContactPhone, keyword))
                .orderByDesc(Order::getId);
        return toPage(orderMapper.selectPage(new Page<>(page, size), wrapper));
    }

    /** 查询订单详情与状态流转日志，非管理员只能查看自己的订单 */
    public OrderDetailVO detail(Long orderId, Long userId, boolean admin) {
        Order order = getExisting(orderId);
        if (!admin && !Objects.equals(order.getUserId(), userId)) {
            throw new BizException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        List<OrderLogVO> logs = orderLogMapper.selectList(new LambdaQueryWrapper<OrderLog>()
                        .eq(OrderLog::getOrderId, orderId)
                        .orderByAsc(OrderLog::getId))
                .stream()
                .map(OrderLogVO::from)
                .toList();
        return new OrderDetailVO(OrderVO.from(order, roomTypeName(order.getRoomTypeId()), loadRoomNos(orderId)), logs);
    }

    /**
     * 房客取消：仅待确认与已确认可取消，取消后逐日归还库存。
     */
    @Transactional
    public OrderVO cancelByCustomer(Long orderId, Long userId, String reason) {
        Order order = getExisting(orderId);
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BizException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        return cancelInternal(order, userId, StringUtils.hasText(reason) ? reason : "房客取消订单");
    }

    /** 房东确认订单，状态流转为已确认 */
    @Transactional
    public OrderVO confirm(Long orderId, Long operatorId) {
        return transition(orderId, OrderStatus.CONFIRMED, operatorId, "房东确认订单");
    }

    /**
     * 房东拒绝：与取消同效，需归还库存。
     */
    @Transactional
    public OrderVO reject(Long orderId, Long operatorId, String reason) {
        Order order = getExisting(orderId);
        if (!OrderStatus.canTransition(order.getStatus(), OrderStatus.CANCELLED)) {
            throw new BizException(ErrorCode.CONFLICT, "订单当前状态为「" + OrderStatus.text(order.getStatus()) + "」，不能拒绝");
        }
        return cancelInternal(order, operatorId, StringUtils.hasText(reason) ? reason : "房东拒绝订单");
    }

    /** 办理入住，状态流转为已入住 */
    @Transactional
    public OrderVO checkIn(Long orderId, Long operatorId) {
        return transition(orderId, OrderStatus.CHECKED_IN, operatorId, "房客入住");
    }

    /** 完成订单，状态流转为已完成 */
    @Transactional
    public OrderVO complete(Long orderId, Long operatorId) {
        return transition(orderId, OrderStatus.COMPLETED, operatorId, "订单完成");
    }

    /**
     * 选择该房型下、在入住区间内没有被其它有效订单占用的房间。
     * 先对房间加行锁，避免并发下单把同一间房分给两个订单。
     * 不做房间重排：已有订单的房间一旦分配就不再变动，凑不出连续房间时直接拒绝下单。
     */
    private List<Room> allocateRooms(Long roomTypeId, LocalDate checkIn, LocalDate checkOut, int roomCount) {
        List<Room> candidates = roomMapper.selectBookableRoomsForUpdate(roomTypeId);
        if (candidates.isEmpty()) {
            throw new BizException(ErrorCode.CONFLICT, "该房型暂无可用房间");
        }
        Set<Long> occupied = new HashSet<>(orderRoomMapper.selectOccupiedRoomIds(roomTypeId, checkIn, checkOut));
        List<Room> free = candidates.stream()
                .filter(room -> !occupied.contains(room.getId()))
                .toList();
        if (free.size() < roomCount) {
            throw new BizException(ErrorCode.CONFLICT,
                    "所选日期每晚虽有余量，但没有同一间房可连续入住整段日期，请更换入住或离店日期，或减少预订间数");
        }
        return free.subList(0, roomCount);
    }

    /**
     * 取消与房东拒绝共用：按状态机校验后取消订单，并依据逐晚明细归还日历库存。
     * 房间分配记录会保留用于展示历史房号，但占用查询会排除已取消订单。
     */
    private OrderVO cancelInternal(Order order, Long operatorId, String reason) {
        if (!OrderStatus.canTransition(order.getStatus(), OrderStatus.CANCELLED)) {
            throw new BizException(ErrorCode.CONFLICT, "订单当前状态为「" + OrderStatus.text(order.getStatus()) + "」，不能取消");
        }

        Order update = new Order();
        update.setId(order.getId());
        update.setStatus(OrderStatus.CANCELLED);
        update.setCancelReason(reason);
        orderMapper.updateById(update);

        List<OrderStay> stays = orderStayMapper.selectList(new LambdaQueryWrapper<OrderStay>()
                .eq(OrderStay::getOrderId, order.getId()));
        for (OrderStay stay : stays) {
            calendarMapper.increaseStock(order.getRoomTypeId(), stay.getStayDate(), order.getRoomCount());
        }

        addLog(order.getId(), order.getStatus(), OrderStatus.CANCELLED, operatorId, reason);
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(reason);
        return OrderVO.from(order, roomTypeName(order.getRoomTypeId()), loadRoomNos(order.getId()));
    }

    /** 仅执行状态机允许的前向流转；确认、入住、完成都不改变库存。 */
    private OrderVO transition(Long orderId, String toStatus, Long operatorId, String remark) {
        Order order = getExisting(orderId);
        if (!OrderStatus.canTransition(order.getStatus(), toStatus)) {
            throw new BizException(ErrorCode.CONFLICT,
                    "订单当前状态为「" + OrderStatus.text(order.getStatus()) + "」，不能执行该操作");
        }

        Order update = new Order();
        update.setId(orderId);
        update.setStatus(toStatus);
        orderMapper.updateById(update);

        addLog(orderId, order.getStatus(), toStatus, operatorId, remark);
        order.setStatus(toStatus);
        return OrderVO.from(order, roomTypeName(order.getRoomTypeId()), loadRoomNos(orderId));
    }

    /** 追加一条订单状态流转日志 */
    private void addLog(Long orderId, String fromStatus, String toStatus, Long operatorId, String remark) {
        OrderLog log = new OrderLog();
        log.setOrderId(orderId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setOperatorId(operatorId);
        log.setRemark(remark);
        orderLogMapper.insert(log);
    }

    /** 将订单分页结果转换为视图分页结果，批量补全房型名称与房间号 */
    private PageResult<OrderVO> toPage(Page<Order> result) {
        // 批量加载房型名称与房间号，避免分页列表逐条查询产生 N+1 问题
        List<Long> typeIds = result.getRecords().stream().map(Order::getRoomTypeId).distinct().toList();
        Map<Long, String> typeNames = typeIds.isEmpty()
                ? Map.of()
                : roomTypeMapper.selectBatchIds(typeIds).stream()
                        .collect(Collectors.toMap(RoomType::getId, RoomType::getName));
        Map<Long, List<String>> roomNosByOrder = loadRoomNosBatch(result.getRecords());

        List<OrderVO> records = result.getRecords().stream()
                .map(order -> OrderVO.from(order, typeNames.get(order.getRoomTypeId()),
                        roomNosByOrder.getOrDefault(order.getId(), List.of())))
                .toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /** 查询单个订单已分配的房间号，按房间号排序 */
    private List<String> loadRoomNos(Long orderId) {
        List<OrderRoom> links = orderRoomMapper.selectList(new LambdaQueryWrapper<OrderRoom>()
                .eq(OrderRoom::getOrderId, orderId));
        if (links.isEmpty()) {
            return List.of();
        }
        return roomMapper.selectBatchIds(links.stream().map(OrderRoom::getRoomId).toList())
                .stream()
                .map(Room::getRoomNo)
                .sorted()
                .toList();
    }

    /** 批量查询多个订单已分配的房间号，按订单分组 */
    private Map<Long, List<String>> loadRoomNosBatch(List<Order> orders) {
        if (orders.isEmpty()) {
            return Map.of();
        }
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        List<OrderRoom> links = orderRoomMapper.selectList(new LambdaQueryWrapper<OrderRoom>()
                .in(OrderRoom::getOrderId, orderIds));
        if (links.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> roomNoById = roomMapper.selectBatchIds(
                        links.stream().map(OrderRoom::getRoomId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Room::getId, Room::getRoomNo));

        Map<Long, List<String>> grouped = links.stream()
                .collect(Collectors.groupingBy(OrderRoom::getOrderId,
                        Collectors.mapping(link -> roomNoById.get(link.getRoomId()),
                                Collectors.toCollection(ArrayList::new))));
        grouped.values().forEach(Collections::sort);
        return grouped;
    }

    /** 按 ID 查询订单，不存在时抛出业务异常 */
    private Order getExisting(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    /** 查询房型名称，房型不存在时返回 null */
    private String roomTypeName(Long roomTypeId) {
        RoomType roomType = roomTypeMapper.selectById(roomTypeId);
        return roomType == null ? null : roomType.getName();
    }

    /** 生成订单号：HS + 时间戳 + 4 位随机数 */
    private String generateOrderNo() {
        return "HS" + LocalDateTime.now().format(ORDER_NO_FORMAT)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}

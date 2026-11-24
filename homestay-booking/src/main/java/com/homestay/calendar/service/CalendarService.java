package com.homestay.calendar.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.homestay.calendar.dto.BatchCalendarRequest;
import com.homestay.calendar.dto.CalendarDayVO;
import com.homestay.calendar.dto.CalendarUpdateRequest;
import com.homestay.calendar.dto.CalendarVO;
import com.homestay.calendar.dto.GenerateCalendarRequest;
import com.homestay.calendar.dto.StayQuoteVO;
import com.homestay.calendar.entity.RoomCalendar;
import com.homestay.calendar.mapper.RoomCalendarMapper;
import com.homestay.common.BizException;
import com.homestay.common.ErrorCode;
import com.homestay.order.mapper.OrderRoomMapper;
import com.homestay.room.entity.Room;
import com.homestay.room.mapper.RoomMapper;
import com.homestay.roomtype.entity.RoomType;
import com.homestay.roomtype.mapper.RoomTypeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CalendarService {

    /** 房东批量操作允许的最大天数 */
    private static final int MAX_ADMIN_RANGE_DAYS = 366;
    /** 房客查询允许的最大天数 */
    private static final int MAX_PUBLIC_RANGE_DAYS = 92;
    /** 单次订单最多可住的晚数，与下单校验保持一致 */
    private static final int MAX_STAY_NIGHTS = 30;

    private final RoomCalendarMapper calendarMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final RoomMapper roomMapper;
    private final OrderRoomMapper orderRoomMapper;

    /** 构造注入所需的 Mapper */
    public CalendarService(RoomCalendarMapper calendarMapper, RoomTypeMapper roomTypeMapper,
                           RoomMapper roomMapper, OrderRoomMapper orderRoomMapper) {
        this.calendarMapper = calendarMapper;
        this.roomTypeMapper = roomTypeMapper;
        this.roomMapper = roomMapper;
        this.orderRoomMapper = orderRoomMapper;
    }

    /**
     * 按房型当前可用房间数生成日历：价格为基准价，库存为可用房间数，已存在的日期跳过。
     */
    @Transactional
    public int generate(Long roomTypeId, GenerateCalendarRequest request) {
        RoomType roomType = getRoomType(roomTypeId);
        validateRange(request.startDate(), request.endDate(), MAX_ADMIN_RANGE_DAYS);
        if (request.startDate().isBefore(LocalDate.now())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "开始日期不能早于今天");
        }

        Long stock = roomMapper.selectCount(new LambdaQueryWrapper<Room>()
                .eq(Room::getRoomTypeId, roomTypeId)
                .eq(Room::getStatus, 1));
        if (stock == 0) {
            throw new BizException(ErrorCode.CONFLICT, "该房型下没有可用房间，请先添加房间");
        }

        List<LocalDate> dates = request.startDate()
                .datesUntil(request.endDate().plusDays(1))
                .toList();
        return calendarMapper.batchInsertIgnore(roomTypeId, dates, roomType.getBasePrice(), stock.intValue());
    }

    /** 查询房型在指定区间内的日历列表 */
    public List<CalendarVO> list(Long roomTypeId, LocalDate from, LocalDate to) {
        getRoomType(roomTypeId);
        validateRange(from, to, MAX_ADMIN_RANGE_DAYS);
        return calendarMapper.selectList(new LambdaQueryWrapper<RoomCalendar>()
                        .eq(RoomCalendar::getRoomTypeId, roomTypeId)
                        .between(RoomCalendar::getStayDate, from, to)
                        .orderByAsc(RoomCalendar::getStayDate))
                .stream()
                .map(CalendarVO::from)
                .toList();
    }

    /**
     * 修改某一天的日历，字段为空表示不修改。
     */
    @Transactional
    public CalendarVO update(Long id, CalendarUpdateRequest request) {
        if (calendarMapper.selectById(id) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "日历记录不存在");
        }
        if (request.price() == null && request.stock() == null && request.status() == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "至少需要修改价格、库存或状态中的一项");
        }

        RoomCalendar update = new RoomCalendar();
        update.setId(id);
        update.setPrice(request.price());
        update.setStock(request.stock());
        update.setStatus(request.status());
        calendarMapper.updateById(update);
        return CalendarVO.from(calendarMapper.selectById(id));
    }

    /**
     * 区间批量改价 / 改库存 / 开关房，返回受影响的日期数。
     */
    @Transactional
    public int batchUpdate(Long roomTypeId, BatchCalendarRequest request) {
        getRoomType(roomTypeId);
        validateRange(request.fromDate(), request.toDate(), MAX_ADMIN_RANGE_DAYS);
        if (request.price() == null && request.stock() == null && request.status() == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "至少需要修改价格、库存或状态中的一项");
        }

        // 只更新请求中显式传入的字段，同时递增 version 供乐观锁识别并发修改
        LambdaUpdateWrapper<RoomCalendar> wrapper = new LambdaUpdateWrapper<RoomCalendar>()
                .eq(RoomCalendar::getRoomTypeId, roomTypeId)
                .between(RoomCalendar::getStayDate, request.fromDate(), request.toDate())
                .set(request.price() != null, RoomCalendar::getPrice, request.price())
                .set(request.stock() != null, RoomCalendar::getStock, request.stock())
                .set(request.status() != null, RoomCalendar::getStatus, request.status())
                .setSql("version = version + 1");
        return calendarMapper.update(null, wrapper);
    }

    /**
     * 房客查看房型日历：只返回上架房型，未生成日历的日期不会出现在结果中，即视为不可订。
     */
    public List<CalendarDayVO> publicCalendar(Long roomTypeId, LocalDate from, LocalDate to) {
        RoomType roomType = roomTypeMapper.selectById(roomTypeId);
        if (roomType == null || !Objects.equals(roomType.getStatus(), 1)) {
            throw new BizException(ErrorCode.NOT_FOUND, "房型不存在或已下架");
        }
        validateRange(from, to, MAX_PUBLIC_RANGE_DAYS);
        return calendarMapper.selectList(new LambdaQueryWrapper<RoomCalendar>()
                        .eq(RoomCalendar::getRoomTypeId, roomTypeId)
                        .between(RoomCalendar::getStayDate, from, to)
                        .orderByAsc(RoomCalendar::getStayDate))
                .stream()
                .map(CalendarDayVO::from)
                .toList();
    }

    /**
     * 按整段入住区间核算报价与可订间数。
     * 可订间数取「每晚最小余量」与「整段空闲的物理房间数」的较小值：
     * 按晚余量只反映某一天，连续可住的房间数才决定这一段能否下单。
     */
    public StayQuoteVO quote(Long roomTypeId, LocalDate checkIn, LocalDate checkOut, int roomCount) {
        RoomType roomType = roomTypeMapper.selectById(roomTypeId);
        if (roomType == null || !Objects.equals(roomType.getStatus(), 1)) {
            throw new BizException(ErrorCode.NOT_FOUND, "房型不存在或已下架");
        }
        if (checkIn == null || checkOut == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "入住与离店日期不能为空");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "离店日期必须晚于入住日期");
        }
        if (roomCount < 1) {
            throw new BizException(ErrorCode.PARAM_INVALID, "房间数至少 1 间");
        }
        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights > MAX_STAY_NIGHTS) {
            return new StayQuoteVO(checkIn, checkOut, nights, 0, BigDecimal.ZERO, BigDecimal.ZERO,
                    false, "单次最多预订 " + MAX_STAY_NIGHTS + " 晚");
        }

        List<LocalDate> dates = checkIn.datesUntil(checkOut).toList();
        Map<LocalDate, RoomCalendar> calendarMap = calendarMapper.selectList(
                        new LambdaQueryWrapper<RoomCalendar>()
                                .eq(RoomCalendar::getRoomTypeId, roomTypeId)
                                .between(RoomCalendar::getStayDate, checkIn, checkOut.minusDays(1)))
                .stream()
                .collect(Collectors.toMap(RoomCalendar::getStayDate, Function.identity()));

        // 总价与可订性分开计算：累计每晚日历价，并保留首个不可订原因用于反馈给房客
        BigDecimal pricePerRoom = BigDecimal.ZERO;
        int minStock = Integer.MAX_VALUE;
        String unavailableReason = "";
        for (LocalDate date : dates) {
            RoomCalendar calendar = calendarMap.get(date);
            if (calendar == null) {
                if (unavailableReason.isEmpty()) {
                    unavailableReason = date + " 未开放预订";
                }
                continue;
            }
            pricePerRoom = pricePerRoom.add(calendar.getPrice());
            if (!Objects.equals(calendar.getStatus(), 1)) {
                if (unavailableReason.isEmpty()) {
                    unavailableReason = date + " 已关房，请选择其他日期";
                }
                continue;
            }
            minStock = Math.min(minStock, calendar.getStock());
            if (calendar.getStock() < roomCount && unavailableReason.isEmpty()) {
                unavailableReason = date + " 库存不足，仅剩 " + calendar.getStock() + " 间";
            }
        }

        // 与下单分配规则一致：排除区间重叠的有效订单，计算整段日期都空闲的物理房间数
        Set<Long> occupied = new HashSet<>(orderRoomMapper.selectOccupiedRoomIds(roomTypeId, checkIn, checkOut));
        long freeRooms = roomMapper.selectBookableRoomIds(roomTypeId).stream()
                .filter(roomId -> !occupied.contains(roomId))
                .count();
        int stockRooms = minStock == Integer.MAX_VALUE ? 0 : minStock;
        int roomsAvailable = (int) Math.max(0, Math.min(stockRooms, freeRooms));

        boolean available = unavailableReason.isEmpty() && roomsAvailable >= roomCount;
        String reason = unavailableReason;
        if (reason.isEmpty() && !available) {
            reason = "所选日期每晚虽有余量，但没有同一间房可连续入住整段日期，请更换入住或离店日期，或减少预订间数";
        }

        BigDecimal totalAmount = pricePerRoom.multiply(BigDecimal.valueOf(roomCount));
        return new StayQuoteVO(checkIn, checkOut, nights, roomsAvailable, pricePerRoom, totalAmount, available, reason);
    }

    /** 按 ID 查询房型，不存在时抛出业务异常 */
    private RoomType getRoomType(Long roomTypeId) {
        RoomType roomType = roomTypeMapper.selectById(roomTypeId);
        if (roomType == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "房型不存在");
        }
        return roomType;
    }

    /** 校验日期区间非空、顺序正确且不超过最大天数 */
    private void validateRange(LocalDate from, LocalDate to, int maxDays) {
        if (from == null || to == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "日期区间不能为空");
        }
        if (to.isBefore(from)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "结束日期不能早于开始日期");
        }
        // 查询区间包含开始和结束两天，因此天数需要 +1
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days > maxDays) {
            throw new BizException(ErrorCode.PARAM_INVALID, "日期区间不能超过 " + maxDays + " 天");
        }
    }
}

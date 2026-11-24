package com.homestay.roomtype.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homestay.calendar.dto.RoomPriceStat;
import com.homestay.calendar.mapper.RoomCalendarMapper;
import com.homestay.common.BizException;
import com.homestay.common.ErrorCode;
import com.homestay.common.PageResult;
import com.homestay.room.dto.RoomFreeStat;
import com.homestay.room.mapper.RoomMapper;
import com.homestay.roomtype.dto.PublicRoomTypeVO;
import com.homestay.roomtype.dto.RoomTypeSearchItemVO;
import com.homestay.roomtype.entity.RoomType;
import com.homestay.roomtype.mapper.RoomTypeMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 房客端房源浏览：只暴露上架房型，价格全部来自日历价。
 */
@Service
public class PublicRoomTypeService {

    /** 列表 / 详情展示价格区间时向后看的天数 */
    private static final int PRICE_WINDOW_DAYS = 30;
    /** 与下单保持一致的单次最长预订晚数 */
    private static final int MAX_NIGHTS = 30;

    private final RoomTypeMapper roomTypeMapper;
    private final RoomCalendarMapper calendarMapper;
    private final RoomMapper roomMapper;

    /** 构造器注入房型、日历与房间 Mapper */
    public PublicRoomTypeService(RoomTypeMapper roomTypeMapper, RoomCalendarMapper calendarMapper,
                                 RoomMapper roomMapper) {
        this.roomTypeMapper = roomTypeMapper;
        this.calendarMapper = calendarMapper;
        this.roomMapper = roomMapper;
    }

    /**
     * 上架房型分页列表，价格区间取未来 30 天的日历价。
     */
    public PageResult<PublicRoomTypeVO> page(long page, long size, String keyword, Integer guests) {
        LambdaQueryWrapper<RoomType> wrapper = new LambdaQueryWrapper<RoomType>()
                .eq(RoomType::getStatus, 1)
                .ge(guests != null, RoomType::getMaxGuests, guests)
                .and(StringUtils.hasText(keyword),
                        w -> w.like(RoomType::getName, keyword).or().like(RoomType::getDescription, keyword))
                .orderByDesc(RoomType::getId);

        Page<RoomType> result = roomTypeMapper.selectPage(new Page<>(page, size), wrapper);
        LocalDate today = LocalDate.now();
        Map<Long, RoomPriceStat> stats = loadPriceStats(
                result.getRecords().stream().map(RoomType::getId).toList(),
                today, today.plusDays(PRICE_WINDOW_DAYS));

        List<PublicRoomTypeVO> records = result.getRecords().stream()
                .map(roomType -> toVO(roomType, stats.get(roomType.getId())))
                .toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 按入住区间和人数搜索可订房型。
     * guests 为总人数，roomCount 为间数，要求 maxGuests * roomCount >= guests。
     */
    public List<RoomTypeSearchItemVO> search(LocalDate checkIn, LocalDate checkOut, int guests, int roomCount) {
        if (checkIn.isBefore(LocalDate.now())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "入住日期不能早于今天");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "离店日期必须晚于入住日期");
        }
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights > MAX_NIGHTS) {
            throw new BizException(ErrorCode.PARAM_INVALID, "单次最多查询 " + MAX_NIGHTS + " 晚");
        }
        if (guests < 1) {
            throw new BizException(ErrorCode.PARAM_INVALID, "入住人数至少为 1");
        }
        if (roomCount < 1) {
            throw new BizException(ErrorCode.PARAM_INVALID, "房间数至少为 1");
        }

        // 第一层过滤：房型在入住的每一晚都已开放，且每晚库存都不少于请求间数
        List<Long> availableIds = calendarMapper.selectAvailableRoomTypeIds(checkIn, checkOut, roomCount, nights);
        if (availableIds.isEmpty()) {
            return List.of();
        }

        // 每晚有余量还不够，整段区间必须有足够的同一批房间空闲，才能保证下单时分配得到连续房间
        Map<Long, Integer> freeRoomsByType = roomMapper.selectFreeRoomStats(checkIn, checkOut, roomCount).stream()
                .collect(Collectors.toMap(RoomFreeStat::getRoomTypeId, RoomFreeStat::getFreeRooms));
        List<Long> bookableIds = availableIds.stream()
                .filter(freeRoomsByType::containsKey)
                .toList();
        if (bookableIds.isEmpty()) {
            return List.of();
        }

        List<RoomType> roomTypes = roomTypeMapper.selectList(new LambdaQueryWrapper<RoomType>()
                .in(RoomType::getId, bookableIds)
                .eq(RoomType::getStatus, 1));
        if (roomTypes.isEmpty()) {
            return List.of();
        }

        Map<Long, RoomPriceStat> stats = loadPriceStats(
                roomTypes.stream().map(RoomType::getId).toList(), checkIn, checkOut);

        return roomTypes.stream()
                .filter(roomType -> roomType.getMaxGuests() * roomCount >= guests)
                .map(roomType -> toSearchItem(roomType, stats.get(roomType.getId()), (int) nights, roomCount,
                        freeRoomsByType.getOrDefault(roomType.getId(), 0)))
                .sorted(Comparator.comparing(RoomTypeSearchItemVO::minPrice))
                .toList();
    }

    /**
     * 房型详情，仅上架房型可见。
     */
    public PublicRoomTypeVO detail(Long id) {
        RoomType roomType = roomTypeMapper.selectById(id);
        if (roomType == null || !Objects.equals(roomType.getStatus(), 1)) {
            throw new BizException(ErrorCode.NOT_FOUND, "房型不存在或已下架");
        }
        LocalDate today = LocalDate.now();
        RoomPriceStat stat = loadPriceStats(List.of(id), today, today.plusDays(PRICE_WINDOW_DAYS)).get(id);
        return toVO(roomType, stat);
    }

    /** 批量查询指定房型在时间区间内的价格统计，返回 roomTypeId -> 统计结果 */
    private Map<Long, RoomPriceStat> loadPriceStats(List<Long> roomTypeIds, LocalDate from, LocalDate toExclusive) {
        if (roomTypeIds.isEmpty()) {
            return Map.of();
        }
        return calendarMapper.selectPriceStats(roomTypeIds, from, toExclusive).stream()
                .collect(Collectors.toMap(RoomPriceStat::getRoomTypeId, Function.identity()));
    }

    /** 将房型实体与价格统计转换为房客端视图 */
    private PublicRoomTypeVO toVO(RoomType roomType, RoomPriceStat stat) {
        // 尚未生成日历时回退到基准价，避免新房型在列表中显示空价格
        BigDecimal minPrice = stat == null ? roomType.getBasePrice() : stat.getMinPrice();
        BigDecimal maxPrice = stat == null ? roomType.getBasePrice() : stat.getMaxPrice();
        return new PublicRoomTypeVO(
                roomType.getId(),
                roomType.getName(),
                roomType.getDescription(),
                roomType.getCoverImage(),
                roomType.getMaxGuests(),
                roomType.getBedType(),
                roomType.getArea(),
                minPrice,
                maxPrice
        );
    }

    /**
     * 剩余间数同时受每晚库存和整段空闲房间数限制，取两者较小值，避免展示大于实际可订量。
     */
    private RoomTypeSearchItemVO toSearchItem(RoomType roomType, RoomPriceStat stat, int nights, int roomCount,
                                              int continuousRooms) {
        if (stat == null) {
            BigDecimal total = roomType.getBasePrice().multiply(BigDecimal.valueOf((long) nights * roomCount));
            return new RoomTypeSearchItemVO(
                    roomType.getId(), roomType.getName(), roomType.getDescription(), roomType.getCoverImage(),
                    roomType.getMaxGuests(), roomType.getBedType(), roomType.getArea(),
                    roomType.getBasePrice(), roomType.getBasePrice(), nights, continuousRooms, total);
        }
        BigDecimal total = stat.getSumPrice().multiply(BigDecimal.valueOf(roomCount));
        int remaining = Math.min(stat.getMinStock(), continuousRooms);
        return new RoomTypeSearchItemVO(
                roomType.getId(), roomType.getName(), roomType.getDescription(), roomType.getCoverImage(),
                roomType.getMaxGuests(), roomType.getBedType(), roomType.getArea(),
                stat.getMinPrice(), stat.getMaxPrice(), nights, remaining, total);
    }
}

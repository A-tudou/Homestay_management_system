package com.homestay.roomtype.dto;

import java.math.BigDecimal;

/**
 * 房客端房型展示：价格区间取未来一段时间的日历价，未配置日历时回落到基准价。
 */
public record PublicRoomTypeVO(
        Long id,
        String name,
        String description,
        String coverImage,
        Integer maxGuests,
        String bedType,
        Integer area,
        BigDecimal minPrice,
        BigDecimal maxPrice
) {
}

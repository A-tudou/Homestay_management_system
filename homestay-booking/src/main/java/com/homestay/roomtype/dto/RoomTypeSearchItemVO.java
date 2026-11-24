package com.homestay.roomtype.dto;

import java.math.BigDecimal;

/**
 * 按日期搜索可订房型的结果项，价格均基于所选入住区间计算。
 */
public record RoomTypeSearchItemVO(
        Long id,
        String name,
        String description,
        String coverImage,
        Integer maxGuests,
        String bedType,
        Integer area,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        int nights,
        int remaining,
        BigDecimal totalAmount
) {
}

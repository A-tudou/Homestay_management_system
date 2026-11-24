package com.homestay.roomtype.dto;

import com.homestay.roomtype.entity.RoomType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 房型对外视图，房东管理端与房客端共用。
 */
public record RoomTypeVO(
        /** 房型 ID */
        Long id,
        /** 房型名称 */
        String name,
        /** 房型描述 */
        String description,
        /** 基准价，单位元/晚 */
        BigDecimal basePrice,
        /** 最大入住人数 */
        Integer maxGuests,
        /** 床型描述 */
        String bedType,
        /** 面积，单位平方米 */
        Integer area,
        /** 封面图地址 */
        String coverImage,
        /** 状态：1 上架 / 0 下架 */
        Integer status,
        /** 创建时间 */
        LocalDateTime createdAt
) {

    /** 由房型实体转换为对外视图 */
    public static RoomTypeVO from(RoomType roomType) {
        return new RoomTypeVO(
                roomType.getId(),
                roomType.getName(),
                roomType.getDescription(),
                roomType.getBasePrice(),
                roomType.getMaxGuests(),
                roomType.getBedType(),
                roomType.getArea(),
                roomType.getCoverImage(),
                roomType.getStatus(),
                roomType.getCreatedAt()
        );
    }
}

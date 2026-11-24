package com.homestay.room.dto;

import java.time.LocalDateTime;

/**
 * 房间对外视图，额外带上所属房型名称，便于列表展示。
 */
public record RoomVO(
        /** 房间 ID */
        Long id,
        /** 所属房型 ID */
        Long roomTypeId,
        /** 所属房型名称 */
        String roomTypeName,
        /** 房间号 */
        String roomNo,
        /** 楼层 */
        Integer floor,
        /** 状态：1 可用 / 0 维修中 */
        Integer status,
        /** 创建时间 */
        LocalDateTime createdAt
) {
}

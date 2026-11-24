package com.homestay.room.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 新增 / 修改房间的请求参数，房间号不可重复。
 */
public record RoomRequest(
        /** 所属房型 ID */
        @NotNull(message = "所属房型不能为空")
        Long roomTypeId,

        /** 房间号：字母、数字或短横线，最长 20 位，全局唯一 */
        @NotBlank(message = "房间号不能为空")
        @Pattern(regexp = "^[A-Za-z0-9-]{1,20}$", message = "房间号只能是字母、数字或短横线，最长 20 位")
        String roomNo,

        /** 楼层，允许 -5 到 200（含地下层） */
        @Min(value = -5, message = "楼层不正确")
        @Max(value = 200, message = "楼层不正确")
        Integer floor
) {
}

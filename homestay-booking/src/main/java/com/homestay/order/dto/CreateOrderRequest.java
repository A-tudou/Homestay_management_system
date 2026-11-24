package com.homestay.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 创建订单请求。入住区间为半开区间 [checkInDate, checkOutDate)，至少 1 晚、单次最多 30 晚。
 */
public record CreateOrderRequest(
        /** 预订的房型 ID */
        @NotNull(message = "房型不能为空")
        Long roomTypeId,

        /** 入住日期（含当天） */
        @NotNull(message = "入住日期不能为空")
        LocalDate checkInDate,

        /** 离店日期（不含当天），必须晚于入住日期 */
        @NotNull(message = "离店日期不能为空")
        LocalDate checkOutDate,

        /** 预订房间数，取值 1-10 */
        @NotNull(message = "房间数不能为空")
        @Min(value = 1, message = "至少预订 1 间")
        @Max(value = 10, message = "单次最多预订 10 间")
        Integer roomCount,

        /** 联系人姓名，最多 50 个字符 */
        @NotBlank(message = "联系人不能为空")
        @Size(max = 50, message = "联系人不能超过 50 个字符")
        String contactName,

        /** 联系人手机号，需为 11 位大陆手机号 */
        @NotBlank(message = "联系电话不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String contactPhone,

        /** 下单备注，最多 255 个字符 */
        @Size(max = 255, message = "备注不能超过 255 个字符")
        String remark
) {
}

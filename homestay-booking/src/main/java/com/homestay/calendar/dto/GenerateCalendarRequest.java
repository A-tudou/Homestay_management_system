package com.homestay.calendar.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * 批量生成房型日历的区间，起止日期均为闭区间（含结束当天），最长 366 天。
 * 价格为房型基准价、库存为当前可用房间数，已存在的日期会跳过。
 */
public record GenerateCalendarRequest(
        /** 开始日期，不能早于今天 */
        @NotNull(message = "开始日期不能为空")
        LocalDate startDate,

        /** 结束日期（含当天） */
        @NotNull(message = "结束日期不能为空")
        LocalDate endDate
) {
}

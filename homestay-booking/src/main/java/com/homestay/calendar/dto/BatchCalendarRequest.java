package com.homestay.calendar.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 区间批量设置：常用于节假日统一涨价或统一关房。
 */
public record BatchCalendarRequest(
        /** 开始日期（含当天） */
        @NotNull(message = "开始日期不能为空")
        LocalDate fromDate,

        /** 结束日期（含当天） */
        @NotNull(message = "结束日期不能为空")
        LocalDate toDate,

        /** 统一设置的单价，单位元，必须大于 0；为空表示不修改 */
        @DecimalMin(value = "0.01", message = "价格必须大于 0")
        BigDecimal price,

        /** 统一设置的库存，取值 0-999；为空表示不修改 */
        @Min(value = 0, message = "库存不能为负数")
        @Max(value = 999, message = "库存数值不合理")
        Integer stock,

        /** 统一设置的开关状态：1 开放 / 0 关闭；为空表示不修改 */
        @Min(value = 0, message = "状态只能是 0 或 1")
        @Max(value = 1, message = "状态只能是 0 或 1")
        Integer status
) {
}

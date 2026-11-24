package com.homestay.calendar.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

/**
 * 单日日历修改：字段为空表示不修改。
 */
public record CalendarUpdateRequest(
        /** 该晚单价，单位元，必须大于 0；为空表示不修改 */
        @DecimalMin(value = "0.01", message = "价格必须大于 0")
        BigDecimal price,

        /** 剩余可售间数，取值 0-999；为空表示不修改 */
        @Min(value = 0, message = "库存不能为负数")
        @Max(value = 999, message = "库存数值不合理")
        Integer stock,

        /** 状态：1 开放 / 0 关闭；为空表示不修改 */
        @Min(value = 0, message = "状态只能是 0 或 1")
        @Max(value = 1, message = "状态只能是 0 或 1")
        Integer status
) {
}

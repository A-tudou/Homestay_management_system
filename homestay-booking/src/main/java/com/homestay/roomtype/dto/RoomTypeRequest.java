package com.homestay.roomtype.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 新增 / 修改房型的请求参数，名称不可与已有房型重复。
 */
public record RoomTypeRequest(
        /** 房型名称，最多 50 个字符 */
        @NotBlank(message = "房型名称不能为空")
        @Size(max = 50, message = "房型名称不能超过 50 个字符")
        String name,

        /** 房型描述，最多 500 个字符 */
        @Size(max = 500, message = "描述不能超过 500 个字符")
        String description,

        /** 基准价，单位元/晚，必须大于 0 */
        @NotNull(message = "基准价不能为空")
        @DecimalMin(value = "0.01", message = "基准价必须大于 0")
        BigDecimal basePrice,

        /** 最大入住人数，取值 1-20 */
        @NotNull(message = "最大入住人数不能为空")
        @Min(value = 1, message = "最大入住人数至少为 1")
        @Max(value = 20, message = "最大入住人数不能超过 20")
        Integer maxGuests,

        /** 床型描述，如“1.8m 大床”，最多 30 个字符 */
        @Size(max = 30, message = "床型描述不能超过 30 个字符")
        String bedType,

        /** 面积，单位平方米，取值 1-2000 */
        @Min(value = 1, message = "面积必须大于 0")
        @Max(value = 2000, message = "面积数值不合理")
        Integer area,

        /** 封面图地址，最多 255 个字符 */
        @Size(max = 255, message = "封面图地址过长")
        String coverImage
) {
}

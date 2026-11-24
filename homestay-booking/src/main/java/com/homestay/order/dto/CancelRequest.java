package com.homestay.order.dto;

import jakarta.validation.constraints.Size;

/**
 * 取消 / 拒绝订单时的原因，可选。
 */
public record CancelRequest(
        /** 取消 / 拒绝原因，可为空，最多 255 个字符 */
        @Size(max = 255, message = "原因不能超过 255 个字符")
        String reason
) {
}

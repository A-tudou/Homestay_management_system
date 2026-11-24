package com.homestay.order.dto;

import java.util.List;

/**
 * 订单详情：订单主体信息 + 按时间正序的状态流转记录。
 */
public record OrderDetailVO(
        /** 订单信息 */
        OrderVO order,
        /** 状态流转日志列表 */
        List<OrderLogVO> logs
) {
}

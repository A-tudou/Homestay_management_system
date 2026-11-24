package com.homestay.order.dto;

import com.homestay.order.entity.OrderLog;
import com.homestay.order.enums.OrderStatus;

import java.time.LocalDateTime;

/**
 * 订单状态流转记录视图，状态均为中文文案与编码成对返回。
 */
public record OrderLogVO(
        /** 变更前状态编码，创建订单时为空 */
        String fromStatus,
        /** 变更前状态中文文案，创建订单时为空 */
        String fromStatusText,
        /** 变更后状态编码 */
        String toStatus,
        /** 变更后状态中文文案 */
        String toStatusText,
        /** 操作人用户 ID */
        Long operatorId,
        /** 备注，如取消/拒绝原因 */
        String remark,
        /** 记录时间 */
        LocalDateTime createdAt
) {

    /** 由订单状态日志实体构建视图对象 */
    public static OrderLogVO from(OrderLog log) {
        return new OrderLogVO(
                log.getFromStatus(),
                log.getFromStatus() == null ? null : OrderStatus.text(log.getFromStatus()),
                log.getToStatus(),
                OrderStatus.text(log.getToStatus()),
                log.getOperatorId(),
                log.getRemark(),
                log.getCreatedAt()
        );
    }
}

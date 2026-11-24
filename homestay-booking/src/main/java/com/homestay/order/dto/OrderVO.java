package com.homestay.order.dto;

import com.homestay.order.entity.Order;
import com.homestay.order.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单列表 / 详情中的订单视图，附带房型名称、房间号与状态中文文案。
 */
public record OrderVO(
        /** 订单 ID */
        Long id,
        /** 订单号 */
        String orderNo,
        /** 房型 ID */
        Long roomTypeId,
        /** 房型名称 */
        String roomTypeName,
        /** 已分配的物理房间号列表 */
        List<String> roomNos,
        /** 入住日期（含当天） */
        LocalDate checkInDate,
        /** 离店日期（不含当天） */
        LocalDate checkOutDate,
        /** 入住晚数 */
        Integer nights,
        /** 预订房间数 */
        Integer roomCount,
        /** 订单总金额，单位元 */
        BigDecimal totalAmount,
        /** 订单状态编码，见 OrderStatus */
        String status,
        /** 订单状态中文文案 */
        String statusText,
        /** 联系人姓名 */
        String contactName,
        /** 联系人手机号 */
        String contactPhone,
        /** 下单备注 */
        String remark,
        /** 取消原因，未取消时为空 */
        String cancelReason,
        /** 下单时间 */
        LocalDateTime createdAt
) {

    /** 由订单实体构建订单视图 */
    public static OrderVO from(Order order, String roomTypeName, List<String> roomNos) {
        return new OrderVO(
                order.getId(),
                order.getOrderNo(),
                order.getRoomTypeId(),
                roomTypeName,
                roomNos,
                order.getCheckInDate(),
                order.getCheckOutDate(),
                order.getNights(),
                order.getRoomCount(),
                order.getTotalAmount(),
                order.getStatus(),
                OrderStatus.text(order.getStatus()),
                order.getContactName(),
                order.getContactPhone(),
                order.getRemark(),
                order.getCancelReason(),
                order.getCreatedAt()
        );
    }
}

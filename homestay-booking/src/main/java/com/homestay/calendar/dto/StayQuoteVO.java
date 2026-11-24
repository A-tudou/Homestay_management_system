package com.homestay.calendar.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 整段入住区间的报价与连续可订房间数。
 * roomsAvailable 同时取「每晚最小余量」与「整段空闲的物理房间数」的较小值，
 * 与下单时的分配逻辑保持一致，避免出现日历显示有余量却订不上的情况。
 */
public record StayQuoteVO(
        /** 入住日期（含当天），区间为 [checkIn, checkOut) */
        LocalDate checkIn,
        /** 离店日期（不含当天） */
        LocalDate checkOut,
        /** 入住晚数 */
        int nights,
        /** 整段区间可连续入住的房间数（每晚余量与整段空闲物理房间数取小） */
        int roomsAvailable,
        /** 每间房整段入住的价格合计，单位元 */
        BigDecimal pricePerRoom,
        /** 订单总金额 = pricePerRoom × 预订间数，单位元 */
        BigDecimal totalAmount,
        /** 所选间数是否可下单 */
        boolean available,
        /** 不可订时的原因说明，可订时为空字符串 */
        String reason
) {
}

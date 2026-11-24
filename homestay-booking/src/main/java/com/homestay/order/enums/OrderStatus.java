package com.homestay.order.enums;

import java.util.Map;
import java.util.Set;

/**
 * 订单状态及允许的流转方向。
 */
public final class OrderStatus {

    /** 待确认：下单后的初始状态 */
    public static final String PENDING = "PENDING";
    /** 已确认：房东确认接单 */
    public static final String CONFIRMED = "CONFIRMED";
    /** 已入住：房东办理入住 */
    public static final String CHECKED_IN = "CHECKED_IN";
    /** 已完成：离店后结束订单 */
    public static final String COMPLETED = "COMPLETED";
    /** 已取消：房客取消或房东拒绝，库存已归还 */
    public static final String CANCELLED = "CANCELLED";

    /** 允许的状态流转表，key 为当前状态，value 为可流转到的目标状态集合 */
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            PENDING, Set.of(CONFIRMED, CANCELLED),
            CONFIRMED, Set.of(CHECKED_IN, CANCELLED),
            CHECKED_IN, Set.of(COMPLETED),
            COMPLETED, Set.of(),
            CANCELLED, Set.of()
    );

    /** 状态对应的中文文案，用于前端展示 */
    private static final Map<String, String> TEXTS = Map.of(
            PENDING, "待确认",
            CONFIRMED, "已确认",
            CHECKED_IN, "已入住",
            COMPLETED, "已完成",
            CANCELLED, "已取消"
    );

    /** 私有构造，防止实例化工具类 */
    private OrderStatus() {
    }

    /** 判断订单状态是否允许从 from 流转到 to */
    public static boolean canTransition(String from, String to) {
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    /** 获取订单状态对应的中文文案，未知状态原样返回 */
    public static String text(String status) {
        return TEXTS.getOrDefault(status, status);
    }
}

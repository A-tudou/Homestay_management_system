package com.homestay.calendar.dto;

import com.homestay.calendar.entity.RoomCalendar;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 房东端日历视图，展示单日价格、库存与开关状态。
 */
public record CalendarVO(
        /** 日历记录 ID */
        Long id,
        /** 房型 ID */
        Long roomTypeId,
        /** 日期（该晚） */
        LocalDate stayDate,
        /** 该晚单价，单位元 */
        BigDecimal price,
        /** 剩余可售间数 */
        Integer stock,
        /** 状态：1 开放 / 0 关闭 */
        Integer status
) {

    /** 由日历实体构建房东端日历视图 */
    public static CalendarVO from(RoomCalendar calendar) {
        return new CalendarVO(
                calendar.getId(),
                calendar.getRoomTypeId(),
                calendar.getStayDate(),
                calendar.getPrice(),
                calendar.getStock(),
                calendar.getStatus()
        );
    }
}

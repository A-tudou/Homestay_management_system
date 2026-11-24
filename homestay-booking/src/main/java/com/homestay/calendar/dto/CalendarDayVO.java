package com.homestay.calendar.dto;

import com.homestay.calendar.entity.RoomCalendar;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 面向房客的日历视图：不暴露关闭原因等内部字段，只给出价格和是否可订。
 */
public record CalendarDayVO(
        /** 日期（该晚） */
        LocalDate date,
        /** 该晚单价，单位元 */
        BigDecimal price,
        /** 剩余可订间数，最小为 0 */
        int remaining,
        /** 当晚是否可订：日历开放且库存大于 0 */
        boolean available
) {

    /** 由日历实体构建房客端日历视图 */
    public static CalendarDayVO from(RoomCalendar calendar) {
        boolean available = Integer.valueOf(1).equals(calendar.getStatus()) && calendar.getStock() > 0;
        return new CalendarDayVO(calendar.getStayDate(), calendar.getPrice(),
                Math.max(calendar.getStock(), 0), available);
    }
}

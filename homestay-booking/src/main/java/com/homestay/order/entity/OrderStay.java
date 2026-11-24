package com.homestay.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;

/**
 * 订单逐晚占用记录，[checkIn, checkOut) 内每晚一行。
 * 取消订单时按这些日期逐晚把库存归还到 room_calendar。
 */
@TableName("order_stay")
public class OrderStay {

    /** 主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属订单 ID */
    private Long orderId;
    /** 占用的一晚（日期） */
    private LocalDate stayDate;

    /** 获取主键 */
    public Long getId() {
        return id;
    }

    /** 设置主键 */
    public void setId(Long id) {
        this.id = id;
    }

    /** 获取所属订单 ID */
    public Long getOrderId() {
        return orderId;
    }

    /** 设置所属订单 ID */
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    /** 获取占用的一晚日期 */
    public LocalDate getStayDate() {
        return stayDate;
    }

    /** 设置占用的一晚日期 */
    public void setStayDate(LocalDate stayDate) {
        this.stayDate = stayDate;
    }
}

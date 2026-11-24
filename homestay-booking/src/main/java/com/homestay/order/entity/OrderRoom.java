package com.homestay.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 订单与物理房间的分配关系，一单订多间时对应多行。
 */
@TableName("order_room")
public class OrderRoom {

    /** 主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属订单 ID */
    private Long orderId;
    /** 分配给该订单的物理房间 ID */
    private Long roomId;

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

    /** 获取分配给该订单的物理房间 ID */
    public Long getRoomId() {
        return roomId;
    }

    /** 设置分配给该订单的物理房间 ID */
    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }
}

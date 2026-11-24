package com.homestay.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 订单状态流转日志，每次状态变更追加一条，用于订单详情中的轨迹展示。
 */
@TableName("order_log")
public class OrderLog {

    /** 主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属订单 ID */
    private Long orderId;
    /** 变更前状态，创建订单时为空 */
    private String fromStatus;
    /** 变更后状态 */
    private String toStatus;
    /** 操作人用户 ID */
    private Long operatorId;
    /** 备注，如取消/拒绝原因 */
    private String remark;
    /** 记录时间 */
    private LocalDateTime createdAt;

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

    /** 获取变更前状态 */
    public String getFromStatus() {
        return fromStatus;
    }

    /** 设置变更前状态 */
    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    /** 获取变更后状态 */
    public String getToStatus() {
        return toStatus;
    }

    /** 设置变更后状态 */
    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    /** 获取操作人用户 ID */
    public Long getOperatorId() {
        return operatorId;
    }

    /** 设置操作人用户 ID */
    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    /** 获取备注 */
    public String getRemark() {
        return remark;
    }

    /** 设置备注 */
    public void setRemark(String remark) {
        this.remark = remark;
    }

    /** 获取记录时间 */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** 设置记录时间 */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

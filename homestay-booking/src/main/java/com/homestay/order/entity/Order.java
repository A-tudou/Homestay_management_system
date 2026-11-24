package com.homestay.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单主表，一次预订对应一条记录。
 * 入住区间为半开区间 [checkInDate, checkOutDate)，即实际占用 checkInDate 到 checkOutDate 前一天的每一晚。
 * status 取值见 OrderStatus，流转：PENDING → CONFIRMED → CHECKED_IN → COMPLETED，PENDING/CONFIRMED 可转 CANCELLED。
 */
@TableName("orders")
public class Order {

    /** 主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 订单号，业务唯一标识 */
    private String orderNo;
    /** 下单房客的用户 ID */
    private Long userId;
    /** 预订的房型 ID */
    private Long roomTypeId;
    /** 入住日期（含当天） */
    private LocalDate checkInDate;
    /** 离店日期（不含当天） */
    private LocalDate checkOutDate;
    /** 入住晚数，等于 checkOutDate - checkInDate */
    private Integer nights;
    /** 预订房间数 */
    private Integer roomCount;
    /** 订单总金额，单位元 */
    private BigDecimal totalAmount;
    /** 订单状态，取值见 OrderStatus */
    private String status;
    /** 联系人姓名 */
    private String contactName;
    /** 联系人手机号 */
    private String contactPhone;
    /** 下单备注 */
    private String remark;
    /** 取消原因，仅取消时填写 */
    private String cancelReason;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 最后更新时间 */
    private LocalDateTime updatedAt;

    /** 获取主键 */
    public Long getId() {
        return id;
    }

    /** 设置主键 */
    public void setId(Long id) {
        this.id = id;
    }

    /** 获取订单号 */
    public String getOrderNo() {
        return orderNo;
    }

    /** 设置订单号 */
    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    /** 获取下单房客的用户 ID */
    public Long getUserId() {
        return userId;
    }

    /** 设置下单房客的用户 ID */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /** 获取预订的房型 ID */
    public Long getRoomTypeId() {
        return roomTypeId;
    }

    /** 设置预订的房型 ID */
    public void setRoomTypeId(Long roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    /** 获取入住日期 */
    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    /** 设置入住日期 */
    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    /** 获取离店日期 */
    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    /** 设置离店日期 */
    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    /** 获取入住晚数 */
    public Integer getNights() {
        return nights;
    }

    /** 设置入住晚数 */
    public void setNights(Integer nights) {
        this.nights = nights;
    }

    /** 获取预订房间数 */
    public Integer getRoomCount() {
        return roomCount;
    }

    /** 设置预订房间数 */
    public void setRoomCount(Integer roomCount) {
        this.roomCount = roomCount;
    }

    /** 获取订单总金额 */
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    /** 设置订单总金额 */
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    /** 获取订单状态 */
    public String getStatus() {
        return status;
    }

    /** 设置订单状态 */
    public void setStatus(String status) {
        this.status = status;
    }

    /** 获取联系人姓名 */
    public String getContactName() {
        return contactName;
    }

    /** 设置联系人姓名 */
    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    /** 获取联系人手机号 */
    public String getContactPhone() {
        return contactPhone;
    }

    /** 设置联系人手机号 */
    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    /** 获取下单备注 */
    public String getRemark() {
        return remark;
    }

    /** 设置下单备注 */
    public void setRemark(String remark) {
        this.remark = remark;
    }

    /** 获取取消原因 */
    public String getCancelReason() {
        return cancelReason;
    }

    /** 设置取消原因 */
    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    /** 获取创建时间 */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** 设置创建时间 */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /** 获取最后更新时间 */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /** 设置最后更新时间 */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

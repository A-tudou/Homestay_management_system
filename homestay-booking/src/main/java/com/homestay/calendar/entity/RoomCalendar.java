package com.homestay.calendar.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 房型每日库存，按“房型 × 每晚”一行维护。
 * 下单通过条件 UPDATE 原子扣减 stock，取消时按 order_stay 逐晚归还。
 * status 1 开放 / 0 关闭（关闭后该晚不可预订）。
 */
@TableName("room_calendar")
public class RoomCalendar {

    /** 主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 房型 ID */
    private Long roomTypeId;
    /** 日期（该晚），与 roomTypeId 组成唯一键 */
    private LocalDate stayDate;
    /** 该晚单价，单位元 */
    private BigDecimal price;
    /** 剩余可售间数，扣减时不允许小于 0 */
    private Integer stock;
    /** 状态：1 开放 / 0 关闭 */
    private Integer status;
    /** 乐观锁版本号，配合条件更新避免并发超卖 */
    private Integer version;
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

    /** 获取房型 ID */
    public Long getRoomTypeId() {
        return roomTypeId;
    }

    /** 设置房型 ID */
    public void setRoomTypeId(Long roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    /** 获取日期 */
    public LocalDate getStayDate() {
        return stayDate;
    }

    /** 设置日期 */
    public void setStayDate(LocalDate stayDate) {
        this.stayDate = stayDate;
    }

    /** 获取该晚单价 */
    public BigDecimal getPrice() {
        return price;
    }

    /** 设置该晚单价 */
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    /** 获取剩余可售间数 */
    public Integer getStock() {
        return stock;
    }

    /** 设置剩余可售间数 */
    public void setStock(Integer stock) {
        this.stock = stock;
    }

    /** 获取状态 */
    public Integer getStatus() {
        return status;
    }

    /** 设置状态 */
    public void setStatus(Integer status) {
        this.status = status;
    }

    /** 获取乐观锁版本号 */
    public Integer getVersion() {
        return version;
    }

    /** 设置乐观锁版本号 */
    public void setVersion(Integer version) {
        this.version = version;
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

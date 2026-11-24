package com.homestay.room.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 物理房间，归属于某个房型；同一房型下可有多间。
 * status 1 可用 / 0 维修中（维修中的房间不参与下单分配）。
 */
@TableName("room")
public class Room {

    /** 主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属房型 ID */
    private Long roomTypeId;
    /** 房间号，全局唯一 */
    private String roomNo;
    /** 所在楼层 */
    private Integer floor;
    /** 状态：1 可用 / 0 维修中 */
    private Integer status;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 最后更新时间 */
    private LocalDateTime updatedAt;

    /** 获取房间主键 */
    public Long getId() {
        return id;
    }

    /** 设置房间主键 */
    public void setId(Long id) {
        this.id = id;
    }

    /** 获取所属房型 ID */
    public Long getRoomTypeId() {
        return roomTypeId;
    }

    /** 设置所属房型 ID */
    public void setRoomTypeId(Long roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    /** 获取房间号 */
    public String getRoomNo() {
        return roomNo;
    }

    /** 设置房间号 */
    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    /** 获取所在楼层 */
    public Integer getFloor() {
        return floor;
    }

    /** 设置所在楼层 */
    public void setFloor(Integer floor) {
        this.floor = floor;
    }

    /** 获取房间状态 */
    public Integer getStatus() {
        return status;
    }

    /** 设置房间状态 */
    public void setStatus(Integer status) {
        this.status = status;
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

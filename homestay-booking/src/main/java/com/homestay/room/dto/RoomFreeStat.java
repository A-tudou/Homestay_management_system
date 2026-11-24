package com.homestay.room.dto;

/**
 * 某房型在整段入住区间内都空闲的物理房间数，用于判断能否连续入住。
 */
public class RoomFreeStat {

    private Long roomTypeId;
    private Integer freeRooms;

    /** 获取房型 ID */
    public Long getRoomTypeId() {
        return roomTypeId;
    }

    /** 设置房型 ID */
    public void setRoomTypeId(Long roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    /** 获取整段区间空闲的房间数 */
    public Integer getFreeRooms() {
        return freeRooms;
    }

    /** 设置整段区间空闲的房间数 */
    public void setFreeRooms(Integer freeRooms) {
        this.freeRooms = freeRooms;
    }
}

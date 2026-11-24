package com.homestay.room.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homestay.room.dto.RoomFreeStat;
import com.homestay.room.entity.Room;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 物理房间 Mapper：除基础 CRUD 外，提供下单时的行锁选房与整段区间空闲房间统计。
 */
public interface RoomMapper extends BaseMapper<Room> {

    /**
     * 加行锁读取该房型可用房间，避免并发下单时把同一间房分配给两个订单。
     */
    @Select("SELECT * FROM room WHERE room_type_id = #{roomTypeId} AND status = 1 ORDER BY room_no FOR UPDATE")
    List<Room> selectBookableRoomsForUpdate(@Param("roomTypeId") Long roomTypeId);

    /**
     * 只读查询该房型可用房间的 id，供房客侧核算连续可订间数使用。
     */
    @Select("SELECT id FROM room WHERE room_type_id = #{roomTypeId} AND status = 1")
    List<Long> selectBookableRoomIds(@Param("roomTypeId") Long roomTypeId);

    /**
     * 统计各房型在整段入住区间内都没有被有效订单占用的房间数，少于 roomCount 的房型直接过滤。
     * 与下单时的房间分配规则一致，避免搜索结果显示可订但实际凑不出连续房间。
     */
    @Select("SELECT r.room_type_id AS roomTypeId, COUNT(*) AS freeRooms FROM room r " +
            "WHERE r.status = 1 AND NOT EXISTS (" +
            "SELECT 1 FROM order_room om JOIN orders o ON o.id = om.order_id " +
            "WHERE om.room_id = r.id " +
            "AND o.status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN', 'COMPLETED') " +
            "AND o.check_in_date < #{checkOut} AND o.check_out_date > #{checkIn}) " +
            "GROUP BY r.room_type_id HAVING COUNT(*) >= #{roomCount}")
    List<RoomFreeStat> selectFreeRoomStats(@Param("checkIn") LocalDate checkIn,
                                           @Param("checkOut") LocalDate checkOut,
                                           @Param("roomCount") int roomCount);
}

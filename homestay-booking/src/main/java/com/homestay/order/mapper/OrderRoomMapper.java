package com.homestay.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homestay.order.entity.OrderRoom;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

public interface OrderRoomMapper extends BaseMapper<OrderRoom> {

    /**
     * 查询与该日期区间重叠的有效订单已占用的房间，用于排除不可再分配的房间。
     * 日期按 [checkIn, checkOut) 半开区间比较，前一单离店当天可与后一单入住当天衔接。
     */
    @Select("SELECT DISTINCT om.room_id FROM order_room om " +
            "JOIN orders o ON o.id = om.order_id " +
            "WHERE o.room_type_id = #{roomTypeId} " +
            "AND o.status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN', 'COMPLETED') " +
            "AND o.check_in_date < #{checkOut} AND o.check_out_date > #{checkIn}")
    List<Long> selectOccupiedRoomIds(@Param("roomTypeId") Long roomTypeId,
                                     @Param("checkIn") LocalDate checkIn,
                                     @Param("checkOut") LocalDate checkOut);
}

package com.homestay.calendar.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homestay.calendar.dto.RoomPriceStat;
import com.homestay.calendar.entity.RoomCalendar;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface RoomCalendarMapper extends BaseMapper<RoomCalendar> {

    /**
     * 批量生成日历，已存在的日期直接跳过（INSERT IGNORE），不会覆盖房东手工改过的价格和库存。
     */
    @Insert("<script>" +
            "INSERT IGNORE INTO room_calendar (room_type_id, stay_date, price, stock, status) VALUES " +
            "<foreach collection='dates' item='d' separator=','>" +
            "(#{roomTypeId}, #{d}, #{price}, #{stock}, 1)" +
            "</foreach>" +
            "</script>")
    int batchInsertIgnore(@Param("roomTypeId") Long roomTypeId,
                          @Param("dates") List<LocalDate> dates,
                          @Param("price") BigDecimal price,
                          @Param("stock") Integer stock);

    /**
     * 原子扣减库存：只有该日开放且库存足够时才会扣成功，靠影响行数判断是否抢到。
     */
    @Update("UPDATE room_calendar SET stock = stock - #{count}, version = version + 1 " +
            "WHERE room_type_id = #{roomTypeId} AND stay_date = #{stayDate} AND status = 1 AND stock >= #{count}")
    int decreaseStock(@Param("roomTypeId") Long roomTypeId,
                      @Param("stayDate") LocalDate stayDate,
                      @Param("count") int count);

    /**
     * 取消订单时归还库存。
     */
    @Update("UPDATE room_calendar SET stock = stock + #{count}, version = version + 1 " +
            "WHERE room_type_id = #{roomTypeId} AND stay_date = #{stayDate}")
    int increaseStock(@Param("roomTypeId") Long roomTypeId,
                      @Param("stayDate") LocalDate stayDate,
                      @Param("count") int count);

    /**
     * 搜索整段区间都可订的房型：区间内每一天都必须开放且库存足够，靠 COUNT 与总晚数相等来保证一天不缺。
     */
    @Select("SELECT room_type_id FROM room_calendar " +
            "WHERE stay_date >= #{checkIn} AND stay_date < #{checkOut} AND status = 1 AND stock >= #{roomCount} " +
            "GROUP BY room_type_id HAVING COUNT(*) = #{nights}")
    List<Long> selectAvailableRoomTypeIds(@Param("checkIn") LocalDate checkIn,
                                          @Param("checkOut") LocalDate checkOut,
                                          @Param("roomCount") int roomCount,
                                          @Param("nights") long nights);

    /**
     * 按房型聚合区间内的最低价、最高价、总价和最少剩余库存。
     */
    @Select("<script>" +
            "SELECT room_type_id AS roomTypeId, MIN(price) AS minPrice, MAX(price) AS maxPrice, " +
            "SUM(price) AS sumPrice, MIN(stock) AS minStock, COUNT(*) AS nightCount " +
            "FROM room_calendar " +
            "WHERE status = 1 AND stay_date &gt;= #{from} AND stay_date &lt; #{toExclusive} " +
            "AND room_type_id IN <foreach collection='roomTypeIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "GROUP BY room_type_id" +
            "</script>")
    List<RoomPriceStat> selectPriceStats(@Param("roomTypeIds") List<Long> roomTypeIds,
                                         @Param("from") LocalDate from,
                                         @Param("toExclusive") LocalDate toExclusive);
}

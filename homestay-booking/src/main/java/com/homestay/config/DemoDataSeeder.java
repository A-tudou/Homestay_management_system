package com.homestay.config;

import com.homestay.calendar.mapper.RoomCalendarMapper;
import com.homestay.room.entity.Room;
import com.homestay.room.mapper.RoomMapper;
import com.homestay.roomtype.entity.RoomType;
import com.homestay.roomtype.mapper.RoomTypeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 演示数据：只有 app.seed.enabled=true 且房型表为空时才写入，避免覆盖真实数据。
 */
@Component
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    /** 演示日历生成的天数 */
    private static final int SEED_CALENDAR_DAYS = 90;

    private final RoomTypeMapper roomTypeMapper;
    private final RoomMapper roomMapper;
    private final RoomCalendarMapper calendarMapper;

    /** 注入房型、房间与日历数据访问对象 */
    public DemoDataSeeder(RoomTypeMapper roomTypeMapper, RoomMapper roomMapper, RoomCalendarMapper calendarMapper) {
        this.roomTypeMapper = roomTypeMapper;
        this.roomMapper = roomMapper;
        this.calendarMapper = calendarMapper;
    }

    /** 房型表为空时写入演示房型、房间与未来日历 */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (roomTypeMapper.selectCount(null) > 0) {
            log.info("已存在房型数据，跳过演示数据初始化");
            return;
        }

        RoomType mountain = createRoomType("山景大床房", "落地窗看山景，含双早",
                "728.00", 2, "1.8m大床", 28, "/images/room-mountain.jpg");
        RoomType family = createRoomType("庭院家庭房", "带独立小院，可住 4 人",
                "980.00", 4, "1.5m双床", 45, "/images/room-courtyard.jpg");

        createRoom(mountain.getId(), "201", 2);
        createRoom(mountain.getId(), "202", 2);
        createRoom(family.getId(), "301", 3);
        createRoom(family.getId(), "302", 3);

        seedCalendar(mountain.getId(), mountain.getBasePrice(), 2);
        seedCalendar(family.getId(), family.getBasePrice(), 2);

        log.info("演示数据初始化完成：2 个房型、4 个房间、{} 天日历", SEED_CALENDAR_DAYS);
    }

    /** 创建并保存演示房型，返回带主键的实体 */
    private RoomType createRoomType(String name, String description, String basePrice,
                                    int maxGuests, String bedType, int area, String coverImage) {
        RoomType roomType = new RoomType();
        roomType.setName(name);
        roomType.setDescription(description);
        roomType.setBasePrice(new BigDecimal(basePrice));
        roomType.setMaxGuests(maxGuests);
        roomType.setBedType(bedType);
        roomType.setArea(area);
        roomType.setCoverImage(coverImage);
        roomType.setStatus(1);
        roomTypeMapper.insert(roomType);
        return roomType;
    }

    /** 创建并保存指定房型下的房间 */
    private void createRoom(Long roomTypeId, String roomNo, int floor) {
        Room room = new Room();
        room.setRoomTypeId(roomTypeId);
        room.setRoomNo(roomNo);
        room.setFloor(floor);
        room.setStatus(1);
        roomMapper.insert(room);
    }

    /** 为房型批量生成未来指定天数的日历，已存在的日期跳过 */
    private void seedCalendar(Long roomTypeId, BigDecimal price, int stock) {
        LocalDate today = LocalDate.now();
        List<LocalDate> dates = today.datesUntil(today.plusDays(SEED_CALENDAR_DAYS)).toList();
        calendarMapper.batchInsertIgnore(roomTypeId, dates, price, stock);
    }
}

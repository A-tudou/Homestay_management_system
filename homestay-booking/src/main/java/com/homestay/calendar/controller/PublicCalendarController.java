package com.homestay.calendar.controller;

import com.homestay.calendar.dto.CalendarDayVO;
import com.homestay.calendar.dto.StayQuoteVO;
import com.homestay.calendar.service.CalendarService;
import com.homestay.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 房型日历查询接口（房客端），路径前缀 /api/public/room-types。
 * 无需登录，只返回上架房型的数据。
 */
@Tag(name = "房客-房型日历", description = "无需登录")
@RestController
@RequestMapping("/api/public/room-types")
public class PublicCalendarController {

    private final CalendarService calendarService;

    /** 构造注入日历服务 */
    public PublicCalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    /** from、to 均为闭区间（含当天），单次最多查询 92 天 */
    @Operation(summary = "查询房型日历与可订状态", description = "未生成日历的日期不会返回，视为不可订；单次最多查询 92 天")
    @GetMapping("/{roomTypeId}/calendar")
    public ApiResponse<List<CalendarDayVO>> calendar(
            @PathVariable Long roomTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(calendarService.publicCalendar(roomTypeId, from, to));
    }

    /** 入住区间为 [checkIn, checkOut)；roomCount 为计划预订间数，用于判断库存是否充足 */
    @Operation(summary = "按入住区间核算报价与连续可订间数",
            description = "可订间数同时考虑每晚余量与整段空闲的物理房间数，与下单时的房间分配规则一致")
    @GetMapping("/{roomTypeId}/quote")
    public ApiResponse<StayQuoteVO> quote(
            @PathVariable Long roomTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(defaultValue = "1") int roomCount) {
        return ApiResponse.ok(calendarService.quote(roomTypeId, checkIn, checkOut, roomCount));
    }
}

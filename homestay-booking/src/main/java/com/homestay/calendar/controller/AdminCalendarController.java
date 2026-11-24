package com.homestay.calendar.controller;

import com.homestay.calendar.dto.BatchCalendarRequest;
import com.homestay.calendar.dto.CalendarUpdateRequest;
import com.homestay.calendar.dto.CalendarVO;
import com.homestay.calendar.dto.GenerateCalendarRequest;
import com.homestay.calendar.service.CalendarService;
import com.homestay.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 日历库存管理接口（房东端），路径前缀 /api/admin。
 * /api/admin/** 由安全配置统一限制为 HOST 角色访问。
 */
@Tag(name = "房东-日历库存", description = "仅房东（HOST）可访问")
@RestController
@RequestMapping("/api/admin")
public class AdminCalendarController {

    private final CalendarService calendarService;

    /** 构造注入日历服务 */
    public AdminCalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    /** 批量生成房型日历 */
    @Operation(summary = "批量生成日历", description = "价格为房型基准价，库存为可用房间数，已存在的日期跳过")
    @PostMapping("/room-types/{roomTypeId}/calendar/generate")
    public ApiResponse<Integer> generate(@PathVariable Long roomTypeId,
                                         @Valid @RequestBody GenerateCalendarRequest request) {
        return ApiResponse.ok(calendarService.generate(roomTypeId, request));
    }

    /** from、to 均为闭区间（含当天），单次查询最多 366 天 */
    @Operation(summary = "查询房型日历")
    @GetMapping("/room-types/{roomTypeId}/calendar")
    public ApiResponse<List<CalendarVO>> list(
            @PathVariable Long roomTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(calendarService.list(roomTypeId, from, to));
    }

    /** 修改单日日历 */
    @Operation(summary = "修改单日价格 / 库存 / 开关房")
    @PutMapping("/calendar/{id}")
    public ApiResponse<CalendarVO> update(@PathVariable Long id,
                                          @Valid @RequestBody CalendarUpdateRequest request) {
        return ApiResponse.ok(calendarService.update(id, request));
    }

    /** fromDate、toDate 均为闭区间（含当天），只修改请求中非空的字段 */
    @Operation(summary = "区间批量改价 / 改库存 / 开关房", description = "返回受影响的日期数")
    @PutMapping("/room-types/{roomTypeId}/calendar/batch")
    public ApiResponse<Integer> batchUpdate(@PathVariable Long roomTypeId,
                                            @Valid @RequestBody BatchCalendarRequest request) {
        return ApiResponse.ok(calendarService.batchUpdate(roomTypeId, request));
    }
}

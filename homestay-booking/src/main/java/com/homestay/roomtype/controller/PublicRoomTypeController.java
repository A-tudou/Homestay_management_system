package com.homestay.roomtype.controller;

import com.homestay.common.ApiResponse;
import com.homestay.common.PageResult;
import com.homestay.roomtype.dto.PublicRoomTypeVO;
import com.homestay.roomtype.dto.RoomTypeSearchItemVO;
import com.homestay.roomtype.service.PublicRoomTypeService;
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
 * 房源浏览接口（房客端），路径前缀 /api/public/room-types。
 * 全部无需登录，且只暴露上架房型。
 */
@Tag(name = "房客-房源浏览", description = "无需登录")
@RestController
@RequestMapping("/api/public/room-types")
public class PublicRoomTypeController {

    private final PublicRoomTypeService publicRoomTypeService;

    /** 构造器注入房客端房型服务 */
    public PublicRoomTypeController(PublicRoomTypeService publicRoomTypeService) {
        this.publicRoomTypeService = publicRoomTypeService;
    }

    /** 上架房型分页列表，价格区间取未来 30 天日历价 */
    @Operation(summary = "上架房型列表", description = "价格区间取未来 30 天日历价，未配置日历时取基准价")
    @GetMapping
    public ApiResponse<PageResult<PublicRoomTypeVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer guests) {
        return ApiResponse.ok(publicRoomTypeService.page(page, size, keyword, guests));
    }

    /** 按入住区间与人数搜索可订房型 */
    @Operation(summary = "按日期与人数搜索可订房型",
            description = "guests 为总人数，roomCount 为间数；只返回整段日期每天都有足够库存、且有足够房间可连续入住的房型，单次最多 30 晚")
    @GetMapping("/search")
    public ApiResponse<List<RoomTypeSearchItemVO>> search(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(defaultValue = "1") int guests,
            @RequestParam(defaultValue = "1") int roomCount) {
        return ApiResponse.ok(publicRoomTypeService.search(checkIn, checkOut, guests, roomCount));
    }

    /** 房型详情，仅上架房型可见 */
    @Operation(summary = "房型详情", description = "仅上架房型可见；价格区间取未来 30 天日历价")
    @GetMapping("/{id}")
    public ApiResponse<PublicRoomTypeVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(publicRoomTypeService.detail(id));
    }
}

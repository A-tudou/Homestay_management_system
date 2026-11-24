package com.homestay.room.controller;

import com.homestay.common.ApiResponse;
import com.homestay.common.PageResult;
import com.homestay.common.StatusRequest;
import com.homestay.room.dto.RoomRequest;
import com.homestay.room.dto.RoomVO;
import com.homestay.room.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 房间管理接口（房东端），路径前缀 /api/admin/rooms。
 * /api/admin/** 由安全配置统一限制为 HOST 角色访问。
 */
@Tag(name = "房东-房间管理", description = "仅房东（HOST）可访问")
@RestController
@RequestMapping("/api/admin/rooms")
public class AdminRoomController {

    private final RoomService roomService;

    /** 构造器注入房间管理服务 */
    public AdminRoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    /** 新增房间 */
    @Operation(summary = "新增房间")
    @PostMapping
    public ApiResponse<RoomVO> create(@Valid @RequestBody RoomRequest request) {
        return ApiResponse.ok(roomService.create(request));
    }

    /** 修改房间 */
    @Operation(summary = "修改房间")
    @PutMapping("/{id}")
    public ApiResponse<RoomVO> update(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        return ApiResponse.ok(roomService.update(id, request));
    }

    /** 设置房间可用或维修中 */
    @Operation(summary = "设为可用 / 维修中")
    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        roomService.updateStatus(id, request.status());
        return ApiResponse.ok();
    }

    /** 删除房间 */
    @Operation(summary = "删除房间")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roomService.delete(id);
        return ApiResponse.ok();
    }

    /** 查询房间详情 */
    @Operation(summary = "房间详情")
    @GetMapping("/{id}")
    public ApiResponse<RoomVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(roomService.get(id));
    }

    /** roomTypeId、status 均为可选过滤条件；page 从 1 开始 */
    @Operation(summary = "房间分页列表")
    @GetMapping
    public ApiResponse<PageResult<RoomVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.ok(roomService.page(page, size, roomTypeId, status));
    }
}

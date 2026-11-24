package com.homestay.roomtype.controller;

import com.homestay.common.ApiResponse;
import com.homestay.common.PageResult;
import com.homestay.common.StatusRequest;
import com.homestay.roomtype.dto.RoomTypeRequest;
import com.homestay.roomtype.dto.RoomTypeVO;
import com.homestay.roomtype.service.RoomTypeService;
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
 * 房型管理接口（房东端），路径前缀 /api/admin/room-types。
 * /api/admin/** 由安全配置统一限制为 HOST 角色访问。
 */
@Tag(name = "房东-房型管理", description = "仅房东（HOST）可访问")
@RestController
@RequestMapping("/api/admin/room-types")
public class AdminRoomTypeController {

    private final RoomTypeService roomTypeService;

    /** 构造器注入房型管理服务 */
    public AdminRoomTypeController(RoomTypeService roomTypeService) {
        this.roomTypeService = roomTypeService;
    }

    /** 新增房型 */
    @Operation(summary = "新增房型")
    @PostMapping
    public ApiResponse<RoomTypeVO> create(@Valid @RequestBody RoomTypeRequest request) {
        return ApiResponse.ok(roomTypeService.create(request));
    }

    /** 修改房型 */
    @Operation(summary = "修改房型")
    @PutMapping("/{id}")
    public ApiResponse<RoomTypeVO> update(@PathVariable Long id, @Valid @RequestBody RoomTypeRequest request) {
        return ApiResponse.ok(roomTypeService.update(id, request));
    }

    /** 设置房型上架或下架 */
    @Operation(summary = "上架 / 下架房型")
    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        roomTypeService.updateStatus(id, request.status());
        return ApiResponse.ok();
    }

    /** 删除房型，房型下仍有房间时不允许删除 */
    @Operation(summary = "删除房型", description = "房型下仍有房间时不允许删除")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roomTypeService.delete(id);
        return ApiResponse.ok();
    }

    /** 查询房型详情 */
    @Operation(summary = "房型详情")
    @GetMapping("/{id}")
    public ApiResponse<RoomTypeVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(roomTypeService.get(id));
    }

    /** page 从 1 开始，size 为每页条数；keyword 按名称模糊匹配，status 可选 */
    @Operation(summary = "房型分页列表")
    @GetMapping
    public ApiResponse<PageResult<RoomTypeVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.ok(roomTypeService.page(page, size, keyword, status));
    }
}

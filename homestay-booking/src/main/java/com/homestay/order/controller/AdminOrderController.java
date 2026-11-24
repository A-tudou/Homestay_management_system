package com.homestay.order.controller;

import com.homestay.common.ApiResponse;
import com.homestay.common.PageResult;
import com.homestay.order.dto.CancelRequest;
import com.homestay.order.dto.OrderDetailVO;
import com.homestay.order.dto.OrderVO;
import com.homestay.order.service.OrderService;
import com.homestay.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单管理接口（房东端），路径前缀 /api/admin/orders。
 * /api/admin/** 由安全配置统一限制为 HOST 角色访问，可查看全部订单并推进状态。
 */
@Tag(name = "房东-订单管理", description = "仅房东（HOST）可访问")
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    /** 构造注入订单服务 */
    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** 分页查询全部订单 */
    @Operation(summary = "订单分页列表", description = "keyword 支持订单号、联系电话模糊搜索")
    @GetMapping
    public ApiResponse<PageResult<OrderVO>> page(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(orderService.page(status, keyword, page, size));
    }

    /** 查询订单详情 */
    @Operation(summary = "订单详情（含状态流转记录）")
    @GetMapping("/{id}")
    public ApiResponse<OrderDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(orderService.detail(id, null, true));
    }

    /** 确认订单 */
    @Operation(summary = "确认订单", description = "待确认 → 已确认")
    @PostMapping("/{id}/confirm")
    public ApiResponse<OrderVO> confirm(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.ok(orderService.confirm(id, operatorId(authentication)));
    }

    /** 拒绝订单 */
    @Operation(summary = "拒绝订单", description = "待确认 → 已取消，归还库存")
    @PostMapping("/{id}/reject")
    public ApiResponse<OrderVO> reject(Authentication authentication,
                                       @PathVariable Long id,
                                       @RequestBody(required = false) CancelRequest request) {
        String reason = request == null ? null : request.reason();
        return ApiResponse.ok(orderService.reject(id, operatorId(authentication), reason));
    }

    /** 办理入住 */
    @Operation(summary = "办理入住", description = "已确认 → 已入住")
    @PostMapping("/{id}/check-in")
    public ApiResponse<OrderVO> checkIn(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.ok(orderService.checkIn(id, operatorId(authentication)));
    }

    /** 完成订单 */
    @Operation(summary = "完成订单", description = "已入住 → 已完成")
    @PostMapping("/{id}/complete")
    public ApiResponse<OrderVO> complete(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.ok(orderService.complete(id, operatorId(authentication)));
    }

    /** 从 Spring Security 上下文取当前操作人（房东）用户 ID，写入订单流转日志 */
    private Long operatorId(Authentication authentication) {
        return ((LoginUser) authentication.getPrincipal()).id();
    }
}

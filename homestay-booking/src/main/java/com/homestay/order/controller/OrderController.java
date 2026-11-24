package com.homestay.order.controller;

import com.homestay.common.ApiResponse;
import com.homestay.common.PageResult;
import com.homestay.order.dto.CancelRequest;
import com.homestay.order.dto.CreateOrderRequest;
import com.homestay.order.dto.OrderDetailVO;
import com.homestay.order.dto.OrderVO;
import com.homestay.order.service.OrderService;
import com.homestay.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 房客订单接口，路径前缀 /api/orders。
 * 需登录（CUSTOMER 或 HOST 均可），所有查询与操作都限定为当前登录用户自己的订单。
 */
@Tag(name = "房客-订单", description = "需登录")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    /** 构造注入订单服务 */
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** 创建订单 */
    @Operation(summary = "创建订单")
    @PostMapping
    public ApiResponse<OrderVO> create(Authentication authentication,
                                       @Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.ok(orderService.create(userId(authentication), request));
    }

    /** 查询我的订单 */
    @Operation(summary = "我的订单")
    @GetMapping("/my")
    public ApiResponse<PageResult<OrderVO>> myOrders(
            Authentication authentication,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(orderService.myOrders(userId(authentication), status, page, size));
    }

    /** 查询我的订单详情 */
    @Operation(summary = "订单详情（含状态流转记录）")
    @GetMapping("/{id}")
    public ApiResponse<OrderDetailVO> detail(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.ok(orderService.detail(id, userId(authentication), false));
    }

    /** 取消订单 */
    @Operation(summary = "取消订单", description = "仅待确认、已确认状态可取消，取消后归还库存")
    @PostMapping("/{id}/cancel")
    public ApiResponse<OrderVO> cancel(Authentication authentication,
                                       @PathVariable Long id,
                                       @RequestBody(required = false) CancelRequest request) {
        String reason = request == null ? null : request.reason();
        return ApiResponse.ok(orderService.cancelByCustomer(id, userId(authentication), reason));
    }

    /** 从 Spring Security 上下文取当前登录用户 ID */
    private Long userId(Authentication authentication) {
        return ((LoginUser) authentication.getPrincipal()).id();
    }
}

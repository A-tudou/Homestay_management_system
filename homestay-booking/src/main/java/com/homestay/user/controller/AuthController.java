package com.homestay.user.controller;

import com.homestay.common.ApiResponse;
import com.homestay.security.LoginUser;
import com.homestay.user.dto.AuthResponse;
import com.homestay.user.dto.LoginRequest;
import com.homestay.user.dto.RegisterRequest;
import com.homestay.user.dto.UserVO;
import com.homestay.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口，路径前缀 /api/auth。
 * 注册、登录无需鉴权；/me 需要携带有效 JWT。
 */
@Tag(name = "认证", description = "注册、登录与当前用户信息")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    /** 构造器注入认证服务 */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 房客注册，成功后返回不含密码的用户信息 */
    @Operation(summary = "房客注册")
    @PostMapping("/register")
    public ApiResponse<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    /** 登录校验，成功后返回 JWT 与用户信息 */
    @Operation(summary = "登录，返回 JWT")
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    /** 从 Spring Security 的 Authentication 中取当前登录用户 ID */
    @Operation(summary = "当前登录用户")
    @GetMapping("/me")
    public ApiResponse<UserVO> me(Authentication authentication) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        return ApiResponse.ok(authService.currentUser(loginUser.id()));
    }
}

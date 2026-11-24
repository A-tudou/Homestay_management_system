package com.homestay.security;

/**
 * 放进 JWT 的登录用户信息，避免每次请求都查库。
 */
public record LoginUser(Long id, String username, String role) {
}

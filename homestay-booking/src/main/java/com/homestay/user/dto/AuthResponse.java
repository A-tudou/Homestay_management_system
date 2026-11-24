package com.homestay.user.dto;

/**
 * 登录成功后的响应：JWT 令牌、有效期与当前用户信息。
 */
public record AuthResponse(
        /** JWT 访问令牌，后续请求放在 Authorization: Bearer {token} */
        String token,
        /** 令牌有效期，单位分钟 */
        long expiresInMinutes,
        /** 登录用户信息（不含密码） */
        UserVO user
) {
}

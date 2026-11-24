package com.homestay.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求。房客与房东使用同一登录入口，角色由账号本身决定。
 */
public record LoginRequest(
        /** 登录用户名 */
        @NotBlank(message = "用户名不能为空")
        String username,

        /** 明文密码，服务端与库中哈希比对 */
        @NotBlank(message = "密码不能为空")
        String password
) {
}

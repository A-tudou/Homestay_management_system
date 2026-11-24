package com.homestay.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 房客注册请求。注册后角色固定为 CUSTOMER、状态为正常。
 */
public record RegisterRequest(
        /** 登录用户名：4-20 位，仅字母、数字、下划线 */
        @NotBlank(message = "用户名不能为空")
        @Size(min = 4, max = 20, message = "用户名长度需在 4-20 位之间")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
        String username,

        /** 明文密码：6-32 位，入库前由服务端加密 */
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 32, message = "密码长度需在 6-32 位之间")
        String password,

        /** 昵称，最多 20 个字符 */
        @NotBlank(message = "昵称不能为空")
        @Size(max = 20, message = "昵称不能超过 20 个字符")
        String nickname,

        /** 手机号，可为空；填写时需为 11 位大陆手机号 */
        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone
) {
}

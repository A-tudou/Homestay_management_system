package com.homestay.user.dto;

import com.homestay.user.entity.User;

/**
 * 对外展示的用户信息，不包含密码等敏感字段。
 */
public record UserVO(
        /** 用户 ID */
        Long id,
        /** 登录用户名 */
        String username,
        /** 昵称 */
        String nickname,
        /** 手机号，可为空 */
        String phone,
        /** 角色：CUSTOMER 房客 / HOST 房东 */
        String role
) {

    /** 由用户实体转换为对外视图 */
    public static UserVO from(User user) {
        return new UserVO(user.getId(), user.getUsername(), user.getNickname(), user.getPhone(), user.getRole());
    }
}

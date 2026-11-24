package com.homestay.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homestay.user.entity.User;

/**
 * 用户 Mapper，房客注册、登录查询等操作均通过它访问 users 表。
 */
public interface UserMapper extends BaseMapper<User> {
}

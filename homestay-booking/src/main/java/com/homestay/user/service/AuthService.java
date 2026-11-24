package com.homestay.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homestay.common.BizException;
import com.homestay.common.ErrorCode;
import com.homestay.security.JwtTokenProvider;
import com.homestay.security.LoginUser;
import com.homestay.user.dto.AuthResponse;
import com.homestay.user.dto.LoginRequest;
import com.homestay.user.dto.RegisterRequest;
import com.homestay.user.dto.UserVO;
import com.homestay.user.entity.User;
import com.homestay.user.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 认证服务：负责房客注册、登录签发 JWT 与当前用户查询。
 * 密码以 BCrypt 哈希入库；被禁用（status=0）的账号不能登录。
 */
@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /** 构造器注入用户 Mapper、密码编码器与 JWT 提供者 */
    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 房客注册：用户名唯一，密码加密存储，角色固定为 CUSTOMER、状态置为正常。
     */
    @Transactional
    public UserVO register(RegisterRequest request) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.username()));
        if (exists > 0) {
            throw new BizException(ErrorCode.CONFLICT, "用户名已存在");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());
        user.setPhone(request.phone());
        user.setRole("CUSTOMER");
        user.setStatus(1);
        userMapper.insert(user);
        return UserVO.from(user);
    }

    /**
     * 登录校验用户名与密码（BCrypt 比对），并拒绝已禁用账号；成功后签发 JWT。
     */
    public AuthResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.username()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (!Objects.equals(user.getStatus(), 1)) {
            throw new BizException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }

        String token = jwtTokenProvider.createToken(new LoginUser(user.getId(), user.getUsername(), user.getRole()));
        return new AuthResponse(token, jwtTokenProvider.getExpireMinutes(), UserVO.from(user));
    }

    /**
     * 按用户 ID 查询当前登录用户信息，用户不存在时按未认证处理。
     */
    public UserVO currentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }
        return UserVO.from(user);
    }
}

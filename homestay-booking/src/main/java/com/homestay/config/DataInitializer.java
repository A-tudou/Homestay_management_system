package com.homestay.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homestay.user.entity.User;
import com.homestay.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 首次启动时初始化一个房东账号，方便调试后台接口。
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /** 注入用户数据访问与密码编码器 */
    public DataInitializer(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 启动时检查是否存在 HOST 账号；不存在才创建默认房东 admin / admin123，避免重复插入。
     */
    @Override
    public void run(String... args) {
        Long hostCount = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, "HOST"));
        if (hostCount > 0) {
            return;
        }
        User host = new User();
        host.setUsername("admin");
        host.setPassword(passwordEncoder.encode("admin123"));
        host.setNickname("房东管理员");
        host.setRole("HOST");
        host.setStatus(1);
        userMapper.insert(host);
        log.warn("已初始化房东账号 admin / admin123，请尽快修改密码");
    }
}

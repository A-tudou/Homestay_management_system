package com.homestay;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用启动类。
 * {@code @MapperScan} 统一扫描各业务模块下的 mapper 包，省去在每个 Mapper 接口上单独标注。
 */
@SpringBootApplication
@MapperScan("com.homestay.**.mapper")
public class HomestayApplication {

    /** 应用入口，启动 Spring Boot 容器 */
    public static void main(String[] args) {
        SpringApplication.run(HomestayApplication.class, args);
    }
}

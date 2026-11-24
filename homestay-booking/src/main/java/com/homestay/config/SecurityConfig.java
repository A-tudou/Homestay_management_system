package com.homestay.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homestay.common.ApiResponse;
import com.homestay.common.ErrorCode;
import com.homestay.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

/**
 * 无状态 JWT 鉴权：公共接口匿名访问，管理端要求 HOST 角色，其余接口要求登录。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** 使用 BCrypt 作为密码编码器 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** 配置无状态安全过滤链：放行公共接口、限制管理端角色并注册 JWT 过滤器 */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   ObjectMapper objectMapper) throws Exception {
        // JWT 通过 Authorization 头传递，不依赖 Cookie/Session，因此关闭 CSRF 并禁用 HTTP Session
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("HOST")
                        .anyRequest().authenticated()
                )
                // 未登录与无权限都返回统一 JSON，避免前端收到 Spring Security 默认 HTML 错误页
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint((request, response, ex) ->
                                writeJson(response, objectMapper, 401, ErrorCode.UNAUTHORIZED.getMessage()))
                        .accessDeniedHandler((request, response, ex) ->
                                writeJson(response, objectMapper, 403, ErrorCode.FORBIDDEN.getMessage()))
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** 向响应写入统一 JSON 格式的错误信息 */
    private static void writeJson(HttpServletResponse response, ObjectMapper objectMapper,
                                  int code, String message) throws IOException {
        response.setStatus(code);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(code, message));
    }
}

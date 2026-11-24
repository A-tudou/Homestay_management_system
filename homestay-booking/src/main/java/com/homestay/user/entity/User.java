package com.homestay.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 系统用户，房客与房东共用 users 表。
 * role 区分身份：CUSTOMER 房客、HOST 房东；status 1 正常、0 禁用（禁用后无法登录）。
 */
@TableName("users")
public class User {

    /** 主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 登录用户名，全局唯一 */
    private String username;
    /** BCrypt 加密后的密码哈希，不保存明文 */
    private String password;
    /** 昵称，展示用 */
    private String nickname;
    /** 手机号，可为空 */
    private String phone;
    /** 角色：CUSTOMER 房客 / HOST 房东（/api/admin/** 仅 HOST 可访问） */
    private String role;
    /** 账号状态：1 正常 / 0 禁用 */
    private Integer status;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 最后更新时间 */
    private LocalDateTime updatedAt;

    /** 获取用户主键 */
    public Long getId() {
        return id;
    }

    /** 设置用户主键 */
    public void setId(Long id) {
        this.id = id;
    }

    /** 获取登录用户名 */
    public String getUsername() {
        return username;
    }

    /** 设置登录用户名 */
    public void setUsername(String username) {
        this.username = username;
    }

    /** 获取密码哈希 */
    public String getPassword() {
        return password;
    }

    /** 设置密码哈希 */
    public void setPassword(String password) {
        this.password = password;
    }

    /** 获取昵称 */
    public String getNickname() {
        return nickname;
    }

    /** 设置昵称 */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /** 获取手机号 */
    public String getPhone() {
        return phone;
    }

    /** 设置手机号 */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /** 获取角色 */
    public String getRole() {
        return role;
    }

    /** 设置角色 */
    public void setRole(String role) {
        this.role = role;
    }

    /** 获取账号状态 */
    public Integer getStatus() {
        return status;
    }

    /** 设置账号状态 */
    public void setStatus(Integer status) {
        this.status = status;
    }

    /** 获取创建时间 */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** 设置创建时间 */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /** 获取最后更新时间 */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /** 设置最后更新时间 */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

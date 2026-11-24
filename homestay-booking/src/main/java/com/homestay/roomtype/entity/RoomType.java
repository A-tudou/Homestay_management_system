package com.homestay.roomtype.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 房型，是价格、库存与房间的归属单位。
 * status 1 上架（房客端可见）/ 0 下架；basePrice 为生成日历时使用的默认每晚价格。
 */
@TableName("room_type")
public class RoomType {

    /** 主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 房型名称，业务上唯一（由服务层校验） */
    private String name;
    /** 房型描述 */
    private String description;
    /** 基准价（元/晚），批量生成日历时作为默认价格 */
    private BigDecimal basePrice;
    /** 最大入住人数 */
    private Integer maxGuests;
    /** 床型描述，如“1.8m 大床” */
    private String bedType;
    /** 面积，单位平方米 */
    private Integer area;
    /** 封面图地址 */
    private String coverImage;
    /** 状态：1 上架 / 0 下架 */
    private Integer status;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 最后更新时间 */
    private LocalDateTime updatedAt;

    /** 获取房型主键 */
    public Long getId() {
        return id;
    }

    /** 设置房型主键 */
    public void setId(Long id) {
        this.id = id;
    }

    /** 获取房型名称 */
    public String getName() {
        return name;
    }

    /** 设置房型名称 */
    public void setName(String name) {
        this.name = name;
    }

    /** 获取房型描述 */
    public String getDescription() {
        return description;
    }

    /** 设置房型描述 */
    public void setDescription(String description) {
        this.description = description;
    }

    /** 获取基准价 */
    public BigDecimal getBasePrice() {
        return basePrice;
    }

    /** 设置基准价 */
    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    /** 获取最大入住人数 */
    public Integer getMaxGuests() {
        return maxGuests;
    }

    /** 设置最大入住人数 */
    public void setMaxGuests(Integer maxGuests) {
        this.maxGuests = maxGuests;
    }

    /** 获取床型描述 */
    public String getBedType() {
        return bedType;
    }

    /** 设置床型描述 */
    public void setBedType(String bedType) {
        this.bedType = bedType;
    }

    /** 获取面积 */
    public Integer getArea() {
        return area;
    }

    /** 设置面积 */
    public void setArea(Integer area) {
        this.area = area;
    }

    /** 获取封面图地址 */
    public String getCoverImage() {
        return coverImage;
    }

    /** 设置封面图地址 */
    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    /** 获取上下架状态 */
    public Integer getStatus() {
        return status;
    }

    /** 设置上下架状态 */
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

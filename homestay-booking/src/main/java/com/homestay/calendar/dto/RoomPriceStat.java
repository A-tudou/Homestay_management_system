package com.homestay.calendar.dto;

import java.math.BigDecimal;

/**
 * 日历价格的聚合结果，供房客端计算价格区间、总价与最少剩余库存。
 */
public class RoomPriceStat {

    private Long roomTypeId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private BigDecimal sumPrice;
    private Integer minStock;
    private Integer nightCount;

    /** 获取房型 ID */
    public Long getRoomTypeId() {
        return roomTypeId;
    }

    /** 设置房型 ID */
    public void setRoomTypeId(Long roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    /** 获取最低单价 */
    public BigDecimal getMinPrice() {
        return minPrice;
    }

    /** 设置最低单价 */
    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    /** 获取最高单价 */
    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    /** 设置最高单价 */
    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    /** 获取价格合计 */
    public BigDecimal getSumPrice() {
        return sumPrice;
    }

    /** 设置价格合计 */
    public void setSumPrice(BigDecimal sumPrice) {
        this.sumPrice = sumPrice;
    }

    /** 获取最少剩余库存 */
    public Integer getMinStock() {
        return minStock;
    }

    /** 设置最少剩余库存 */
    public void setMinStock(Integer minStock) {
        this.minStock = minStock;
    }

    /** 获取晚数 */
    public Integer getNightCount() {
        return nightCount;
    }

    /** 设置晚数 */
    public void setNightCount(Integer nightCount) {
        this.nightCount = nightCount;
    }
}

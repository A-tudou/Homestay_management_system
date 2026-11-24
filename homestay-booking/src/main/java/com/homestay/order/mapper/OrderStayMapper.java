package com.homestay.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homestay.order.entity.OrderStay;

/**
 * 订单入住明细 Mapper，一条记录对应订单的一晚，取消时据此逐晚归还库存。
 */
public interface OrderStayMapper extends BaseMapper<OrderStay> {
}

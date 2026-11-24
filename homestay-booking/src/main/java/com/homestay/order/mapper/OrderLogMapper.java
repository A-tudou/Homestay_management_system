package com.homestay.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homestay.order.entity.OrderLog;

/**
 * 订单状态流转日志 Mapper，记录每次状态变更及操作人，便于追踪。
 */
public interface OrderLogMapper extends BaseMapper<OrderLog> {
}

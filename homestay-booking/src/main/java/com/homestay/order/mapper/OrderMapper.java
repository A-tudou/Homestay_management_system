package com.homestay.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homestay.order.entity.Order;

/**
 * 订单主表 Mapper，继承 BaseMapper 后获得通用 CRUD 与条件构造器查询能力。
 */
public interface OrderMapper extends BaseMapper<Order> {
}

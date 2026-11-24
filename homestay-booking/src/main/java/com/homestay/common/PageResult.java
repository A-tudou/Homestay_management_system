package com.homestay.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 分页响应结构。
 */
public record PageResult<T>(List<T> records, long total, long page, long size) {

    /** 由 MyBatis-Plus 分页对象转换 */
    public static <T> PageResult<T> of(Page<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /** 由明细与分页参数直接构造 */
    public static <T> PageResult<T> of(List<T> records, long total, long page, long size) {
        return new PageResult<>(records, total, page, size);
    }
}

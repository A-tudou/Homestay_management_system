package com.homestay.roomtype.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homestay.common.BizException;
import com.homestay.common.ErrorCode;
import com.homestay.common.PageResult;
import com.homestay.room.entity.Room;
import com.homestay.room.mapper.RoomMapper;
import com.homestay.roomtype.dto.RoomTypeRequest;
import com.homestay.roomtype.dto.RoomTypeVO;
import com.homestay.roomtype.entity.RoomType;
import com.homestay.roomtype.mapper.RoomTypeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 房型管理服务（房东端）：增删改查与上下架，房型名称保持唯一。
 */
@Service
public class RoomTypeService {

    private final RoomTypeMapper roomTypeMapper;
    private final RoomMapper roomMapper;

    /** 构造器注入房型与房间 Mapper */
    public RoomTypeService(RoomTypeMapper roomTypeMapper, RoomMapper roomMapper) {
        this.roomTypeMapper = roomTypeMapper;
        this.roomMapper = roomMapper;
    }

    /**
     * 新增房型：先校验名称未被占用，新房的初始状态为上架（1）。
     */
    @Transactional
    public RoomTypeVO create(RoomTypeRequest request) {
        ensureNameAvailable(request.name(), null);

        RoomType roomType = new RoomType();
        applyRequest(roomType, request);
        roomType.setStatus(1);
        roomTypeMapper.insert(roomType);
        return RoomTypeVO.from(roomType);
    }

    /**
     * 修改房型：名称查重时排除自身，避免修改时与旧记录冲突。
     */
    @Transactional
    public RoomTypeVO update(Long id, RoomTypeRequest request) {
        RoomType roomType = getExisting(id);
        ensureNameAvailable(request.name(), id);
        applyRequest(roomType, request);
        roomTypeMapper.updateById(roomType);
        return RoomTypeVO.from(roomTypeMapper.selectById(id));
    }

    /** 上架 / 下架 */
    @Transactional
    public void updateStatus(Long id, Integer status) {
        getExisting(id);
        RoomType update = new RoomType();
        update.setId(id);
        update.setStatus(status);
        roomTypeMapper.updateById(update);
    }

    /**
     * 房型下还有房间时不允许删除，避免房间变成孤儿数据。
     */
    @Transactional
    public void delete(Long id) {
        getExisting(id);
        Long roomCount = roomMapper.selectCount(new LambdaQueryWrapper<Room>().eq(Room::getRoomTypeId, id));
        if (roomCount > 0) {
            throw new BizException(ErrorCode.CONFLICT, "该房型下还有 " + roomCount + " 个房间，请先删除房间");
        }
        roomTypeMapper.deleteById(id);
    }

    /** 查询房型详情 */
    public RoomTypeVO get(Long id) {
        return RoomTypeVO.from(getExisting(id));
    }

    /**
     * 房型分页查询：keyword 对名称模糊匹配，status 可选过滤，按 ID 倒序（新在前）。
     */
    public PageResult<RoomTypeVO> page(long page, long size, String keyword, Integer status) {
        LambdaQueryWrapper<RoomType> wrapper = new LambdaQueryWrapper<RoomType>()
                .like(StringUtils.hasText(keyword), RoomType::getName, keyword)
                .eq(status != null, RoomType::getStatus, status)
                .orderByDesc(RoomType::getId);

        Page<RoomType> result = roomTypeMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords().stream().map(RoomTypeVO::from).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    /** 按 ID 查询房型，不存在时抛 404 业务异常 */
    private RoomType getExisting(Long id) {
        RoomType roomType = roomTypeMapper.selectById(id);
        if (roomType == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "房型不存在");
        }
        return roomType;
    }

    /** 校验房型名称唯一；excludeId 用于修改时排除自身 */
    private void ensureNameAvailable(String name, Long excludeId) {
        Long count = roomTypeMapper.selectCount(new LambdaQueryWrapper<RoomType>()
                .eq(RoomType::getName, name)
                .ne(excludeId != null, RoomType::getId, excludeId));
        if (count > 0) {
            throw new BizException(ErrorCode.CONFLICT, "房型名称已存在");
        }
    }

    /** 将请求参数写入房型实体 */
    private void applyRequest(RoomType roomType, RoomTypeRequest request) {
        roomType.setName(request.name());
        roomType.setDescription(request.description());
        roomType.setBasePrice(request.basePrice());
        roomType.setMaxGuests(request.maxGuests());
        roomType.setBedType(request.bedType());
        roomType.setArea(request.area());
        roomType.setCoverImage(request.coverImage());
    }
}

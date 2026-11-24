package com.homestay.room.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homestay.common.BizException;
import com.homestay.common.ErrorCode;
import com.homestay.common.PageResult;
import com.homestay.room.dto.RoomRequest;
import com.homestay.room.dto.RoomVO;
import com.homestay.room.entity.Room;
import com.homestay.room.mapper.RoomMapper;
import com.homestay.roomtype.entity.RoomType;
import com.homestay.roomtype.mapper.RoomTypeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 房间管理服务（房东端）：维护房型下的物理房间，房间号全局唯一。
 */
@Service
public class RoomService {

    private final RoomMapper roomMapper;
    private final RoomTypeMapper roomTypeMapper;

    /** 构造器注入房间与房型 Mapper */
    public RoomService(RoomMapper roomMapper, RoomTypeMapper roomTypeMapper) {
        this.roomMapper = roomMapper;
        this.roomTypeMapper = roomTypeMapper;
    }

    /**
     * 新增房间：校验所属房型存在、房间号未被占用，新房间初始状态为可用（1）。
     */
    @Transactional
    public RoomVO create(RoomRequest request) {
        ensureRoomTypeExists(request.roomTypeId());
        ensureRoomNoAvailable(request.roomNo(), null);

        Room room = new Room();
        applyRequest(room, request);
        room.setStatus(1);
        roomMapper.insert(room);
        return toVO(room, getRoomTypeName(request.roomTypeId()));
    }

    /**
     * 修改房间：房间号查重时排除自身，允许调整所属房型、房间号与楼层。
     */
    @Transactional
    public RoomVO update(Long id, RoomRequest request) {
        Room room = getExisting(id);
        ensureRoomTypeExists(request.roomTypeId());
        ensureRoomNoAvailable(request.roomNo(), id);

        applyRequest(room, request);
        roomMapper.updateById(room);
        return toVO(roomMapper.selectById(id), getRoomTypeName(request.roomTypeId()));
    }

    /** 1 可用，0 维修中 */
    @Transactional
    public void updateStatus(Long id, Integer status) {
        getExisting(id);
        Room update = new Room();
        update.setId(id);
        update.setStatus(status);
        roomMapper.updateById(update);
    }

    /** 删除房间，仅校验存在性，历史订单通过 order_room 保留房间 ID 引用 */
    @Transactional
    public void delete(Long id) {
        getExisting(id);
        roomMapper.deleteById(id);
    }

    /**
     * 房间分页查询：按房型、状态过滤，按房型 ID、房间号升序。
     * 房型名称通过一次批量查询填充，避免逐行查询造成 N+1。
     */
    public PageResult<RoomVO> page(long page, long size, Long roomTypeId, Integer status) {
        LambdaQueryWrapper<Room> wrapper = new LambdaQueryWrapper<Room>()
                .eq(roomTypeId != null, Room::getRoomTypeId, roomTypeId)
                .eq(status != null, Room::getStatus, status)
                .orderByAsc(Room::getRoomTypeId)
                .orderByAsc(Room::getRoomNo);

        Page<Room> result = roomMapper.selectPage(new Page<>(page, size), wrapper);
        Map<Long, String> typeNames = loadRoomTypeNames(result.getRecords());
        List<RoomVO> records = result.getRecords().stream()
                .map(room -> toVO(room, typeNames.get(room.getRoomTypeId())))
                .toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /** 按 ID 查询房间，不存在时抛 404 业务异常 */
    public RoomVO get(Long id) {
        Room room = getExisting(id);
        return toVO(room, getRoomTypeName(room.getRoomTypeId()));
    }

    /** 批量查询分页结果涉及的房型名称，返回 roomTypeId -> name */
    private Map<Long, String> loadRoomTypeNames(List<Room> rooms) {
        List<Long> typeIds = rooms.stream().map(Room::getRoomTypeId).distinct().toList();
        if (typeIds.isEmpty()) {
            return Map.of();
        }
        return roomTypeMapper.selectBatchIds(typeIds).stream()
                .collect(Collectors.toMap(RoomType::getId, RoomType::getName));
    }

    /** 按房型 ID 查询房型名称，不存在时返回 null */
    private String getRoomTypeName(Long roomTypeId) {
        RoomType roomType = roomTypeMapper.selectById(roomTypeId);
        return roomType == null ? null : roomType.getName();
    }

    /** 按 ID 查询房间，不存在时抛 404 业务异常 */
    private Room getExisting(Long id) {
        Room room = roomMapper.selectById(id);
        if (room == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "房间不存在");
        }
        return room;
    }

    /** 校验所属房型存在，不存在时抛 404 业务异常 */
    private void ensureRoomTypeExists(Long roomTypeId) {
        if (roomTypeMapper.selectById(roomTypeId) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "所属房型不存在");
        }
    }

    /** 校验房间号唯一；excludeId 用于修改时排除自身 */
    private void ensureRoomNoAvailable(String roomNo, Long excludeId) {
        Long count = roomMapper.selectCount(new LambdaQueryWrapper<Room>()
                .eq(Room::getRoomNo, roomNo)
                .ne(excludeId != null, Room::getId, excludeId));
        if (count > 0) {
            throw new BizException(ErrorCode.CONFLICT, "房间号 " + roomNo + " 已存在");
        }
    }

    /** 将请求参数写入房间实体 */
    private void applyRequest(Room room, RoomRequest request) {
        room.setRoomTypeId(request.roomTypeId());
        room.setRoomNo(request.roomNo());
        room.setFloor(request.floor());
    }

    /** 将房间实体与房型名称转换为对外视图 */
    private RoomVO toVO(Room room, String roomTypeName) {
        return new RoomVO(room.getId(), room.getRoomTypeId(), roomTypeName, room.getRoomNo(),
                room.getFloor(), room.getStatus(), room.getCreatedAt());
    }
}

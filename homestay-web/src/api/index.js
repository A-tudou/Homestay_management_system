/**
 * 接口集中出口：每个方法只声明「请求哪个地址、传什么参数」。
 * baseURL、鉴权头、以及 { code, message, data } 的解包都在 ./http.js 的拦截器里完成，
 * 因此这里的 Promise 直接 resolve 出业务 data（失败时 reject）。
 */
import http from './http'

/* ==================== 认证 ==================== */
export const authApi = {
  // 登录：参数 { username, password }，返回 { token, user }
  login(data) {
    return http.post('/auth/login', data)
  },
  // 注册房客账号：参数 { username, password, nickname, phone }；注册接口不返回 token，需再用新账号调用 login
  register(data) {
    return http.post('/auth/register', data)
  },
  // 获取当前登录用户资料：用于刷新会话或补齐本地缺失的 user（含 role）
  me() {
    return http.get('/auth/me')
  }
}

/* ==================== 房型（房客端公开接口） ==================== */
export const roomApi = {
  // 房型分页列表：参数 { page, size, keyword, guests }，返回 { records, total }
  list(params) {
    return http.get('/public/room-types', { params })
  },
  // 按日期查可订房型：参数 { checkIn, checkOut, guests, roomCount }，结果额外带每晚余量与整单报价字段
  search(params) {
    return http.get('/public/room-types/search', { params })
  },
  // 房型详情：返回 minPrice / maxPrice 等展示字段，用于详情页价格区间
  detail(id) {
    return http.get(`/public/room-types/${id}`)
  },

  /* ---- 房态日历与报价 ---- */
  // 查询某房型一段时间内的房态：参数 { from, to }，返回每天 { date, price, remaining, available }
  calendar(id, params) {
    return http.get(`/public/room-types/${id}/calendar`, { params })
  },
  // 试算整单价格：参数 { checkIn, checkOut, roomCount }，返回 { nights, totalAmount, available, reason }
  quote(id, params) {
    return http.get(`/public/room-types/${id}/quote`, { params })
  }
}

/* ==================== 订单（房客端） ==================== */
export const orderApi = {
  // 创建订单：参数 { roomTypeId, checkInDate, checkOutDate, roomCount, contactName, contactPhone, remark }
  create(data) {
    return http.post('/orders', data)
  },
  // 我的订单分页：参数 { status, page, size }，status 为空表示全部
  my(params) {
    return http.get('/orders/my', { params })
  },
  // 订单详情：返回 { order, logs }，logs 为状态流转记录
  detail(id) {
    return http.get(`/orders/${id}`)
  },
  // 取消订单：参数 { reason }；仅待确认/已确认可取消，成功后立即归还房间库存
  cancel(id, data) {
    return http.post(`/orders/${id}/cancel`, data || {})
  }
}

/* ==================== 后台：房型管理 ==================== */
export const adminRoomTypeApi = {
  // 房型分页：参数 { page, size, keyword, status }，status 1 上架 / 0 下架
  page(params) {
    return http.get('/admin/room-types', { params })
  },
  // 房型详情（编辑回显用）
  detail(id) {
    return http.get(`/admin/room-types/${id}`)
  },
  // 新建房型：参数 { name, description, basePrice, maxGuests, bedType, area, coverImage }
  create(data) {
    return http.post('/admin/room-types', data)
  },
  // 更新房型：参数同 create，覆盖式提交
  update(id, data) {
    return http.put(`/admin/room-types/${id}`, data)
  },
  // 上架 / 下架：status 1 上架、0 下架；下架后不出现在房客端
  updateStatus(id, status) {
    return http.patch(`/admin/room-types/${id}/status`, { status })
  },
  // 删除房型：该房型下仍有房间时后端会拒绝
  remove(id) {
    return http.delete(`/admin/room-types/${id}`)
  }
}

/* ==================== 后台：房间管理 ==================== */
export const adminRoomApi = {
  // 房间分页：参数 { page, size, roomTypeId, status }，status 1 可用 / 0 维修中
  page(params) {
    return http.get('/admin/rooms', { params })
  },
  // 房间详情（编辑回显用）
  detail(id) {
    return http.get(`/admin/rooms/${id}`)
  },
  // 新建房间：参数 { roomTypeId, roomNo, floor }
  create(data) {
    return http.post('/admin/rooms', data)
  },
  // 更新房间：参数同 create
  update(id, data) {
    return http.put(`/admin/rooms/${id}`, data)
  },
  // 设置房间状态：仅可用(1)/维修中(0)，房间维度只影响自身，不影响同房型其他房间
  updateStatus(id, status) {
    return http.patch(`/admin/rooms/${id}/status`, { status })
  },
  // 删除房间：存在历史订单或日历占用时后端会拒绝
  remove(id) {
    return http.delete(`/admin/rooms/${id}`)
  }
}

/* ==================== 后台：房态日历 ==================== */
export const adminCalendarApi = {
  // 按月查询某房型的日历：参数 { from, to }，返回该区间内已生成的日期记录
  list(roomTypeId, params) {
    return http.get(`/admin/room-types/${roomTypeId}/calendar`, { params })
  },
  // 批量生成日历：参数 { startDate, endDate }，已存在日期自动跳过，返回实际新增天数
  generate(roomTypeId, data) {
    return http.post(`/admin/room-types/${roomTypeId}/calendar/generate`, data)
  },
  // 修改单日日历：参数 { price, stock, status }，按日历记录 id 精确更新
  update(id, data) {
    return http.put(`/admin/calendar/${id}`, data)
  },
  // 区间批量修改：参数 { fromDate, toDate, price?, stock?, status? }，未传字段不改动，返回影响天数
  batchUpdate(roomTypeId, data) {
    return http.put(`/admin/room-types/${roomTypeId}/calendar/batch`, data)
  }
}

/* ==================== 后台：订单管理 ==================== */
export const adminOrderApi = {
  // 订单分页：参数 { page, size, status, keyword }，keyword 匹配订单号或联系电话
  page(params) {
    return http.get('/admin/orders', { params })
  },
  // 订单详情：返回 { order, logs }，用于抽屉展示与状态流转记录
  detail(id) {
    return http.get(`/admin/orders/${id}`)
  },
  // 确认订单：PENDING -> CONFIRMED，此时会为订单分配具体房间
  confirm(id) {
    return http.post(`/admin/orders/${id}/confirm`)
  },
  // 拒绝订单：PENDING -> CANCELLED，可附 reason，成功后归还已占用的库存
  reject(id, reason) {
    return http.post(`/admin/orders/${id}/reject`, { reason: reason || undefined })
  },
  // 办理入住：CONFIRMED -> CHECKED_IN
  checkIn(id) {
    return http.post(`/admin/orders/${id}/check-in`)
  },
  // 完成订单：CHECKED_IN -> COMPLETED
  complete(id) {
    return http.post(`/admin/orders/${id}/complete`)
  }
}

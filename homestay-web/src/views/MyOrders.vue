<script setup>
/**
 * 我的订单：按状态页签分页展示房客自己的订单，可查看详情与状态流转记录，
 * 待确认/已确认的订单支持自行取消（取消后立即释放库存）。
 */
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderApi } from '../api'
import { formatDateTime } from '../utils/date'

// 状态页签：value 为空表示不筛选，其余与后端订单状态枚举一一对应
const statusTabs = [
  { label: '全部', value: '' },
  { label: '待确认', value: 'PENDING' },
  { label: '已确认', value: 'CONFIRMED' },
  { label: '已入住', value: 'CHECKED_IN' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' }
]

// 订单状态 -> Element Plus 标签颜色映射，文案直接用后端返回的 statusText
const statusType = {
  PENDING: 'warning',
  CONFIRMED: 'primary',
  CHECKED_IN: 'success',
  COMPLETED: 'info',
  CANCELLED: 'info'
}

const activeStatus = ref('')
const orders = ref([])
const total = ref(0)
const page = ref(1)
// 每页 5 条，订单卡片高度较大，不适合一页铺太多
const size = 5
const loading = ref(false)

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

// 只有待确认和已确认可取消；已入住/已完成/已取消都不允许
function canCancel(order) {
  return order && ['PENDING', 'CONFIRMED'].includes(order.status)
}

async function load() {
  loading.value = true
  try {
    const data = await orderApi.my({
      status: activeStatus.value || undefined,
      page: page.value,
      size
    })
    orders.value = data.records || []
    total.value = Number(data.total || 0)
  } catch {
    orders.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function changeStatus(value) {
  // 点击当前页签不重复请求
  if (activeStatus.value === value) return
  activeStatus.value = value
  page.value = 1
  load()
}

// 详情单独请求，返回 { order, logs }，抽屉里的状态流转时间线依赖 logs
async function openDetail(order) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await orderApi.detail(order.id)
  } catch {
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

async function cancelOrder(order) {
  // 二次确认，用户点「再想想」时 ElMessageBox 会 reject，直接中止
  try {
    await ElMessageBox.confirm(
      `确定取消订单 ${order.orderNo} 吗？取消后将立即释放房间库存。`,
      '取消订单',
      { confirmButtonText: '确定取消', cancelButtonText: '再想想', type: 'warning' }
    )
  } catch {
    return
  }

  try {
    await orderApi.cancel(order.id, { reason: '房客主动取消' })
    ElMessage.success('订单已取消')
    // 详情抽屉里取消时同步关闭它，并刷新列表状态
    detailVisible.value = false
    load()
  } catch {
    // 错误提示已由请求拦截器统一处理
  }
}

onMounted(load)
</script>

<template>
  <div class="container">
    <div class="page-head">
      <div>
        <h1>我的订单</h1>
        <p>查看预订进度，待确认或已确认的订单可自行取消。</p>
      </div>
      <el-button text @click="load">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <div class="status-tabs">
      <button
        v-for="tab in statusTabs"
        :key="tab.value"
        class="status-tab"
        :class="{ active: activeStatus === tab.value }"
        @click="changeStatus(tab.value)"
      >
        {{ tab.label }}
      </button>
    </div>

    <div v-loading="loading" class="order-list">
      <article v-for="order in orders" :key="order.id" class="order-card surface-panel">
        <div class="order-head">
          <div class="order-head-meta">
            <span class="order-no">订单号 {{ order.orderNo }}</span>
            <span class="order-time">下单于 {{ formatDateTime(order.createdAt) }}</span>
          </div>
          <el-tag :type="statusType[order.status] || 'info'" effect="light" round>
            {{ order.statusText }}
          </el-tag>
        </div>

        <div class="order-body">
          <div class="order-main">
            <h3>{{ order.roomTypeName }}</h3>
            <p class="order-dates">
              {{ order.checkInDate }} 入住 · {{ order.checkOutDate }} 离店 · {{ order.nights }} 晚 · {{ order.roomCount }} 间
            </p>
            <p class="order-contact">{{ order.contactName }} · {{ order.contactPhone }}</p>
            <p v-if="order.roomNos?.length" class="order-rooms">房间号：{{ order.roomNos.join('、') }}</p>
          </div>

          <div class="order-side">
            <div class="order-amount">¥{{ order.totalAmount }}</div>
            <div class="order-actions">
              <el-button text @click="openDetail(order)">查看详情</el-button>
              <el-button v-if="canCancel(order)" text type="danger" @click="cancelOrder(order)">取消订单</el-button>
            </div>
          </div>
        </div>
      </article>
    </div>

    <div v-if="!loading && !orders.length" class="empty-wrap">
      <el-empty description="还没有相关订单">
        <el-button type="primary" round @click="$router.push('/')">去看看房源</el-button>
      </el-empty>
    </div>

    <div v-if="total > size" class="pager">
      <el-pagination
        v-model:current-page="page"
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="size"
        @current-change="load"
      />
    </div>

    <el-dialog v-model="detailVisible" title="订单详情" width="580px">
      <div v-loading="detailLoading" class="detail-dialog">
        <template v-if="detail">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="订单号" :span="2">{{ detail.order.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="房型">{{ detail.order.roomTypeName }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusType[detail.order.status] || 'info'" size="small" round>
                {{ detail.order.statusText }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="入住日期">{{ detail.order.checkInDate }}</el-descriptions-item>
            <el-descriptions-item label="离店日期">{{ detail.order.checkOutDate }}</el-descriptions-item>
            <el-descriptions-item label="入住晚数">{{ detail.order.nights }} 晚</el-descriptions-item>
            <el-descriptions-item label="房间数">{{ detail.order.roomCount }} 间</el-descriptions-item>
            <el-descriptions-item label="房间号" :span="2">
              {{ detail.order.roomNos?.length ? detail.order.roomNos.join('、') : '待分配' }}
            </el-descriptions-item>
            <el-descriptions-item label="联系人">
              {{ detail.order.contactName }} · {{ detail.order.contactPhone }}
            </el-descriptions-item>
            <el-descriptions-item label="订单金额">
              <strong class="amount">¥{{ detail.order.totalAmount }}</strong>
            </el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">{{ detail.order.remark || '无' }}</el-descriptions-item>
            <el-descriptions-item v-if="detail.order.cancelReason" label="取消原因" :span="2">
              {{ detail.order.cancelReason }}
            </el-descriptions-item>
          </el-descriptions>

          <h4 class="timeline-title">状态流转</h4>
          <el-timeline v-if="detail.logs?.length">
            <el-timeline-item
              v-for="(log, index) in detail.logs"
              :key="index"
              :timestamp="formatDateTime(log.createdAt)"
              placement="top"
            >
              <div class="log-line">
                <span v-if="log.fromStatusText">{{ log.fromStatusText }} → </span>
                <strong>{{ log.toStatusText }}</strong>
              </div>
              <p v-if="log.remark" class="log-remark">{{ log.remark }}</p>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无流转记录" :image-size="60" />
        </template>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="detail && canCancel(detail.order)"
          type="danger"
          plain
          @click="cancelOrder(detail.order)"
        >
          取消订单
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.status-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 22px;
}

.status-tab {
  padding: 8px 18px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: var(--surface);
  color: var(--muted);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
}

.status-tab:hover {
  color: var(--brand);
  border-color: var(--brand);
}

.status-tab.active {
  background: var(--brand);
  border-color: var(--brand);
  color: #fff;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 120px;
}

.order-card {
  padding: 20px 24px;
}

.order-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 14px;
  border-bottom: 1px dashed var(--line);
}

.order-head-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 18px;
  font-size: 12.5px;
  color: var(--muted);
}

.order-no {
  color: var(--ink);
  font-weight: 600;
  letter-spacing: 0.5px;
}

.order-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding-top: 16px;
}

.order-main h3 {
  margin: 0 0 8px;
  font-size: 18px;
}

.order-main p {
  color: var(--muted);
  font-size: 13px;
  line-height: 1.7;
}

.order-rooms {
  color: var(--brand) !important;
}

.order-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
  flex-shrink: 0;
}

.order-amount {
  font-size: 24px;
  font-weight: 700;
  color: var(--brand);
}

.order-actions {
  display: flex;
  align-items: center;
}

.timeline-title {
  margin: 22px 0 14px;
  font-size: 15px;
}

.log-line {
  font-size: 13.5px;
}

.log-remark {
  margin-top: 4px;
  color: var(--muted);
  font-size: 12.5px;
}

.amount {
  color: var(--brand);
}

@media (max-width: 720px) {
  .order-body {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .order-side {
    align-items: flex-start;
    width: 100%;
    flex-direction: row;
    justify-content: space-between;
  }
}
</style>

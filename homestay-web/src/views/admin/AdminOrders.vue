<script setup>
/**
 * 后台订单管理：按状态/关键词分页查询，表格内完成状态流转，抽屉查看订单详情与流转记录。
 * 状态流转：待确认 -> 已确认 -> 已入住 -> 已完成，待确认也可直接拒绝（归还库存）。
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { adminOrderApi } from '../../api'
import { formatDateTime } from '../../utils/date'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const filter = reactive({ status: '', keyword: '' })

const statusOptions = [
  { value: 'PENDING', label: '待确认' },
  { value: 'CONFIRMED', label: '已确认' },
  { value: 'CHECKED_IN', label: '已入住' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CANCELLED', label: '已取消' }
]

// 状态 -> 标签颜色；文案使用后端 statusText，避免前后端不一致
const statusType = {
  PENDING: 'warning',
  CONFIRMED: 'primary',
  CHECKED_IN: 'success',
  COMPLETED: 'info',
  CANCELLED: 'danger'
}

const drawerVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
// 状态流转请求进行中，用于禁用表格里的操作按钮，防止重复提交
const acting = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await adminOrderApi.page({
      page: page.value,
      size: size.value,
      status: filter.status || undefined,
      keyword: filter.keyword.trim() || undefined
    })
    list.value = data.records || []
    total.value = Number(data.total || 0)
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 查询条件变化时回到第一页，避免页码越界
function search() {
  page.value = 1
  load()
}

function reset() {
  filter.status = ''
  filter.keyword = ''
  search()
}

async function openDetail(row) {
  drawerVisible.value = true
  detail.value = null
  await loadDetail(row.id)
}

async function loadDetail(id) {
  detailLoading.value = true
  try {
    detail.value = await adminOrderApi.detail(id)
  } catch {
    detail.value = null
  } finally {
    detailLoading.value = false
  }
}

// 统一的流转入口：先弹确认框（拒绝时改弹输入框收集原因），再调对应接口
async function runAction(row, action) {
  const id = row.id
  acting.value = true
  try {
    if (action === 'confirm') {
      await ElMessageBox.confirm('确认接受该订单吗？', '确认订单', {
        type: 'info',
        confirmButtonText: '确认',
        cancelButtonText: '取消'
      })
      await adminOrderApi.confirm(id)
      ElMessage.success('订单已确认')
    } else if (action === 'reject') {
      // prompt 返回 { value }，允许留空，故 inputValidator 始终通过
      const { value } = await ElMessageBox.prompt('可填写拒绝原因（选填）', '拒绝订单', {
        confirmButtonText: '确定拒绝',
        cancelButtonText: '取消',
        inputPlaceholder: '如：房间临时维护',
        inputValidator: () => true
      })
      await adminOrderApi.reject(id, value)
      ElMessage.success('订单已拒绝，库存已归还')
    } else if (action === 'check-in') {
      await ElMessageBox.confirm('确认给该订单办理入住吗？', '办理入住', {
        type: 'info',
        confirmButtonText: '办理入住',
        cancelButtonText: '取消'
      })
      await adminOrderApi.checkIn(id)
      ElMessage.success('已办理入住')
    } else if (action === 'complete') {
      await ElMessageBox.confirm('确认完成该订单吗？', '完成订单', {
        type: 'info',
        confirmButtonText: '完成',
        cancelButtonText: '取消'
      })
      await adminOrderApi.complete(id)
      ElMessage.success('订单已完成')
    }
    // 操作成功后同时刷新列表；抽屉打开时再刷新一次详情，保证状态与日志同步
    load()
    if (drawerVisible.value) {
      loadDetail(id)
    }
  } catch {
    /* 取消操作或接口错误，错误提示由 http 拦截器统一处理 */
  } finally {
    acting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="admin-card">
      <div class="admin-toolbar">
        <el-input
          v-model="filter.keyword"
          placeholder="订单号 / 联系电话"
          clearable
          @keyup.enter="search"
          @clear="search"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="filter.status" placeholder="订单状态" clearable @change="search">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="orderNo" label="订单号" width="170" />
        <el-table-column prop="roomTypeName" label="房型" min-width="140" show-overflow-tooltip />
        <el-table-column label="房间号" width="120">
          <template #default="{ row }">
            <span v-if="row.roomNos?.length">{{ row.roomNos.join('、') }}</span>
            <span v-else class="admin-hint">待分配</span>
          </template>
        </el-table-column>
        <el-table-column label="入住 / 离店" width="210">
          <template #default="{ row }">
            {{ row.checkInDate }} → {{ row.checkOutDate }}
            <span class="admin-hint">（{{ row.nights }} 晚）</span>
          </template>
        </el-table-column>
        <el-table-column prop="roomCount" label="间数" width="70" />
        <el-table-column label="金额" width="110">
          <template #default="{ row }">¥{{ Number(row.totalAmount).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="联系人" width="150">
          <template #default="{ row }">
            <div>{{ row.contactName }}</div>
            <div class="admin-hint">{{ row.contactPhone }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType[row.status] || 'info'" effect="light">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <!-- 按当前状态只展示下一个可执行动作，避免出现非法流转 -->
            <template v-if="row.status === 'PENDING'">
              <el-button link type="success" :disabled="acting" @click="runAction(row, 'confirm')">确认</el-button>
              <el-button link type="danger" :disabled="acting" @click="runAction(row, 'reject')">拒绝</el-button>
            </template>
            <el-button
              v-else-if="row.status === 'CONFIRMED'"
              link
              type="success"
              :disabled="acting"
              @click="runAction(row, 'check-in')"
            >
              入住
            </el-button>
            <el-button
              v-else-if="row.status === 'CHECKED_IN'"
              link
              type="primary"
              :disabled="acting"
              @click="runAction(row, 'complete')"
            >
              完成
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="admin-pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          background
          layout="total, sizes, prev, pager, next"
          :page-sizes="[10, 20, 50]"
          :total="total"
          @current-change="load"
          @size-change="search"
        />
      </div>
    </div>

    <el-drawer v-model="drawerVisible" title="订单详情" size="520px">
      <div v-loading="detailLoading" class="order-detail">
        <template v-if="detail">
          <div class="detail-head">
            <div>
              <p class="admin-hint">订单号</p>
              <h3>{{ detail.order.orderNo }}</h3>
            </div>
            <el-tag :type="statusType[detail.order.status] || 'info'" effect="light" size="large">
              {{ detail.order.statusText }}
            </el-tag>
          </div>

          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="房型">{{ detail.order.roomTypeName }}</el-descriptions-item>
            <el-descriptions-item label="房间号">
              {{ detail.order.roomNos?.length ? detail.order.roomNos.join('、') : '待分配' }}
            </el-descriptions-item>
            <el-descriptions-item label="入住日期">{{ detail.order.checkInDate }}</el-descriptions-item>
            <el-descriptions-item label="离店日期">{{ detail.order.checkOutDate }}</el-descriptions-item>
            <el-descriptions-item label="晚数 / 间数">
              {{ detail.order.nights }} 晚 / {{ detail.order.roomCount }} 间
            </el-descriptions-item>
            <el-descriptions-item label="订单金额">
              ¥{{ Number(detail.order.totalAmount).toFixed(2) }}
            </el-descriptions-item>
            <el-descriptions-item label="联系人">
              {{ detail.order.contactName }}（{{ detail.order.contactPhone }}）
            </el-descriptions-item>
            <el-descriptions-item label="备注">{{ detail.order.remark || '-' }}</el-descriptions-item>
            <el-descriptions-item v-if="detail.order.cancelReason" label="取消原因">
              {{ detail.order.cancelReason }}
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatDateTime(detail.order.createdAt) }}</el-descriptions-item>
          </el-descriptions>

          <div class="detail-actions">
            <template v-if="detail.order.status === 'PENDING'">
              <el-button type="success" :loading="acting" @click="runAction(detail.order, 'confirm')">确认订单</el-button>
              <el-button type="danger" plain :loading="acting" @click="runAction(detail.order, 'reject')">拒绝订单</el-button>
            </template>
            <el-button
              v-else-if="detail.order.status === 'CONFIRMED'"
              type="primary"
              :loading="acting"
              @click="runAction(detail.order, 'check-in')"
            >
              办理入住
            </el-button>
            <el-button
              v-else-if="detail.order.status === 'CHECKED_IN'"
              type="primary"
              :loading="acting"
              @click="runAction(detail.order, 'complete')"
            >
              完成订单
            </el-button>
          </div>

          <h4 class="detail-title">状态流转记录</h4>
          <el-timeline v-if="detail.logs?.length">
            <el-timeline-item
              v-for="(log, index) in detail.logs"
              :key="index"
              :timestamp="formatDateTime(log.createdAt)"
              placement="top"
            >
              <p class="log-line">
                {{ log.fromStatusText ? `${log.fromStatusText} → ` : '' }}{{ log.toStatusText }}
              </p>
              <p v-if="log.remark" class="admin-hint">{{ log.remark }}</p>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无流转记录" :image-size="80" />
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.order-detail {
  min-height: 240px;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.detail-head h3 {
  margin: 4px 0 0;
  font-size: 19px;
  letter-spacing: 0.5px;
}

.detail-actions {
  display: flex;
  gap: 12px;
  margin: 20px 0 8px;
}

.detail-title {
  margin: 26px 0 16px;
  padding-top: 18px;
  border-top: 1px dashed var(--line);
  font-size: 15px;
  letter-spacing: 1px;
}

.log-line {
  margin: 0 0 4px;
  font-size: 13.5px;
  color: var(--ink);
}
</style>

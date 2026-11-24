<script setup>
/**
 * 日历库存：按房型、按月维护每一天的价格与可售房量。
 * 三种修改方式：
 * 1) 生成日历：为一段日期创建日历记录（已存在跳过）；
 * 2) 区间批量设置：对一段已生成日期统一改价格/库存/开关房；
 * 3) 单日修改：点日历格子精确改某一天。
 */
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { adminCalendarApi, adminRoomTypeApi } from '../../api'
import { addDays, formatDate, today } from '../../utils/date'

const roomTypes = ref([])
const selectedTypeId = ref(null)
const calendarDate = ref(new Date())
const days = ref([])
const loading = ref(false)
// 已加载的月份区间（from~to）；用于判断是否真的需要重新请求
let loadedKey = ''

// 日期字符串 -> 日历记录，便于按格取当天价格与库存
const dayMap = computed(() => {
  const map = {}
  for (const day of days.value) {
    map[day.stayDate] = day
  }
  return map
})

// 当前月概览：总数、关房天数、售罄天数（status 0 关房 / 1 开房；开房且库存为 0 即售罄）
const monthSummary = computed(() => {
  const total = days.value.length
  const closed = days.value.filter((d) => d.status === 0).length
  const soldOut = days.value.filter((d) => d.status === 1 && d.stock === 0).length
  return { total, closed, soldOut }
})

const monthLabel = computed(() => {
  const d = calendarDate.value instanceof Date ? calendarDate.value : new Date(calendarDate.value)
  return `${d.getFullYear()} 年 ${d.getMonth() + 1} 月`
})

// 取某月首尾日期，同时生成用于去重的 key
function monthBounds(date) {
  const d = date instanceof Date ? date : new Date(date)
  const start = new Date(d.getFullYear(), d.getMonth(), 1)
  const end = new Date(d.getFullYear(), d.getMonth() + 1, 0)
  return { from: formatDate(start), to: formatDate(end), key: `${formatDate(start)}~${formatDate(end)}` }
}

async function loadRoomTypes() {
  try {
    // 房型数量少，一次取 100 条直接作为下拉选项
    const data = await adminRoomTypeApi.page({ page: 1, size: 100 })
    roomTypes.value = data.records || []
    if (roomTypes.value.length) {
      selectedTypeId.value = roomTypes.value[0].id
    }
  } catch {
    roomTypes.value = []
  }
}

async function loadCalendar(force = false) {
  if (!selectedTypeId.value) {
    days.value = []
    return
  }
  const { from, to, key } = monthBounds(calendarDate.value)
  // 同一月份已加载过就跳过；force 用于修改数据后强制刷新
  if (!force && key === loadedKey) return

  loading.value = true
  try {
    days.value = await adminCalendarApi.list(selectedTypeId.value, { from, to })
    loadedKey = key
  } catch {
    days.value = []
  } finally {
    loading.value = false
  }
}

// 切换房型时清掉缓存 key 并强制刷新
watch(selectedTypeId, () => {
  loadedKey = ''
  loadCalendar(true)
})

// 切换月份走正常加载（同月会命中缓存直接返回）
watch(calendarDate, () => {
  loadCalendar()
})

/* ---------------- 生成日历 ---------------- */
const generateVisible = ref(false)
const generating = ref(false)
const generateForm = reactive({ range: [] })

function openGenerate() {
  // 默认生成今天起 30 天，符合房客端详情页展示的未来 30 天范围
  generateForm.range = [formatDate(today()), formatDate(addDays(today(), 29))]
  generateVisible.value = true
}

function disabledPast(date) {
  return date.getTime() < today().getTime()
}

async function submitGenerate() {
  if (!generateForm.range || generateForm.range.length !== 2) {
    ElMessage.warning('请选择生成范围')
    return
  }
  generating.value = true
  try {
    const count = await adminCalendarApi.generate(selectedTypeId.value, {
      startDate: generateForm.range[0],
      endDate: generateForm.range[1]
    })
    ElMessage.success(`已新增 ${count} 天日历（已存在的日期自动跳过）`)
    generateVisible.value = false
    // 生成范围若跨月：当前月直接刷新，否则把日历切到起始月（watch 会触发加载）
    const target = new Date(`${generateForm.range[0]}T00:00:00`)
    if (monthBounds(target).key === monthBounds(calendarDate.value).key) {
      loadCalendar(true)
    } else {
      calendarDate.value = target
    }
  } catch {
    /* 错误提示由 http 拦截器统一处理 */
  } finally {
    generating.value = false
  }
}

/* ---------------- 区间批量设置 ---------------- */
const batchVisible = ref(false)
const batching = ref(false)
const batchForm = reactive({ range: [], price: null, stock: null, status: null })

function openBatch() {
  // 默认作用范围为当前查看的整月
  const { from, to } = monthBounds(calendarDate.value)
  batchForm.range = [from, to]
  batchForm.price = null
  batchForm.stock = null
  batchForm.status = null
  batchVisible.value = true
}

async function submitBatch() {
  if (!batchForm.range || batchForm.range.length !== 2) {
    ElMessage.warning('请选择生效范围')
    return
  }
  // 三项都为空等于没有要修改的内容，直接拦截
  if (batchForm.price === null && batchForm.stock === null && batchForm.status === null) {
    ElMessage.warning('请至少填写价格、库存或开关房中的一项')
    return
  }
  batching.value = true
  try {
    const count = await adminCalendarApi.batchUpdate(selectedTypeId.value, {
      fromDate: batchForm.range[0],
      toDate: batchForm.range[1],
      // 只有填写过的字段才提交，留空表示该项不修改
      price: batchForm.price ?? undefined,
      stock: batchForm.stock ?? undefined,
      status: batchForm.status ?? undefined
    })
    ElMessage.success(`已更新 ${count} 天日历`)
    batchVisible.value = false
    loadCalendar(true)
  } catch {
    /* 错误提示由 http 拦截器统一处理 */
  } finally {
    batching.value = false
  }
}

/* ---------------- 单日修改 ---------------- */
const dayVisible = ref(false)
const savingDay = ref(false)
const dayForm = reactive({ id: null, date: '', price: 0, stock: 0, status: 1 })

// 取某个格子对应的记录；非本月（上/下月补白格）不处理
function dayInfo(data) {
  if (data.type !== 'current-month') return null
  return dayMap.value[data.day] || null
}

function openDay(data) {
  const info = dayInfo(data)
  if (!info) {
    // 本月格子但查不到记录，说明该天还没生成日历，引导用户先去生成
    if (data.type === 'current-month') {
      ElMessage.info('该日期还没有生成日历，请先使用「生成日历」')
    }
    return
  }
  Object.assign(dayForm, {
    id: info.id,
    date: info.stayDate,
    price: Number(info.price),
    stock: info.stock,
    status: info.status
  })
  dayVisible.value = true
}

async function saveDay() {
  savingDay.value = true
  try {
    // 单日修改必须带日历记录 id，后端按 id 更新这一天
    await adminCalendarApi.update(dayForm.id, {
      price: dayForm.price,
      stock: dayForm.stock,
      status: dayForm.status
    })
    ElMessage.success(`${dayForm.date} 的日历已更新`)
    dayVisible.value = false
    loadCalendar(true)
  } catch {
    /* 错误提示由 http 拦截器统一处理 */
  } finally {
    savingDay.value = false
  }
}

// 格子配色：未生成 / 关房 / 售罄 / 正常售卖
function cellClass(data) {
  const info = dayInfo(data)
  if (!info) return 'is-empty'
  if (info.status === 0) return 'is-closed'
  if (info.stock === 0) return 'is-sold-out'
  return 'is-open'
}

onMounted(loadRoomTypes)
</script>

<template>
  <div>
    <div v-if="!roomTypes.length" class="admin-card">
      <el-empty description="还没有房型，请先创建房型后再设置日历" />
    </div>

    <template v-else>
      <div class="admin-card">
        <div class="admin-toolbar">
          <el-select v-model="selectedTypeId" placeholder="选择房型">
            <el-option v-for="type in roomTypes" :key="type.id" :label="type.name" :value="type.id" />
          </el-select>
          <span class="admin-hint">
            {{ monthLabel }}已生成 {{ monthSummary.total }} 天，关房 {{ monthSummary.closed }} 天，售罄 {{ monthSummary.soldOut }} 天
          </span>
          <el-button class="grow" @click="openGenerate">生成日历</el-button>
          <el-button type="primary" plain @click="openBatch">区间批量设置</el-button>
        </div>

        <div v-loading="loading" class="cal-wrap">
          <el-calendar v-model="calendarDate">
            <template #date-cell="{ data }">
              <div class="cal-cell" :class="cellClass(data)" @click="openDay(data)">
                <span class="cal-day">{{ data.day.split('-')[2] }}</span>
                <template v-if="dayInfo(data)">
                  <span class="cal-price">¥{{ Number(dayInfo(data).price).toFixed(0) }}</span>
                  <span class="cal-stock">
                    {{ dayInfo(data).status === 0 ? '已关房' : `余 ${dayInfo(data).stock} 间` }}
                  </span>
                </template>
                <span v-else class="cal-none">{{ data.type === 'current-month' ? '未生成' : '' }}</span>
              </div>
            </template>
          </el-calendar>
        </div>

        <div class="cal-legend">
          <span><i class="dot open" />正常售卖</span>
          <span><i class="dot sold-out" />已售罄</span>
          <span><i class="dot closed" />已关房</span>
          <span><i class="dot empty" />未生成</span>
          <span class="admin-hint">点击日期可修改单日价格、库存与开关房</span>
        </div>
      </div>
    </template>

    <el-dialog v-model="generateVisible" title="生成日历" width="460px">
      <el-form label-width="92px">
        <el-form-item label="生成范围">
          <el-date-picker
            v-model="generateForm.range"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            :disabled-date="disabledPast"
          />
        </el-form-item>
      </el-form>
      <p class="admin-hint">
        价格为房型基准价，库存为当前可用房间数；已存在的日期会跳过，最多一次生成 366 天。
      </p>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" :loading="generating" @click="submitGenerate">开始生成</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchVisible" title="区间批量设置" width="520px">
      <el-form label-width="92px">
        <el-form-item label="生效范围">
          <el-date-picker
            v-model="batchForm.range"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
          />
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="batchForm.price" :min="0.01" :precision="2" :step="10" controls-position="right" placeholder="不修改" />
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number v-model="batchForm.stock" :min="0" :max="999" controls-position="right" placeholder="不修改" />
        </el-form-item>
        <el-form-item label="开关房">
          <el-select v-model="batchForm.status" placeholder="不修改" clearable style="width: 100%">
            <el-option label="开房（可预订）" :value="1" />
            <el-option label="关房（不可预订）" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
      <p class="admin-hint">留空的项不会修改；批量设置只影响已经生成的日期。</p>
      <template #footer>
        <el-button @click="batchVisible = false">取消</el-button>
        <el-button type="primary" :loading="batching" @click="submitBatch">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dayVisible" :title="`修改 ${dayForm.date} 的日历`" width="460px">
      <el-form label-width="92px">
        <el-form-item label="价格">
          <el-input-number v-model="dayForm.price" :min="0.01" :precision="2" :step="10" controls-position="right" />
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number v-model="dayForm.stock" :min="0" :max="999" controls-position="right" />
        </el-form-item>
        <el-form-item label="是否开房">
          <el-switch v-model="dayForm.status" :active-value="1" :inactive-value="0" active-text="开房" inactive-text="关房" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dayVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingDay" @click="saveDay">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.cal-wrap {
  min-height: 320px;
}

.cal-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  height: 100%;
  padding: 4px 2px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.cal-cell:hover {
  background: rgba(47, 107, 95, 0.08);
}

.cal-day {
  font-size: 13px;
  color: var(--ink);
}

.cal-price {
  font-size: 12px;
  font-weight: 600;
  color: var(--brand);
}

.cal-stock {
  font-size: 11px;
  color: var(--muted);
}

.cal-none {
  font-size: 11px;
  color: #b7b2a7;
}

.cal-cell.is-closed {
  background: rgba(192, 133, 82, 0.1);
}

.cal-cell.is-closed .cal-stock {
  color: var(--accent);
}

.cal-cell.is-sold-out {
  background: rgba(220, 80, 80, 0.07);
}

.cal-cell.is-sold-out .cal-stock {
  color: #d05a5a;
}

.cal-legend {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
  padding-top: 14px;
  margin-top: 8px;
  border-top: 1px dashed var(--line);
  font-size: 12.5px;
  color: var(--muted);
}

.cal-legend span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.cal-legend .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.dot.open {
  background: #4f8b7d;
}

.dot.sold-out {
  background: #d05a5a;
}

.dot.closed {
  background: var(--accent);
}

.dot.empty {
  background: #c9c4b9;
}

.cal-wrap :deep(.el-calendar-table td) {
  border-color: var(--line);
}

.cal-wrap :deep(.el-calendar-table td.is-selected) {
  background: transparent;
}

.cal-wrap :deep(.el-calendar-table .el-calendar-day) {
  height: 82px;
  padding: 4px;
}

.cal-wrap :deep(.el-calendar-table td.is-selected .cal-cell) {
  outline: 2px solid var(--brand);
  outline-offset: -2px;
}

.cal-wrap :deep(.el-calendar__header) {
  padding: 8px 0 16px;
}
</style>

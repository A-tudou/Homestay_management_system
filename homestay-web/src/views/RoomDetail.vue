<script setup>
/**
 * 房型详情页：展示房价区间与未来 30 天房态，右侧面板选择日期后试算整单价格并下单。
 * 试算走 quote 接口，只有 available 为 true 才允许进入联系信息弹窗。
 */
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { auth } from '../composables/useAuth'
import { orderApi, roomApi } from '../api'
import { addDays, formatDate, nightsBetween, today, weekday } from '../utils/date'

const route = useRoute()
const router = useRouter()
const id = route.params.id

const loading = ref(true)
const room = ref(null)
const calendar = ref([])
const coverBroken = ref(false)

const booking = reactive({ range: [], roomCount: 1 })
const quote = ref(null)
const quoteLoading = ref(false)
// 报价请求序号：每发起一次自增，响应回来时序号不一致说明已被更新的请求覆盖，直接丢弃
let quoteSeq = 0

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref()
const contact = reactive({ name: '', phone: '', remark: '' })

const rules = {
  name: [{ required: true, message: '请填写入住人姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请填写联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ]
}

const priceText = computed(() => {
  if (!room.value) return ''
  const min = Number(room.value.minPrice)
  const max = Number(room.value.maxPrice)
  return min === max ? `¥${min}` : `¥${min} - ${max}`
})

function disabledDate(date) {
  return date.getTime() < today().getTime()
}

async function loadRoom() {
  // 详情加载失败（如房型已下架/不存在）直接回首页
  try {
    room.value = await roomApi.detail(id)
  } catch {
    router.replace('/')
    return
  } finally {
    loading.value = false
  }

  // 房态日历单独请求：区间为今天起 30 天（[今天, 今天+29] 共 30 个可订夜晚）
  try {
    calendar.value =
      (await roomApi.calendar(id, {
        from: formatDate(today()),
        to: formatDate(addDays(today(), 29))
      })) || []
  } catch {
    calendar.value = []
  }

  // 从列表/搜索页带过来的日期预填到预订面板；超出 1-30 晚的区间忽略
  const { checkIn, checkOut } = route.query
  if (checkIn && checkOut) {
    const nights = nightsBetween(String(checkIn), String(checkOut))
    if (nights > 0 && nights <= 30) {
      booking.range = [String(checkIn), String(checkOut)]
    }
  }
}

async function refreshQuote() {
  // 序号用于竞态处理：快速改日期时只采纳最后一次请求的结果
  const seq = ++quoteSeq
  quote.value = null

  const [checkIn, checkOut] = booking.range || []
  if (!checkIn || !checkOut) return

  // 半开区间下晚数须 >= 1，否则说明区间无效（如起止同一天）
  const nights = nightsBetween(checkIn, checkOut)
  if (nights < 1) return

  quoteLoading.value = true
  try {
    const data = await roomApi.quote(id, { checkIn, checkOut, roomCount: booking.roomCount })
    if (seq !== quoteSeq) return
    // 金额保留两位小数，避免浮点数出现 1998.9999999 之类的展示问题
    quote.value = {
      nights: data.nights,
      totalAmount: Math.round(Number(data.totalAmount) * 100) / 100,
      available: data.available,
      reason: data.reason
    }
  } catch {
    if (seq !== quoteSeq) return
    // 试算失败也给出明确不可订提示，而不是让按钮保持可点
    quote.value = {
      nights,
      totalAmount: 0,
      available: false,
      reason: '暂时无法核算价格，请稍后再试'
    }
  } finally {
    if (seq === quoteSeq) {
      quoteLoading.value = false
    }
  }
}

// 日期或房间数变化就重新试算；watch 数组里逐项监听，避免对象引用变化判断失效
watch(
  () => [booking.range?.[0], booking.range?.[1], booking.roomCount],
  refreshQuote
)

function openBooking() {
  if (!auth.isLoggedIn.value) {
    ElMessage.warning('预订前请先登录')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (!quote.value) {
    ElMessage.warning('请先选择入住日期')
    return
  }
  // 库存不足或日期不可订时，直接把后端给出的 reason 提示给用户
  if (!quote.value.available) {
    ElMessage.warning(quote.value.reason || '所选日期暂不可预订')
    return
  }
  // 联系人默认回填当前账号资料，用户仍可修改
  if (!contact.name) contact.name = auth.user.value?.nickname || ''
  if (!contact.phone) contact.phone = auth.user.value?.phone || ''
  dialogVisible.value = true
}

async function submitOrder() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const [checkIn, checkOut] = booking.range
    await orderApi.create({
      roomTypeId: Number(id),
      checkInDate: checkIn,
      checkOutDate: checkOut,
      roomCount: booking.roomCount,
      contactName: contact.name.trim(),
      contactPhone: contact.phone.trim(),
      remark: contact.remark.trim() || undefined
    })
    dialogVisible.value = false
    ElMessage.success('预订成功，等待房东确认')
    // 下单后跳到我的订单，便于查看待确认状态
    router.push('/orders')
  } finally {
    submitting.value = false
  }
}

onMounted(loadRoom)
</script>

<template>
  <div class="container detail-page" v-loading="loading">
    <div class="back-line">
      <el-button text @click="router.push('/')">
        <el-icon><ArrowLeft /></el-icon>
        返回房源列表
      </el-button>
    </div>

    <template v-if="room">
      <div class="surface-panel detail-top">
        <div class="detail-cover">
          <img
            v-if="room.coverImage && !coverBroken"
            :src="room.coverImage"
            :alt="room.name"
            @error="coverBroken = true"
          />
          <div v-else class="cover-fallback">
            <el-icon :size="52"><House /></el-icon>
          </div>
        </div>

        <div class="detail-info">
          <h1>{{ room.name }}</h1>
          <p class="desc">{{ room.description || '安静舒适的房间，等你来住上几晚。' }}</p>
          <ul class="facts">
            <li><span>宜住</span>{{ room.maxGuests }} 人</li>
            <li><span>床型</span>{{ room.bedType || '以现场为准' }}</li>
            <li><span>面积</span>{{ room.area ? `${room.area} ㎡` : '—' }}</li>
          </ul>
          <div class="price-line">
            <span class="amount">{{ priceText }}</span>
            <span class="unit">/ 晚（近 30 天价格区间）</span>
          </div>
        </div>
      </div>

      <div class="detail-grid">
        <div class="detail-main">
          <section class="surface-panel block">
            <h2 class="section-title">近期房价与余量</h2>
            <p class="hint">以下为未来 30 天已开放日期，余量为当晚房量；跨晚预订需要同一间房整段可用。</p>
            <div class="cal-grid">
              <div
                v-for="day in calendar"
                :key="day.date"
                class="cal-cell"
                :class="{ off: !day.available || day.remaining <= 0 }"
              >
                <span class="cal-date">{{ day.date.slice(5) }}</span>
                <span class="cal-week">{{ weekday(day.date) }}</span>
                <span class="cal-price">¥{{ day.price }}</span>
                <span class="cal-stock">{{ day.available ? `余 ${day.remaining}` : '不可订' }}</span>
              </div>
            </div>
            <el-empty v-if="!calendar.length" description="近期暂无开放日期" :image-size="80" />
          </section>

          <section class="surface-panel block">
            <h2 class="section-title">预订说明</h2>
            <ul class="policy">
              <li>入住时间为 14:00 之后，离店时间为次日 12:00 之前。</li>
              <li>每晚价格以日历实时价为准，提交订单后总价不再变动。</li>
              <li>订单需房东确认，确认前可在「我的订单」自行取消。</li>
              <li>单次最多预订 30 晚、10 间房。</li>
            </ul>
          </section>
        </div>

        <aside class="booking-panel surface-panel">
          <h2 class="section-title">预订这间房</h2>

          <div class="booking-field">
            <label>入住 / 离店</label>
            <el-date-picker
              v-model="booking.range"
              type="daterange"
              value-format="YYYY-MM-DD"
              range-separator="至"
              start-placeholder="入住日期"
              end-placeholder="离店日期"
              :disabled-date="disabledDate"
              size="large"
            />
          </div>

          <div class="booking-field">
            <label>房间数</label>
            <el-input-number v-model="booking.roomCount" :min="1" :max="10" size="large" />
          </div>

          <div v-if="quoteLoading" class="quote-loading">正在核算价格…</div>

          <div v-else-if="quote" class="quote">
            <div class="quote-row">
              <span>入住晚数</span>
              <strong>{{ quote.nights }} 晚</strong>
            </div>
            <div class="quote-row">
              <span>合计金额</span>
              <strong class="amount">¥{{ quote.totalAmount }}</strong>
            </div>
            <p v-if="!quote.available" class="quote-warn">{{ quote.reason }}</p>
          </div>

          <el-button
            type="primary"
            size="large"
            class="book-btn"
            :disabled="quoteLoading || (quote && !quote.available)"
            @click="openBooking"
          >
            立即预订
          </el-button>
          <p class="tip">提交后由房东确认，可在「我的订单」查看进度。</p>
        </aside>
      </div>
    </template>

    <el-dialog v-model="dialogVisible" title="填写联系信息" width="440px">
      <el-form ref="formRef" :model="contact" :rules="rules" label-position="top">
        <el-form-item label="入住人姓名" prop="name">
          <el-input v-model="contact.name" placeholder="请填写入住人姓名" maxlength="20" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="contact.phone" placeholder="用于接收确认信息" maxlength="11" />
        </el-form-item>
        <el-form-item label="备注（可选）" prop="remark">
          <el-input
            v-model="contact.remark"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="如预计到店时间、特殊需求"
          />
        </el-form-item>
      </el-form>

      <div v-if="quote" class="dialog-summary">
        <p>{{ room?.name }} · {{ booking.range[0] }} 至 {{ booking.range[1] }} · {{ quote.nights }} 晚 · {{ booking.roomCount }} 间</p>
        <strong>¥{{ quote.totalAmount }}</strong>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitOrder">确认预订</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.detail-page {
  padding-top: 8px;
}

.back-line {
  margin: 16px 0;
}

.detail-top {
  display: grid;
  grid-template-columns: 1.15fr 1fr;
  overflow: hidden;
}

.detail-cover {
  min-height: 340px;
  background: #e9e5dc;
}

.detail-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  max-height: 440px;
}

.detail-cover .cover-fallback {
  width: 100%;
  height: 100%;
  min-height: 340px;
  display: grid;
  place-items: center;
  color: rgba(47, 107, 95, 0.45);
  background: linear-gradient(135deg, #e4ece8, #f0ece3);
}

.detail-info {
  padding: 34px 36px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  justify-content: center;
}

.detail-info h1 {
  margin: 0;
  font-size: 30px;
  letter-spacing: 1px;
}

.detail-info .desc {
  color: var(--muted);
  font-size: 14px;
  line-height: 1.8;
}

.facts {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  list-style: none;
  padding: 0;
  margin: 0;
}

.facts li {
  padding: 8px 14px;
  border-radius: 10px;
  background: var(--brand-soft);
  color: var(--ink);
  font-size: 13px;
}

.facts li span {
  color: var(--muted);
  margin-right: 6px;
}

.price-line {
  margin-top: auto;
  padding-top: 10px;
}

.price-line .amount {
  font-size: 30px;
  font-weight: 700;
  color: var(--brand);
}

.price-line .unit {
  font-size: 12px;
  color: var(--muted);
  margin-left: 6px;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 24px;
  margin-top: 24px;
  align-items: start;
}

.block {
  padding: 26px 28px;
  margin-bottom: 24px;
}

.hint {
  color: var(--muted);
  font-size: 12.5px;
  margin: -8px 0 16px;
}

.cal-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(92px, 1fr));
  gap: 10px;
}

.cal-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid var(--line);
  background: #fbfaf7;
  font-size: 12px;
}

.cal-date {
  font-size: 14px;
  font-weight: 600;
}

.cal-week {
  color: var(--muted);
}

.cal-price {
  color: var(--brand);
  font-weight: 600;
  margin-top: 4px;
}

.cal-stock {
  color: var(--muted);
}

.cal-cell.off {
  background: #f3f1ec;
  color: #a8a29a;
}

.cal-cell.off .cal-price {
  color: #a8a29a;
}

.policy {
  margin: 0;
  padding-left: 18px;
  color: var(--muted);
  font-size: 13.5px;
  line-height: 2;
}

.booking-panel {
  padding: 26px 24px;
  position: sticky;
  top: 96px;
}

.booking-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.booking-field label {
  font-size: 12px;
  color: var(--muted);
  letter-spacing: 1px;
}

.booking-field :deep(.el-date-editor),
.booking-field :deep(.el-input-number) {
  width: 100%;
}

.quote {
  border-top: 1px dashed var(--line);
  padding-top: 14px;
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.quote-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13.5px;
  color: var(--muted);
}

.quote-row strong {
  color: var(--ink);
}

.quote-row .amount {
  color: var(--brand);
  font-size: 22px;
}

.quote-warn {
  color: #c45656;
  font-size: 12.5px;
}

.quote-loading {
  color: var(--muted);
  font-size: 13px;
  padding: 8px 0 16px;
}

.book-btn {
  width: 100%;
}

.tip {
  margin-top: 12px;
  font-size: 12px;
  color: var(--muted);
  text-align: center;
  line-height: 1.6;
}

.dialog-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 12px;
  background: var(--brand-soft);
  font-size: 12.5px;
  color: var(--muted);
}

.dialog-summary strong {
  color: var(--brand);
  font-size: 18px;
  white-space: nowrap;
}

@media (max-width: 960px) {
  .detail-top {
    grid-template-columns: 1fr;
  }

  .detail-info {
    padding: 24px;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }

  .booking-panel {
    position: static;
  }
}
</style>

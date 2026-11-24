<script setup>
/**
 * 搜索空房页：选择入住区间、人数与房间数后查询可订房型。
 * 搜索条件同步到 URL query，刷新页面或从详情页返回时可以直接复现上次查询。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import RoomCard from '../components/RoomCard.vue'
import { roomApi } from '../api'
import { today } from '../utils/date'

const route = useRoute()
const router = useRouter()

const form = reactive({
  range: [],
  guests: 2,
  roomCount: 1
})

const loading = ref(false)
const searched = ref(false)
const results = ref([])

// 表单区间转成接口与卡片使用的 { checkIn, checkOut }；未选完整返回 null
const dates = computed(() => {
  if (!form.range || form.range.length !== 2) return null
  return { checkIn: form.range[0], checkOut: form.range[1] }
})

// 结果摘要：晚数取首条结果（同一次查询所有房型晚数相同），无结果时显示 0 晚
const summary = computed(() => {
  if (!searched.value || !dates.value) return ''
  const nights = results.value[0]?.nights || 0
  return `${dates.value.checkIn} 至 ${dates.value.checkOut} · ${nights} 晚 · ${form.guests} 位 · ${form.roomCount} 间，找到 ${results.value.length} 个房型`
})

// 禁选今天之前的日期，避免查询过去区间
function disabledDate(date) {
  return date.getTime() < today().getTime()
}

async function runSearch() {
  if (!form.range || form.range.length !== 2) {
    ElMessage.warning('请先选择入住和离店日期')
    return
  }

  // 每次搜索都覆盖 query（replace 不新增历史记录），保证快照与当前结果一致
  router.replace({
    path: '/search',
    query: {
      checkIn: form.range[0],
      checkOut: form.range[1],
      guests: form.guests,
      roomCount: form.roomCount
    }
  })

  loading.value = true
  searched.value = true
  try {
    results.value =
      (await roomApi.search({
        checkIn: form.range[0],
        checkOut: form.range[1],
        guests: form.guests,
        roomCount: form.roomCount
      })) || []
  } catch {
    // 搜索失败时清空结果，错误提示由请求拦截器统一弹出
    results.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  // URL 带日期时回填表单并自动搜索，支持分享链接/刷新/从详情页返回
  const { checkIn, checkOut, guests, roomCount } = route.query
  if (checkIn && checkOut) {
    form.range = [String(checkIn), String(checkOut)]
    form.guests = Number(guests) || 2
    form.roomCount = Number(roomCount) || 1
    runSearch()
  }
})
</script>

<template>
  <div class="container">
    <div class="page-head">
      <div>
        <h1>搜索空房</h1>
        <p>选择入住区间，实时查询可订房型与整单价格。</p>
      </div>
    </div>

    <div class="surface-panel search-form">
      <div class="search-item date-item">
        <label>入住 / 离店</label>
        <el-date-picker
          v-model="form.range"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="入住日期"
          end-placeholder="离店日期"
          :disabled-date="disabledDate"
          size="large"
        />
      </div>
      <div class="search-item">
        <label>入住人数</label>
        <el-select v-model="form.guests" size="large">
          <el-option v-for="n in 8" :key="n" :label="`${n} 人`" :value="n" />
        </el-select>
      </div>
      <div class="search-item">
        <label>房间数</label>
        <el-select v-model="form.roomCount" size="large">
          <el-option v-for="n in 5" :key="n" :label="`${n} 间`" :value="n" />
        </el-select>
      </div>
      <el-button type="primary" size="large" class="search-btn" :loading="loading" @click="runSearch">
        搜索空房
      </el-button>
    </div>

    <template v-if="searched">
      <div class="result-bar">
        <p>{{ summary }}</p>
      </div>

      <div v-loading="loading" class="room-grid">
        <RoomCard
          v-for="item in results"
          :key="item.id"
          :room="item"
          :result="item"
          :dates="dates"
        />
      </div>

      <div v-if="!loading && !results.length" class="empty-wrap">
        <el-empty description="所选日期暂无可订房型，试试调整日期或人数" />
      </div>
    </template>

    <div v-else class="surface-panel empty-intro">
      <el-icon :size="34"><Calendar /></el-icon>
      <h3>先选一段想出发的日子</h3>
      <p>系统会按每晚真实价格与余量，为你算出整单总价。</p>
    </div>
  </div>
</template>

<style scoped>
.search-form {
  display: grid;
  grid-template-columns: minmax(300px, 2fr) 1fr 1fr auto;
  gap: 14px;
  align-items: end;
  padding: 20px;
  margin-bottom: 28px;
}

.search-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.search-item label {
  font-size: 12px;
  color: var(--muted);
  letter-spacing: 1px;
}

.search-item :deep(.el-select),
.search-item :deep(.el-date-editor) {
  width: 100%;
}

.search-btn {
  height: 40px;
  padding: 0 26px;
}

.result-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
  color: var(--muted);
  font-size: 14px;
}

.empty-intro {
  padding: 60px 24px;
  text-align: center;
  color: var(--muted);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.empty-intro h3 {
  margin: 6px 0 0;
  color: var(--ink);
  font-size: 18px;
}

.empty-intro p {
  font-size: 13px;
}

@media (max-width: 960px) {
  .search-form {
    grid-template-columns: 1fr 1fr;
  }

  .date-item {
    grid-column: 1 / -1;
  }

  .search-btn {
    grid-column: 1 / -1;
  }
}
</style>

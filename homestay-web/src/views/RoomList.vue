<script setup>
/**
 * 房源首页：展示房型分页列表，带关键词/人数筛选；顶部搜索卡选择日期后跳转到搜索页。
 */
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import RoomCard from '../components/RoomCard.vue'
import { roomApi } from '../api'
import { today } from '../utils/date'

const router = useRouter()

const loading = ref(false)
const rooms = ref([])
const total = ref(0)
const page = ref(1)
// 固定每页 9 条，与三列栅格保持整行
const size = 9
const keyword = ref('')
const guests = ref(null)

// 顶部大图搜索卡的条件，只在点击搜索时校验并跳转
const hero = reactive({
  range: [],
  guests: 2,
  roomCount: 1
})

const heroImage = '/images/hero-home.jpg'

// 禁选今天之前的日期
function disabledDate(date) {
  return date.getTime() < today().getTime()
}

// 统一的分页加载：空关键词/人数不传，避免后端把空串当作筛选条件
async function load() {
  loading.value = true
  try {
    const data = await roomApi.list({
      page: page.value,
      size,
      keyword: keyword.value.trim() || undefined,
      guests: guests.value || undefined
    })
    rooms.value = data.records || []
    total.value = Number(data.total || 0)
  } catch {
    rooms.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 筛选条件变化后必须回到第一页，否则可能停在不存在的页码上
function applyFilter() {
  page.value = 1
  load()
}

function goSearch() {
  if (!hero.range || hero.range.length !== 2) {
    ElMessage.warning('请先选择入住和离店日期')
    return
  }
  // 带日期跳到搜索页，由搜索页按区间查可订房型并展示整单报价
  router.push({
    path: '/search',
    query: {
      checkIn: hero.range[0],
      checkOut: hero.range[1],
      guests: hero.guests,
      roomCount: hero.roomCount
    }
  })
}

onMounted(load)
</script>

<template>
  <div class="home">
    <section class="hero container" :style="{ backgroundImage: `linear-gradient(115deg, rgba(20, 34, 30, 0.82) 0%, rgba(20, 34, 30, 0.35) 55%, rgba(20, 34, 30, 0.15) 100%), url(${heroImage})` }">
      <div class="hero-content">
        <p class="eyebrow">山野之间 · 好好住下</p>
        <h1>把周末，交给山风与晚霞</h1>
        <p class="hero-sub">精选山景与庭院房型，实时房价与库存，线上预订即时确认。</p>
      </div>

      <div class="search-card">
        <div class="search-item date-item">
          <label>入住 / 离店</label>
          <el-date-picker
            v-model="hero.range"
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
          <el-select v-model="hero.guests" size="large">
            <el-option v-for="n in 8" :key="n" :label="`${n} 人`" :value="n" />
          </el-select>
        </div>
        <div class="search-item">
          <label>房间数</label>
          <el-select v-model="hero.roomCount" size="large">
            <el-option v-for="n in 5" :key="n" :label="`${n} 间`" :value="n" />
          </el-select>
        </div>
        <el-button type="primary" size="large" class="search-btn" @click="goSearch">
          搜索空房
        </el-button>
      </div>
    </section>

    <section class="container">
      <div class="page-head">
        <div>
          <h1>精选房源</h1>
          <p>共 {{ total }} 个房型可预订</p>
        </div>
        <div class="filters">
          <el-input
            v-model="keyword"
            placeholder="搜索房型名称"
            clearable
            size="large"
            @keyup.enter="applyFilter"
            @clear="applyFilter"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-select v-model="guests" placeholder="入住人数" clearable size="large" class="guests-select" @change="applyFilter">
            <el-option v-for="n in 6" :key="n" :label="`${n} 人`" :value="n" />
          </el-select>
          <el-button type="primary" size="large" @click="applyFilter">筛选</el-button>
        </div>
      </div>

      <div v-loading="loading" class="room-grid room-grid-list">
        <RoomCard v-for="room in rooms" :key="room.id" :room="room" />
      </div>

      <div v-if="!loading && !rooms.length" class="empty-wrap">
        <el-empty description="没有找到符合条件的房源" />
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
    </section>
  </div>
</template>

<style scoped>
.hero {
  position: relative;
  margin-top: 24px;
  min-height: 460px;
  border-radius: 24px;
  padding: 48px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 40px;
  background-size: cover;
  background-position: center;
  overflow: hidden;
}

.hero-content {
  max-width: 560px;
  color: #fff;
}

.eyebrow {
  letter-spacing: 4px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.75);
  margin-bottom: 14px;
}

.hero-content h1 {
  margin: 0;
  font-size: 42px;
  line-height: 1.25;
  letter-spacing: 2px;
  font-family: 'Songti SC', 'STSong', serif;
  text-shadow: 0 6px 24px rgba(0, 0, 0, 0.25);
}

.hero-sub {
  margin-top: 16px;
  font-size: 15px;
  color: rgba(255, 255, 255, 0.82);
}

.search-card {
  display: grid;
  grid-template-columns: minmax(280px, 2fr) 1fr 1fr auto;
  gap: 14px;
  align-items: end;
  padding: 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 22px 50px -26px rgba(15, 26, 23, 0.65);
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

.filters {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.filters :deep(.el-input) {
  width: 220px;
}

.guests-select {
  width: 130px;
}

.room-grid-list {
  min-height: 120px;
}

@media (max-width: 960px) {
  .hero {
    padding: 32px;
    min-height: 420px;
  }

  .hero-content h1 {
    font-size: 32px;
  }

  .search-card {
    grid-template-columns: 1fr 1fr;
  }

  .date-item {
    grid-column: 1 / -1;
  }

  .search-btn {
    grid-column: 1 / -1;
  }
}

@media (max-width: 720px) {
  .hero {
    padding: 24px;
    min-height: 380px;
  }

  .hero-content h1 {
    font-size: 26px;
  }

  .filters :deep(.el-input) {
    width: 100%;
  }

  .filters {
    width: 100%;
  }

  .filters .el-button {
    flex: 1;
  }
}
</style>

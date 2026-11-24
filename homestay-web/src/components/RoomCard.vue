<script setup>
/**
 * 房型卡片：房客端首页与搜索结果共用。
 * 传入 result（搜索接口的整单报价）时展示余量、总价和「立即预订」，否则展示价格区间与「查看详情」。
 */
import { computed, ref } from 'vue'
import { House } from '@element-plus/icons-vue'

const props = defineProps({
  // 房型数据：id、name、coverImage、description、maxGuests、bedType、area、minPrice、maxPrice
  room: { type: Object, required: true },
  // 搜索时的日期区间 { checkIn, checkOut }，仅用于透传到详情页，卡片本身不改日期
  dates: { type: Object, default: null },
  // 搜索结果项：额外带 remaining（剩余房量）、nights、roomCount、totalAmount
  result: { type: Object, default: null }
})

// 封面图加载失败时置为 true，回退到占位图标
const broken = ref(false)

const image = computed(() => (!broken.value && props.room.coverImage ? props.room.coverImage : ''))

// 卡片所有入口统一跳到房型详情；带日期时把区间放进 query，详情页可预填预订面板
const link = computed(() => ({
  name: 'room-detail',
  params: { id: props.room.id },
  query: props.dates
    ? { checkIn: props.dates.checkIn, checkOut: props.dates.checkOut }
    : {}
}))

// 非搜索态的价格文案：最低价缺失显示「价格待定」，最高价缺失或相等时只显示一个价格
const priceText = computed(() => {
  const min = Number(props.room.minPrice)
  const max = Number(props.room.maxPrice)
  if (!Number.isFinite(min)) return '价格待定'
  if (!Number.isFinite(max) || min === max) return `¥${min}`
  return `¥${min} - ${max}`
})
</script>

<template>
  <article class="room-card">
    <router-link :to="link" class="cover">
      <img v-if="image" :src="image" :alt="room.name" loading="lazy" @error="broken = true" />
      <div v-else class="cover-fallback">
        <el-icon :size="42"><House /></el-icon>
      </div>
      <span v-if="result" class="cover-badge">余 {{ result.remaining }} 间</span>
    </router-link>

    <div class="body">
      <h3>
        <router-link :to="link">{{ room.name }}</router-link>
      </h3>
      <p class="desc">{{ room.description || '安静舒适的房间，等你来住上几晚。' }}</p>
      <ul class="meta">
        <li>宜住 {{ room.maxGuests }} 人</li>
        <li v-if="room.bedType">{{ room.bedType }}</li>
        <li v-if="room.area">{{ room.area }} ㎡</li>
      </ul>
      <div class="footer">
        <div class="price">
          <template v-if="result">
            <span class="amount">¥{{ result.totalAmount }}</span>
            <span class="unit">/ {{ result.nights }} 晚 · {{ result.roomCount }} 间</span>
          </template>
          <template v-else>
            <span class="amount">{{ priceText }}</span>
            <span class="unit">/ 晚</span>
          </template>
        </div>
        <el-button type="primary" round @click="$router.push(link)">
          {{ result ? '立即预订' : '查看详情' }}
        </el-button>
      </div>
    </div>
  </article>
</template>

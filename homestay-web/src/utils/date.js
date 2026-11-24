/**
 * 日期工具：统一以「本地时区的 YYYY-MM-DD 字符串」作为与后端交互的日期格式。
 * 预订区间为半开区间 [入住日, 离店日)，因此晚数 = 离店日 - 入住日，
 * 离店当天不计费也不占房（相当于退房）。
 */
const WEEKDAYS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

// Date 或可解析的日期字符串 -> 'YYYY-MM-DD'（本地时区，不走 UTC，避免跨时区偏移一天）
export function formatDate(value) {
  const date = value instanceof Date ? value : new Date(value)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

// 今天 0 点：用于日期选择器禁选过去日期、以及默认查询/生成区间的起点
export function today() {
  const date = new Date()
  date.setHours(0, 0, 0, 0)
  return date
}

// 在给定日期上加减天数，返回 Date；用于「未来 30 天」等区间的右端点
export function addDays(value, days) {
  const date = value instanceof Date ? new Date(value) : new Date(`${value}T00:00:00`)
  date.setDate(date.getDate() + days)
  return date
}

// 计算两日期间隔的晚数：半开区间下即两个日期之差，用于计价与「最多 30 晚」等校验
export function nightsBetween(checkIn, checkOut) {
  const start = new Date(`${checkIn}T00:00:00`)
  const end = new Date(`${checkOut}T00:00:00`)
  return Math.round((end.getTime() - start.getTime()) / 86400000)
}

// 日期字符串 -> 中文星期，用于房态日历的单元格展示
export function weekday(dateStr) {
  return WEEKDAYS[new Date(`${dateStr}T00:00:00`).getDay()]
}

// 后端 ISO 时间（如 2026-11-22T09:30:00）-> 'YYYY-MM-DD HH:mm'，空值返回空串
export function formatDateTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

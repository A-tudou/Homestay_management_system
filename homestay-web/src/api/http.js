import axios from 'axios'
import { ElMessage } from 'element-plus'
import { auth } from '../composables/useAuth'

// 开发环境由 Vite 将 /api 代理到 8080，业务代码无需写死后端地址
const http = axios.create({
  baseURL: '/api',
  timeout: 15000
})

http.interceptors.request.use((config) => {
  // 每次请求实时读取 token，登录或退出后无需重建 axios 实例
  if (auth.token.value) {
    config.headers.Authorization = `Bearer ${auth.token.value}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    // 后端统一返回 { code, message, data }，这里解包 data，页面只处理业务数据
    const body = response.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) {
        return body.data
      }
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body
  },
  async (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message || error.message || '网络异常，请稍后再试'

    if (status === 401) {
      // 动态引入 router，避免 http -> router -> api -> http 的循环依赖
      auth.clear()
      ElMessage.warning('登录状态已失效，请重新登录')
      const { default: router } = await import('../router')
      const current = router.currentRoute.value
      // 记录原地址，重新登录后可返回之前访问的页面
      if (!current.path.startsWith('/login')) {
        router.push({ path: '/login', query: { redirect: current.fullPath } })
      }
    } else {
      ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)

export default http

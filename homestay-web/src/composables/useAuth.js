import { computed, reactive } from 'vue'

const TOKEN_KEY = 'homestay_token'
const USER_KEY = 'homestay_user'

function readUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    // 本地用户数据损坏时按未登录处理，避免阻断应用启动
    return null
  }
}

// 登录状态持久化到 localStorage，刷新页面后仍可恢复会话
const state = reactive({
  token: localStorage.getItem(TOKEN_KEY) || '',
  user: readUser()
})

export const auth = {
  token: computed(() => state.token),
  user: computed(() => state.user),
  isLoggedIn: computed(() => !!state.token),
  setSession(token, user) {
    state.token = token
    state.user = user || null
    localStorage.setItem(TOKEN_KEY, token)
    if (user) {
      localStorage.setItem(USER_KEY, JSON.stringify(user))
    } else {
      localStorage.removeItem(USER_KEY)
    }
  },
  setUser(user) {
    state.user = user
    if (user) {
      localStorage.setItem(USER_KEY, JSON.stringify(user))
    }
  },
  clear() {
    state.token = ''
    state.user = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }
}

export function isAuthenticated() {
  return !!state.token
}

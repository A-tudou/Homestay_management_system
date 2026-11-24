<script setup>
/**
 * 登录 / 注册页：同一页面用页签切换两种表单，注册成功后自动登录。
 * 登录成功仅按 redirect 参数回跳，角色分流不在这里做（房东从顶部菜单进入管理台，路由守卫再校验角色）。
 */
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '../api'
import { auth } from '../composables/useAuth'

const route = useRoute()
const router = useRouter()

// 默认页签由 query.tab 决定，顶部「注册」按钮会带 tab=register 进来
const activeTab = ref(route.query.tab === 'register' ? 'register' : 'login')
const loading = ref(false)

const loginFormRef = ref()
const registerFormRef = ref()

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', confirm: '', nickname: '', phone: '' })

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]{4,20}$/, message: '4-20 位字母、数字或下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需在 6-32 位之间', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      // 自定义校验：确认密码必须与 password 实时一致
      validator: (rule, value, callback) => {
        if (value === registerForm.password) callback()
        else callback(new Error('两次输入的密码不一致'))
      },
      trigger: 'blur'
    }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 20, message: '昵称不能超过 20 个字符', trigger: 'blur' }
  ],
  phone: [
    {
      // 手机号选填：留空直接通过，填写则校验大陆手机号格式
      validator: (rule, value, callback) => {
        if (!value || /^1[3-9]\d{9}$/.test(value)) callback()
        else callback(new Error('手机号格式不正确'))
      },
      trigger: 'blur'
    }
  ]
}

const visual = '/images/hero-login.jpg'

// 只接受站内绝对路径，防止 redirect 参数被构造成外部地址（开放重定向）
function redirectTarget() {
  const redirect = route.query.redirect
  return typeof redirect === 'string' && redirect.startsWith('/') ? redirect : '/'
}

async function submitLogin() {
  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const data = await authApi.login({
      username: loginForm.username.trim(),
      password: loginForm.password
    })
    // 保存 token 与用户资料（含 role），刷新页面后从 localStorage 恢复
    auth.setSession(data.token, data.user)
    ElMessage.success(`欢迎回来，${data.user?.nickname || data.user?.username}`)
    // 用 replace 而非 push，避免登录页留在历史记录里被回退
    router.replace(redirectTarget())
  } finally {
    loading.value = false
  }
}

async function submitRegister() {
  const valid = await registerFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authApi.register({
      username: registerForm.username.trim(),
      password: registerForm.password,
      nickname: registerForm.nickname.trim(),
      phone: registerForm.phone.trim() || undefined
    })
    // 注册接口不返回 token，紧接着用同一账号登录，省去用户再输一次
    const data = await authApi.login({
      username: registerForm.username.trim(),
      password: registerForm.password
    })
    auth.setSession(data.token, data.user)
    ElMessage.success('注册成功，已为你自动登录')
    router.replace(redirectTarget())
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-visual" :style="{ backgroundImage: `linear-gradient(180deg, rgba(20, 34, 30, 0.15), rgba(20, 34, 30, 0.78)), url(${visual})` }">
        <div class="visual-copy">
          <p class="eyebrow">MOUNTAIN STAY</p>
          <h2>住进山野里</h2>
          <p>登录后可查看实时房态、在线下单，并随时管理你的订单。</p>
        </div>
      </div>

      <div class="login-form-wrap">
        <div class="form-head">
          <h1>{{ activeTab === 'login' ? '欢迎回来' : '创建账号' }}</h1>
          <p>{{ activeTab === 'login' ? '登录后继续你的山居之旅。' : '注册一个房客账号，立即开始预订。' }}</p>
        </div>

        <el-tabs v-model="activeTab" class="auth-tabs">
          <el-tab-pane label="登录" name="login">
            <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-position="top" @submit.prevent>
              <el-form-item label="用户名" prop="username">
                <el-input v-model="loginForm.username" size="large" placeholder="请输入用户名" autocomplete="username" />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input
                  v-model="loginForm.password"
                  type="password"
                  size="large"
                  show-password
                  placeholder="请输入密码"
                  autocomplete="current-password"
                  @keyup.enter="submitLogin"
                />
              </el-form-item>
              <el-button type="primary" size="large" class="submit" :loading="loading" @click="submitLogin">
                登录
              </el-button>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="注册" name="register">
            <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-position="top" @submit.prevent>
              <el-form-item label="用户名" prop="username">
                <el-input v-model="registerForm.username" size="large" placeholder="4-20 位字母、数字或下划线" />
              </el-form-item>
              <el-form-item label="昵称" prop="nickname">
                <el-input v-model="registerForm.nickname" size="large" placeholder="怎么称呼你" maxlength="20" />
              </el-form-item>
              <el-form-item label="手机号（可选）" prop="phone">
                <el-input v-model="registerForm.phone" size="large" placeholder="用于接收订单通知" maxlength="11" />
              </el-form-item>
              <div class="form-row">
                <el-form-item label="密码" prop="password">
                  <el-input v-model="registerForm.password" type="password" size="large" show-password placeholder="6-32 位" />
                </el-form-item>
                <el-form-item label="确认密码" prop="confirm">
                  <el-input v-model="registerForm.confirm" type="password" size="large" show-password placeholder="再次输入密码" />
                </el-form-item>
              </div>
              <el-button type="primary" size="large" class="submit" :loading="loading" @click="submitRegister">
                注册并登录
              </el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  padding: 56px 24px 24px;
}

.login-card {
  display: grid;
  grid-template-columns: 1.05fr 1fr;
  width: 100%;
  max-width: 940px;
  min-height: 560px;
  border-radius: 24px;
  overflow: hidden;
  background: var(--surface);
  border: 1px solid var(--line);
  box-shadow: 0 32px 70px -46px rgba(31, 42, 46, 0.7);
}

.login-visual {
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: flex-end;
  padding: 40px;
  color: #fff;
}

.visual-copy {
  max-width: 320px;
}

.eyebrow {
  font-size: 11px;
  letter-spacing: 4px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 10px;
}

.visual-copy h2 {
  margin: 0 0 12px;
  font-size: 30px;
  letter-spacing: 2px;
  font-family: 'Songti SC', 'STSong', serif;
}

.visual-copy p {
  font-size: 13px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.82);
}

.login-form-wrap {
  padding: 44px 48px;
}

.form-head h1 {
  margin: 0 0 8px;
  font-size: 26px;
  letter-spacing: 1px;
}

.form-head p {
  color: var(--muted);
  font-size: 13.5px;
  margin-bottom: 18px;
}

.auth-tabs :deep(.el-tabs__item) {
  font-size: 15px;
}

.auth-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: var(--line);
}

.submit {
  width: 100%;
  margin-top: 8px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

@media (max-width: 860px) {
  .login-page {
    padding: 24px 16px;
  }

  .login-card {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .login-visual {
    min-height: 180px;
    padding: 24px;
  }

  .visual-copy h2 {
    font-size: 24px;
  }

  .login-form-wrap {
    padding: 28px 24px 34px;
  }
}
</style>

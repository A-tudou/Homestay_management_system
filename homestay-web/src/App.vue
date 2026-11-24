<script setup>
/**
 * 全局外壳：根据当前路由的 meta.admin 决定使用房客端布局还是后台布局，
 * 房客端包含顶部导航、页脚和 <router-view> 路由出口，后台布局由 AdminLayout 自行渲染。
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { auth } from './composables/useAuth'

const router = useRouter()
const route = useRoute()
const { user, isLoggedIn } = auth

// 管理端路由带有 meta.admin，用于整体切换布局（不显示前台头尾）
const isAdmin = computed(() => route.meta.admin === true)

// 顶部用户下拉菜单：命令项直接映射到跳转或退出登录
function onCommand(command) {
  if (command === 'orders') {
    router.push('/orders')
  } else if (command === 'admin') {
    router.push('/admin')
  } else if (command === 'logout') {
    auth.clear()
    ElMessage.success('已退出登录')
    // 退出后若当前页需要登录/房东权限，回到首页，避免停留在无权限页面
    if (route.meta.requiresAuth || route.meta.requiresHost) {
      router.push('/')
    }
  }
}
</script>

<template>
  <div v-if="isAdmin" class="admin-root">
    <router-view />
  </div>

  <div v-else class="app-shell">
    <header class="app-header">
      <div class="container header-inner">
        <router-link to="/" class="brand">
          <span class="brand-mark">山</span>
          <span class="brand-text">
            <strong>山居小筑</strong>
            <small>MOUNTAIN STAY</small>
          </span>
        </router-link>

        <nav class="nav">
          <router-link to="/" class="nav-link" exact-active-class="active">房源</router-link>
          <router-link to="/search" class="nav-link" active-class="active">搜索空房</router-link>
          <router-link to="/orders" class="nav-link" active-class="active">我的订单</router-link>
        </nav>

        <div class="header-actions">
          <template v-if="isLoggedIn">
            <el-dropdown trigger="click" @command="onCommand">
              <span class="user-chip">
                <el-icon><User /></el-icon>
                {{ user?.nickname || user?.username }}
                <el-icon class="arrow"><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="orders">我的订单</el-dropdown-item>
                  <!-- 仅房东角色显示后台入口，房客看不到该菜单项 -->
                  <el-dropdown-item v-if="user?.role === 'HOST'" command="admin">管理后台</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button text @click="router.push('/login')">登录</el-button>
            <!-- 注册按钮通过 query.tab 让登录页直接打开注册页签 -->
            <el-button type="primary" round @click="router.push({ path: '/login', query: { tab: 'register' } })">
              注册
            </el-button>
          </template>
        </div>
      </div>
    </header>

    <main class="app-main">
      <router-view />
    </main>

    <footer class="app-footer">
      <div class="container footer-inner">
        <div>
          <strong>山居小筑</strong>
          <p>把日子过成山间的风，欢迎来住上几晚。</p>
        </div>
        <div class="footer-meta">
          <span>在线预订 · 即时确认</span>
          <span>© {{ new Date().getFullYear() }} 山居小筑</span>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
/**
 * 后台整体布局：左侧菜单 + 顶部面包屑/用户菜单 + 子路由出口。
 * 菜单项与路由一一对应，靠 router-link 的 active-class 高亮当前页。
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { auth } from '../composables/useAuth'

const route = useRoute()
const router = useRouter()
const { user } = auth

// 侧边栏菜单配置：path 与 router 中的子路由保持一致
const menus = [
  { path: '/admin/room-types', label: '房型管理', icon: 'OfficeBuilding' },
  { path: '/admin/rooms', label: '房间管理', icon: 'House' },
  { path: '/admin/calendar', label: '日历库存', icon: 'Calendar' },
  { path: '/admin/orders', label: '订单管理', icon: 'Tickets' }
]

// 标题取当前子路由的 meta.title，同时用于顶部面包屑
const currentTitle = computed(() => route.meta.title || '管理台')

function onCommand(command) {
  if (command === 'front') {
    // 回到房客端首页，不退出登录
    router.push('/')
  } else if (command === 'logout') {
    auth.clear()
    ElMessage.success('已退出登录')
    // 退出后带上 redirect，重新登录可直接回到后台
    router.push({ path: '/login', query: { redirect: '/admin' } })
  }
}
</script>

<template>
  <div class="admin-shell">
    <aside class="admin-aside">
      <router-link to="/admin/room-types" class="admin-brand">
        <span class="mark">山</span>
        <span>
          <strong>山居小筑</strong>
          <small>HOST CONSOLE</small>
        </span>
      </router-link>

      <nav class="admin-nav">
        <router-link
          v-for="item in menus"
          :key="item.path"
          :to="item.path"
          class="admin-nav-link"
          active-class="active"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </router-link>
      </nav>

      <div class="admin-aside-foot">
        <p>山居小筑 · 房东工作台</p>
        <p>v1.0</p>
      </div>
    </aside>

    <div class="admin-body">
      <header class="admin-topbar">
        <div>
          <p class="admin-crumb">管理台 / {{ currentTitle }}</p>
          <h1>{{ currentTitle }}</h1>
        </div>

        <el-dropdown trigger="click" @command="onCommand">
          <span class="admin-user">
            <el-icon><User /></el-icon>
            {{ user?.nickname || user?.username || '房东' }}
            <el-icon class="arrow"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="front">返回前台</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>

      <main class="admin-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

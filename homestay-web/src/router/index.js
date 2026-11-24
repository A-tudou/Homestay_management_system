import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '../api'
import { auth, isAuthenticated } from '../composables/useAuth'

const routes = [
  {
    path: '/',
    name: 'home',
    component: () => import('../views/RoomList.vue'),
    meta: { title: '房源' }
  },
  {
    path: '/search',
    name: 'search',
    component: () => import('../views/Search.vue'),
    meta: { title: '搜索空房' }
  },
  {
    path: '/rooms/:id',
    name: 'room-detail',
    component: () => import('../views/RoomDetail.vue'),
    meta: { title: '房型详情' }
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录 / 注册' }
  },
  {
    path: '/orders',
    name: 'orders',
    component: () => import('../views/MyOrders.vue'),
    meta: { title: '我的订单', requiresAuth: true }
  },
  // 管理端复用同一工程，admin 元信息用于切换布局，requiresHost 用于角色拦截
  {
    path: '/admin',
    component: () => import('../layouts/AdminLayout.vue'),
    meta: { requiresHost: true, admin: true },
    children: [
      {
        path: '',
        redirect: '/admin/room-types'
      },
      {
        path: 'room-types',
        name: 'admin-room-types',
        component: () => import('../views/admin/AdminRoomTypes.vue'),
        meta: { title: '房型管理' }
      },
      {
        path: 'rooms',
        name: 'admin-rooms',
        component: () => import('../views/admin/AdminRooms.vue'),
        meta: { title: '房间管理' }
      },
      {
        path: 'calendar',
        name: 'admin-calendar',
        component: () => import('../views/admin/AdminCalendar.vue'),
        meta: { title: '日历库存' }
      },
      {
        path: 'orders',
        name: 'admin-orders',
        component: () => import('../views/admin/AdminOrders.vue'),
        meta: { title: '订单管理' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    return savedPosition || { top: 0 }
  }
})

// 全局守卫：统一处理页面标题、登录拦截和管理端角色校验
router.beforeEach(async (to) => {
  if (to.meta.title) {
    document.title = `${to.meta.title} · 山居小筑`
  }
  if (to.meta.requiresAuth && !isAuthenticated()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresHost) {
    if (!isAuthenticated()) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
    if (!auth.user.value?.role) {
      // 旧会话可能只有 token 没有角色，进入管理端前通过 /auth/me 补齐用户资料
      try {
        auth.setUser(await authApi.me())
      } catch {
        return { path: '/login', query: { redirect: to.fullPath } }
      }
    }
    // 前端守卫只负责页面拦截，后端仍会对 /api/admin/** 再次校验 HOST 权限
    if (auth.user.value?.role !== 'HOST') {
      ElMessage.error('仅房东账号可进入管理台')
      return { path: '/' }
    }
  }
  if (to.path === '/login' && isAuthenticated()) {
    return { path: '/' }
  }
  return true
})

export default router

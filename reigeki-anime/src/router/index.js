import { createRouter, createWebHistory } from 'vue-router'
import { hasValidToken, hasValidAdminToken } from '../api/session.js'

/**
 * 路由表。
 *
 * 相比原实现的三处改动：
 *   1. **补上登录后的落地页**。原先 / 直接重定向到 /login，登录成功后无处可去。
 *   2. **组件改为动态 import**。Vue Router 会据此按路由拆分产物，
 *      访问登录页时不会把社员中心的代码也一起下载。
 *   3. **加兜底路由**。原先未知地址匹配不到任何组件，页面渲染成空白，
 *      看起来像应用崩了。
 *
 * meta 约定：
 *   requiresAuth 需要登录，未登录跳登录页并带上 redirect
 *   guestOnly    仅未登录可访问（登录/注册页），已登录时跳回首页
 */

const routes = [
  {
    path: '/',
    name: 'home',
    component: () => import('../views/LandingView.vue'),
    meta: { title: '燃尽动漫' }
  },
  {
    path: '/dashboard',
    name: 'dashboard',
    component: () => import('../views/HomeView.vue'),
    meta: { requiresAuth: true, title: '社员中心' }
  },
  {
    path: '/watch/:animeId',
    name: 'watch',
    component: () => import('../views/WatchView.vue'),
    meta: { title: '播放' }
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { guestOnly: true, title: '登录' }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../views/RegisterView.vue'),
    meta: { guestOnly: true, title: '注册' }
  },
  /* ---------- 后台管理（与前台社员会话完全隔离） ---------- */
  {
    path: '/admin/login',
    name: 'admin-login',
    component: () => import('../views/admin/AdminLoginView.vue'),
    meta: { title: '后台登录' }
  },
  {
    path: '/admin',
    component: () => import('../views/admin/AdminLayout.vue'),
    meta: { adminAuth: true, title: '后台管理' },
    children: [
      { path: '', redirect: { name: 'admin-animes' } },
      {
        path: 'animes',
        name: 'admin-animes',
        component: () => import('../views/admin/AdminAnimesView.vue'),
        meta: { adminAuth: true, title: '番剧管理' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('../views/NotFoundView.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    // 浏览器前进/后退时恢复原来的滚动位置，普通跳转回到顶部
    return savedPosition || { top: 0 }
  }
})

/**
 * 全局前置守卫。
 *
 * 这里只做「本地是否还有未过期的 token」这一层快速判断，不做签名校验，
 * 目的是少一次注定失败的往返、避免受保护页面闪现。真正的鉴权始终在后端：
 * HomeView 挂载时会调 /api/auth/me，token 无效就会被 request.js 的
 * 401 处理清掉会话并弹回登录页。
 */
router.beforeEach((to) => {
  /* ---- 后台管理区：独立于前台社员登录态 ---- */
  if (to.path.startsWith('/admin')) {
    // 已登录后台却访问登录页 → 直接进管理页
    if (to.name === 'admin-login') {
      return hasValidAdminToken() ? { path: '/admin' } : true
    }
    // 未登录后台访问管理页 → 弹回后台登录页并带 redirect
    if (to.matched.some((r) => r.meta.adminAuth) && !hasValidAdminToken()) {
      return { name: 'admin-login', query: { redirect: to.fullPath } }
    }
    return true
  }

  const authenticated = hasValidToken()

  if (to.meta.requiresAuth && !authenticated) {
    return {
      name: 'login',
      // 首页是默认落地页，被弹回时加 redirect 只会让地址栏变脏
      query: to.fullPath === '/' ? {} : { redirect: to.fullPath }
    }
  }

  if (to.meta.guestOnly && authenticated) {
    return { name: 'home' }
  }

  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 燃尽动漫` : '燃尽动漫'
})

export default router

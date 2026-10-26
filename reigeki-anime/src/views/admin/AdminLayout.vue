<template>
  <div class="admin-shell">
    <!-- ===== 侧边栏 ===== -->
    <aside class="side">
      <router-link class="brand" to="/">
        <span class="mark" aria-hidden="true">🔥</span>
        <span class="name">燃尽<b>后台</b></span>
      </router-link>

      <nav class="nav">
        <router-link class="nav-item" :to="{ name: 'admin-animes' }">
          <span class="ic" aria-hidden="true">📺</span> 番剧管理
        </router-link>
        <span class="nav-item is-disabled" title="更多模块规划中">
          <span class="ic" aria-hidden="true">🧩</span> 更多模块
        </span>
      </nav>

      <div class="side-foot">
        <router-link class="back" to="/">← 返回首页</router-link>
        <div class="whoami">
          <span class="avatar" aria-hidden="true">{{ initial }}</span>
          <div>
            <b>{{ adminUser?.nickname || '管理员' }}</b>
            <span>{{ adminUser?.username || '—' }}</span>
          </div>
        </div>
        <button type="button" class="logout" @click="onLogout">退出登录</button>
      </div>
    </aside>

    <!-- ===== 内容区 ===== -->
    <main class="content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
/**
 * 后台管理布局。
 *
 * 侧边栏 + 内容区的经典后台骨架；前台社员顶栏在 /admin 路由下不渲染
 * （见 App.vue 的 isAdminRoute），两套界面互不干扰。
 * 管理员信息读自管理员专用存储；进到这里前路由守卫已确保 token 未过期，
 * token 真正失效时由 request.js 的 401 订阅统一弹回登录页。
 */

import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { adminLogout } from '../../api/admin.js'
import { readAdminUser, clearAdminSession } from '../../api/session.js'
import { useToast } from '../../composables/useToast.js'

const router = useRouter()
const toast = useToast()

const adminUser = computed(() => readAdminUser())

const initial = computed(() => {
  const name = adminUser.value?.nickname || adminUser.value?.username || '管'
  return name.trim().charAt(0).toUpperCase()
})

async function onLogout() {
  try {
    await adminLogout()
  } catch {
    /* 退出接口失败不阻塞本地清理 */
  }
  clearAdminSession()
  toast.info('已退出后台登录')
  router.replace({ name: 'admin-login' })
}
</script>

<style scoped>
.admin-shell {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  display: grid;
  grid-template-columns: 232px 1fr;
}

/* ----- 侧边栏 ----- */
.side {
  display: flex;
  flex-direction: column;
  gap: 26px;
  padding: 22px 16px;
  border-right: 1px solid var(--line-soft);
  background: rgba(4, 16, 28, .55);
  backdrop-filter: blur(14px);
  position: sticky;
  top: 0;
  height: 100vh;
}
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: var(--text);
  padding: 4px 6px;
}
.brand .mark {
  width: 32px; height: 32px; border-radius: 10px;
  display: grid; place-items: center; font-size: 16px;
  background: linear-gradient(135deg, var(--fire-1), var(--fire-2));
}
.brand .name { font-size: 15px; letter-spacing: 1px; }
.brand .name b { color: var(--fire-3); }

.nav { display: flex; flex-direction: column; gap: 6px; }
.nav-item {
  display: flex; align-items: center; gap: 10px;
  padding: 11px 14px;
  border-radius: 12px;
  font-size: 14px;
  color: var(--muted);
  text-decoration: none;
  border: 1px solid transparent;
  transition: background .2s, color .2s;
}
.nav-item:hover { color: var(--text); background: rgba(30, 167, 232, .1); }
.nav-item.router-link-active {
  color: var(--ember);
  background: rgba(30, 167, 232, .14);
  border-color: var(--line-soft);
  font-weight: 700;
}
.nav-item .ic { font-size: 16px; }
.nav-item.is-disabled { opacity: .45; cursor: not-allowed; }

.side-foot {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 6px;
}
.back { font-size: 13px; color: var(--muted); text-decoration: none; }
.back:hover { color: var(--ember); }
.whoami {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 12px;
  background: rgba(30, 167, 232, .08);
  border: 1px solid var(--line-soft);
}
.whoami .avatar {
  width: 32px; height: 32px; border-radius: 50%; flex: 0 0 auto;
  display: grid; place-items: center;
  font-size: 13px; font-weight: 700; color: #04263f;
  background: linear-gradient(135deg, var(--ember), var(--fire-1));
}
.whoami b { display: block; font-size: 13px; }
.whoami span { font-size: 12px; color: var(--muted); }
.logout {
  padding: 9px 12px;
  border-radius: 10px;
  border: 1px solid rgba(255, 90, 90, .35);
  background: rgba(255, 61, 61, .08);
  color: var(--danger);
  font-size: 13px;
  cursor: pointer;
  font-family: inherit;
  transition: background .2s;
}
.logout:hover { background: rgba(255, 61, 61, .16); }

/* ----- 内容区 ----- */
.content {
  padding: 30px 34px 60px;
  min-width: 0;
}

@media (max-width: 860px) {
  .admin-shell { grid-template-columns: 1fr; }
  .side { position: static; height: auto; flex-direction: row; align-items: center; flex-wrap: wrap; gap: 14px; }
  .nav { flex-direction: row; }
  .side-foot { margin-top: 0; margin-left: auto; flex-direction: row; align-items: center; }
  .content { padding: 22px 16px 50px; }
}
</style>

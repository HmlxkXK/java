<template>
  <!--
    首页模式：顶栏本体就是那张秋季横幅，导航 / 搜索 / 用户区浮在横幅上，
    logo 落在横幅左下角，和截图里的 B 站首页一致。
    其他页面：保留原来的紧凑深色顶栏，但换成同样的「品牌 + 导航 + 搜索」结构。
  -->
  <header v-if="isHome" class="bili-hero-header">
    <div class="bili-banner" aria-hidden="true"></div>

    <div class="bili-header-row">
      <router-link class="bili-logo-big" to="/">
        <span class="logo-tv" aria-hidden="true">📺</span>
        <span>燃尽动漫</span>
        <small>REIGEKI ANIME</small>
      </router-link>

      <nav class="bili-nav" aria-label="主导航">
        <router-link to="/" class="on">首页</router-link>
        <a href="#animes" @click.prevent="goSection('animes')">番剧</a>
        <a href="#animes" @click.prevent="goSection('animes')">直播</a>
        <a href="#animes" @click.prevent="goSection('animes')">游戏中心</a>
        <a href="#animes" @click.prevent="goSection('animes')">会员购</a>
        <a href="#animes" @click.prevent="goSection('animes')">漫画</a>
        <a href="#animes" @click.prevent="goSection('animes')">赛事</a>
      </nav>

      <form class="bili-search" role="search" @submit.prevent="onSearch">
        <input v-model="keyword" type="search" :placeholder="hotPlaceholder" aria-label="搜索番剧" />
        <button type="submit" aria-label="搜索">🔍</button>
      </form>

      <div class="bili-header-right">
        <template v-if="isAuthenticated">
          <router-link to="/dashboard" class="bili-avatar" :title="displayName">{{ initial }}</router-link>
          <router-link to="/dashboard" class="bili-mini-link">大会员</router-link>
          <button type="button" class="bili-mini-link" @click="toast.info('消息功能开发中')">消息</button>
          <button type="button" class="bili-mini-link" @click="toast.info('动态功能开发中')">动态</button>
          <button type="button" class="bili-mini-link" @click="toast.info('收藏功能开发中')">收藏</button>
          <button type="button" class="bili-upload" @click="toast.info('投稿功能开发中')">投稿</button>
        </template>
        <template v-else>
          <span class="bili-avatar bili-avatar--guest" title="未登录">访</span>
          <button type="button" class="bili-mini-link" @click="toast.info('登录后查看消息')">消息</button>
          <button type="button" class="bili-mini-link" @click="toast.info('登录后查看动态')">动态</button>
          <button type="button" class="bili-mini-link" @click="toast.info('登录后查看收藏')">收藏</button>
          <router-link to="/login" class="bili-login-btn">登录</router-link>
          <router-link to="/register" class="bili-upload">注册</router-link>
        </template>
      </div>
    </div>

    <div class="bili-banner-foot">
      <router-link class="bili-logo-wordmark" to="/">
        <b>燃尽</b>动漫
        <span>把你的热爱，烧到最旺</span>
      </router-link>
    </div>
  </header>

  <header v-else class="topbar">
    <router-link class="topbar-brand" to="/">
      <span class="mark" aria-hidden="true">🔥</span>
      <span class="name">燃尽<b>动漫</b></span>
    </router-link>

    <nav class="topbar-nav" aria-label="主导航">
      <router-link to="/">首页</router-link>
      <router-link to="/dashboard" v-if="isAuthenticated">番剧</router-link>
      <a href="/#animes">排行榜</a>
    </nav>

    <form class="topbar-search" role="search" @submit.prevent="onSearch">
      <input v-model="keyword" type="search" placeholder="搜索番剧" aria-label="搜索番剧" />
      <button type="submit" aria-label="搜索">🔍</button>
    </form>

    <div class="topbar-right">
      <router-link class="topbar-btn topbar-btn--ghost" to="/admin/login">后台管理</router-link>

      <template v-if="isAuthenticated">
        <router-link class="topbar-link" to="/dashboard">社员中心</router-link>
        <span class="topbar-user" :title="user?.email || ''">
          <span class="avatar" aria-hidden="true">{{ initial }}</span>
          {{ displayName }}
        </span>
        <button type="button" class="topbar-btn" @click="onLogout">退出登录</button>
      </template>
      <template v-else>
        <router-link class="topbar-link" to="/login">登录</router-link>
        <router-link class="topbar-btn topbar-btn--solid" to="/register">免费注册</router-link>
      </template>
    </div>
  </header>
</template>

<script setup>
/**
 * 自适应顶栏。
 *
 * - 首页（/）：横幅式 B 站风格顶栏，秋季 banner 作背景，导航 / 搜索 / 用户区浮于其上；
 * - 其他页面：紧凑深色顶栏，但沿用同一套「品牌 + 导航 + 搜索 + 用户区」结构。
 *
 * 登录态仍由 useAuth 驱动；登录 / 注册页由 App.vue 控制不渲染本组件。
 */
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth.js'
import { useToast } from '../composables/useToast.js'

const route = useRoute()
const router = useRouter()
const toast = useToast()
const { state, isAuthenticated, displayName, logout } = useAuth()

const isHome = computed(() => route.name === 'home')
const user = computed(() => state.user)
const keyword = ref('')

const hotPlaceholder = '碧海之章'

// 昵称首字符作头像；中文取第一个字，英文取首字母
const initial = computed(() => {
  const name = displayName.value || '社'
  return name.trim().charAt(0).toUpperCase()
})

function onSearch() {
  const q = keyword.value.trim()
  if (!q) {
    toast.info('请输入要搜索的番剧')
    return
  }
  // 首页把关键词带给番剧区做前端过滤；带 hash 让浏览器滚动过去
  router.push({ path: '/', hash: '#animes', query: { q } })
}

function goSection(id) {
  if (route.name !== 'home') {
    router.push({ path: '/', hash: `#${id}` })
  } else {
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' })
  }
}

function onLogout() {
  logout()
  toast.info('已退出登录')
  router.replace({ name: 'home' })
}
</script>

<style scoped>
/* ============ 首页横幅顶栏 ============ */
.bili-hero-header {
  position: relative;
  height: 168px;
  border-radius: 0 0 8px 8px;
  overflow: hidden;
}
.bili-banner {
  position: absolute;
  inset: 0;
  background: url('/images/banner-autumn.png') center 46% / cover no-repeat;
  z-index: 0;
}
.bili-banner::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(0, 0, 0, .16), transparent 38%, rgba(0, 0, 0, .1));
}
.bili-header-row {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 18px;
  height: 64px;
  padding: 0 3vw;
}
.bili-logo-big {
  display: flex;
  align-items: baseline;
  gap: 6px;
  color: #fff;
  text-decoration: none;
  font-size: 20px;
  font-weight: 900;
  letter-spacing: 1px;
  text-shadow: 0 2px 8px rgba(0, 0, 0, .35);
  white-space: nowrap;
}
.bili-logo-big .logo-tv { font-size: 22px; }
.bili-logo-big small {
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 2px;
  opacity: .78;
}
.bili-nav {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 0 1 auto;
  overflow: hidden;
}
.bili-nav a {
  color: rgba(255, 255, 255, .9);
  text-decoration: none;
  font-size: 13px;
  white-space: nowrap;
  text-shadow: 0 1px 4px rgba(0, 0, 0, .4);
  transition: color .2s;
}
.bili-nav a:hover, .bili-nav a.on { color: #fff; font-weight: 700; }
.bili-search {
  display: flex;
  align-items: center;
  flex: 1 1 300px;
  max-width: 440px;
  min-width: 160px;
  height: 36px;
  border-radius: 8px;
  background: rgba(255, 255, 255, .92);
  overflow: hidden;
  margin-left: auto;
}
.bili-search input {
  flex: 1;
  min-width: 0;
  height: 100%;
  border: 0;
  outline: 0;
  padding: 0 12px;
  font-size: 13px;
  color: #222;
  background: transparent;
}
.bili-search button {
  width: 42px;
  height: 100%;
  border: 0;
  cursor: pointer;
  background: transparent;
  font-size: 15px;
}
.bili-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 0 0 auto;
}
.bili-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #fb7299, #fc8bab);
  color: #fff;
  font-size: 13px;
  font-weight: 800;
  text-decoration: none;
  border: 2px solid rgba(255, 255, 255, .85);
}
.bili-avatar--guest { background: linear-gradient(135deg, #9aa8b5, #b8c4cf); }
.bili-mini-link {
  border: 0;
  background: transparent;
  color: rgba(255, 255, 255, .92);
  font-size: 13px;
  cursor: pointer;
  text-shadow: 0 1px 4px rgba(0, 0, 0, .4);
  text-decoration: none;
  white-space: nowrap;
}
.bili-mini-link:hover { color: #fff; }
.bili-login-btn {
  color: #fff;
  text-decoration: none;
  font-size: 13px;
  white-space: nowrap;
}
.bili-upload {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  padding: 0 16px;
  border-radius: 8px;
  background: #fb7299;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  text-decoration: none;
  border: 0;
  cursor: pointer;
  white-space: nowrap;
}
.bili-upload:hover { background: #f25d8e; }
.bili-banner-foot {
  position: absolute;
  left: 3vw;
  bottom: 20px;
  z-index: 2;
}
.bili-logo-wordmark {
  display: flex;
  align-items: baseline;
  gap: 12px;
  color: #fff;
  text-decoration: none;
  font-size: 30px;
  font-weight: 900;
  letter-spacing: 1px;
  text-shadow: 0 3px 12px rgba(0, 0, 0, .45);
}
.bili-logo-wordmark b { color: #ffd9e6; }
.bili-logo-wordmark span {
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 2px;
  opacity: .88;
}

@media (max-width: 1100px) {
  .bili-nav a:nth-child(n + 5) { display: none; }
}
@media (max-width: 820px) {
  .bili-header-row { gap: 10px; padding: 0 4vw; }
  .bili-nav { display: none; }
  .bili-header-right .bili-mini-link { display: none; }
  .bili-logo-wordmark { font-size: 24px; }
  .bili-logo-wordmark span { display: none; }
}

/* ============ 非首页紧凑顶栏（沿用 B 站结构，深色主题） ============ */
.topbar {
  display: flex;
  align-items: center;
  gap: 20px;
  min-height: 58px;
  padding: 8px 5vw;
  background: var(--glass);
  border-bottom: 1px solid var(--line-soft);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  position: sticky;
  top: 0;
  z-index: 20;
}
.topbar-nav { display: flex; gap: 18px; }
.topbar-nav a {
  color: var(--muted);
  text-decoration: none;
  font-size: 13px;
}
.topbar-nav a:hover, .topbar-nav a.router-link-active { color: var(--ember); }
.topbar-search {
  display: flex;
  align-items: center;
  height: 34px;
  border-radius: 8px;
  background: rgba(255, 255, 255, .08);
  border: 1px solid var(--line-soft);
  overflow: hidden;
  width: 220px;
  margin-left: auto;
}
.topbar-search input {
  flex: 1;
  min-width: 0;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--text);
  padding: 0 10px;
  font-size: 13px;
}
.topbar-search button {
  width: 34px;
  height: 100%;
  border: 0;
  background: transparent;
  color: var(--ember);
  cursor: pointer;
}
</style>

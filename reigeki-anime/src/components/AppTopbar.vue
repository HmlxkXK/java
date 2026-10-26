<template>
  <header class="topbar">
    <router-link class="topbar-brand" to="/">
      <span class="mark" aria-hidden="true">🔥</span>
      <span class="name">燃尽<b>动漫</b></span>
    </router-link>

    <div class="topbar-right">
      <!-- 后台管理入口：无论社员是否登录都可进入后台登录页 -->
      <router-link class="topbar-btn topbar-btn--ghost" to="/admin/login">后台管理</router-link>

      <!-- 已登录：展示社员信息 + 社员中心入口 + 退出 -->
      <template v-if="isAuthenticated">
        <router-link class="topbar-link" to="/dashboard">社员中心</router-link>
        <span class="topbar-user" :title="user?.email || ''">
          <span class="avatar" aria-hidden="true">{{ initial }}</span>
          {{ displayName }}
        </span>
        <button type="button" class="topbar-btn" @click="onLogout">退出登录</button>
      </template>

      <!-- 未登录：提供登录 / 注册入口 -->
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
 * - 未登录（落地页 / 公开页）：显示「登录 / 免费注册」，引导转化；
 * - 已登录：显示「社员中心」入口、昵称头像与「退出登录」。
 *
 * 是否登录由 useAuth 的响应式状态驱动；登录/注册页由 App.vue 控制不渲染本组件，
 * 因此这里不必再处理那两页的布局冲突。
 */

import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth.js'
import { useToast } from '../composables/useToast.js'

const router = useRouter()
const toast = useToast()
const { state, isAuthenticated, displayName, logout } = useAuth()

const user = computed(() => state.user)

// 昵称首字符作头像；中文取第一个字，英文取首字母
const initial = computed(() => {
  const name = displayName.value || '社'
  return name.trim().charAt(0).toUpperCase()
})

function onLogout() {
  logout()
  toast.info('已退出登录')
  router.replace({ name: 'home' })
}
</script>

<template>
  <EmberBackground />
  <AppTopbar v-if="!isAuthRoute && !isAdminRoute" />
  <router-view />
  <ToastHost />
</template>

<script setup>
/**
 * 应用根组件。
 *
 * 顶栏在「非登录/注册页」都渲染，且会根据登录态自适应显示登录/注册按钮或社员信息：
 * 登录/注册页是左右两栏、垂直居中的整屏布局，挂上顶栏会把内容往下挤，破坏视觉重心，
 * 所以这两页单独隐藏顶栏。
 */

import { computed } from 'vue'
import { useRoute } from 'vue-router'
import EmberBackground from './components/EmberBackground.vue'
import AppTopbar from './components/AppTopbar.vue'
import ToastHost from './components/ToastHost.vue'

const route = useRoute()
// 登录 / 注册页不显示顶栏，避免挤压居中布局
const isAuthRoute = computed(() => route.name === 'login' || route.name === 'register')
// 后台管理区有自己的侧边栏布局，不渲染前台社员顶栏
const isAdminRoute = computed(() => route.path.startsWith('/admin'))
</script>

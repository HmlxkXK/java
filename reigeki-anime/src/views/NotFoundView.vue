<template>
  <main class="page notfound">
    <span class="nf-code">404</span>
    <h1>这条线路上没有车站</h1>
    <p>
      地址 <code>{{ path }}</code> 不存在。可能是手输错了，或者这个页面还没做出来。
    </p>
    <div class="nf-actions">
      <router-link class="ghost-btn" :to="isAuthenticated ? '/' : '/login'">
        {{ isAuthenticated ? '回到社员中心' : '去登录' }}
      </router-link>
    </div>
  </main>
</template>

<script setup>
/**
 * 404 页。
 *
 * 原来 router 里没有兜底路由，访问任何未知地址都会渲染空白页
 * （router-view 匹配不到任何组件），看起来像应用崩了。
 */

import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuth } from '../composables/useAuth.js'

const route = useRoute()
const { isAuthenticated } = useAuth()

const path = computed(() => route.fullPath)
</script>

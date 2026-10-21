<template>
  <div class="embers" aria-hidden="true">
    <span
      v-for="p in particles"
      :key="p.id"
      class="ember"
      :style="p.style"
    ></span>
  </div>
</template>

<script setup>
/**
 * 上升火星粒子背景。
 *
 * 相比原实现的三处改动：
 *   1. **声明式渲染**。原来用 document.createElement 手动塞 DOM，绕过了 Vue 的
 *      渲染层；现在交给 v-for，样式一并算好，逻辑只在 setup 阶段跑一次。
 *   2. **尊重 prefers-reduced-motion**。系统开启「减弱动态效果」时完全不渲染粒子 ——
 *      前庭敏感的用户不该被迫接受满屏飘动的装饰元素。
 *   3. **按视口宽度减量**。粒子的主要绘制开销来自每个元素各带一层 box-shadow 模糊，
 *      数量减半即可显著降低开销，窄屏上也基本看不出差别。
 *
 * 粒子配置在 setup 里生成一次：App.vue 始终挂载本组件，不会反复创建。
 */

import { ref, onMounted, onUnmounted } from 'vue'

const DESKTOP_COUNT = 36
const MOBILE_COUNT = 16
const COMPACT_WIDTH = 900

const particles = ref([])
let media = null
let resizeTimer = null

function build(width) {
  // 系统要求减弱动效时，一个粒子都不渲染
  if (media?.matches) {
    particles.value = []
    return
  }
  const count = width < COMPACT_WIDTH ? MOBILE_COUNT : DESKTOP_COUNT
  particles.value = Array.from({ length: count }, (_, i) => {
    const diameter = 3 + Math.random() * 6
    return {
      id: i,
      style: {
        left: Math.random() * 100 + 'vw',
        width: diameter + 'px',
        height: diameter + 'px',
        '--drift': Math.random() * 120 - 60 + 'px',
        // 越大的火星烧得越久，看起来更像真的在飘散（原实现是 6~14s 的随机区间）
        animationDuration: 8 + (diameter / 9) * 6 + 's',
        // 负延迟让粒子一开始就散布在上升途中的不同高度，避免同一起点齐刷刷冒出
        animationDelay: -Math.random() * 12 + 's'
      }
    }
  })
}

function handleMediaChange() {
  build(window.innerWidth)
}

function handleResize() {
  // 只在跨越断点时重建，拖动窗口不至于每变化一像素就重排几十个节点
  clearTimeout(resizeTimer)
  resizeTimer = setTimeout(() => {
    const expected = window.innerWidth < COMPACT_WIDTH ? MOBILE_COUNT : DESKTOP_COUNT
    if (!media?.matches && particles.value.length !== expected) build(window.innerWidth)
  }, 200)
}

onMounted(() => {
  media = window.matchMedia('(prefers-reduced-motion: reduce)')
  build(window.innerWidth)
  media.addEventListener('change', handleMediaChange)
  window.addEventListener('resize', handleResize, { passive: true })
})

onUnmounted(() => {
  clearTimeout(resizeTimer)
  media?.removeEventListener('change', handleMediaChange)
  window.removeEventListener('resize', handleResize)
})
</script>

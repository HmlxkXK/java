<template>
  <div class="embers" ref="box"></div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const box = ref(null)

onMounted(() => {
  const N = 36
  for (let i = 0; i < N; i++) {
    const e = document.createElement('div')
    e.className = 'ember'
    const size = 3 + Math.random() * 6
    e.style.left = Math.random() * 100 + 'vw'
    e.style.width = e.style.height = size + 'px'
    e.style.setProperty('--drift', (Math.random() * 120 - 60) + 'px')
    e.style.animationDuration = (6 + Math.random() * 8) + 's'
    e.style.animationDelay = (-Math.random() * 12) + 's'
    box.value.appendChild(e)
  }
})
</script>

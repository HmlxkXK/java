<template>
  <div class="toast-host" aria-live="polite" aria-atomic="false">
    <transition-group name="toast">
      <div
        v-for="t in toasts"
        :key="t.id"
        class="toast"
        :class="'toast-' + t.type"
        role="status"
      >
        <span class="toast-icon" aria-hidden="true"></span>
        <span class="toast-msg">{{ t.message }}</span>
        <button type="button" class="toast-close" aria-label="关闭提示" @click="remove(t.id)">×</button>
      </div>
    </transition-group>
  </div>
</template>

<script setup>
import { useToast } from '../composables/useToast.js'

const { toasts, remove } = useToast()
</script>

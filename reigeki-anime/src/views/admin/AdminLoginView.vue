<template>
  <div class="wrap admin-wrap">
    <section class="hero">
      <span class="badge">ADMIN · 站点管理</span>
      <h1>燃尽动漫<small>管 理 后 台</small></h1>
      <p>这里维护首页「本周热门番剧」等对外展示的数据。非管理员请返回前台。</p>
      <div class="stats">
        <div class="stat"><b>🛡️</b><span>仅限管理员访问</span></div>
        <div class="stat"><b>📺</b><span>首页内容可维护</span></div>
        <div class="stat"><b>⚡</b><span>改动即时生效</span></div>
      </div>
    </section>

    <section class="card">
      <div class="logo">
        <div class="mark">🛡️</div>
        <h2>后台<span>登录</span></h2>
      </div>

      <form novalidate @submit.prevent="onSubmit">
        <div class="field" :class="{ 'has-error': errors.username }">
          <label for="admin-user">管理员用户名</label>
          <div class="ctl">
            <span class="ic" aria-hidden="true">👤</span>
            <input
              id="admin-user"
              v-model.trim="form.username"
              type="text"
              placeholder="输入管理员用户名"
              autocomplete="username"
              :aria-invalid="Boolean(errors.username)"
              @input="clearError('username')"
            />
          </div>
          <p v-if="errors.username" class="err">{{ errors.username }}</p>
        </div>

        <div class="field" :class="{ 'has-error': errors.password }">
          <label for="admin-pwd">密码</label>
          <div class="ctl has-eye">
            <span class="ic" aria-hidden="true">🔒</span>
            <input
              id="admin-pwd"
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="输入登录密码"
              autocomplete="current-password"
              :aria-invalid="Boolean(errors.password)"
              @input="clearError('password')"
            />
            <button
              type="button"
              class="eye"
              :aria-label="showPassword ? '隐藏密码' : '显示密码'"
              @click="showPassword = !showPassword"
            >{{ showPassword ? '隐藏' : '显示' }}</button>
          </div>
          <p v-if="errors.password" class="err">{{ errors.password }}</p>
        </div>

        <div class="row">
          <label><input v-model="form.remember" type="checkbox" /> 记住我（7 天）</label>
        </div>

        <button class="btn" type="submit" :disabled="loading">
          {{ loading ? '登录中…' : '登录后台' }}
        </button>

        <p class="alt">
          后台不提供注册，账号由站点负责人开通。
          <router-link to="/">返回前台 →</router-link>
        </p>
      </form>
    </section>
  </div>
</template>

<script setup>
/**
 * 后台登录页。
 *
 * 与前台社员登录页刻意分开：
 *   - 只有「用户名 + 密码」一种登录方式，没有注册 / 验证码 / 社交登录；
 *   - 会话写入管理员专用的存储键（reigeki_admin_*），与社员会话互不覆盖；
 *   - 登录成功后按 ?redirect= 回跳到被拦截的管理页，默认进 /admin。
 */

import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { adminLogin } from '../../api/admin.js'
import { writeAdminSession } from '../../api/session.js'
import { useToast } from '../../composables/useToast.js'
import { validateRequired } from '../../utils/validators.js'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const form = reactive({ username: '', password: '', remember: false })
const errors = reactive({ username: '', password: '' })
const showPassword = ref(false)
const loading = ref(false)

function clearError(field) {
  errors[field] = ''
}

/** 只允许站内相对路径，避免开放重定向 */
function resolveRedirect() {
  const target = route.query.redirect
  if (typeof target === 'string' && target.startsWith('/') && !target.startsWith('//')) {
    return target
  }
  return '/admin'
}

async function onSubmit() {
  errors.username = validateRequired(form.username, '用户名')
  errors.password = validateRequired(form.password, '密码')
  if (errors.username || errors.password) return

  loading.value = true
  try {
    const data = await adminLogin({
      username: form.username,
      password: form.password,
      remember: form.remember
    })
    writeAdminSession(data.token, data.user || { username: form.username })
    toast.success(`🛡️ 欢迎回来，${data?.user?.nickname || form.username}！`)
    router.replace(resolveRedirect())
  } catch (error) {
    errors.password = error.message
    toast.error(error.message)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.admin-wrap .card .mark {
  background: linear-gradient(135deg, #7f53ff, #1ea7e8);
}
</style>

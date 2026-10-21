<template>
  <div class="wrap">
    <section class="hero">
      <span class="badge">ANIME · 热血 · 二次元</span>
      <h1>燃尽动漫<small>R E I G E K I&nbsp;&nbsp;A N I M E</small></h1>
      <p>在这里，每一次登录都是一次点燃。追番、社区、同好公会——把你的热爱烧到最旺。</p>
      <div class="stats">
        <div class="stat"><b>3,200+</b><span>连载番剧</span></div>
        <div class="stat"><b>86万</b><span>活跃社员</span></div>
        <div class="stat"><b>24h</b><span>不打烊</span></div>
      </div>
    </section>

    <section class="card">
      <div class="logo">
        <div class="mark">🔥</div>
        <h2>燃尽<span>动漫</span></h2>
      </div>

      <form novalidate @submit.prevent="onSubmit">
        <div class="field" :class="{ 'has-error': errors.account }">
          <label for="user">账号 / 邮箱</label>
          <div class="ctl">
            <span class="ic" aria-hidden="true">👤</span>
            <input
              id="user"
              v-model.trim="form.account"
              type="text"
              placeholder="输入你的社员账号"
              autocomplete="username"
              :aria-invalid="Boolean(errors.account)"
              aria-describedby="user-err"
              @input="clearError('account')"
            />
          </div>
          <p v-if="errors.account" id="user-err" class="err">{{ errors.account }}</p>
        </div>

        <div class="field" :class="{ 'has-error': errors.password }">
          <label for="pwd">密码</label>
          <div class="ctl has-eye">
            <span class="ic" aria-hidden="true">🔒</span>
            <input
              id="pwd"
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="输入登录密码"
              autocomplete="current-password"
              :aria-invalid="Boolean(errors.password)"
              aria-describedby="pwd-err"
              @input="clearError('password')"
            />
            <button
              type="button"
              class="eye"
              :aria-label="showPassword ? '隐藏密码' : '显示密码'"
              :aria-pressed="showPassword"
              @click="showPassword = !showPassword"
            >{{ showPassword ? '隐藏' : '显示' }}</button>
          </div>
          <p v-if="errors.password" id="pwd-err" class="err">{{ errors.password }}</p>
        </div>

        <div class="row">
          <label><input v-model="form.remember" type="checkbox" /> 记住我</label>
          <a href="#" @click.prevent="onForgot">忘记密码？</a>
        </div>

        <button class="btn" type="submit" :disabled="loading">
          {{ loading ? '登录中…' : '登 录' }}
        </button>

        <div class="social">
          <button type="button" @click="onUnavailable('手机验证码')">📱 手机验证码</button>
          <button type="button" @click="onUnavailable('微信登录')">💬 微信登录</button>
        </div>

        <p class="alt">还没有账号？<router-link to="/register">立即注册 →</router-link></p>
      </form>
    </section>
  </div>
</template>

<script setup>
/**
 * 登录页。
 *
 * 相比原实现的改动：
 *   - fetch + alert 换成统一的 API 层与 toast；
 *   - 逐字段内联报错，不再是「一次 alert 只报一个问题」；
 *   - 登录成功后真正跳转（原来只弹个 alert，页面停在原地，
 *     而且当时根本没有可跳的页面）；
 *   - 支持 ?redirect= 回跳，从受保护页面被弹回来时能回到原处；
 *   - 勾选「记住我」会记住账号名，下次自动回填（密码永不落地）。
 *
 * 「记住我」的语义：后端据此签发 7 天 token（否则用默认有效期）。
 * 前端固定用 localStorage 存 token，不因勾选而改变存储位置。
 */

import { reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth.js'
import { useToast } from '../composables/useToast.js'
import { validateRequired } from '../utils/validators.js'

const route = useRoute()
const router = useRouter()
const toast = useToast()
const { login } = useAuth()

const REMEMBER_KEY = 'reigeki_remember_account'

const form = reactive({
  account: '',
  password: '',
  remember: false
})

const errors = reactive({ account: '', password: '' })
const showPassword = ref(false)
const loading = ref(false)

onMounted(() => {
  // 回填上次「记住我」的账号；注册页跳过来时会带 ?email=
  const fromRegister = route.query.email
  if (typeof fromRegister === 'string' && fromRegister) {
    form.account = fromRegister
    return
  }
  try {
    const saved = localStorage.getItem(REMEMBER_KEY)
    if (saved) {
      form.account = saved
      form.remember = true
    }
  } catch {
    /* 存储不可用时忽略 */
  }
})

function clearError(field) {
  errors[field] = ''
}

/** 只允许站内相对路径，避免 ?redirect=//evil.com 这类开放重定向 */
function resolveRedirect() {
  const target = route.query.redirect
  if (typeof target === 'string' && target.startsWith('/') && !target.startsWith('//')) {
    return target
  }
  // 默认登录后进入社员中心（落地页是公开页，不应作为登录后的落点）
  return '/dashboard'
}

async function onSubmit() {
  errors.account = validateRequired(form.account, '账号')
  errors.password = validateRequired(form.password, '密码')
  if (errors.account || errors.password) return

  loading.value = true
  try {
    const data = await login({
      account: form.account,
      password: form.password,
      remember: form.remember
    })

    // 记住账号（只存账号名，密码不落地）
    try {
      if (form.remember) localStorage.setItem(REMEMBER_KEY, form.account)
      else localStorage.removeItem(REMEMBER_KEY)
    } catch {
      /* 忽略 */
    }

    toast.success(`🔥 欢迎回来，${data?.user?.nickname || form.account}！`)
    router.replace(resolveRedirect())
  } catch (error) {
    // 账号不存在与密码错误后端返回同一句「账号或密码错误」，这里不做区分，
    // 只把后端的文案原样展示，免得前端自行推测而泄露账号是否注册
    errors.password = error.message
    toast.error(error.message)
  } finally {
    loading.value = false
  }
}

function onForgot() {
  toast.info('找回密码功能尚未接入后端')
}

function onUnavailable(name) {
  toast.info(`${name}尚未接入后端`)
}
</script>

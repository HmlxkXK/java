<template>
  <div class="wrap">
    <section class="hero">
      <span class="badge">JOIN · 成为社员</span>
      <h1>加入燃尽动漫<small>B E&nbsp;&nbsp;A&nbsp;&nbsp;P A R T&nbsp;&nbsp;O F&nbsp;&nbsp;I T</small></h1>
      <p>创建账号，点亮你的二次元身份。追番、发帖、组队开黑，和 86 万社员一起把热爱烧到最旺。</p>
      <div class="perks">
        <div class="perk"><span class="dot">🎁</span> 新社员专享 · 30 天会员体验</div>
        <div class="perk"><span class="dot">⚡</span> 同步追番进度 · 多端无缝切换</div>
        <div class="perk"><span class="dot">🛡️</span> 账号加密保护 · 隐私可控</div>
      </div>
    </section>

    <section class="card">
      <div class="logo">
        <div class="mark">🔥</div>
        <h2>燃尽<span>动漫</span></h2>
      </div>
      <p class="sub">创建你的社员账号</p>

      <form novalidate @submit.prevent="onSubmit">
        <div class="field" :class="{ 'has-error': errors.nickname }">
          <label for="nick">昵称</label>
          <div class="ctl">
            <span class="ic" aria-hidden="true">🎭</span>
            <input
              id="nick"
              v-model.trim="form.nickname"
              type="text"
              placeholder="给自己起个燃系名字"
              autocomplete="nickname"
              maxlength="50"
              :aria-invalid="Boolean(errors.nickname)"
              aria-describedby="nick-err"
              @input="clearError('nickname')"
              @blur="revalidate('nickname')"
            />
          </div>
          <p v-if="errors.nickname" id="nick-err" class="err">{{ errors.nickname }}</p>
        </div>

        <div class="field" :class="{ 'has-error': errors.email }">
          <label for="email">账号 / 邮箱</label>
          <div class="ctl">
            <span class="ic" aria-hidden="true">👤</span>
            <input
              id="email"
              v-model.trim="form.email"
              type="email"
              placeholder="用于登录与找回密码"
              autocomplete="email"
              maxlength="100"
              :aria-invalid="Boolean(errors.email)"
              aria-describedby="email-err"
              @input="clearError('email')"
              @blur="revalidate('email')"
            />
          </div>
          <p v-if="errors.email" id="email-err" class="err">{{ errors.email }}</p>
        </div>

        <div class="field" :class="{ 'has-error': errors.password }">
          <label for="pwd">密码</label>
          <div class="ctl has-eye">
            <span class="ic" aria-hidden="true">🔒</span>
            <input
              id="pwd"
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="至少 8 位，含字母与数字"
              autocomplete="new-password"
              maxlength="64"
              :aria-invalid="Boolean(errors.password)"
              aria-describedby="pwd-err strength-hint"
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

          <div v-if="strength.label" class="strength" :data-level="strength.score">
            <div class="strength-bar" aria-hidden="true">
              <i v-for="n in 4" :key="n" :class="{ on: n <= strength.score }"></i>
            </div>
            <span class="strength-text" id="strength-hint">{{ strength.label }} · {{ strength.hint }}</span>
          </div>

          <p v-if="errors.password" id="pwd-err" class="err">{{ errors.password }}</p>
        </div>

        <div class="field" :class="{ 'has-error': errors.confirm }">
          <label for="pwd2">确认密码</label>
          <div class="ctl">
            <span class="ic" aria-hidden="true">✅</span>
            <input
              id="pwd2"
              v-model="form.confirm"
              :type="showPassword ? 'text' : 'password'"
              placeholder="再输一次密码"
              autocomplete="new-password"
              maxlength="64"
              :aria-invalid="Boolean(errors.confirm)"
              aria-describedby="pwd2-err"
              @input="clearError('confirm')"
            />
          </div>
          <p v-if="errors.confirm" id="pwd2-err" class="err">{{ errors.confirm }}</p>
        </div>

        <div class="field" :class="{ 'has-error': errors.code }">
          <label for="code">邮箱验证码</label>
          <div class="code-row">
            <input
              id="code"
              v-model.trim="form.code"
              type="text"
              inputmode="numeric"
              placeholder="6 位验证码"
              maxlength="6"
              autocomplete="one-time-code"
              :aria-invalid="Boolean(errors.code)"
              aria-describedby="code-err"
              @input="clearError('code')"
            />
            <button
              type="button"
              :disabled="counting || sending"
              @click="onSendCode"
            >{{ sending ? '发送中…' : codeText }}</button>
          </div>
          <p v-if="errors.code" id="code-err" class="err">{{ errors.code }}</p>
        </div>

        <label class="agree" :class="{ 'has-error': errors.agree }">
          <input v-model="form.agree" type="checkbox" @change="clearError('agree')" />
          <span>我已阅读并同意 <a href="#" @click.prevent>《用户协议》</a> 与 <a href="#" @click.prevent>《隐私政策》</a>，承诺遵守社区公约。</span>
        </label>
        <p v-if="errors.agree" class="err err-agree">{{ errors.agree }}</p>

        <button class="btn" type="submit" :disabled="submitting">
          {{ submitting ? '注册中…' : '注 册' }}
        </button>

        <p class="alt">已有账号？<router-link to="/login">立即登录 →</router-link></p>
      </form>
    </section>
  </div>
</template>

<script setup>
/**
 * 注册页。
 *
 * 相比原实现的改动：
 *   - 修掉一个真 bug：原来「获取验证码」按钮只在**成功之后**才进入倒计时，
 *     请求飞行期间是可以连点的，连点两次就会发两条验证码、并弹两个 alert。
 *     现在加了 sending 状态，请求期间按钮直接禁用。
 *   - 倒计时改为基于截止时间戳计算，而不是 setInterval 里 t--。
 *     标签页切到后台时浏览器会节流定时器，自减写法会走慢（切回来还剩一堆秒数），
 *     时间戳写法无论怎么节流，显示的都是真实剩余秒数。
 *   - 逐字段内联报错 + 密码强度提示，不再靠 alert 一个个试。
 *   - 注册成功后跳登录页并回填邮箱（后端注册接口不返回 token，无法自动登录）。
 */

import { reactive, ref, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth.js'
import { useToast } from '../composables/useToast.js'
import {
  validateNickname,
  validateEmail,
  validatePassword,
  validateConfirmPassword,
  validateCode,
  passwordStrength
} from '../utils/validators.js'

const router = useRouter()
const toast = useToast()
const { register, sendCode } = useAuth()

const COOLDOWN_SECONDS = 60

const form = reactive({
  nickname: '',
  email: '',
  password: '',
  confirm: '',
  code: '',
  agree: false
})

const errors = reactive({
  nickname: '',
  email: '',
  password: '',
  confirm: '',
  code: '',
  agree: ''
})

const showPassword = ref(false)
const sending = ref(false)
const submitting = ref(false)

// ---- 验证码倒计时：基于截止时间戳，不受定时器节流影响 ----
const cooldownUntil = ref(0)
const now = ref(Date.now())
let ticker = null

const remaining = computed(() =>
  cooldownUntil.value ? Math.max(0, Math.ceil((cooldownUntil.value - now.value) / 1000)) : 0
)
const counting = computed(() => remaining.value > 0)
const codeText = computed(() => (counting.value ? `${remaining.value}s` : '获取验证码'))

function startCooldown() {
  cooldownUntil.value = Date.now() + COOLDOWN_SECONDS * 1000
  now.value = Date.now()
  clearInterval(ticker)
  ticker = setInterval(() => {
    now.value = Date.now()
    if (now.value >= cooldownUntil.value) {
      clearInterval(ticker)
      ticker = null
      cooldownUntil.value = 0
    }
  }, 250)
}

// ---- 校验 ----
const strength = computed(() => passwordStrength(form.password))

function clearError(field) {
  errors[field] = ''
}

/** 失焦时补一次校验，但只在用户已经输入过内容时提示，避免空表单满屏红字 */
function revalidate(field) {
  const map = {
    nickname: () => validateNickname(form.nickname),
    email: () => validateEmail(form.email)
  }
  const fn = map[field]
  if (!fn) return
  const value = form[field]
  if (!value) return
  errors[field] = fn()
}

function validateAll() {
  errors.nickname = validateNickname(form.nickname)
  errors.email = validateEmail(form.email)
  errors.password = validatePassword(form.password)
  errors.confirm = validateConfirmPassword(form.password, form.confirm)
  errors.code = validateCode(form.code)
  errors.agree = form.agree ? '' : '请先阅读并同意用户协议与隐私政策'
  return Object.values(errors).every((v) => !v)
}

// ---- 发送验证码 ----
async function onSendCode() {
  if (sending.value || counting.value) return

  const emailError = validateEmail(form.email)
  if (emailError) {
    errors.email = emailError
    toast.warn(emailError)
    return
  }

  sending.value = true
  try {
    const data = await sendCode(form.email)
    // 演示环境后端把明文验证码放在返回值里；真实环境这里应是一句「已发送」
    if (/^\d{6}$/.test(String(data || ''))) {
      form.code = String(data)
      errors.code = ''
      toast.success(`演示环境验证码 ${data}，已自动填入`)
    } else {
      toast.success(data || '验证码已发送，请查收邮箱')
    }
    startCooldown()
  } catch (error) {
    toast.error(error.message)
  } finally {
    sending.value = false
  }
}

// ---- 提交注册 ----
async function onSubmit() {
  if (submitting.value) return
  if (!validateAll()) {
    toast.warn('请先修正表单中标红的内容')
    return
  }

  submitting.value = true
  try {
    const user = await register({
      nickname: form.nickname,
      email: form.email,
      password: form.password,
      code: form.code
    })
    toast.success(`🎉 注册成功，欢迎加入燃尽动漫，${user?.nickname || form.nickname}！`)

    // 密码类字段不留在内存里，避免页签回退时还在输入框里
    form.password = ''
    form.confirm = ''

    router.replace({ name: 'login', query: { email: form.email } })
  } catch (error) {
    toast.error(error.message)
    // 把后端的字段级文案落到对应输入框上，用户不用自己猜是哪里错了
    if (/验证码/.test(error.message)) errors.code = error.message
    else if (/邮箱/.test(error.message)) errors.email = error.message
    else if (/昵称/.test(error.message)) errors.nickname = error.message
    else if (/密码/.test(error.message)) errors.password = error.message
  } finally {
    submitting.value = false
  }
}

onUnmounted(() => {
  clearInterval(ticker)
})
</script>

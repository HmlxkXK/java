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
          <label for="nick">昵称（登录账号）</label>
          <div class="ctl">
            <span class="ic" aria-hidden="true">🎭</span>
            <input
              id="nick"
              v-model.trim="form.nickname"
              type="text"
              placeholder="2-50 字，作为登录账号"
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
          <label for="code">图片验证码</label>
          <div class="code-row">
            <input
              id="code"
              v-model.trim="form.code"
              type="text"
              inputmode="text"
              placeholder="输入右侧字符"
              maxlength="4"
              autocomplete="off"
              :aria-invalid="Boolean(errors.code)"
              aria-describedby="code-err"
              @input="clearError('code')"
            />
            <canvas
              ref="captchaCanvas"
              class="captcha-canvas"
              width="120"
              height="42"
              role="button"
              tabindex="0"
              title="点击刷新验证码"
              aria-label="图片验证码，点击刷新"
              @click="refreshCaptcha"
              @keydown.enter.prevent="refreshCaptcha"
              @keydown.space.prevent="refreshCaptcha"
            ></canvas>
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
 * 账号体系：昵称即登录账号（唯一），注册不再需要邮箱。
 * 验证码：前端 canvas 本地生成的图片验证码，本地比对，不发送到后端；
 *         点击图片可刷新。这样后端 register 只需 nickname + password。
 * 注册成功后跳登录页并回填昵称（后端注册接口不返回 token，无法自动登录）。
 */

import { reactive, ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth.js'
import { useToast } from '../composables/useToast.js'
import {
  validateNickname,
  validatePassword,
  validateConfirmPassword,
  validateCode,
  passwordStrength
} from '../utils/validators.js'

const router = useRouter()
const toast = useToast()
const { register } = useAuth()

const form = reactive({
  nickname: '',
  password: '',
  confirm: '',
  code: '',
  agree: false
})

const errors = reactive({
  nickname: '',
  password: '',
  confirm: '',
  code: '',
  agree: ''
})

const showPassword = ref(false)
const submitting = ref(false)

// ---- 图片验证码（canvas 本地生成，忽略大小写比对） ----
const captchaCanvas = ref(null)
const captchaText = ref('')
// 去掉了易混淆的 0/O/1/l/I
const CAPTCHA_CHARS = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789'

function drawCaptcha() {
  const canvas = captchaCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  const w = canvas.width
  const h = canvas.height

  ctx.fillStyle = '#eef4fb'
  ctx.fillRect(0, 0, w, h)

  // 生成 4 位随机字符
  let text = ''
  for (let i = 0; i < 4; i++) {
    text += CAPTCHA_CHARS[Math.floor(Math.random() * CAPTCHA_CHARS.length)]
  }
  captchaText.value = text

  // 字符：随机颜色、轻微旋转、逐字绘制
  const colors = ['#185fa5', '#993c1d', '#0f6e56', '#534ab7', '#a32d2d']
  ctx.font = 'bold 24px sans-serif'
  ctx.textBaseline = 'middle'
  for (let i = 0; i < text.length; i++) {
    ctx.save()
    ctx.translate(12 + i * 27, h / 2)
    ctx.rotate((Math.random() - 0.5) * 0.4)
    ctx.fillStyle = colors[Math.floor(Math.random() * colors.length)]
    ctx.fillText(text[i], 0, 0)
    ctx.restore()
  }

  // 干扰线
  ctx.strokeStyle = 'rgba(40,90,150,.25)'
  ctx.lineWidth = 1
  for (let i = 0; i < 4; i++) {
    ctx.beginPath()
    ctx.moveTo(Math.random() * w, Math.random() * h)
    ctx.lineTo(Math.random() * w, Math.random() * h)
    ctx.stroke()
  }

  // 噪点
  for (let i = 0; i < 24; i++) {
    ctx.fillStyle = `rgba(30,70,120,${Math.random() * 0.35})`
    ctx.fillRect(Math.random() * w, Math.random() * h, 2, 2)
  }
}

function refreshCaptcha() {
  form.code = ''
  errors.code = ''
  drawCaptcha()
}

onMounted(drawCaptcha)

// ---- 校验 ----
const strength = computed(() => passwordStrength(form.password))

function clearError(field) {
  errors[field] = ''
}

/** 失焦时补一次校验，但只在用户已经输入过内容时提示，避免空表单满屏红字 */
function revalidate(field) {
  if (field === 'nickname') {
    if (!form.nickname) return
    errors.nickname = validateNickname(form.nickname)
  }
}

function validateAll() {
  errors.nickname = validateNickname(form.nickname)
  errors.password = validatePassword(form.password)
  errors.confirm = validateConfirmPassword(form.password, form.confirm)
  errors.code = validateCode(form.code)
  if (!errors.code && form.code.toUpperCase() !== captchaText.value.toUpperCase()) {
    errors.code = '验证码不正确'
  }
  errors.agree = form.agree ? '' : '请先阅读并同意用户协议与隐私政策'
  return Object.values(errors).every((v) => !v)
}

// ---- 提交注册 ----
async function onSubmit() {
  if (submitting.value) return
  if (!validateAll()) {
    if (errors.code === '验证码不正确') refreshCaptcha()
    toast.warn('请先修正表单中标红的内容')
    return
  }

  submitting.value = true
  try {
    const user = await register({
      nickname: form.nickname,
      password: form.password
    })
    toast.success(`🎉 注册成功，欢迎加入燃尽动漫，${user?.nickname || form.nickname}！`)

    // 密码类字段不留在内存里，避免页签回退时还在输入框里
    form.password = ''
    form.confirm = ''

    router.replace({ name: 'login', query: { account: form.nickname } })
  } catch (error) {
    toast.error(error.message)
    // 把后端的字段级文案落到对应输入框上，用户不用自己猜是哪里错了
    if (/昵称|账号/.test(error.message)) errors.nickname = error.message
    else if (/密码/.test(error.message)) errors.password = error.message
  } finally {
    submitting.value = false
  }
}
</script>

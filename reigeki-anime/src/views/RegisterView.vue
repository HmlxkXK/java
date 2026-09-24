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

      <form @submit.prevent="onRegister">
        <div class="field">
          <label for="nick">昵称</label>
          <span class="ic">🎭</span>
          <input id="nick" v-model="nick" type="text" placeholder="给自己起个燃系名字" autocomplete="nickname" />
        </div>

        <div class="field">
          <label for="email">账号 / 邮箱</label>
          <span class="ic">👤</span>
          <input id="email" v-model="email" type="email" placeholder="用于登录与找回密码" autocomplete="email" />
        </div>

        <div class="field">
          <label for="pwd">密码</label>
          <span class="ic">🔒</span>
          <input id="pwd" v-model="pwd" type="password" placeholder="至少 8 位，含字母与数字" autocomplete="new-password" />
          <div class="tip">建议：<b>8 位以上</b>，混合字母、数字与符号</div>
        </div>

        <div class="field">
          <label for="pwd2">确认密码</label>
          <span class="ic">✅</span>
          <input id="pwd2" v-model="pwd2" type="password" placeholder="再输一次密码" autocomplete="new-password" />
        </div>

        <div class="field">
          <label for="code">邮箱验证码</label>
          <div class="code-row">
            <input id="code" v-model="code" type="text" placeholder="6 位验证码" />
            <button type="button" :disabled="counting" @click="sendCode">{{ codeText }}</button>
          </div>
        </div>

        <label class="agree">
          <input type="checkbox" v-model="agree" />
          <span>我已阅读并同意 <a href="#">《用户协议》</a> 与 <a href="#">《隐私政策》</a>，承诺遵守社区公约。</span>
        </label>

        <button class="btn" type="submit">注 册</button>

        <p class="alt">已有账号？<router-link to="/login">立即登录 →</router-link></p>
      </form>
    </section>
  </div>
</template>

<script setup>
import { ref, onUnmounted } from 'vue'

// 后端地址（Vite 5173 -> Spring Boot 8081，后端已开启 CORS）
const BASE = 'http://localhost:8081'

const nick = ref('')
const email = ref('')
const pwd = ref('')
const pwd2 = ref('')
const code = ref('')
const agree = ref(false)

const counting = ref(false)
const codeText = ref('获取验证码')
let timer = null

function isValidEmail(v) {
  return /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(v)
}

function startCountdown() {
  let t = 60
  counting.value = true
  codeText.value = t + 's'
  timer = setInterval(() => {
    t--
    codeText.value = t + 's'
    if (t <= 0) {
      clearInterval(timer)
      counting.value = false
      codeText.value = '获取验证码'
    }
  }, 1000)
}

// 获取验证码：调用后端生成并“发送”（演示直接回传明文，方便测试）
async function sendCode() {
  if (!isValidEmail(email.value)) {
    alert('请先填写正确的邮箱')
    return
  }
  try {
    const res = await fetch(`${BASE}/api/auth/send-code?email=${encodeURIComponent(email.value)}`, { method: 'POST' })
    const json = await res.json()
    if (json.code === 200) {
      alert('验证码已发送（演示）：' + (json.data || ''))
      startCountdown()
    } else {
      alert(json.message || '验证码发送失败')
    }
  } catch (e) {
    alert('请求失败：' + e.message)
  }
}

// 注册：调用后端接口完成校验与落库
async function onRegister() {
  if (!nick.value) { alert('请填写昵称'); return }
  if (!isValidEmail(email.value)) { alert('邮箱格式不正确'); return }
  if (pwd.value.length < 8) { alert('密码至少 8 位'); return }
  if (pwd.value !== pwd2.value) { alert('两次密码不一致'); return }
  if (code.value.length !== 6) { alert('请输入 6 位验证码'); return }
  if (!agree.value) { alert('请先同意用户协议与隐私政策'); return }

  try {
    const res = await fetch(`${BASE}/api/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        nickname: nick.value,
        email: email.value,
        password: pwd.value,
        code: code.value
      })
    })
    const json = await res.json()
    if (json.code === 200) {
      alert('🎉 注册成功，欢迎加入燃尽动漫，' + nick.value + '！')
    } else {
      alert(json.message || '注册失败')
    }
  } catch (e) {
    alert('请求失败：' + e.message)
  }
}

onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

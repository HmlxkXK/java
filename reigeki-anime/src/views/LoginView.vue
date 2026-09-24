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

      <form @submit.prevent="onLogin">
        <div class="field">
          <label for="user">账号 / 邮箱</label>
          <span class="ic">👤</span>
          <input id="user" v-model="user" type="text" placeholder="输入你的社员账号" autocomplete="username" />
        </div>
        <div class="field">
          <label for="pwd">密码</label>
          <span class="ic">🔒</span>
          <input id="pwd" v-model="pwd" type="password" placeholder="输入登录密码" autocomplete="current-password" />
        </div>

        <div class="row">
          <label><input type="checkbox" v-model="remember" /> 记住我</label>
          <a href="#">忘记密码？</a>
        </div>

        <button class="btn" type="submit">登 录</button>

        <div class="social">
          <button type="button">📱 手机验证码</button>
          <button type="button">💬 微信登录</button>
        </div>

        <p class="alt">还没有账号？<router-link to="/register">立即注册 →</router-link></p>
      </form>
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const user = ref('')
const pwd = ref('')
const remember = ref(false)

function onLogin() {
  if (!user.value || !pwd.value) {
    alert('请输入账号和密码')
    return
  }
  alert('🔥 登录请求已发送（示例）：' + user.value)
}
</script>

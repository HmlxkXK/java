/**
 * 会话状态。
 *
 * 解决的问题：原先前端登录后只是把 token 写进 localStorage 就完事，
 * **没有任何地方读它** —— 刷新页面等于登出、页面上也看不出已登录。
 *
 * 这里用「模块级单例 + reactive」的方式做状态共享：模块只在首次被 import 时
 * 执行一次，所有组件拿到的都是同一份 state，因此不需要 Vuex / Pinia。
 * 当前应用只有登录态这一个全局状态，引入状态库的收益远小于成本。
 */

import { reactive, computed } from 'vue'
import {
  readToken,
  readUser,
  writeSession,
  writeUser,
  clearSession,
  decodeToken,
  isTokenExpired,
  onUnauthorized
} from '../api/session.js'
import * as authApi from '../api/auth.js'

const state = reactive({
  token: readToken(),
  user: readUser()
})

// 外部（request.js）侦测到 401 时会调用 notifyUnauthorized()，
// 这里把响应式状态一并复位，否则内存里还留着过期的 token 和用户信息。
onUnauthorized(() => {
  state.token = null
  state.user = null
})

/** 是否已登录（本地判断，仅用于 UI 展示与路由放行） */
const isAuthenticated = computed(
  () => Boolean(state.token) && !isTokenExpired(state.token)
)

/** 展示用名称：昵称优先，退化到邮箱 */
const displayName = computed(
  () => state.user?.nickname || state.user?.email || '社员'
)

/** token 载荷，用于展示过期时间等 */
const tokenPayload = computed(() => decodeToken(state.token))

/** token 过期时间戳（毫秒），无 token 时为 null */
const expiresAt = computed(() => {
  const exp = tokenPayload.value?.exp
  return typeof exp === 'number' ? exp * 1000 : null
})

function setSession(token, user) {
  state.token = token || null
  state.user = user || null
  writeSession(token, user)
}

function logout() {
  state.token = null
  state.user = null
  clearSession()
}

/**
 * 登录并写入会话。
 * @returns {Promise<object>} 后端的 LoginVO（含 token / expiresIn / user）
 */
async function login(payload) {
  const data = await authApi.login(payload)
  setSession(data?.token, data?.user)
  return data
}

/**
 * 注册。
 * 后端注册接口**不返回 token**，所以注册成功后仍是未登录状态，
 * 需要引导用户去登录页，而不是直接放行。
 */
function register(payload) {
  return authApi.register(payload)
}

function sendCode(email) {
  return authApi.sendCode(email)
}

/**
 * 用后端校验当前 token，并刷新缓存的用户信息。
 * 这是唯一能确认「token 真的还有效」的方式——本地只判断 exp，不验签名。
 */
async function refreshMe() {
  const user = await authApi.fetchMe()
  state.user = user
  writeUser(user)
  return user
}

export function useAuth() {
  return {
    state,
    isAuthenticated,
    displayName,
    tokenPayload,
    expiresAt,
    login,
    logout,
    register,
    sendCode,
    refreshMe
  }
}

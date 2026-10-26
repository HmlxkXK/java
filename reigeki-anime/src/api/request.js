/**
 * 统一的 HTTP 请求层。
 *
 * 存在的理由：原来两个 View 各自写了一遍 fetch + 判 code + alert，
 * 重复且容易漏。这里集中处理四件容易出错的事：
 *
 *   1. **业务错误**。后端约定失败时 HTTP 状态码**仍然是 200**，错误写在
 *      响应体的 `code` 里（见后端 common/Result.java 的类注释）。
 *      所以绝不能只用 `res.ok` 判断，必须读 `json.code !== 200`。
 *   2. **网络/超时**。fetch 默认无限等待，后端没起时会一直挂着。
 *      这里用 AbortController 加超时，并翻译成能看懂的提示。
 *   3. **鉴权**。自动注入 `Authorization: Bearer <token>`，
 *      并在 token 失效时清会话、通知上层跳登录。
 *   4. **错误归一化**。所有失败都抛 ApiError，调用方只需 catch 一次。
 */

import {
  readToken,
  readAdminToken,
  notifyUnauthorized,
  notifyAdminUnauthorized,
  isTokenExpired
} from './session.js'

/** 走 Vite 代理（/api -> http://127.0.0.1:8081，见 vite.config.js），故留空用相对路径 */
const BASE = ''

const DEFAULT_TIMEOUT = 15_000

/**
 * 统一的错误对象。
 * @property {number} code 业务码：正值同后端（400/401/500），
 *                         负数表示请求根本没到达业务层（网络/超时/响应非法）
 */
export class ApiError extends Error {
  constructor(code, message) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

/** 请求没到达业务层时使用的错误码，避免与后端 400/401/500 混淆 */
export const NETWORK_ERROR = -1

/**
 * 发起请求。
 *
 * @param {string} path   以 / 开头的接口路径，如 '/api/auth/login'
 * @param {object} [options]
 * @param {string} [options.method='GET']
 * @param {object} [options.body]    会被 JSON 序列化
 * @param {object} [options.params]  查询参数，值为 undefined/null 的键会被跳过
 * @param {boolean|'admin'} [options.auth] 注入 token 并在 401 时清会话；
 *                         true 用社员 token，'admin' 用后台管理员 token
 * @param {number} [options.timeout] 毫秒
 * @returns {Promise<any>} 直接返回响应体的 data 字段
 */
export async function request(path, options = {}) {
  const {
    method = 'GET',
    body,
    params,
    auth = false,
    timeout = DEFAULT_TIMEOUT
  } = options

  let url = BASE + path
  if (params) {
    const search = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null) search.append(key, value)
    })
    const query = search.toString()
    if (query) url += (url.includes('?') ? '&' : '?') + query
  }

  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (auth) {
    // auth 传 'admin' 时走后台管理员会话，401 后清的是管理员会话、回后台登录页
    const isAdmin = auth === 'admin'
    const token = isAdmin ? readAdminToken() : readToken()
    // 本地已判定过期就不发这次注定失败的请求，省一次往返也少一条后端 warn 日志
    if (token && isTokenExpired(token)) {
      isAdmin ? notifyAdminUnauthorized() : notifyUnauthorized()
      throw new ApiError(401, '登录已过期，请重新登录')
    }
    if (token) headers.Authorization = `Bearer ${token}`
  }

  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), timeout)

  let res
  try {
    res = await fetch(url, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: controller.signal
    })
  } catch (error) {
    if (error?.name === 'AbortError') {
      throw new ApiError(NETWORK_ERROR, `请求超时（${timeout / 1000}s），请确认后端服务已启动`)
    }
    throw new ApiError(NETWORK_ERROR, '无法连接后端服务，请确认它正在运行')
  } finally {
    clearTimeout(timer)
  }

  // HTTP 层就不是 200：典型是代理拿不到后端（502/504）或路径写错（404）
  if (!res.ok) {
    throw new ApiError(NETWORK_ERROR, `服务异常（HTTP ${res.status}）`)
  }

  let json
  try {
    json = await res.json()
  } catch {
    // 后端返回了 HTML 错误页或空响应时走到这里
    throw new ApiError(NETWORK_ERROR, '响应格式不正确，请检查后端是否正常运行')
  }

  if (json.code !== 200) {
    if (json.code === 401 && auth) {
      auth === 'admin' ? notifyAdminUnauthorized() : notifyUnauthorized()
    }
    throw new ApiError(json.code, json.message || '请求失败')
  }

  return json.data
}

export const http = {
  get: (path, options) => request(path, { ...options, method: 'GET' }),
  post: (path, options) => request(path, { ...options, method: 'POST' }),
  put: (path, options) => request(path, { ...options, method: 'PUT' }),
  delete: (path, options) => request(path, { ...options, method: 'DELETE' })
}

/**
 * 会话存储。
 *
 * 只做「读写 token / user」这一件事，刻意不依赖 vue、也不依赖 router，
 * 这样 request.js 可以安全引用它而不会和 router -> view -> composable 形成循环依赖。
 * 需要响应式状态请用 composables/useAuth.js。
 *
 * 局部存储（localStorage）而不是 sessionStorage：
 * 「记住我」由后端签发 7 天的 token，前端若存 sessionStorage，
 * 关掉标签页就丢，和「记住我」的语义矛盾。勾选与否交给后端控制 token 有效期。
 */

export const TOKEN_KEY = 'reigeki_token'
export const USER_KEY = 'reigeki_user'

/**
 * 后台管理员会话的存储键。
 * 与社员会话完全隔离：社员登录 / 后台登录互不影响、可同时存在。
 */
export const ADMIN_TOKEN_KEY = 'reigeki_admin_token'
export const ADMIN_USER_KEY = 'reigeki_admin_user'

/** 读取 token；取不到或为空串都返回 null，避免下游到处判空 */
export function readToken() {
  try {
    return localStorage.getItem(TOKEN_KEY) || null
  } catch {
    // 隐私模式 / 存储被禁用时 localStorage 会抛异常，降级为「未登录」
    return null
  }
}

/** 读取缓存的用户信息 */
export function readUser() {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

/** 写入会话 */
export function writeSession(token, user) {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token)
    if (user) localStorage.setItem(USER_KEY, JSON.stringify(user))
  } catch {
    /* 存储不可用时静默忽略：当前会话仍可用，只是刷新后需要重新登录 */
  }
}

/** 只更新缓存的用户信息，不动 token */
export function writeUser(user) {
  writeSession(null, user)
}

/** 清空会话 */
export function clearSession() {
  try {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  } catch {
    /* 同上 */
  }
}

/**
 * 解析 JWT 载荷。
 *
 * 这里只做 base64 解码用于读取 exp / sub 等信息做展示与过期预判，
 * 不校验签名 —— 签名只有后端能验，前端解码的结果绝不作为「已授权」的依据，
 * 真正的授权判断始终由后端的 /api/auth/me 决定。
 */
export function decodeToken(token) {
  if (!token || typeof token !== 'string') return null
  const parts = token.split('.')
  if (parts.length !== 3) return null
  try {
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4)
    // atob 只认 latin1，中文昵称需要先转成字节再按 UTF-8 解码，否则乱码
    const binary = atob(padded)
    const bytes = Uint8Array.from(binary, (c) => c.charCodeAt(0))
    return JSON.parse(new TextDecoder('utf-8').decode(bytes))
  } catch {
    return null
  }
}

/** token 是否已过期。无 exp 字段时按「未过期」处理，交给后端最终裁决 */
export function isTokenExpired(token) {
  const payload = decodeToken(token)
  if (!payload || typeof payload.exp !== 'number') return false
  // 留 30 秒余量，避免请求发出瞬间刚好过期
  return payload.exp * 1000 <= Date.now() + 30_000
}

/** 当前是否存在「未过期」的 token */
export function hasValidToken() {
  const token = readToken()
  return Boolean(token) && !isTokenExpired(token)
}

/* ---------------- 后台管理员会话（与社员会话同构、键不同） ---------------- */

export function readAdminToken() {
  try {
    return localStorage.getItem(ADMIN_TOKEN_KEY) || null
  } catch {
    return null
  }
}

export function readAdminUser() {
  try {
    const raw = localStorage.getItem(ADMIN_USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export function writeAdminSession(token, user) {
  try {
    if (token) localStorage.setItem(ADMIN_TOKEN_KEY, token)
    if (user) localStorage.setItem(ADMIN_USER_KEY, JSON.stringify(user))
  } catch {
    /* 存储不可用时静默忽略 */
  }
}

export function clearAdminSession() {
  try {
    localStorage.removeItem(ADMIN_TOKEN_KEY)
    localStorage.removeItem(ADMIN_USER_KEY)
  } catch {
    /* 同上 */
  }
}

export function hasValidAdminToken() {
  const token = readAdminToken()
  return Boolean(token) && !isTokenExpired(token)
}

/**
 * 401 订阅机制。
 *
 * request.js 在「带鉴权」的请求收到 401 时通知订阅者，
 * 由 main.js 订阅并做跳转 —— 这样 request.js 不必知道 router 的存在。
 */
const unauthorizedHandlers = new Set()

export function onUnauthorized(handler) {
  unauthorizedHandlers.add(handler)
  return () => unauthorizedHandlers.delete(handler)
}

export function notifyUnauthorized() {
  clearSession()
  unauthorizedHandlers.forEach((handler) => {
    try {
      handler()
    } catch {
      /* 单个订阅者出错不应影响其它订阅者 */
    }
  })
}

/** 管理端的 401 订阅：与社员互不相干，失效后回后台登录页 */
const adminUnauthorizedHandlers = new Set()

export function onAdminUnauthorized(handler) {
  adminUnauthorizedHandlers.add(handler)
  return () => adminUnauthorizedHandlers.delete(handler)
}

export function notifyAdminUnauthorized() {
  clearAdminSession()
  adminUnauthorizedHandlers.forEach((handler) => {
    try {
      handler()
    } catch {
      /* 同上 */
    }
  })
}

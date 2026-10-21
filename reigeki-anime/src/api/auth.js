/**
 * 认证相关接口。
 *
 * 后端提供的接口（见后端 controller/AuthController.java）：
 *   POST /api/auth/send-code?email=xxx
 *   POST /api/auth/register
 *   POST /api/auth/login
 *   GET  /api/auth/me
 *   PUT  /api/auth/profile   （修改昵称 / 密码，需登录）
 *   POST /api/auth/logout
 *
 * 每个方法都只做「参数整理 + 调用 + 返回 data」，业务判断留给调用方。
 */

import { http } from './request.js'

/**
 * 获取邮箱验证码。
 *
 * 演示环境后端把明文验证码放在返回值里（真实项目应发邮件、这里只回一句“已发送”），
 * 所以调用方需要判断返回值是否为 6 位数字来区分环境。
 * @param {string} email
 * @returns {Promise<string>} 验证码明文，或提示文案
 */
export function sendCode(email) {
  return http.post('/api/auth/send-code', {
    params: { email: String(email ?? '').trim() }
  })
}

/**
 * 注册。
 * @param {{nickname:string,email:string,password:string,code:string}} payload
 * @returns {Promise<{id:number,nickname:string,email:string,createTime:string}>}
 */
export function register(payload) {
  return http.post('/api/auth/register', {
    body: {
      nickname: String(payload.nickname ?? '').trim(),
      // 邮箱在服务端还会做一次 lowercase + trim，这里先归一化以减少歧义
      email: String(payload.email ?? '').trim(),
      password: payload.password,
      code: String(payload.code ?? '').trim()
    }
  })
}

/**
 * 登录。
 * @param {{account:string,password:string,remember?:boolean}} payload
 * @returns {Promise<{token:string,tokenType:string,expiresIn:number,user:object}>}
 */
export function login(payload) {
  return http.post('/api/auth/login', {
    body: {
      account: String(payload.account ?? '').trim(),
      password: payload.password,
      remember: Boolean(payload.remember)
    }
  })
}

/**
 * 查询当前登录用户。同时充当「token 是否仍然有效」的验证手段。
 * @returns {Promise<{id:number,nickname:string,email:string,createTime:string}>}
 */
export function fetchMe() {
  return http.get('/api/auth/me', { auth: true })
}

/**
 * 退出登录。无状态方案下主要作用是让前端随后清除本地 token。
 * @returns {Promise<void>}
 */
export function logout() {
  return http.post('/api/auth/logout')
}

/**
 * 修改当前登录社员的资料（昵称 / 密码）。
 * 字段均可选：只传 nickname 改昵称；传 newPassword + oldPassword 改密码。
 * @param {{nickname?:string,newPassword?:string,oldPassword?:string}} payload
 * @returns {Promise<{id:number,nickname:string,email:string,createTime:string}>}
 */
export function updateProfile(payload) {
  return http.put('/api/auth/profile', {
    auth: true,
    body: {
      nickname: payload.nickname || undefined,
      newPassword: payload.newPassword || undefined,
      oldPassword: payload.oldPassword || undefined
    }
  })
}

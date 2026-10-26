/**
 * 后台管理相关接口。
 *
 * 对应后端：
 *   POST /api/admin/login            登录（无注册）
 *   GET  /api/admin/me               当前管理员
 *   POST /api/admin/logout           退出
 *   GET  /api/admin/animes           番剧全量列表
 *   POST /api/admin/animes           新增番剧
 *   PUT  /api/admin/animes/{id}      编辑番剧
 *   PUT  /api/admin/animes/{id}/visible?visible=true|false  上下架
 *   DELETE /api/admin/animes/{id}    删除番剧
 */

import { http } from './request.js'

/**
 * 后台登录。
 * @param {{username:string,password:string,remember?:boolean}} payload
 * @returns {Promise<{token:string,user:{username:string,nickname:string}}>}
 */
export function adminLogin(payload) {
  return http.post('/api/admin/login', {
    body: {
      username: String(payload.username ?? '').trim(),
      password: payload.password,
      remember: Boolean(payload.remember)
    }
  })
}

/** 当前管理员。同时充当「管理端 token 是否仍有效」的验证手段 */
export function fetchAdminMe() {
  return http.get('/api/admin/me', { auth: 'admin' })
}

/** 退出后台登录。无状态方案，前端随后清本地 token */
export function adminLogout() {
  return http.post('/api/admin/logout', { auth: 'admin' })
}

/** 番剧全量列表（含已下架） */
export function fetchAdminAnimes() {
  return http.get('/api/admin/animes', { auth: 'admin' })
}

/** 新增番剧 */
export function createAnime(payload) {
  return http.post('/api/admin/animes', { auth: 'admin', body: payload })
}

/** 编辑番剧 */
export function updateAnime(id, payload) {
  return http.put(`/api/admin/animes/${id}`, { auth: 'admin', body: payload })
}

/** 上下架切换。visible=false 后首页不再展示 */
export function toggleAnimeVisible(id, visible) {
  return http.put(`/api/admin/animes/${id}/visible`, {
    auth: 'admin',
    params: { visible }
  })
}

/** 删除番剧 */
export function deleteAnime(id) {
  return http.delete(`/api/admin/animes/${id}`, { auth: 'admin' })
}

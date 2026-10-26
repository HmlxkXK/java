/**
 * 时间格式化工具。
 *
 * 用途主要是展示 JWT 的过期时间（HomeView 的会话卡片）。
 * 后端返回的 createTime 已经是 'yyyy-MM-dd HH:mm:ss' 字符串，直接展示即可，
 * 但 JWT 里的 exp 是 Unix 秒，需要在这里转成人话。
 */

function pad(n) {
  return String(n).padStart(2, '0')
}

/**
 * 时间戳（毫秒）转 'YYYY-MM-DD HH:mm'
 * @param {number} ms
 */
export function formatDateTime(ms) {
  if (!Number.isFinite(ms)) return '—'
  const d = new Date(ms)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/**
 * 格式化「已是字符串」的时间值（后端 LocalDateTime 序列化产物）。
 * 兼容 '2026-10-26T08:59:00' 与 '2026-10-26 08:59:00' 两种形态；
 * 传进来的若是时间戳数字，退回 formatDateTime 处理。
 * @param {string|number} value
 */
export function formatDateTimeText(value) {
  if (typeof value === 'string' && value.trim()) {
    return value.replace('T', ' ').slice(0, 16)
  }
  return formatDateTime(value)
}

/**
 * 把「距离某个时间点还有多久」说成人话。
 * 用于展示 token 剩余有效期。
 *
 * @param {number} ms 目标时间戳（毫秒）
 * @returns {string} 例如 '6 天 23 小时' / '已过期'
 */
export function formatRemaining(ms) {
  const diff = ms - Date.now()
  if (!Number.isFinite(ms)) return '—'
  if (diff <= 0) return '已过期'

  const totalMinutes = Math.floor(diff / 60_000)
  const days = Math.floor(totalMinutes / 1440)
  const hours = Math.floor((totalMinutes % 1440) / 60)
  const minutes = totalMinutes % 60

  if (days > 0) return `${days} 天 ${hours} 小时`
  if (hours > 0) return `${hours} 小时 ${minutes} 分`
  return `${Math.max(1, minutes)} 分钟`
}

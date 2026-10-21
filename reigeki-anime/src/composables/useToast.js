/**
 * 轻量消息提示，替代原来的 alert()。
 *
 * 为什么换掉 alert：
 *   - 它会阻塞主线程，动画和输入框全部卡住；
 *   - 无法区分成功/失败，也没有样式，和这套界面完全不搭；
 *   - 一次只能弹一个，连点两次「获取验证码」会连弹两个模态框。
 *
 * 同样是模块级单例 + reactive，由 components/ToastHost.vue 在一个地方渲染。
 */

import { reactive } from 'vue'

/** 最多同时显示几条，避免刷屏把界面糊住 */
const MAX_VISIBLE = 4

const toasts = reactive([])
let seq = 0

function remove(id) {
  const index = toasts.findIndex((t) => t.id === id)
  if (index > -1) toasts.splice(index, 1)
}

/**
 * 弹一条提示。
 * @param {string} message
 * @param {'success'|'error'|'info'|'warn'} [type='info']
 * @param {number} [duration=3200] 毫秒，传 0 表示不自动消失
 * @returns {number} toast id
 */
export function pushToast(message, type = 'info', duration = 3200) {
  const text = String(message ?? '').trim()
  if (!text) return -1

  const id = ++seq
  toasts.push({ id, message: text, type })

  // 超出上限时先挤掉最早的
  while (toasts.length > MAX_VISIBLE) toasts.shift()

  if (duration > 0) setTimeout(() => remove(id), duration)
  return id
}

export function useToast() {
  return {
    toasts,
    remove,
    success: (msg, duration) => pushToast(msg, 'success', duration),
    error: (msg, duration) => pushToast(msg, 'error', duration),
    // 错误信息多来自后端，读的时间需要更长
    warn: (msg, duration) => pushToast(msg, 'warn', duration),
    info: (msg, duration) => pushToast(msg, 'info', duration)
  }
}

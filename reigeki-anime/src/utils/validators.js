/**
 * 表单校验规则。
 *
 * 这些规则与后端 dto/RegisterRequest.java、dto/LoginRequest.java 上的
 * Bean Validation 注解**一一对应**，阈值刻意保持一致：
 *   nickname  @NotBlank, @Size(max=50)
 *   email     @NotBlank, @Email, @Size(max=100)
 *   password  @NotBlank, @Size(min=8, max=64)
 *   code      @NotBlank, @Pattern(^\d{6}$)
 *
 * 前端校验只为「即时反馈、少一次无效请求」，**不是安全边界**，
 * 后端始终会再校验一遍。所以这里宁可宽松也不要更严，
 * 否则会出现「前端拦下但后端其实接受」的假报错。
 */

/** 与后端 AuthServiceImpl.EMAIL_PATTERN 完全相同的正则 */
const EMAIL_PATTERN = /^[^@\s]+@[^@\s]+\.[^@\s]+$/

const NICKNAME_MAX = 50
const EMAIL_MAX = 100
const PASSWORD_MIN = 8
const PASSWORD_MAX = 64

/** @returns {string} 错误文案，空串表示通过 */
export function validateNickname(value) {
  const v = String(value ?? '').trim()
  if (!v) return '请填写昵称'
  if (v.length > NICKNAME_MAX) return `昵称最长 ${NICKNAME_MAX} 个字符`
  return ''
}

/** @returns {string} 错误文案，空串表示通过 */
export function validateEmail(value) {
  const v = String(value ?? '').trim()
  if (!v) return '请填写邮箱'
  if (v.length > EMAIL_MAX) return `邮箱最长 ${EMAIL_MAX} 个字符`
  if (!EMAIL_PATTERN.test(v)) return '邮箱格式不正确'
  return ''
}

/** @returns {string} 错误文案，空串表示通过 */
export function validatePassword(value) {
  const v = String(value ?? '')
  if (!v) return '请填写密码'
  if (v.length < PASSWORD_MIN) return `密码至少 ${PASSWORD_MIN} 位`
  if (v.length > PASSWORD_MAX) return `密码最长 ${PASSWORD_MAX} 位`
  return ''
}

/** @returns {string} 错误文案，空串表示通过 */
export function validateConfirmPassword(password, confirm) {
  if (!confirm) return '请再输入一次密码'
  if (password !== confirm) return '两次输入的密码不一致'
  return ''
}

/** @returns {string} 错误文案，空串表示通过 */
export function validateCode(value) {
  const v = String(value ?? '').trim()
  if (!v) return '请填写邮箱验证码'
  if (!/^\d{6}$/.test(v)) return '验证码为 6 位数字'
  return ''
}

/** 登录页只要求非空，避免和后端的「账号或密码错误」重复提示 */
export function validateRequired(value, label) {
  return String(value ?? '').trim() ? '' : `请填写${label}`
}

/**
 * 密码强度评估，用于注册页的强度条。
 *
 * 只做启发式打分（长度 + 字符种类），不承诺任何安全等级——
 * 后端目前也没有强度要求（只要 8-64 位）。所以这是纯 UX 提示，
 * 即使得 0 分也不会阻止提交。
 *
 * @param {string} value
 * @returns {{score:number, label:string, hint:string}} score 0-4
 */
export function passwordStrength(value) {
  const v = String(value ?? '')
  if (!v) return { score: 0, label: '', hint: '' }

  let kinds = 0
  if (/[a-z]/.test(v)) kinds++
  if (/[A-Z]/.test(v)) kinds++
  if (/\d/.test(v)) kinds++
  if (/[^\w]/.test(v)) kinds++

  // 长度先定档，字符种类再往上加成，避免 "aaaaaaaa" 拿到高分
  let score = 0
  if (v.length >= PASSWORD_MIN) score = 1
  if (v.length >= 12) score = 2
  score = Math.min(4, score + Math.max(0, kinds - 1))

  if (v.length < PASSWORD_MIN) {
    return { score: 1, label: '太短', hint: `至少 ${PASSWORD_MIN} 位` }
  }
  const labels = ['很弱', '较弱', '中等', '较强', '很强']
  const hints = [
    '建议混合大小写、数字与符号',
    '建议混合大小写、数字与符号',
    '再长一些或加符号会更稳',
    '不错',
    '很好'
  ]
  return { score, label: labels[score], hint: hints[score] }
}

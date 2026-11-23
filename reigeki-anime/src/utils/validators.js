/**
 * 表单校验规则。
 *
 * 这些规则与后端 dto/RegisterRequest.java、dto/LoginRequest.java 上的
 * Bean Validation 注解**一一对应**，阈值刻意保持一致：
 *   nickname  @NotBlank, @Size(min=2,max=50), 账号字符规则
 *   password  @NotBlank, @Size(min=8, max=64)
 *
 * 注册不再需要邮箱：昵称即登录账号。图片验证码由前端 canvas 生成并在本地校验，
 * 不发送至后端。
 *
 * 前端校验只为「即时反馈、少一次无效请求」，**不是安全边界**，
 * 后端始终会再校验一遍。所以这里宁可宽松也不要更严，
 * 否则会出现「前端拦下但后端其实接受」的假报错。
 */

/** 昵称（登录账号）允许的字符：字母、数字、下划线、中文、连字符，长度 2-50 */
const NICKNAME_PATTERN = /^[\w\u4e00-\u9fa5-]+$/
const NICKNAME_MIN = 2
const NICKNAME_MAX = 50
const PASSWORD_MIN = 8
const PASSWORD_MAX = 64

/** @returns {string} 错误文案，空串表示通过 */
export function validateNickname(value) {
  const v = String(value ?? '').trim()
  if (!v) return '请填写昵称'
  if (v.length < NICKNAME_MIN) return `昵称至少 ${NICKNAME_MIN} 个字符`
  if (v.length > NICKNAME_MAX) return `昵称最长 ${NICKNAME_MAX} 个字符`
  if (!NICKNAME_PATTERN.test(v)) return '昵称只能包含字母、数字、下划线、中文和连字符'
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

/** 图片验证码：只校验是否填写，正确性由 canvas 组件本地比对 */
export function validateCode(value) {
  return String(value ?? '').trim() ? '' : '请填写图片验证码'
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

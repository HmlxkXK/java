<template>
  <main class="page">
    <section class="page-head" v-reveal>
      <div>
        <span class="badge">MEMBER · 社员中心</span>
        <h1>欢迎回来，<em>{{ displayName }}</em></h1>
        <p>这里的数据全部来自后端 <code>GET /api/auth/me</code>，不是前端硬编码的。</p>
      </div>
      <button type="button" class="ghost-btn" :disabled="loading" @click="load">
        {{ loading ? '刷新中…' : '重新校验会话' }}
      </button>
    </section>

    <p v-if="loadError" class="alert" role="alert">
      {{ loadError }}
      <button type="button" class="link-btn" @click="load">重试</button>
    </p>

    <div class="grid">
      <section class="panel" v-reveal>
        <h2>账号信息</h2>
        <dl class="kv">
          <div><dt>社员 ID</dt><dd>{{ user?.id ?? '—' }}</dd></div>
          <div><dt>昵称</dt><dd>{{ user?.nickname || '—' }}</dd></div>
          <div><dt>邮箱</dt><dd>{{ user?.email || '—' }}</dd></div>
          <div><dt>注册时间</dt><dd>{{ user?.createTime || '—' }}</dd></div>
        </dl>
        <p class="panel-note">
          昵称 / 密码现在可以直接改了，点右侧「修改资料」即可。
        </p>
        <button type="button" class="mini-btn panel-edit" @click="openEdit">修改资料</button>
      </section>

      <section class="panel" v-reveal="{ delay: 90 }">
        <h2>会话状态</h2>
        <dl class="kv">
          <div><dt>令牌类型</dt><dd>{{ tokenPayload?.sub ? 'Bearer · HS256' : '—' }}</dd></div>
          <div><dt>签发时间</dt><dd>{{ issuedAt ? formatDateTime(issuedAt) : '—' }}</dd></div>
          <div><dt>有效期至</dt><dd>{{ expiresAt ? formatDateTime(expiresAt) : '—' }}</dd></div>
          <div><dt>剩余</dt><dd :class="{ warn: isExpiringSoon }">{{ remaining }}</dd></div>
        </dl>
        <p class="panel-note">
          登录时勾选「记住我」会签发 7 天令牌，否则用后端默认有效期。
          令牌存在 localStorage，前端只解码 <code>exp</code> 做展示，签名由后端校验。
        </p>
      </section>
    </div>

    <section class="panel panel-wide" v-reveal>
      <h2>内容模块</h2>
      <p class="panel-note">
        以下模块的界面位置已留好，但后端目前只有 <code>/api/auth/*</code> 接口，
        没有对应的数据来源，所以这里不做假数据填充。
      </p>
      <div class="placeholders">
        <div v-for="m in modules" :key="m.title" class="placeholder">
          <span class="ph-ic" aria-hidden="true">{{ m.icon }}</span>
          <b>{{ m.title }}</b>
          <span>{{ m.desc }}</span>
          <em>等待后端接口</em>
        </div>
      </div>
    </section>

    <!-- 修改资料弹层 -->
    <Teleport to="body">
      <div v-if="editing" class="modal-mask" @click.self="closeEdit">
        <div class="modal" role="dialog" aria-modal="true" aria-label="修改资料">
          <div class="modal-head">
            <h3>修改资料</h3>
            <button type="button" class="modal-x" aria-label="关闭" @click="closeEdit">×</button>
          </div>

          <form novalidate @submit.prevent="onSave">
            <div class="field" :class="{ 'has-error': errors.nickname }">
              <label for="nick">昵称</label>
              <input id="nick" v-model.trim="form.nickname" type="text" maxlength="50" @input="clearError('nickname')" />
              <p v-if="errors.nickname" class="err">{{ errors.nickname }}</p>
            </div>

            <div class="divider"><span>如需改密码，两项都填；留空则只改昵称</span></div>

            <div class="field" :class="{ 'has-error': errors.oldPassword }">
              <label for="old">原密码</label>
              <input id="old" v-model="form.oldPassword" :type="showPwd ? 'text' : 'password'" autocomplete="current-password" @input="clearError('oldPassword')" />
              <p v-if="errors.oldPassword" class="err">{{ errors.oldPassword }}</p>
            </div>

            <div class="field" :class="{ 'has-error': errors.newPassword }">
              <label for="np">新密码</label>
              <input id="np" v-model="form.newPassword" :type="showPwd ? 'text' : 'password'" maxlength="64" autocomplete="new-password" @input="clearError('newPassword')" />
              <p v-if="errors.newPassword" class="err">{{ errors.newPassword }}</p>
            </div>

            <label class="eye-row">
              <input v-model="showPwd" type="checkbox" /> 显示密码
            </label>

            <div class="modal-actions">
              <button type="button" class="ghost-btn" :disabled="saving" @click="closeEdit">取消</button>
              <button type="submit" class="btn" :disabled="saving">
                {{ saving ? '保存中…' : '保存修改' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>
  </main>
</template>

<script setup>
/**
 * 登录后的社员中心（路由 /dashboard，需登录）。
 *
 * 相比最初版本的两处增强：
 *   1. **资料可改**：新增「修改资料」弹层，调用后端 PUT /api/auth/profile
 *      （昵称 / 密码，改密码需校验原密码），成功后即时刷新本地缓存；
 *   2. **进入动效**：各区块用 v-reveal 指令做滚动渐入，和落地页保持一致。
 */

import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useAuth } from '../composables/useAuth.js'
import { useToast } from '../composables/useToast.js'
import { updateProfile } from '../api/auth.js'
import { writeUser } from '../api/session.js'
import { formatDateTime, formatRemaining } from '../utils/datetime.js'
import { validateNickname, validatePassword } from '../utils/validators.js'

const { state, displayName, tokenPayload, expiresAt, refreshMe } = useAuth()

const user = computed(() => state.user)
const loading = ref(false)
const loadError = ref('')

const modules = [
  { icon: '📺', title: '追番进度', desc: '续看、收藏、更新提醒' },
  { icon: '💬', title: '社区动态', desc: '发帖、评论、同好互动' },
  { icon: '🛡️', title: '同好公会', desc: '建团、组队、排行' }
]

const issuedAt = computed(() => {
  const iat = tokenPayload.value?.iat
  return typeof iat === 'number' ? iat * 1000 : null
})

// 剩余有效期每分钟刷新一次，让倒计时看起来是活的
const clock = ref(Date.now())
let ticker = null

const remaining = computed(() => {
  clock.value // 建立依赖，让 ticker 推进时重新求值
  return expiresAt.value ? formatRemaining(expiresAt.value) : '—'
})

const isExpiringSoon = computed(
  () => Boolean(expiresAt.value) && expiresAt.value - Date.now() < 24 * 3600 * 1000
)

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    await refreshMe()
  } catch (error) {
    // 401 会由 request.js 通知上层清会话并跳登录，这里只处理其它错误
    if (error.code !== 401) loadError.value = error.message
  } finally {
    loading.value = false
  }
}

// ---------- 修改资料 ----------
const editing = ref(false)
const saving = ref(false)
const showPwd = ref(false)
const form = reactive({ nickname: '', oldPassword: '', newPassword: '' })
const errors = reactive({ nickname: '', oldPassword: '', newPassword: '' })

function openEdit() {
  form.nickname = user.value?.nickname || ''
  form.oldPassword = ''
  form.newPassword = ''
  errors.nickname = errors.oldPassword = errors.newPassword = ''
  editing.value = true
}

function closeEdit() {
  if (saving.value) return
  editing.value = false
}

function clearError(field) {
  errors[field] = ''
}

function validate() {
  errors.nickname = validateNickname(form.nickname)
  const wantPwd = Boolean(form.newPassword) || Boolean(form.oldPassword)
  if (wantPwd) {
    errors.newPassword = validatePassword(form.newPassword)
    if (!form.oldPassword) errors.oldPassword = '修改密码需先填写原密码'
  } else {
    errors.oldPassword = ''
  }
  // 昵称没改也没填密码：提示至少做一件事
  if (!errors.nickname && !wantPwd) errors.nickname = '请填写要修改的内容'
  return !errors.nickname && !errors.oldPassword && !errors.newPassword
}

const toast = useToast()

async function onSave() {
  if (saving.value) return
  if (!validate()) return

  saving.value = true
  try {
    const payload = { nickname: form.nickname || undefined }
    if (form.newPassword) {
      payload.oldPassword = form.oldPassword
      payload.newPassword = form.newPassword
    }
    const updated = await updateProfile(payload)
    // 同步本地缓存，顶部昵称立刻变化
    writeUser(updated)
    state.user = updated
    toast.success('资料已更新 🔥')
    editing.value = false
  } catch (error) {
    toast.error(error.message)
    if (/原密码/.test(error.message)) errors.oldPassword = error.message
    else if (/昵称/.test(error.message)) errors.nickname = error.message
    else if (/密码/.test(error.message)) errors.newPassword = error.message
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  load()
  ticker = setInterval(() => {
    clock.value = Date.now()
  }, 60_000)
})

onUnmounted(() => clearInterval(ticker))
</script>

<style scoped>
.panel-edit {
  margin-top: 16px;
}
.mini-btn {
  display: inline-block;
  padding: 8px 16px;
  border-radius: 10px;
  border: 1px solid var(--line);
  background: rgba(30,167,232,.12);
  color: var(--ember);
  font-size: 13px;
  cursor: pointer;
  font-family: inherit;
  transition: background .2s;
}
.mini-btn:hover:not(:disabled) { background: rgba(30,167,232,.22); }

/* ----- 修改资料弹层 ----- */
.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(6,3,9,.6);
  backdrop-filter: blur(6px);
  animation: maskIn .2s ease;
}
@keyframes maskIn { from { opacity: 0 } to { opacity: 1 } }
.modal {
  width: 420px;
  max-width: 92vw;
  background: var(--glass);
  border: 1px solid var(--line);
  border-radius: 18px;
  padding: 22px 24px 24px;
  backdrop-filter: blur(20px);
  box-shadow: 0 30px 80px rgba(0,0,0,.6);
  animation: popIn .25s cubic-bezier(.2,.7,.2,1);
}
.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.modal-head h3 { font-size: 18px; font-weight: 800; }
.modal-x {
  border: none; background: none; color: var(--muted);
  font-size: 24px; line-height: 1; cursor: pointer; padding: 0 4px;
}
.modal-x:hover { color: #fff; }

.field { margin-bottom: 14px; }
.field label { display: block; font-size: 13px; color: var(--muted); margin-bottom: 7px; letter-spacing: 1px; }
.field input {
  width: 100%;
  padding: 11px 13px;
  border-radius: 11px;
  border: 1px solid rgba(120,200,255,.25);
  background: rgba(0,0,0,.28);
  color: #fff;
  font-size: 15px;
  outline: none;
  transition: border-color .2s, box-shadow .2s;
  font-family: inherit;
}
.field input:focus { border-color: var(--fire-1); box-shadow: 0 0 0 3px rgba(30,167,232,.18); }
.field.has-error input { border-color: var(--fire-2); }
.err { font-size: 12px; color: var(--danger); margin-top: 6px; }

.divider {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 6px 0 16px;
  color: var(--muted);
  font-size: 12px;
}
.divider::before, .divider::after {
  content: ""; flex: 1; height: 1px; background: var(--line-soft);
}
.eye-row {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; color: var(--muted); margin-bottom: 18px; cursor: pointer;
}
.eye-row input { accent-color: var(--fire-1); }

.modal-actions { display: flex; gap: 12px; justify-content: flex-end; }
.modal-actions .btn { width: auto; padding: 11px 22px; letter-spacing: 1px; }
@media (prefers-reduced-motion: reduce){ .modal, .modal-mask { animation: none } }
</style>

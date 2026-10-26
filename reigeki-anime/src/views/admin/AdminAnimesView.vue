<template>
  <div class="animes">
    <!-- ===== 页头 ===== -->
    <header class="head">
      <div>
        <span class="badge">ANIME · 首页数据源</span>
        <h1>番剧管理</h1>
        <p>这里维护前台首页「本周热门番剧」板块展示的内容：增删改查、调整顺序、上下架。</p>
      </div>
      <div class="head-actions">
        <button type="button" class="ghost-btn" :disabled="loading" @click="load">
          {{ loading ? '刷新中…' : '刷新' }}
        </button>
        <button type="button" class="btn" @click="openCreate">+ 新增番剧</button>
      </div>
    </header>

    <p v-if="loadError" class="alert" role="alert">
      {{ loadError }}
      <button type="button" class="link-btn" @click="load">重试</button>
    </p>

    <!-- ===== 统计 ===== -->
    <div class="stats">
      <div class="stat"><b>{{ list.length }}</b><span>全部番剧</span></div>
      <div class="stat"><b>{{ visibleCount }}</b><span>首页展示中</span></div>
      <div class="stat"><b>{{ list.length - visibleCount }}</b><span>已下架</span></div>
    </div>

    <!-- ===== 列表 ===== -->
    <section class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th class="col-sort">顺序</th>
            <th>番剧</th>
            <th>标签</th>
            <th>评分</th>
            <th>话数</th>
            <th>状态</th>
            <th>更新时间</th>
            <th class="col-ops">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading && !list.length">
            <td colspan="8" class="empty">加载中…</td>
          </tr>
          <tr v-else-if="!list.length">
            <td colspan="8" class="empty">还没有番剧，点右上角「新增番剧」创建第一条数据</td>
          </tr>
          <tr v-for="a in list" :key="a.id" :class="{ 'row-hidden': !a.visible }">
            <td class="col-sort"><span class="sort-chip">{{ a.sort }}</span></td>
            <td>
              <div class="anime-cell">
                <span class="swatch" :style="{ background: a.cover || fallbackCover(a) }" aria-hidden="true"></span>
                <div class="anime-text">
                  <b>{{ a.title }}</b>
                  <span>{{ a.description || '—' }}</span>
                </div>
              </div>
            </td>
            <td><span class="tag-chip">{{ a.tag || '—' }}</span></td>
            <td><b class="rating">★ {{ a.rating || '—' }}</b></td>
            <td>{{ a.episode != null ? a.episode + ' 话' : '—' }}</td>
            <td>
              <label class="switch" :title="a.visible ? '点击下架' : '点击上架'">
                <input
                  type="checkbox"
                  :checked="a.visible"
                  :disabled="togglingId === a.id"
                  @change="onToggle(a)"
                />
                <span class="slider"></span>
              </label>
            </td>
            <td class="time">{{ formatDateTimeText(a.updateTime) }}</td>
            <td class="col-ops">
              <button type="button" class="op" @click="openEdit(a)">编辑</button>
              <button type="button" class="op op-danger" @click="confirmDelete(a)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </section>

    <!-- ===== 新增 / 编辑弹层 ===== -->
    <Teleport to="body">
      <div v-if="modal.open" class="modal-mask" @click.self="closeModal">
        <div class="modal" role="dialog" aria-modal="true" :aria-label="modal.isEdit ? '编辑番剧' : '新增番剧'">
          <div class="modal-head">
            <h3>{{ modal.isEdit ? '编辑番剧' : '新增番剧' }}</h3>
            <button type="button" class="modal-x" aria-label="关闭" @click="closeModal">×</button>
          </div>

          <form novalidate @submit.prevent="onSave">
            <div class="grid2">
              <div class="field" :class="{ 'has-error': errors.title }">
                <label for="f-title">番剧名 *</label>
                <input id="f-title" v-model.trim="form.title" type="text" maxlength="50" @input="clearError('title')" />
                <p v-if="errors.title" class="err">{{ errors.title }}</p>
              </div>
              <div class="field">
                <label for="f-tag">标签</label>
                <input id="f-tag" v-model.trim="form.tag" type="text" maxlength="20" placeholder="热血 / 奇幻 / 治愈…" />
              </div>
              <div class="field" :class="{ 'has-error': errors.rating }">
                <label for="f-rating">评分</label>
                <input id="f-rating" v-model.trim="form.rating" type="text" maxlength="4" placeholder="9.2" @input="clearError('rating')" />
                <p v-if="errors.rating" class="err">{{ errors.rating }}</p>
              </div>
              <div class="field" :class="{ 'has-error': errors.episode }">
                <label for="f-ep">更新至（话）</label>
                <input id="f-ep" v-model.number="form.episode" type="number" min="0" @input="clearError('episode')" />
                <p v-if="errors.episode" class="err">{{ errors.episode }}</p>
              </div>
              <div class="field" :class="{ 'has-error': errors.sort }">
                <label for="f-sort">展示顺序（越小越靠前）</label>
                <input id="f-sort" v-model.number="form.sort" type="number" min="0" @input="clearError('sort')" />
                <p v-if="errors.sort" class="err">{{ errors.sort }}</p>
              </div>
              <div class="field">
                <label class="check-line">
                  <input v-model="form.visible" type="checkbox" />
                  在首页展示（上架）
                </label>
              </div>
            </div>

            <div class="field" :class="{ 'has-error': errors.description }">
              <label for="f-desc">一句话简介</label>
              <input id="f-desc" v-model.trim="form.description" type="text" maxlength="100" @input="clearError('description')" />
              <p v-if="errors.description" class="err">{{ errors.description }}</p>
            </div>

            <div class="field">
              <label>封面渐变（卡片头图背景）</label>
              <div class="presets">
                <button
                  v-for="p in COVER_PRESETS"
                  :key="p"
                  type="button"
                  class="preset"
                  :class="{ active: form.cover === p }"
                  :style="{ background: p }"
                  :aria-label="'选择封面 ' + p"
                  @click="form.cover = p"
                ></button>
              </div>
              <input v-model.trim="form.cover" type="text" placeholder="也可直接粘贴 CSS 渐变，如 linear-gradient(135deg,#1ea7e8,#0b6fd0)" />
            </div>

            <div class="modal-actions">
              <button type="button" class="ghost-btn" :disabled="saving" @click="closeModal">取消</button>
              <button type="submit" class="btn" :disabled="saving">
                {{ saving ? '保存中…' : '保存' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>

    <!-- ===== 删除确认弹层 ===== -->
    <Teleport to="body">
      <div v-if="deleting" class="modal-mask" @click.self="deleting = null">
        <div class="modal modal-sm" role="dialog" aria-modal="true" aria-label="删除确认">
          <div class="modal-head">
            <h3>删除番剧</h3>
            <button type="button" class="modal-x" aria-label="关闭" @click="deleting = null">×</button>
          </div>
          <p class="confirm-text">
            确定删除「<b>{{ deleting?.title }}</b>」吗？删除后前台首页立刻不再展示，该操作不可恢复。
          </p>
          <div class="modal-actions">
            <button type="button" class="ghost-btn" :disabled="deletingBusy" @click="deleting = null">取消</button>
            <button type="button" class="btn btn-danger" :disabled="deletingBusy" @click="onDelete">
              {{ deletingBusy ? '删除中…' : '确认删除' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
/**
 * 番剧管理页。
 *
 * 管理前台首页「本周热门番剧」的全部数据：
 *   - 列表 / 新增 / 编辑 / 删除走 /api/admin/animes*（带管理端 token）；
 *   - 上下架开关直接调 visible 接口，改完首页立刻生效；
 *   - cover 存 CSS 渐变字符串，提供常用预设一键选择。
 */

import { computed, reactive, ref, onMounted } from 'vue'
import {
  fetchAdminAnimes,
  createAnime,
  updateAnime,
  toggleAnimeVisible,
  deleteAnime
} from '../../api/admin.js'
import { formatDateTimeText } from '../../utils/datetime.js'
import { useToast } from '../../composables/useToast.js'

const toast = useToast()

/* 表里没有渐变时的兜底封面 */
const FALLBACKS = [
  'linear-gradient(135deg,#1ea7e8,#0b6fd0)',
  'linear-gradient(135deg,#36d1dc,#5b86e5)',
  'linear-gradient(135deg,#7f53ff,#647dee)'
]
const COVER_PRESETS = [
  'linear-gradient(135deg,#1ea7e8,#0b6fd0)',
  'linear-gradient(135deg,#36d1dc,#5b86e5)',
  'linear-gradient(135deg,#5fd0ff,#2b86e0)',
  'linear-gradient(135deg,#9fd8ff,#cfeeff)',
  'linear-gradient(135deg,#7f53ff,#647dee)',
  'linear-gradient(135deg,#11998e,#38ef7d)'
]
function fallbackCover(a) {
  return FALLBACKS[(a.id || 0) % FALLBACKS.length]
}

/* ---------- 列表 ---------- */
const list = ref([])
const loading = ref(false)
const loadError = ref('')
const visibleCount = computed(() => list.value.filter((a) => a.visible).length)

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    list.value = await fetchAdminAnimes()
  } catch (error) {
    if (error.code !== 401) loadError.value = error.message
  } finally {
    loading.value = false
  }
}

/* ---------- 上下架 ---------- */
const togglingId = ref(null)

async function onToggle(anime) {
  togglingId.value = anime.id
  try {
    const updated = await toggleAnimeVisible(anime.id, !anime.visible)
    Object.assign(anime, updated)
    toast.success(updated.visible ? `「${updated.title}」已上架` : `「${updated.title}」已下架`)
  } catch (error) {
    toast.error(error.message)
  } finally {
    togglingId.value = null
  }
}

/* ---------- 新增 / 编辑弹层 ---------- */
const modal = reactive({ open: false, isEdit: false, id: null })
const saving = ref(false)
const form = reactive({
  title: '', description: '', tag: '', rating: '',
  episode: 12, sort: 0, visible: true, cover: COVER_PRESETS[0]
})
const errors = reactive({ title: '', description: '', rating: '', episode: '', sort: '' })

function clearError(field) {
  errors[field] = ''
}

function openCreate() {
  modal.isEdit = false
  modal.id = null
  Object.assign(form, {
    title: '', description: '', tag: '', rating: '',
    episode: 12, sort: list.value.length, visible: true, cover: COVER_PRESETS[0]
  })
  Object.keys(errors).forEach((k) => (errors[k] = ''))
  modal.open = true
}

function openEdit(anime) {
  modal.isEdit = true
  modal.id = anime.id
  Object.assign(form, {
    title: anime.title || '',
    description: anime.description || '',
    tag: anime.tag || '',
    rating: anime.rating || '',
    episode: anime.episode ?? 0,
    sort: anime.sort ?? 0,
    visible: Boolean(anime.visible),
    cover: anime.cover || COVER_PRESETS[0]
  })
  Object.keys(errors).forEach((k) => (errors[k] = ''))
  modal.open = true
}

function closeModal() {
  if (saving.value) return
  modal.open = false
}

function validate() {
  errors.title = form.title ? '' : '请输入番剧名'
  errors.description = (form.description || '').length <= 100 ? '' : '简介最长 100 字'
  const rating = (form.rating || '').trim()
  errors.rating = !rating || /^\d{1,2}(\.\d)?$/.test(rating)
    ? (Number(rating) > 10 ? '评分应在 0-10 之间' : '')
    : '评分格式如 9.2'
  errors.episode = form.episode == null || form.episode === '' || (Number.isInteger(form.episode) && form.episode >= 0)
    ? '' : '话数应为非负整数'
  errors.sort = form.sort != null && Number.isInteger(form.sort) && form.sort >= 0 ? '' : '顺序应为非负整数'
  return !errors.title && !errors.description && !errors.rating && !errors.episode && !errors.sort
}

async function onSave() {
  if (saving.value) return
  if (!validate()) return

  saving.value = true
  try {
    const payload = {
      title: form.title,
      description: form.description,
      tag: form.tag,
      rating: form.rating,
      episode: form.episode ?? 0,
      sort: form.sort ?? 0,
      visible: form.visible,
      cover: form.cover
    }
    const saved = modal.isEdit
      ? await updateAnime(modal.id, payload)
      : await createAnime(payload)
    toast.success(modal.isEdit ? '保存成功' : '新增成功')
    if (modal.isEdit) {
      const idx = list.value.findIndex((a) => a.id === modal.id)
      if (idx >= 0) list.value.splice(idx, 1, saved)
    } else {
      list.value.push(saved)
    }
    // 顺序可能变化，统一回源排一次
    list.value.sort((a, b) => a.sort - b.sort || a.id - b.id)
    modal.open = false
  } catch (error) {
    toast.error(error.message)
  } finally {
    saving.value = false
  }
}

/* ---------- 删除 ---------- */
const deleting = ref(null)
const deletingBusy = ref(false)

function confirmDelete(anime) {
  deleting.value = anime
}

async function onDelete() {
  if (!deleting.value || deletingBusy.value) return
  deletingBusy.value = true
  try {
    await deleteAnime(deleting.value.id)
    list.value = list.value.filter((a) => a.id !== deleting.value.id)
    toast.success(`「${deleting.value.title}」已删除`)
    deleting.value = null
  } catch (error) {
    toast.error(error.message)
  } finally {
    deletingBusy.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.animes { display: flex; flex-direction: column; gap: 22px; }

.head { display: flex; align-items: flex-end; justify-content: space-between; gap: 18px; flex-wrap: wrap; }
.head h1 { font-size: 30px; font-weight: 900; margin: 12px 0 8px; }
.head p { color: var(--muted); font-size: 14px; }
.head-actions { display: flex; gap: 12px; }

.stats { display: flex; gap: 16px; flex-wrap: wrap; }
.stat {
  display: flex; align-items: baseline; gap: 10px;
  padding: 14px 20px;
  border-radius: 14px;
  background: var(--glass);
  border: 1px solid var(--line-soft);
}
.stat b { font-size: 24px; font-weight: 900; font-variant-numeric: tabular-nums; }
.stat span { font-size: 13px; color: var(--muted); }

/* ----- 表格 ----- */
.table-wrap {
  border: 1px solid var(--line-soft);
  border-radius: 16px;
  background: var(--glass);
  overflow-x: auto;
}
.table { width: 100%; border-collapse: collapse; font-size: 13px; min-width: 760px; }
.table th, .table td { padding: 12px 14px; text-align: left; border-bottom: 1px solid var(--line-soft); }
.table thead th {
  color: var(--muted); font-weight: 600; font-size: 12px; letter-spacing: 1px;
  background: rgba(30, 167, 232, .06);
  white-space: nowrap;
}
.table tbody tr:last-child td { border-bottom: none; }
.table tbody tr { transition: background .15s; }
.table tbody tr:hover { background: rgba(30, 167, 232, .05); }
.row-hidden { opacity: .5; }
.empty { text-align: center; color: var(--muted); padding: 36px 0 !important; }

.col-sort { width: 60px; }
.sort-chip {
  display: inline-grid; place-items: center;
  width: 28px; height: 28px; border-radius: 9px;
  background: rgba(30, 167, 232, .12);
  color: var(--ember); font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.anime-cell { display: flex; align-items: center; gap: 12px; min-width: 220px; }
.swatch {
  width: 52px; height: 34px; border-radius: 9px; flex: 0 0 auto;
  border: 1px solid var(--line-soft);
}
.anime-text b { display: block; font-size: 14px; }
.anime-text span {
  display: block; font-size: 12px; color: var(--muted);
  max-width: 320px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}

.tag-chip {
  display: inline-block; padding: 4px 10px; border-radius: 999px;
  background: rgba(255, 255, 255, .07);
  border: 1px solid var(--line-soft);
  font-size: 12px; white-space: nowrap;
}
.rating { color: #9fe4ff; white-space: nowrap; }
.time { color: var(--muted); white-space: nowrap; }

/* 上下架开关 */
.switch { position: relative; display: inline-block; width: 40px; height: 22px; cursor: pointer; }
.switch input { opacity: 0; width: 0; height: 0; }
.slider {
  position: absolute; inset: 0; border-radius: 999px;
  background: rgba(255, 255, 255, .14);
  transition: background .2s;
}
.slider::before {
  content: ""; position: absolute; left: 3px; top: 3px;
  width: 16px; height: 16px; border-radius: 50%;
  background: #fff; transition: transform .2s;
}
.switch input:checked + .slider { background: var(--fire-1); }
.switch input:checked + .slider::before { transform: translateX(18px); }
.switch input:disabled + .slider { opacity: .6; }

/* 操作列 */
.col-ops { width: 130px; white-space: nowrap; }
.op {
  padding: 6px 12px; margin-right: 8px;
  border-radius: 9px;
  border: 1px solid var(--line);
  background: rgba(30, 167, 232, .12);
  color: var(--ember);
  font-size: 12px; cursor: pointer; font-family: inherit;
  transition: background .2s;
}
.op:hover { background: rgba(30, 167, 232, .24); }
.op-danger { border-color: rgba(255, 90, 90, .4); background: rgba(255, 61, 61, .1); color: var(--danger); }
.op-danger:hover { background: rgba(255, 61, 61, .2); }

/* ----- 弹层 ----- */
.modal-mask {
  position: fixed; inset: 0; z-index: 80;
  display: grid; place-items: center; padding: 20px;
  background: rgba(6, 3, 9, .6);
  backdrop-filter: blur(6px);
  animation: maskIn .2s ease;
}
@keyframes maskIn { from { opacity: 0 } to { opacity: 1 } }
.modal {
  width: 560px; max-width: 94vw; max-height: 88vh; overflow-y: auto;
  background: var(--glass);
  border: 1px solid var(--line);
  border-radius: 18px;
  padding: 22px 24px 24px;
  backdrop-filter: blur(20px);
  box-shadow: 0 30px 80px rgba(0, 0, 0, .6);
  animation: popIn .25s cubic-bezier(.2, .7, .2, 1);
}
.modal-sm { width: 420px; }
.modal-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.modal-head h3 { font-size: 18px; font-weight: 800; }
.modal-x {
  border: none; background: none; color: var(--muted);
  font-size: 24px; line-height: 1; cursor: pointer; padding: 0 4px;
}
.modal-x:hover { color: #fff; }

.grid2 { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }
.field { margin-bottom: 14px; }
.field label { display: block; font-size: 13px; color: var(--muted); margin-bottom: 7px; letter-spacing: 1px; }
.field input {
  width: 100%;
  padding: 11px 13px;
  border-radius: 11px;
  border: 1px solid rgba(120, 200, 255, .25);
  background: rgba(0, 0, 0, .28);
  color: #fff;
  font-size: 14px;
  outline: none;
  transition: border-color .2s, box-shadow .2s;
  font-family: inherit;
}
.field input:focus { border-color: var(--fire-1); box-shadow: 0 0 0 3px rgba(30, 167, 232, .18); }
.field.has-error input { border-color: var(--danger); }
.err { font-size: 12px; color: var(--danger); margin-top: 6px; }
.check-line { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.check-line input { accent-color: var(--fire-1); width: 15px; height: 15px; }

/* 封面预设 */
.presets { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 8px; }
.preset {
  width: 46px; height: 30px; border-radius: 8px;
  border: 2px solid transparent; cursor: pointer; padding: 0;
  transition: transform .15s, border-color .15s;
}
.preset:hover { transform: translateY(-2px); }
.preset.active { border-color: #fff; box-shadow: 0 0 0 2px rgba(30, 167, 232, .5); }

.confirm-text { font-size: 14px; line-height: 1.7; color: var(--text); margin-bottom: 18px; }

.modal-actions { display: flex; gap: 12px; justify-content: flex-end; margin-top: 6px; }
.modal-actions .btn { width: auto; padding: 11px 22px; }
.btn-danger {
  background: linear-gradient(90deg, #ff5a5a, #d0342c);
  color: #fff;
}
@media (prefers-reduced-motion: reduce) { .modal, .modal-mask { animation: none } }
@media (max-width: 640px) { .grid2 { grid-template-columns: 1fr; } }
</style>

<template>
  <main class="page watch-page">
    <nav class="crumbs" v-reveal>
      <router-link to="/" class="back">← 返回首页</router-link>
      <span class="sep">/</span>
      <span class="here">{{ anime?.title || '加载中…' }}</span>
    </nav>

    <p v-if="loadError" class="alert" role="alert">
      {{ loadError }}
      <button type="button" class="link-btn" @click="load">重试</button>
    </p>

    <template v-else>
      <!-- ===== 播放器 ===== -->
      <section class="player-shell" v-reveal>
        <div class="player-box">
          <!-- key 换集时强制重建 video 元素，避免浏览器复用旧流的缓冲 -->
          <video
            v-if="current"
            :key="current.id"
            ref="videoEl"
            class="player"
            :src="videoUrl(current.id)"
            :poster="undefined"
            controls
            playsinline
            preload="metadata"
            @ended="onEnded"
          ></video>
          <div v-else class="player player-empty">
            <span>{{ episodesLoading ? '正在加载剧集…' : '这部番剧还没有上传任何剧集' }}</span>
          </div>
        </div>

        <header class="player-head">
          <div class="player-title">
            <h1>{{ anime?.title || '…' }}</h1>
            <p class="player-desc">{{ anime?.description }}</p>
          </div>
          <div class="player-badges">
            <span v-if="anime?.rating" class="badge-pill">★ {{ anime.rating }}</span>
            <span v-if="anime?.tag" class="badge-pill">{{ anime.tag }}</span>
          </div>
        </header>

        <p class="player-note">
          视频经后端 <code>GET /api/videos/{id}</code> 以 HTTP Range 分段流式传输，
          支持拖动进度条，不必等整段下载完。
        </p>
      </section>

      <!-- ===== 选集 ===== -->
      <section class="panel panel-wide" v-reveal>
        <h2>选集 <small v-if="episodes.length">共 {{ episodes.length }} 集</small></h2>
        <div v-if="episodesLoading" class="ep-skeletons" aria-hidden="true">
          <span v-for="i in 2" :key="i" class="skeleton-block sk-ep"></span>
        </div>
        <div v-else-if="episodes.length" class="ep-list">
          <button
            v-for="ep in episodes"
            :key="ep.id"
            type="button"
            class="ep-item"
            :class="{ active: current && current.id === ep.id }"
            @click="switchEpisode(ep)"
          >
            <b>{{ ep.episodeNo }}. {{ ep.title || `第 ${ep.episodeNo} 集` }}</b>
            <span v-if="ep.durationSeconds">{{ formatDuration(ep.durationSeconds) }}</span>
            <em v-if="current && current.id === ep.id">播放中</em>
          </button>
        </div>
        <p v-else class="panel-note">管理员还没有为这部番剧添加剧集。</p>
      </section>
    </template>
  </main>
</template>

<script setup>
/**
 * 播放页（路由 /watch/:animeId）。
 *
 * 数据全部来自后端：
 *   - 番剧信息      GET /api/animes/:id
 *   - 剧集列表      GET /api/animes/:id/episodes
 *   - 视频本体      GET /api/videos/:episodeId（video 标签原生请求，
 *                    Range 分段逻辑在浏览器与后端之间完成，前端不干预）
 *
 * 换集直接改 current，video 元素因 :key 变化整体重建，不会出现
 * 上一集的缓冲残留。一集播完自动连播下一集（onEnded）。
 */

import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchAnime, fetchEpisodes, videoUrl } from '../api/anime.js'

const route = useRoute()
const router = useRouter()

const anime = ref(null)
const episodes = ref([])
const current = ref(null)
const episodesLoading = ref(false)
const loadError = ref('')

const videoEl = ref(null)

/** 当前集数（找不到时按第 1 集播） */
const currentEpisodeNo = computed(() =>
  current.value ? current.value.episodeNo : 0
)

function formatDuration(seconds) {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return m > 0 ? `${m}分${s}秒` : `${s}秒`
}

async function load() {
  const animeId = Number(route.params.animeId)
  if (!Number.isInteger(animeId) || animeId <= 0) {
    router.replace({ name: 'not-found' })
    return
  }
  loadError.value = ''
  episodesLoading.value = true
  try {
    const [info, eps] = await Promise.all([
      fetchAnime(animeId),
      fetchEpisodes(animeId).catch(() => [])
    ])
    anime.value = info
    episodes.value = eps
    // URL 上带 ?ep=2 时直达指定集，否则从第 1 集开始
    const wanted = Number(route.query.ep)
    current.value =
      eps.find((e) => e.episodeNo === wanted) || eps[0] || null
  } catch (error) {
    loadError.value = error.message
  } finally {
    episodesLoading.value = false
  }
}

function switchEpisode(ep) {
  current.value = ep
  // 换集同步到地址栏，刷新/分享后仍停在这一集
  router.replace({ query: { ...route.query, ep: ep.episodeNo } })
}

/** 播完自动连播下一集 */
function onEnded() {
  const next = episodes.value.find((e) => e.episodeNo === currentEpisodeNo.value + 1)
  if (next) switchEpisode(next)
}

// 路由参数变化（同页面不同番剧）时重新加载
watch(() => route.params.animeId, (v, old) => {
  if (v !== old && route.name === 'watch') load()
})

onMounted(load)
</script>

<style scoped>
.watch-page {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.crumbs {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: var(--muted);
}
.crumbs .back {
  color: var(--ember);
  text-decoration: none;
}
.crumbs .back:hover { text-decoration: underline; }
.crumbs .here {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 60vw;
}

/* ===== 播放器 ===== */
.player-shell {
  background: var(--glass);
  border: 1px solid var(--line);
  border-radius: 18px;
  padding: 14px;
  backdrop-filter: blur(14px);
}
.player-box {
  border-radius: 12px;
  overflow: hidden;
  background: #000;
  box-shadow: 0 14px 40px rgba(0,0,0,.45);
}
.player {
  display: block;
  width: 100%;
  aspect-ratio: 16 / 9;
  background: #000;
}
.player-empty {
  display: grid;
  place-items: center;
  color: var(--muted);
  font-size: 14px;
}

.player-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 6px 0;
}
.player-title h1 { font-size: 20px; margin-bottom: 4px; }
.player-desc {
  font-size: 13px;
  color: var(--muted);
  line-height: 1.7;
}
.player-badges {
  display: flex;
  gap: 8px;
  flex: none;
}
.badge-pill {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  background: rgba(30,167,232,.14);
  color: var(--ember);
  border: 1px solid var(--line-soft);
}

.player-note {
  margin-top: 10px;
  padding: 0 6px;
  font-size: 12px;
  color: var(--muted);
}

/* ===== 选集 ===== */
.ep-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 10px;
}
.ep-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  text-align: left;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid var(--line-soft);
  background: rgba(0,0,0,.22);
  color: #fff;
  cursor: pointer;
  font-family: inherit;
  transition: background .2s, border-color .2s, transform .2s;
}
.ep-item:hover {
  background: rgba(30,167,232,.1);
  border-color: rgba(30,167,232,.4);
  transform: translateY(-2px);
}
.ep-item.active {
  border-color: var(--fire-1);
  background: rgba(30,167,232,.16);
}
.ep-item b {
  font-size: 13px;
  font-weight: 700;
  line-height: 1.4;
}
.ep-item span { font-size: 11px; color: var(--muted); }
.ep-item em {
  position: absolute;
  top: 8px;
  right: 10px;
  font-size: 10px;
  font-style: normal;
  color: var(--ember);
  letter-spacing: 1px;
}

.ep-skeletons {
  display: flex;
  gap: 10px;
}
.sk-ep {
  flex: 0 0 210px;
  height: 64px;
  border-radius: 12px;
}

@media (max-width: 640px) {
  .player-head { flex-direction: column; }
}
</style>

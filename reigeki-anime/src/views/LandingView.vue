<template>
  <div class="bili-page">
    <ThreeScene variant="light" />
    <!-- ===== 分区入口：两行圆形图标 ===== -->
    <section class="channel-section" aria-label="分区导航">
      <div class="channel-grid">
        <button
          v-for="(c, i) in channels"
          :key="c.name"
          type="button"
          class="channel"
          @click="goChannel(c)"
          v-reveal="{ delay: i * 25 }"
        >
          <span class="channel-ic" :style="{ background: c.bg }" aria-hidden="true">{{ c.icon }}</span>
          <span>{{ c.name }}</span>
        </button>
      </div>
    </section>

    <!-- ===== 主内容：左侧轮播 + 右侧视频网格 ===== -->
    <main class="main-wrap">
      <div class="primary-layout">
        <!-- 左侧大轮播 -->
        <div class="carousel" aria-label="推荐轮播">
          <div
            v-for="(s, i) in slides"
            :key="s.title"
            class="slide"
            :class="{ active: i === slideIndex }"
            :style="{ backgroundImage: `url(${s.image})` }"
          >
            <div class="slide-shade"></div>
            <div class="slide-caption">
              <span class="slide-badge">{{ s.badge }}</span>
              <h2>{{ s.title }}</h2>
              <p>{{ s.subtitle }}</p>
            </div>
          </div>
          <button type="button" class="carousel-nav prev" @click="prevSlide" aria-label="上一张">‹</button>
          <button type="button" class="carousel-nav next" @click="nextSlide" aria-label="下一张">›</button>
          <div class="carousel-dots">
            <button
              v-for="(s, i) in slides"
              :key="i"
              type="button"
              :class="{ on: i === slideIndex }"
              @click="slideIndex = i"
              :aria-label="`第 ${i + 1} 张`"
            ></button>
          </div>
        </div>

        <!-- 右侧视频卡片 -->
        <div class="video-grid" id="animes">
          <template v-if="animesLoading">
            <div v-for="i in 6" :key="i" class="video-card anime-skeleton">
              <span class="skeleton-block sk-cover"></span>
              <span class="skeleton-block" style="height: 16px; width: 88%; margin: 9px 0 6px"></span>
              <span class="skeleton-block" style="height: 12px; width: 55%"></span>
            </div>
          </template>

          <div v-else-if="animesError" class="video-fallback" role="alert">
            <p>番剧加载失败：{{ animesError }}</p>
            <button type="button" class="retry-btn" @click="loadAnimes">重试</button>
          </div>

          <div v-else-if="!filtered.length" class="video-fallback" role="status">
            <p>没有匹配的番剧，换个关键词或分区试试</p>
            <button type="button" class="retry-btn" @click="router.push({ path: '/' })">查看全部</button>
          </div>

          <template v-else>
            <article
              v-for="(v, i) in visibleAnimes"
              :key="v.id"
              class="video-card"
              v-reveal="{ delay: i * 45 }"
            >
              <router-link class="video-cover" :to="`/watch/${v.id}`">
                <img :src="v.image" :alt="v.title" loading="lazy" />
                <span class="video-stats">
                  <span>▶ {{ v.views }}</span>
                  <span>💬 {{ v.danmaku }}</span>
                </span>
                <span class="video-duration">{{ v.duration }}</span>
              </router-link>
              <div class="video-info">
                <h3 :title="v.title">{{ v.title }}</h3>
                <div class="video-meta">
                  <span class="up">📺 {{ v.up }}</span>
                  <span>{{ v.time }}</span>
                </div>
              </div>
            </article>
          </template>
        </div>
      </div>
    </main>

    <!-- ===== 换一换 / 更多推荐 ===== -->
    <section class="more-section">
      <div class="section-bar">
        <h2>为你推荐</h2>
        <button type="button" class="refresh-btn" @click="refreshAnimes">🔄 换一换</button>
      </div>
      <div class="video-grid video-grid--wide" v-reveal>
        <article v-for="(v, i) in moreAnimes" :key="`m-${v.id}`" class="video-card">
          <router-link class="video-cover" :to="`/watch/${v.id}`">
            <img :src="v.image" :alt="v.title" loading="lazy" />
            <span class="video-stats">
              <span>▶ {{ v.views }}</span>
              <span>💬 {{ v.danmaku }}</span>
            </span>
            <span class="video-duration">{{ v.duration }}</span>
          </router-link>
          <div class="video-info">
            <h3 :title="v.title">{{ v.title }}</h3>
            <div class="video-meta">
              <span class="up">📺 {{ v.up }}</span>
              <span>{{ v.time }}</span>
            </div>
          </div>
        </article>
      </div>
    </section>

    <footer class="bili-footer">
      <span>🔥 燃尽动漫</span>
      <span>一个演示用的二次元社区 · 数据来自本地后端</span>
    </footer>
  </div>
</template>

<script setup>
/**
 * 公开首页（B 站风格）。
 *
 * 布局对应设计稿：分区入口 → 左侧轮播 + 右侧视频网格 → 为你推荐。
 * 番剧标题 / 标签来自后端 GET /api/animes；封面用本地生成的图片，
 * 播放量、弹幕、UP 主等为演示性展示数据（后端暂无这些字段）。
 */
import { computed, ref, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchPublicAnimes } from '../api/anime.js'
import ThreeScene from '../components/ThreeScene.vue'

const route = useRoute()
const router = useRouter()

/* 分区点击：内容分区滚到本页番剧区并按标签过滤；个人中心类跳社员中心 */
function goChannel(c) {
  if (c.to) {
    router.push(c.to)
    return
  }
  router.push({ path: '/', hash: '#animes', query: c.tag ? { tag: c.tag } : {} })
}

const slides = [
  { title: '碧海之章', subtitle: '少年与深海共生，在潮汐尽头点亮最后的光', badge: '独家 · 热血', image: '/images/carousel-hero.png' },
  { title: '樱落之时', subtitle: '转学少女与一段被遗忘的约定', badge: '治愈 · 新番', image: '/images/cover-samurai.png' },
  { title: '星轨彼岸', subtitle: '跨越光年的通讯，连接两个文明的少年', badge: '科幻 · 推荐', image: '/images/cover-astronaut.png' }
]
const slideIndex = ref(0)
let slideTimer = null
function nextSlide() { slideIndex.value = (slideIndex.value + 1) % slides.length }
function prevSlide() { slideIndex.value = (slideIndex.value - 1 + slides.length) % slides.length }
onMounted(() => { slideTimer = setInterval(nextSlide, 5000) })
onUnmounted(() => clearInterval(slideTimer))

/*
 * 分区入口：带 tag 的滚到本页番剧区按标签过滤；
 * 带 to 的跳真实路由（社员中心）。后端番剧的 tag 目前是
 * 热血 / 奇幻 / 动作 / 治愈 / 科幻 / 日常，其余分区先归到「番剧」全集。
 */
const channels = [
  { name: '动态', icon: '🌊', bg: 'linear-gradient(135deg,#ffb64d,#ff8a3d)', to: '/dashboard' },
  { name: '热门', icon: '🔥', bg: 'linear-gradient(135deg,#ff6a6a,#f2355b)', tag: '' },
  { name: '番剧', icon: '📺', bg: 'linear-gradient(135deg,#7ec8ff,#3b9dff)', to: '/anime' },
  { name: '国创', icon: '🐉', bg: 'linear-gradient(135deg,#9ad9ff,#4aa3ff)', tag: '奇幻' },
  { name: '综艺', icon: '🎤', bg: 'linear-gradient(135deg,#ffd166,#ff9f43)', tag: '日常' },
  { name: '动画', icon: '✨', bg: 'linear-gradient(135deg,#c8a2ff,#8f6dff)', tag: '' },
  { name: '鬼畜', icon: '😜', bg: 'linear-gradient(135deg,#a0e8ff,#4fc3f7)', tag: '' },
  { name: '舞蹈', icon: '💃', bg: 'linear-gradient(135deg,#ffb3d1,#ff6fa5)', tag: '' },
  { name: '娱乐', icon: '🎪', bg: 'linear-gradient(135deg,#ffcf8b,#ff9d5c)', tag: '' },
  { name: '科技数码', icon: '💻', bg: 'linear-gradient(135deg,#a8c7fa,#6f9dff)', tag: '科幻' },
  { name: '美食', icon: '🍜', bg: 'linear-gradient(135deg,#ffcf9e,#ff9f68)', tag: '日常' },
  { name: '汽车', icon: '🚗', bg: 'linear-gradient(135deg,#b7d3ff,#7aa8ff)', tag: '' },
  { name: '游戏', icon: '🎮', bg: 'linear-gradient(135deg,#c4b5fd,#8b7cf6)', tag: '动作' },
  { name: '音乐', icon: '🎵', bg: 'linear-gradient(135deg,#ffb3c6,#ff7096)', tag: '' },
  { name: '影视', icon: '🎬', bg: 'linear-gradient(135deg,#9fb8ff,#6a86ff)', tag: '' },
  { name: '知识', icon: '📚', bg: 'linear-gradient(135deg,#8fd6c4,#4fbfa2)', tag: '科幻' },
  { name: '资讯', icon: '📰', bg: 'linear-gradient(135deg,#b0bec5,#78909c)', tag: '' },
  { name: '小剧场', icon: '🎭', bg: 'linear-gradient(135deg,#ffc1e0,#ff7fb8)', tag: '治愈' },
  { name: '时尚美妆', icon: '💄', bg: 'linear-gradient(135deg,#ffb6c1,#ff7a90)', tag: '' },
  { name: '直播', icon: '📡', bg: 'linear-gradient(135deg,#ff9a9e,#f86a8d)', tag: '' }
]

/* 封面图按顺序对应后端番剧 id；后端无图片字段时用本地生成图补齐 */
const COVERS = [
  '/images/cover-hero.png',
  '/images/cover-friends.png',
  '/images/cover-pink.png',
  '/images/cover-chick.png',
  '/images/cover-cpu.png',
  '/images/cover-portrait.png',
  '/images/cover-whale.png',
  '/images/cover-samurai.png',
  '/images/cover-astronaut.png',
  '/images/cover-cat.png',
  '/images/banner-autumn.png',
  '/images/carousel-hero.png'
]
const UPS = ['燃尽编辑部', '深海放映厅', '次元观察员', '番剧小分队', '像素研究所', '光影记录者']
const VIEW_SAMPLES = ['128.6万', '26.3万', '17.1万', '65.7万', '48.5万', '10.9万']
const DANMAKU_SAMPLES = ['1.2万', '8941', '6.8万', '4465', '3721', '9631']
const TIME_SAMPLES = ['昨天', '2 天前', '3 天前', '4 天前', '5 天前', '1 周前']
const DURATION_SAMPLES = ['12:36', '08:12', '15:47', '05:23', '20:53', '03:41']

const animes = ref([])
const animesLoading = ref(false)
const animesError = ref('')
const offset = ref(0)

function decorate(list) {
  return list.map((a, i) => ({
    ...a,
    image: COVERS[i % COVERS.length],
    views: VIEW_SAMPLES[i % VIEW_SAMPLES.length],
    danmaku: DANMAKU_SAMPLES[i % DANMAKU_SAMPLES.length],
    duration: DURATION_SAMPLES[i % DURATION_SAMPLES.length],
    up: UPS[i % UPS.length],
    time: TIME_SAMPLES[i % TIME_SAMPLES.length]
  }))
}

const decorated = computed(() => decorate(animes.value))
const searchQuery = computed(() => (route.query.q || '').trim())
const tagQuery = computed(() => (route.query.tag || '').trim())

/* 搜索框（q）与分区入口（tag）共用一套过滤；两者都为空时展示全部 */
const filtered = computed(() => {
  let list = decorated.value
  const q = searchQuery.value.toLowerCase()
  if (q) {
    list = list.filter(
      (a) => a.title.toLowerCase().includes(q) || (a.tag || '').toLowerCase().includes(q)
    )
  }
  if (tagQuery.value) {
    list = list.filter((a) => (a.tag || '') === tagQuery.value)
  }
  return list
})

/* 右侧 6 张：从当前偏移位置循环取；“换一换”滚动 offset */
const visibleAnimes = computed(() => {
  const list = filtered.value
  if (!list.length) return []
  return Array.from({ length: Math.min(6, list.length) }, (_, i) => list[(offset.value + i) % list.length])
})
const moreAnimes = computed(() => {
  const list = filtered.value
  if (!list.length) return []
  return Array.from({ length: Math.min(6, list.length) }, (_, i) => list[(offset.value + 3 + i) % list.length])
})

function refreshAnimes() {
  if (!filtered.value.length) return
  offset.value = (offset.value + 2) % filtered.value.length
}

async function loadAnimes() {
  animesLoading.value = true
  animesError.value = ''
  try {
    animes.value = await fetchPublicAnimes()
  } catch (error) {
    animesError.value = error.message
  } finally {
    animesLoading.value = false
  }
}

watch(() => [route.query.q, route.query.tag], () => { offset.value = 0 })

onMounted(() => { loadAnimes() })
</script>

<style scoped>
.bili-page {
  position: relative;
  z-index: 0; /* 建立堆叠上下文，让内部 z-index:-1 的 3D 层落在白底之上、内容之下 */
  min-height: 100vh;
  background: #fff;
  color: #18191c;
  padding-bottom: 0;
}

/* ===== 分区入口 ===== */
.channel-section {
  background: #fff;
  padding: 22px 0 6px;
}
.channel-grid {
  max-width: 1500px;
  margin: 0 auto;
  padding: 0 3vw;
  display: grid;
  grid-template-columns: repeat(10, minmax(0, 1fr));
  gap: 18px 8px;
}
.channel {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 7px;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
  font-family: inherit;
  color: #18191c;
  text-decoration: none;
  font-size: 13px;
  transition: transform .2s, color .2s;
}
.channel:hover { transform: translateY(-4px); color: #fb7299; }
.channel-ic {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 22px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, .08);
}

/* ===== 主内容 ===== */
.main-wrap {
  max-width: 1500px;
  margin: 0 auto;
  padding: 14px 3vw 0;
}
.primary-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.02fr);
  gap: 18px;
  align-items: start;
}

/* 轮播 */
.carousel {
  position: relative;
  aspect-ratio: 16 / 10;
  border-radius: 10px;
  overflow: hidden;
  background: #0b1622;
}
.slide {
  position: absolute;
  inset: 0;
  background-position: center;
  background-size: cover;
  opacity: 0;
  transition: opacity .6s ease;
}
.slide.active { opacity: 1; }
.slide-shade {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 45%, rgba(0, 0, 0, .78));
}
.slide-caption {
  position: absolute;
  left: 22px;
  right: 22px;
  bottom: 46px;
  color: #fff;
}
.slide-badge {
  display: inline-block;
  padding: 3px 9px;
  border-radius: 4px;
  background: #fb7299;
  font-size: 11px;
  font-weight: 700;
  margin-bottom: 10px;
}
.slide-caption h2 { font-size: clamp(20px, 2.4vw, 32px); font-weight: 900; }
.slide-caption p { margin-top: 8px; font-size: 13px; opacity: .9; }
.carousel-nav {
  position: absolute;
  top: 50%;
  translate: 0 -50%;
  width: 34px;
  height: 52px;
  border: 0;
  border-radius: 8px;
  background: rgba(0, 0, 0, .3);
  color: #fff;
  font-size: 28px;
  line-height: 1;
  cursor: pointer;
  opacity: 0;
  transition: opacity .2s, background .2s;
}
.carousel:hover .carousel-nav { opacity: 1; }
.carousel-nav:hover { background: rgba(0, 0, 0, .55); }
.carousel-nav.prev { left: 8px; }
.carousel-nav.next { right: 8px; }
.carousel-dots {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 16px;
  display: flex;
  justify-content: center;
  gap: 8px;
}
.carousel-dots button {
  width: 8px;
  height: 8px;
  border: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, .5);
  cursor: pointer;
  transition: width .2s, background .2s;
}
.carousel-dots button.on { width: 20px; border-radius: 4px; background: #fb7299; }

/* 视频卡片网格 */
.video-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px 10px;
}
.video-card { min-width: 0; }
.video-cover {
  position: relative;
  display: block;
  aspect-ratio: 16 / 10;
  border-radius: 8px;
  overflow: hidden;
  background: #f1f2f3;
}
.video-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform .3s;
}
.video-card:hover .video-cover img { transform: scale(1.05); }
.video-stats {
  position: absolute;
  left: 8px;
  right: 8px;
  bottom: 7px;
  display: flex;
  gap: 10px;
  color: #fff;
  font-size: 11px;
  text-shadow: 0 1px 3px rgba(0, 0, 0, .8);
}
.video-duration {
  position: absolute;
  right: 6px;
  bottom: 6px;
  padding: 1px 5px;
  border-radius: 3px;
  background: rgba(0, 0, 0, .6);
  color: #fff;
  font-size: 11px;
}
.video-stats { right: 54px; }
.video-info { padding: 7px 1px 0; }
.video-info h3 {
  font-size: 13px;
  font-weight: 500;
  line-height: 1.45;
  color: #18191c;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 38px;
}
.video-card:hover .video-info h3 { color: #fb7299; }
.video-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  margin-top: 3px;
  font-size: 11px;
  color: #9499a0;
}
.video-meta .up {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.video-fallback {
  grid-column: 1 / -1;
  padding: 30px;
  border: 1px dashed #d6d9dd;
  border-radius: 10px;
  color: #9499a0;
  text-align: center;
  font-size: 14px;
}
.retry-btn {
  margin-top: 10px;
  padding: 6px 16px;
  border: 0;
  border-radius: 6px;
  background: #fb7299;
  color: #fff;
  cursor: pointer;
}

/* 骨架 */
.skeleton-block {
  display: block;
  border-radius: 6px;
  background: linear-gradient(90deg, #f1f2f3, #e6e8eb, #f1f2f3);
  background-size: 200% 100%;
  animation: shimmer 1.4s ease-in-out infinite;
}
.sk-cover { width: 100%; aspect-ratio: 16 / 10; }
@keyframes shimmer { from { background-position: 200% 0 } to { background-position: -200% 0 } }

/* ===== 为你推荐 ===== */
.more-section {
  max-width: 1500px;
  margin: 0 auto;
  padding: 34px 3vw 10px;
}
.section-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.section-bar h2 { font-size: 20px; font-weight: 800; }
.refresh-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border: 1px solid #e3e5e7;
  border-radius: 8px;
  background: #fff;
  color: #61666d;
  font-size: 13px;
  cursor: pointer;
}
.refresh-btn:hover { color: #fb7299; border-color: #fb7299; }
.video-grid--wide { grid-template-columns: repeat(6, minmax(0, 1fr)); }

/* ===== 页脚 ===== */
.bili-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 40px;
  padding: 28px 3vw;
  border-top: 1px solid #f1f2f3;
  color: #9499a0;
  font-size: 13px;
}
.bili-footer span:first-child { color: #fb7299; font-weight: 800; }

/* ===== 响应式 ===== */
@media (max-width: 1200px) {
  .channel-grid { grid-template-columns: repeat(8, minmax(0, 1fr)); }
  .video-grid--wide { grid-template-columns: repeat(4, minmax(0, 1fr)); }
}
@media (max-width: 900px) {
  .primary-layout { grid-template-columns: 1fr; }
  .channel-grid { grid-template-columns: repeat(5, minmax(0, 1fr)); }
  .video-grid--wide { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
@media (max-width: 600px) {
  .video-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .channel-grid { grid-template-columns: repeat(4, minmax(0, 1fr)); }
  .video-grid--wide { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (prefers-reduced-motion: reduce) {
  .skeleton-block { animation: none; }
  .slide { transition: none; }
}
</style>

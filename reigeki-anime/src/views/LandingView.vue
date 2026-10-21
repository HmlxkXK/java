<template>
  <div class="landing">
    <!-- ===== Hero ===== -->
    <section class="hero">
      <div class="hero-glow" aria-hidden="true"></div>
      <div class="hero-inner">
        <span class="badge" v-reveal>ANIME · 热血 · 二次元</span>
        <h1 class="hero-title" v-reveal="{ delay: 80 }">
          燃尽<span>动漫</span>
          <small>把你的热爱，烧到最旺</small>
        </h1>
        <p class="hero-sub" v-reveal="{ delay: 160 }">
          追番、社区、同好公会，一个都不少。和 86 万社员一起，把每一次心动都变成连载。
        </p>

        <div class="hero-cta" v-reveal="{ delay: 240 }">
          <template v-if="isAuthenticated">
            <router-link class="btn btn-solid" to="/dashboard">进入社员中心 →</router-link>
          </template>
          <template v-else>
            <router-link class="btn btn-solid" to="/register">立即加入 →</router-link>
            <router-link class="btn btn-ghost" to="/login">我已注册，去登录</router-link>
          </template>
        </div>

        <ul class="hero-stats" v-reveal="{ delay: 320 }">
          <li v-for="s in stats" :key="s.label">
            <b class="count" :data-target="s.target" :data-suffix="s.suffix || ''">{{ s.prefix || '' }}0{{ s.suffix || '' }}</b>
            <span>{{ s.label }}</span>
          </li>
        </ul>
      </div>

      <!-- 漂浮的番剧卡，纯装饰 -->
      <div class="float-cards" aria-hidden="true">
        <span
          v-for="(c, i) in floatCards"
          :key="i"
          class="float-card floaty"
          :style="{ background: c.bg, left: c.left + '%', top: c.top + '%', animationDelay: c.delay + 's' }"
        >{{ c.text }}</span>
      </div>
    </section>

    <!-- ===== 热门番剧 ===== -->
    <section class="section">
      <header class="section-head" v-reveal>
        <h2>本周热门番剧</h2>
        <p>数据是演示用的示例内容，等后端接上「番剧接口」即可替换为真实数据。</p>
      </header>

      <div class="anime-grid">
        <article
          v-for="(a, i) in animes"
          :key="a.title"
          class="anime-card"
          v-reveal="{ delay: i * 70 }"
        >
          <div class="anime-cover" :style="{ background: a.cover }">
            <span class="anime-rating">★ {{ a.rating }}</span>
            <span class="anime-tag">{{ a.tag }}</span>
          </div>
          <div class="anime-body">
            <h3>{{ a.title }}</h3>
            <p>{{ a.desc }}</p>
            <div class="anime-meta">
              <span>更新至 {{ a.episode }} 话</span>
              <button type="button" class="mini-btn" @click="onFollow(a)">{{ a.followed ? '已追' : '+ 追番' }}</button>
            </div>
          </div>
        </article>
      </div>
    </section>

    <!-- ===== 模块能力 ===== -->
    <section class="section">
      <header class="section-head" v-reveal>
        <h2>一个账号，打通全部热爱</h2>
        <p>登录后即可使用以下模块，进度多端同步。</p>
      </header>

      <div class="modules">
        <router-link
          v-for="(m, i) in modules"
          :key="m.title"
          class="module"
          :to="isAuthenticated ? '/dashboard' : '/register'"
          v-reveal="{ delay: i * 90 }"
        >
          <span class="module-ic" :style="{ background: m.bg }" aria-hidden="true">{{ m.icon }}</span>
          <b>{{ m.title }}</b>
          <span class="module-desc">{{ m.desc }}</span>
          <span class="module-go">了解详情 →</span>
        </router-link>
      </div>
    </section>

    <!-- ===== CTA 条 ===== -->
    <section class="cta-band" v-reveal>
      <div class="cta-inner">
        <h2>现在加入，领取新社员专属礼包</h2>
        <p>30 天会员体验 · 同步追番进度 · 账号加密保护</p>
        <router-link class="btn btn-solid" to="/register">免费创建账号 →</router-link>
      </div>
    </section>

    <!-- 标签滚动墙 -->
    <div class="marquee" aria-hidden="true">
      <div class="marquee-track">
        <span v-for="(t, i) in [...tags, ...tags]" :key="i" class="tag-chip">{{ t }}</span>
      </div>
    </div>

    <footer class="landing-footer">
      <span class="mark">🔥</span>
      <span>燃尽动漫 · 一个演示用的二次元社区</span>
    </footer>
  </div>
</template>

<script setup>
/**
 * 站点主页（落地页）。
 *
 * 这是「公开页」：任何人无需登录就能看，承担「展示 + 转化（引导注册）」的职责，
 * 与登录后才能进的「社员中心 /dashboard」分工不同。
 *
 * 动效策略：
 *   - 全程用 v-reveal 指令做滚动渐入，配合 global.css 的 .reveal 过渡，零额外依赖；
 *   - Hero 数字用 IntersectionObserver 触发「从 0 滚到目标值」，只播一次；
 *   - 漂浮番剧卡、标签墙用纯 CSS keyframes，尊重 prefers-reduced-motion。
 */

import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useAuth } from '../composables/useAuth.js'

const { isAuthenticated } = useAuth()

const stats = [
  { target: 3200, suffix: '+', label: '连载番剧' },
  { target: 86, suffix: '万', label: '活跃社员' },
  { target: 24, suffix: 'h', label: '不打烊' }
]

const floatCards = [
  { text: '🔥 进击的巨人', bg: 'linear-gradient(135deg,#ff7a18,#ff3d3d)', left: 6, top: 22, delay: 0 },
  { text: '🌊 鬼灭之刃', bg: 'linear-gradient(135deg,#36d1dc,#5b86e5)', left: 78, top: 16, delay: 1.2 },
  { text: '⚡ 咒术回战', bg: 'linear-gradient(135deg,#f7971e,#ffd200)', left: 14, top: 64, delay: 2.1 },
  { text: '🌸 你的名字', bg: 'linear-gradient(135deg,#ee9ca7,#ffdde1)', left: 84, top: 60, delay: 0.6 }
]

const animes = ref([
  { title: '焰之 chapter', desc: '少年与火焰共生，在末世点燃最后的光。', cover: 'linear-gradient(135deg,#ff7a18,#ff3d3d)', rating: '9.2', tag: '热血', episode: 12, followed: false },
  { title: '潮汐之歌', desc: '深海少女用歌声唤醒沉睡的古城。', cover: 'linear-gradient(135deg,#36d1dc,#5b86e5)', rating: '8.9', tag: '奇幻', episode: 8, followed: false },
  { title: '雷鸣剑客', desc: '以雷为刃的浪人，斩尽世间不公。', cover: 'linear-gradient(135deg,#f7971e,#ffd200)', rating: '9.0', tag: '动作', episode: 20, followed: false },
  { title: '樱落之时', desc: '转学少女与一段被遗忘的约定。', cover: 'linear-gradient(135deg,#ee9ca7,#ffdde1)', rating: '8.7', tag: '治愈', episode: 5, followed: false },
  { title: '星轨彼岸', desc: '跨越光年的通讯，连接两个文明的少年。', cover: 'linear-gradient(135deg,#7f53ff,#647dee)', rating: '9.1', tag: '科幻', episode: 15, followed: false },
  { title: '荒野炊烟', desc: '末日废土里，美食是最温柔的抵抗。', cover: 'linear-gradient(135deg,#11998e,#38ef7d)', rating: '8.8', tag: '日常', episode: 30, followed: false }
])

const modules = [
  { icon: '📺', title: '追番进度', desc: '续看、收藏、更新提醒，多端同步', bg: 'rgba(255,120,40,.16)' },
  { icon: '💬', title: '社区动态', desc: '发帖、评论、和同好一起讨论', bg: 'rgba(91,134,229,.16)' },
  { icon: '🛡️', title: '同好公会', desc: '建团、组队、冲排行榜', bg: 'rgba(247,151,30,.16)' }
]

const tags = ['热血', '奇幻', '治愈', '科幻', '日常', '运动', '悬疑', '恋爱', '机战', '搞笑', '美食', '音乐']

function onFollow(a) {
  a.followed = !a.followed
}

// ---- 数字滚动：进入视口后从 0 滚到目标值，只播一次 ----
let numberObserver = null
function animateCount(el) {
  const target = Number(el.dataset.target) || 0
  const suffix = el.dataset.suffix || ''
  const prefix = (el.textContent || '').replace(/[\d.]+$/, '').replace(new RegExp(suffix + '$'), '')
  const duration = 1200
  const start = performance.now()
  function tick(now) {
    const p = Math.min(1, (now - start) / duration)
    // easeOutCubic
    const eased = 1 - Math.pow(1 - p, 3)
    const val = Math.round(target * eased)
    el.textContent = `${prefix}${val}${suffix}`
    if (p < 1) requestAnimationFrame(tick)
  }
  requestAnimationFrame(tick)
}

onMounted(async () => {
  await nextTick()
  if (typeof IntersectionObserver === 'undefined') {
    document.querySelectorAll('.count').forEach(animateCount)
    return
  }
  numberObserver = new IntersectionObserver(
    (entries) => {
      for (const entry of entries) {
        if (entry.isIntersecting) {
          animateCount(entry.target)
          numberObserver.unobserve(entry.target)
        }
      }
    },
    { threshold: 0.5 }
  )
  document.querySelectorAll('.count').forEach((el) => numberObserver.observe(el))
})

onUnmounted(() => numberObserver?.disconnect())
</script>

<style scoped>
.landing {
  position: relative;
  z-index: 1;
}

/* ===== Hero ===== */
.hero {
  position: relative;
  min-height: 92vh;
  display: flex;
  align-items: center;
  overflow: hidden;
  padding: 40px 7vw;
}
.hero-glow {
  position: absolute;
  width: 60vw;
  height: 60vw;
  max-width: 760px;
  max-height: 760px;
  right: -10vw;
  top: -10vw;
  background: radial-gradient(circle, rgba(255,90,40,.35), transparent 60%);
  filter: blur(20px);
  animation: pulseGlow 7s ease-in-out infinite;
  pointer-events: none;
}
.hero-inner {
  position: relative;
  z-index: 2;
  max-width: 720px;
}
.hero-title {
  font-size: clamp(48px, 8vw, 104px);
  line-height: 1.02;
  font-weight: 900;
  margin: 18px 0 18px;
  background: linear-gradient(90deg, #fff, #ffd36b 45%, #ff5a3c);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  text-shadow: 0 0 50px rgba(255,90,40,.25);
}
.hero-title span {
  -webkit-text-fill-color: var(--fire-3);
  color: var(--fire-3);
}
.hero-title small {
  display: block;
  font-size: .26em;
  letter-spacing: 6px;
  font-weight: 700;
  color: var(--muted);
  -webkit-text-fill-color: var(--muted);
  margin-top: 14px;
}
.hero-sub {
  font-size: 18px;
  line-height: 1.8;
  color: var(--muted);
  max-width: 560px;
}
.hero-cta {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  margin: 30px 0 38px;
}
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 14px 26px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 800;
  letter-spacing: 1px;
  text-decoration: none;
  cursor: pointer;
  transition: transform .25s, box-shadow .25s, background .25s;
  border: 1px solid transparent;
}
.btn-solid {
  color: #2a0b06;
  background: linear-gradient(90deg, var(--ember), var(--fire-1) 50%, var(--fire-2));
  background-size: 200% 100%;
  box-shadow: 0 12px 30px rgba(255,90,40,.45);
}
.btn-solid:hover { background-position: 100% 0; transform: translateY(-2px); box-shadow: 0 18px 42px rgba(255,90,40,.6); }
.btn-ghost {
  color: var(--ember);
  background: rgba(255,120,40,.1);
  border-color: var(--line);
}
.btn-ghost:hover { background: rgba(255,120,40,.2); transform: translateY(-2px); }

.hero-stats {
  display: flex;
  gap: 40px;
  flex-wrap: wrap;
  list-style: none;
  padding: 0;
}
.hero-stats li { display: flex; flex-direction: column; gap: 4px; }
.hero-stats b {
  font-size: clamp(30px, 4vw, 44px);
  font-weight: 900;
  color: #fff;
  font-variant-numeric: tabular-nums;
}
.hero-stats span { font-size: 13px; color: var(--muted); }

/* 漂浮装饰卡 */
.float-cards { position: absolute; inset: 0; z-index: 1; pointer-events: none; }
.float-card {
  position: absolute;
  padding: 10px 16px;
  border-radius: 14px;
  font-size: 15px;
  font-weight: 700;
  color: #fff;
  box-shadow: 0 16px 40px rgba(0,0,0,.4);
  white-space: nowrap;
  opacity: .85;
}

/* ===== 通用 section ===== */
.section {
  max-width: 1180px;
  margin: 0 auto;
  padding: 70px 7vw;
}
.section-head { margin-bottom: 34px; }
.section-head h2 {
  font-size: clamp(26px, 3.4vw, 40px);
  font-weight: 900;
  line-height: 1.2;
}
.section-head p { margin-top: 12px; color: var(--muted); font-size: 15px; }

/* ===== 番剧卡片 ===== */
.anime-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 22px;
}
.anime-card {
  background: var(--glass);
  border: 1px solid var(--line-soft);
  border-radius: 18px;
  overflow: hidden;
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  transition: transform .3s, box-shadow .3s, border-color .3s;
}
.anime-card:hover {
  transform: translateY(-8px);
  border-color: var(--line);
  box-shadow: 0 24px 60px rgba(0,0,0,.5);
}
.anime-cover {
  position: relative;
  height: 150px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 12px;
}
.anime-rating {
  background: rgba(0,0,0,.45);
  color: #ffd36b;
  font-weight: 800;
  font-size: 13px;
  padding: 4px 9px;
  border-radius: 999px;
}
.anime-tag {
  background: rgba(255,255,255,.18);
  color: #fff;
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  backdrop-filter: blur(4px);
}
.anime-body { padding: 16px 18px 18px; }
.anime-body h3 { font-size: 17px; margin-bottom: 6px; }
.anime-body p { font-size: 13px; color: var(--muted); line-height: 1.6; min-height: 42px; }
.anime-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  font-size: 12px;
  color: var(--muted);
}
.mini-btn {
  border: 1px solid var(--line);
  background: rgba(255,120,40,.12);
  color: var(--ember);
  border-radius: 9px;
  padding: 6px 12px;
  font-size: 12px;
  cursor: pointer;
  transition: background .2s, color .2s;
}
.mini-btn:hover { background: rgba(255,120,40,.24); }

/* ===== 模块 ===== */
.modules {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 20px;
}
.module {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 24px;
  border-radius: 18px;
  background: var(--glass);
  border: 1px solid var(--line-soft);
  text-decoration: none;
  color: var(--text);
  transition: transform .3s, border-color .3s, box-shadow .3s;
}
.module:hover { transform: translateY(-6px); border-color: var(--line); box-shadow: 0 22px 52px rgba(0,0,0,.45); }
.module-ic {
  width: 52px; height: 52px;
  border-radius: 14px;
  display: grid; place-items: center;
  font-size: 24px;
  margin-bottom: 6px;
}
.module b { font-size: 18px; }
.module-desc { font-size: 13px; color: var(--muted); line-height: 1.6; }
.module-go { margin-top: 6px; font-size: 13px; color: var(--fire-3); }

/* ===== CTA ===== */
.cta-band {
  max-width: 1180px;
  margin: 20px auto 0;
  padding: 0 7vw;
}
.cta-inner {
  background: linear-gradient(120deg, rgba(255,122,24,.22), rgba(255,61,61,.18));
  border: 1px solid var(--line);
  border-radius: 24px;
  padding: 48px;
  text-align: center;
  backdrop-filter: blur(14px);
}
.cta-inner h2 { font-size: clamp(22px, 3vw, 34px); font-weight: 900; }
.cta-inner p { color: var(--muted); margin: 12px 0 24px; }

/* ===== 标签墙 ===== */
.marquee {
  margin: 60px 0 0;
  overflow: hidden;
  border-top: 1px solid var(--line-soft);
  border-bottom: 1px solid var(--line-soft);
  padding: 18px 0;
  -webkit-mask: linear-gradient(90deg, transparent, #000 12%, #000 88%, transparent);
  mask: linear-gradient(90deg, transparent, #000 12%, #000 88%, transparent);
}
.marquee-track {
  display: flex;
  gap: 14px;
  width: max-content;
  animation: marquee 26s linear infinite;
}
.tag-chip {
  flex: 0 0 auto;
  padding: 8px 16px;
  border-radius: 999px;
  background: rgba(255,255,255,.06);
  border: 1px solid var(--line-soft);
  color: var(--text);
  font-size: 14px;
  white-space: nowrap;
}
.marquee:hover .marquee-track { animation-play-state: paused; }

/* ===== 页脚 ===== */
.landing-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 40px 7vw 60px;
  color: var(--muted);
  font-size: 14px;
}
.landing-footer .mark { font-size: 18px; }

@media (max-width: 900px) {
  .hero { min-height: 86vh; }
  .hero-stats { gap: 26px; }
  .cta-inner { padding: 34px 22px; }
}
</style>

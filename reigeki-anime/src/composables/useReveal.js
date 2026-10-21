/**
 * 滚动揭示指令 v-reveal。
 *
 * 元素进入视口时加 .is-visible，配合 global.css 里的 .reveal 过渡实现「渐入上浮」。
 * 用法：
 *   <section v-reveal>...</section>
 *   <div v-reveal="{ delay: 120 }">...</div>   // 错峰动画
 *
 * 设计取舍：
 *   - 用 IntersectionObserver 而非监听 scroll：避免每帧重排，几十个元素同时动画也不卡；
 *   - 只触发一次（unobserve），反复进出视口不会重复播放，体验更克制；
 *   - 系统开启「减弱动态效果」时 global.css 里直接把 .reveal 置为终态，这里不再处理。
 */

const REVEAL_CLASS = 'reveal'
const VISIBLE_CLASS = 'is-visible'
const THRESHOLD = 0.12

export const revealDirective = {
  mounted(el, binding) {
    el.classList.add(REVEAL_CLASS)

    // 错峰延迟：用 transition-delay 实现，而非 setTimeout，避免堆积定时器
    const delay = Number(binding.value?.delay)
    if (Number.isFinite(delay) && delay > 0) {
      el.style.transitionDelay = `${delay}ms`
    }

    // 不支持 IntersectionObserver 的环境（极老浏览器）直接显示，不阻塞内容
    if (typeof IntersectionObserver === 'undefined') {
      el.classList.add(VISIBLE_CLASS)
      return
    }

    const io = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            el.classList.add(VISIBLE_CLASS)
            io.unobserve(el)
          }
        }
      },
      { threshold: THRESHOLD, rootMargin: '0px 0px -8% 0px' }
    )
    io.observe(el)
    el._revealObserver = io
  },
  unmounted(el) {
    el._revealObserver?.disconnect()
    el._revealObserver = null
  }
}

export default revealDirective

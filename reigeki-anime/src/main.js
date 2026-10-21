import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { onUnauthorized } from './api/session.js'
import { pushToast } from './composables/useToast.js'
import { revealDirective } from './composables/useReveal.js'
import './styles/global.css'

const app = createApp(App)
app.use(router)
// 滚动进入视口时触发的渐显指令：v-reveal / v-reveal="{ delay: 120 }"
app.directive('reveal', revealDirective)

/**
 * 全局 401 处理。
 *
 * 放在 main.js 而不是 request.js 里，是为了让请求层不必知道 router 的存在，
 * 否则会形成 router -> view -> composable -> api -> router 的循环依赖。
 *
 * 触发场景：token 已过期或被吊销，但本地还没来得及判断，请求先被后端拒了。
 * 此时清会话（session.js 内部已做）并回登录页；如果当前已经在登录页，
 * 说明是登录请求本身失败了，交给页面自己展示错误，不再重复弹提示。
 */
onUnauthorized(() => {
  const current = router.currentRoute.value
  if (current.name === 'login') return

  pushToast('登录状态已失效，请重新登录', 'warn')
  router.replace({
    name: 'login',
    query: current.fullPath && current.fullPath !== '/' ? { redirect: current.fullPath } : {}
  })
})

app.mount('#app')

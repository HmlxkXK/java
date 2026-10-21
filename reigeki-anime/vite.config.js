import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    open: true,
    // 把 /api 反向代理到 Spring Boot，前端因此始终请求同源地址（浏览器眼里
    // 只有 5173，不存在跨域）。好处有三个：
    //   1. 前端不再硬编码后端端口，后端换端口只改这一处；
    //   2. 不依赖后端 CorsConfig 放行，预检请求也少一次往返；
    //   3. 与生产一致——生产环境通常也是由 Nginx 把 /api 转发到后端。
    // 代理目标用 127.0.0.1 而不是 localhost：Node 17+ 对 localhost 会优先解析
    // 成 IPv6 的 ::1，显式写 IPv4 可以避开这类解析差异。
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8081',
        changeOrigin: true
      }
    }
  }
})

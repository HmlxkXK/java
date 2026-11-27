<template>
  <div ref="host" class="three-scene" aria-hidden="true"></div>
</template>

<script setup>
/**
 * 可复用的 Three.js 3D 背景：发光线框能量核心 + 环绕粒子星云 + 鼠标视差。
 *
 * 设计要点：
 *   - three.js 动态 import，拆成独立异步 chunk，不阻塞首屏。
 *   - variant='dark'（登录页）：深色底 + additive 发光核心，蔚蓝配色。
 *   - variant='light'（B站首页）：纯白底，改用普通混合的浅蓝粒子 + 半透明线框，
 *     避免 additive 在白底上不可见；整体低对比，不干扰阅读。
 *   - prefers-reduced-motion：降到较慢的旋转速度而不是完全静止，
 *     保留「能量核心在动」的视觉反馈（其它 DOM 动效仍然尊重系统设置）。
 *   - 切标签页暂停 rAF；ResizeObserver 同步画布；卸载时释放全部 GPU 资源；
 *     WebGL / import 失败静默降级。
 */
import { onMounted, onBeforeUnmount, ref } from 'vue'

const props = defineProps({
  variant: { type: String, default: 'dark' } // 'dark' | 'light'
})

const host = ref(null)
let THREE = null
let renderer, scene, camera, group, core, ringA, ringB, particles
let rafId = 0
let media, reduceMotion = false, hidden = false
let resizeObserver
let disposed = false

const pointer = { x: 0, y: 0 }
const target = { x: 0, y: 0 }
const isLight = () => props.variant === 'light'

function buildScene() {
  const el = host.value
  if (!el || !THREE) return

  const w = el.clientWidth || 1200
  const h = el.clientHeight || 600

  renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true, powerPreference: 'low-power' })
  renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2))
  renderer.setSize(w, h, false)
  el.appendChild(renderer.domElement)

  scene = new THREE.Scene()
  camera = new THREE.PerspectiveCamera(55, w / h, 0.1, 100)
  camera.position.z = isLight() ? 12 : 11

  group = new THREE.Group()
  scene.add(group)

  // 登录页核心偏左避开右侧表单；首页居中偏右避开轮播/卡片文字
  if (!isLight()) {
    group.position.x = -3.2
    group.position.y = 0.3
    group.scale.setScalar(0.85)
  } else {
    group.position.x = 3.5
    group.position.y = -0.5
    group.scale.setScalar(1.15)
  }

  const light = isLight()
  const coreColor = light ? 0x2b9fe8 : 0x8fd4ff
  const lineColor = light ? 0x4aa8e8 : 0x9fe0ff
  const particleColor = light ? 0x7cc4f0 : 0x6fc8ff

  // 线框二十面体能量核心
  const icoGeo = new THREE.IcosahedronGeometry(2.1, 1)
  const icoMat = new THREE.MeshBasicMaterial({
    color: lineColor,
    wireframe: true,
    transparent: true,
    opacity: light ? 0.42 : 0.55,
    blending: light ? THREE.NormalBlending : THREE.AdditiveBlending,
    depthWrite: false
  })
  core = new THREE.Mesh(icoGeo, icoMat)
  group.add(core)

  // 内层发光球（深色才有明显的 additive 辉光）
  if (!light) {
    const glowGeo = new THREE.IcosahedronGeometry(1.35, 1)
    const glowMat = new THREE.MeshBasicMaterial({
      color: coreColor,
      transparent: true,
      opacity: 0.16,
      blending: THREE.AdditiveBlending,
      depthWrite: false
    })
    const glow = new THREE.Mesh(glowGeo, glowMat)
    core.add(glow)
  }

  // 两道反向倾斜光环
  const torusGeo = new THREE.TorusGeometry(3.1, 0.015, 8, 120)
  const torusMat = new THREE.MeshBasicMaterial({
    color: lineColor,
    transparent: true,
    opacity: light ? 0.4 : 0.5,
    blending: light ? THREE.NormalBlending : THREE.AdditiveBlending,
    depthWrite: false
  })
  ringA = new THREE.Mesh(torusGeo, torusMat)
  ringA.rotation.x = Math.PI / 2.6
  ringB = new THREE.Mesh(torusGeo.clone(), torusMat.clone())
  ringB.rotation.x = -Math.PI / 2.6
  ringB.rotation.y = Math.PI / 3
  group.add(ringA, ringB)

  // 粒子星云（圆盘分布）
  const count = (light ? 700 : 900) * (isMobile() ? 0.5 : 1)
  const positions = new Float32Array(count * 3)
  for (let i = 0; i < count; i++) {
    const r = 3 + Math.pow(Math.random(), 0.7) * 9
    const theta = Math.random() * Math.PI * 2
    positions[i * 3] = Math.cos(theta) * r
    positions[i * 3 + 1] = (Math.random() - 0.5) * 9
    positions[i * 3 + 2] = Math.sin(theta) * r - 4
  }
  const pGeo = new THREE.BufferGeometry()
  pGeo.setAttribute('position', new THREE.BufferAttribute(positions, 3))
  const pMat = new THREE.PointsMaterial({
    color: particleColor,
    size: light ? 0.05 : 0.07,
    transparent: true,
    opacity: light ? 0.68 : 0.75,
    blending: light ? THREE.NormalBlending : THREE.AdditiveBlending,
    depthWrite: false,
    sizeAttenuation: true
  })
  particles = new THREE.Points(pGeo, pMat)
  scene.add(particles)

  renderFrame()
}

function isMobile() {
  return window.matchMedia('(max-width: 768px)').matches
}

function renderFrame() {
  if (!renderer || disposed) return
  // 系统开启「减弱动态效果」时降速而不是停帧，避免 3D 看起来像一张静态图。
  const speed = reduceMotion ? 0.6 : 1
  if (core) {
    core.rotation.x += 0.0022 * speed
    core.rotation.y += 0.0031 * speed
  }
  if (ringA) ringA.rotation.z += 0.0035 * speed
  if (ringB) ringB.rotation.z -= 0.0028 * speed
  if (particles) particles.rotation.y += 0.0006 * speed

  if (group) {
    pointer.x += (target.x - pointer.x) * 0.045
    pointer.y += (target.y - pointer.y) * 0.045
    group.rotation.y = pointer.x * 0.28
    group.rotation.x = pointer.y * 0.18
  }

  renderer.render(scene, camera)
}

function loop() {
  if (hidden) return
  renderFrame()
  rafId = requestAnimationFrame(loop)
}

function onResize() {
  const el = host.value
  if (!el || !renderer || !camera) return
  const w = el.clientWidth || 1
  const h = el.clientHeight || 1
  camera.aspect = w / h
  camera.updateProjectionMatrix()
  renderer.setSize(w, h, false)
  if (reduceMotion) renderFrame()
}

function onPointerMove(e) {
  target.x = (e.clientX / window.innerWidth) * 2 - 1
  target.y = -((e.clientY / window.innerHeight) * 2 - 1)
}

function onVisibility() {
  hidden = document.hidden
  cancelAnimationFrame(rafId)
  if (!hidden) loop()
}

function onMediaChange(e) {
  reduceMotion = e.matches
  cancelAnimationFrame(rafId)
  if (!hidden) loop()
}

function dispose() {
  disposed = true
  cancelAnimationFrame(rafId)
  if (media) media.removeEventListener('change', onMediaChange)
  document.removeEventListener('visibilitychange', onVisibility)
  window.removeEventListener('pointermove', onPointerMove)
  if (resizeObserver) resizeObserver.disconnect()
  else window.removeEventListener('resize', onResize)

  if (scene) {
    scene.traverse((obj) => {
      if (obj.geometry) obj.geometry.dispose()
      if (obj.material) {
        const mats = Array.isArray(obj.material) ? obj.material : [obj.material]
        mats.forEach((m) => m.dispose())
      }
    })
  }
  if (renderer) {
    renderer.dispose()
    if (renderer.domElement && renderer.domElement.parentNode) {
      renderer.domElement.parentNode.removeChild(renderer.domElement)
    }
  }
  renderer = scene = camera = group = core = ringA = ringB = particles = null
}

onMounted(async () => {
  media = window.matchMedia('(prefers-reduced-motion: reduce)')
  reduceMotion = media.matches
  hidden = document.hidden

  try {
    THREE = await import('three')
    if (disposed) return
    buildScene()
  } catch (e) {
    // WebGL 不可用或加载失败：静默降级，页面其余部分照常工作
    return
  }
  if (!renderer) return

  media.addEventListener('change', onMediaChange)
  document.addEventListener('visibilitychange', onVisibility)
  window.addEventListener('pointermove', onPointerMove, { passive: true })

  if (typeof ResizeObserver !== 'undefined' && host.value) {
    resizeObserver = new ResizeObserver(onResize)
    resizeObserver.observe(host.value)
  } else {
    window.addEventListener('resize', onResize, { passive: true })
  }

  if (!hidden) loop()
})

onBeforeUnmount(dispose)
</script>

<style scoped>
.three-scene {
  position: absolute;
  inset: 0;
  z-index: -1;
  pointer-events: none;
  overflow: hidden;
}
.three-scene :deep(canvas) {
  display: block;
  width: 100%;
  height: 100%;
}
</style>

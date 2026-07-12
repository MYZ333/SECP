<template>
  <canvas ref="cv" class="pnet" aria-hidden="true"></canvas>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  count: { type: Number, default: 70 },   // 粒子数量
  repel: { type: Number, default: 120 },  // 鼠标斥力半径(px)
  link:  { type: Number, default: 130 },  // 连线阈值(px)
})

const cv = ref(null)
let ctx, raf = 0, ro = null
let W = 0, H = 0, dpr = 1
let parts = []
const mouse = { x: -9999, y: -9999 }
const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches

const rand = (a, b) => a + Math.random() * (b - a)

function seed() {
  const n = Math.max(0, Math.round(props.count))
  parts = Array.from({ length: n }, () => ({
    x: Math.random() * W,
    y: Math.random() * H,
    vx: rand(-0.28, 0.28),
    vy: rand(-0.28, 0.28),
    r: rand(1.1, 2.4),
  }))
}

function resize() {
  if (!cv.value) return
  const el = cv.value
  dpr = Math.min(window.devicePixelRatio || 1, 2)
  W = el.clientWidth
  H = el.clientHeight
  el.width = Math.round(W * dpr)
  el.height = Math.round(H * dpr)
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  if (!parts.length) seed()
}

function step() {
  ctx.clearRect(0, 0, W, H)
  const rep2 = props.repel * props.repel
  const link = props.link, link2 = link * link

  for (const p of parts) {
    // 鼠标斥力
    const dx = p.x - mouse.x, dy = p.y - mouse.y
    const d2 = dx * dx + dy * dy
    if (d2 < rep2 && d2 > 0.01) {
      const f = (1 - Math.sqrt(d2) / props.repel) * 0.8
      const inv = 1 / Math.sqrt(d2)
      p.vx += dx * inv * f
      p.vy += dy * inv * f
    }
    // 速度阻尼 + 位移
    p.vx *= 0.96; p.vy *= 0.96
    p.x += p.vx; p.y += p.vy
    // 边界回绕
    if (p.x < -20) p.x = W + 20; else if (p.x > W + 20) p.x = -20
    if (p.y < -20) p.y = H + 20; else if (p.y > H + 20) p.y = -20
  }

  // 连线
  for (let i = 0; i < parts.length; i++) {
    const a = parts[i]
    for (let j = i + 1; j < parts.length; j++) {
      const b = parts[j]
      const dx = a.x - b.x, dy = a.y - b.y
      const d2 = dx * dx + dy * dy
      if (d2 < link2) {
        const t = 1 - Math.sqrt(d2) / link
        ctx.strokeStyle = `rgba(46, 111, 224, ${t * 0.28})`
        ctx.lineWidth = 1
        ctx.beginPath(); ctx.moveTo(a.x, a.y); ctx.lineTo(b.x, b.y); ctx.stroke()
      }
    }
  }

  // 粒子点
  for (const p of parts) {
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
    ctx.fillStyle = 'rgba(55, 182, 217, 0.7)'
    ctx.fill()
  }

  raf = requestAnimationFrame(step)
}

function onMove(e) {
  const r = cv.value.getBoundingClientRect()
  mouse.x = e.clientX - r.left
  mouse.y = e.clientY - r.top
}
function onLeave() { mouse.x = mouse.y = -9999 }

onMounted(() => {
  ctx = cv.value.getContext('2d')
  resize()
  ro = new ResizeObserver(resize)
  ro.observe(cv.value)
  window.addEventListener('mousemove', onMove, { passive: true })
  window.addEventListener('mouseout', onLeave, { passive: true })
  if (reduced) { step(); cancelAnimationFrame(raf) } // 静态渲染一帧
  else raf = requestAnimationFrame(step)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(raf)
  ro && ro.disconnect()
  window.removeEventListener('mousemove', onMove)
  window.removeEventListener('mouseout', onLeave)
})
</script>

<style scoped>
.pnet {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  display: block;
  pointer-events: none;
}
</style>

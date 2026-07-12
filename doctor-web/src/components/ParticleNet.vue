<template>
  <canvas ref="canvas" class="particle-net" aria-hidden="true"></canvas>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'

const props = defineProps({
  count: { type: Number, default: 68 },
  link: { type: Number, default: 145 },
  repel: { type: Number, default: 135 }
})

const canvas = ref(null)
let context
let frame = 0
let observer
let width = 0
let height = 0
let ratio = 1
let points = []
const pointer = { x: -9999, y: -9999 }
const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches

function random(min, max) { return min + Math.random() * (max - min) }
function seed() {
  points = Array.from({ length: props.count }, () => ({
    x: Math.random() * width,
    y: Math.random() * height,
    vx: random(-0.22, 0.22),
    vy: random(-0.22, 0.22),
    radius: random(1, 2.1)
  }))
}
function resize() {
  ratio = Math.min(window.devicePixelRatio || 1, 2)
  width = canvas.value.clientWidth
  height = canvas.value.clientHeight
  canvas.value.width = Math.round(width * ratio)
  canvas.value.height = Math.round(height * ratio)
  context.setTransform(ratio, 0, 0, ratio, 0, 0)
  seed()
}
function draw() {
  context.clearRect(0, 0, width, height)
  const linkSquared = props.link * props.link
  const repelSquared = props.repel * props.repel
  for (const point of points) {
    const dx = point.x - pointer.x
    const dy = point.y - pointer.y
    const distanceSquared = dx * dx + dy * dy
    if (distanceSquared < repelSquared && distanceSquared > 0.01) {
      const distance = Math.sqrt(distanceSquared)
      const force = (1 - distance / props.repel) * 0.5
      point.vx += dx / distance * force
      point.vy += dy / distance * force
    }
    point.vx *= 0.97
    point.vy *= 0.97
    point.x += point.vx
    point.y += point.vy
    if (point.x < -16) point.x = width + 16
    if (point.x > width + 16) point.x = -16
    if (point.y < -16) point.y = height + 16
    if (point.y > height + 16) point.y = -16
  }
  for (let i = 0; i < points.length; i++) {
    for (let j = i + 1; j < points.length; j++) {
      const a = points[i]
      const b = points[j]
      const dx = a.x - b.x
      const dy = a.y - b.y
      const distanceSquared = dx * dx + dy * dy
      if (distanceSquared < linkSquared) {
        const opacity = (1 - Math.sqrt(distanceSquared) / props.link) * 0.22
        context.beginPath()
        context.moveTo(a.x, a.y)
        context.lineTo(b.x, b.y)
        context.strokeStyle = `rgba(46, 111, 224, ${opacity})`
        context.lineWidth = 1
        context.stroke()
      }
    }
  }
  for (const point of points) {
    context.beginPath()
    context.arc(point.x, point.y, point.radius, 0, Math.PI * 2)
    context.fillStyle = 'rgba(46, 111, 224, .48)'
    context.fill()
  }
  frame = requestAnimationFrame(draw)
}
function onPointerMove(event) {
  const rect = canvas.value.getBoundingClientRect()
  pointer.x = event.clientX - rect.left
  pointer.y = event.clientY - rect.top
}
function onPointerLeave() { pointer.x = pointer.y = -9999 }

onMounted(() => {
  context = canvas.value.getContext('2d')
  observer = new ResizeObserver(resize)
  observer.observe(canvas.value)
  window.addEventListener('pointermove', onPointerMove, { passive: true })
  window.addEventListener('blur', onPointerLeave)
  resize()
  draw()
  if (reducedMotion) cancelAnimationFrame(frame)
})
onBeforeUnmount(() => {
  cancelAnimationFrame(frame)
  observer?.disconnect()
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('blur', onPointerLeave)
})
</script>

<style scoped>
.particle-net { position: absolute; inset: 0; width: 100%; height: 100%; pointer-events: none; }
</style>

/* 全局自定义指令：滚动揭示 v-reveal 与 数字滚动 v-countup */

// v-reveal：元素进入视口时淡入上滑，支持交错 v-reveal="index"
const revealObserver = typeof IntersectionObserver !== 'undefined'
  ? new IntersectionObserver((entries) => {
      entries.forEach((e) => {
        if (e.isIntersecting) {
          const el = e.target
          const delay = Number(el.dataset.revealDelay || 0)
          setTimeout(() => el.classList.add('is-in'), delay)
          revealObserver.unobserve(el)
        }
      })
    }, { threshold: 0.12, rootMargin: '0px 0px -8% 0px' })
  : null

export const reveal = {
  mounted(el, binding) {
    el.setAttribute('data-reveal', '')
    const i = Number(binding.value || 0)
    el.dataset.revealDelay = String(i * 80)
    if (revealObserver) revealObserver.observe(el)
    else el.classList.add('is-in')
  },
  unmounted(el) { if (revealObserver) revealObserver.unobserve(el) }
}

// v-countup：数字从 0 滚动到目标值。用法 <span v-countup="score" />
export const countup = {
  mounted(el, binding) { animateCount(el, 0, Number(binding.value) || 0) },
  updated(el, binding) {
    const to = Number(binding.value) || 0
    const from = Number(el.dataset.cur) || 0
    if (to !== from) animateCount(el, from, to)
  }
}

function animateCount(el, from, to) {
  const dur = 900
  const start = performance.now()
  const step = (now) => {
    const p = Math.min(1, (now - start) / dur)
    const eased = 1 - Math.pow(1 - p, 3) // easeOutCubic
    const val = Math.round(from + (to - from) * eased)
    el.textContent = val.toLocaleString('en-US')
    el.dataset.cur = String(val)
    if (p < 1) requestAnimationFrame(step)
    else { el.textContent = to.toLocaleString('en-US'); el.dataset.cur = String(to) }
  }
  requestAnimationFrame(step)
}

export default {
  install(app) {
    app.directive('reveal', reveal)
    app.directive('countup', countup)
  }
}

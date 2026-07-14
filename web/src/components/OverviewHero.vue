<template>
  <section class="overview-hero" :class="`is-${variant}`" @pointermove="moveSurface" @pointerleave="resetSurface">
    <div class="hero-grid" aria-hidden="true"></div>
    <div class="hero-scan" aria-hidden="true"></div>

    <div class="hero-copy">
      <p class="eyebrow">{{ eyebrow }}</p>
      <h1>{{ title }}</h1>
      <p class="hero-description">{{ description }}</p>
    </div>

    <div class="hero-side">
      <div v-if="variant === 'health'" class="health-wave" aria-hidden="true">
        <svg viewBox="0 0 520 140" preserveAspectRatio="none">
          <defs>
            <linearGradient id="overviewHealthWave" x1="0" y1="0" x2="1" y2="0">
              <stop offset="0" stop-color="#2e6fe0" stop-opacity="0" />
              <stop offset="0.35" stop-color="#2e6fe0" />
              <stop offset="0.7" stop-color="#37b6d9" />
              <stop offset="1" stop-color="#37b6d9" stop-opacity="0" />
            </linearGradient>
          </defs>
          <path class="wave-track" d="M0 76H92l18-26 19 54 25-88 25 112 24-52h70l17-19 22 38 25-70 24 96 24-45H520" />
          <path class="wave-line" d="M0 76H92l18-26 19 54 25-88 25 112 24-52h70l17-19 22 38 25-70 24 96 24-45H520" />
        </svg>
      </div>

      <div v-else-if="variant === 'points'" class="points-visual" aria-hidden="true">
        <div class="coin-stage">
          <div class="coin-flight">
            <div class="coin">
              <span class="coin-face coin-front"><el-icon class="coin-star"><Star /></el-icon></span>
              <span class="coin-face coin-back"><el-icon class="coin-star"><Star /></el-icon></span>
              <span class="coin-edge"></span>
            </div>
          </div>
          <span class="coin-shadow"></span>
        </div>
        <div class="balance-readout">
          <span>{{ metricLabel }}</span>
          <strong>{{ metricValue }}</strong>
        </div>
      </div>

      <div v-else class="system-hub" aria-hidden="true">
        <span class="hub-link link-top"></span>
        <span class="hub-link link-right"></span>
        <span class="hub-link link-bottom"></span>
        <span class="hub-link link-left"></span>

        <span class="hub-node node-top"><el-icon><User /></el-icon></span>
        <span class="hub-node node-right"><el-icon><Goods /></el-icon></span>
        <span class="hub-node node-bottom"><el-icon><FirstAidKit /></el-icon></span>
        <span class="hub-node node-left"><el-icon><Document /></el-icon></span>
        <span class="hub-core"><span class="core-ring"></span><el-icon><Monitor /></el-icon></span>
      </div>

      <el-button class="hero-cta" :icon="ArrowRight" @click="emit('action')">{{ ctaLabel }}</el-button>
    </div>
  </section>
</template>

<script setup>
import { ArrowRight, Document, FirstAidKit, Goods, Monitor, Star, User } from '@element-plus/icons-vue'

defineProps({
  variant: { type: String, required: true },
  eyebrow: { type: String, required: true },
  title: { type: String, required: true },
  description: { type: String, required: true },
  ctaLabel: { type: String, required: true },
  metricLabel: { type: String, default: '' },
  metricValue: { type: [String, Number], default: '' },
})

const emit = defineEmits(['action'])
const reducedMotion = typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches

function moveSurface(event) {
  if (event.pointerType === 'touch' || reducedMotion) return
  const hero = event.currentTarget
  const rect = hero.getBoundingClientRect()
  const x = ((event.clientX - rect.left) / rect.width - 0.5) * 12
  const y = ((event.clientY - rect.top) / rect.height - 0.5) * 10
  hero.style.setProperty('--surface-x', `${x.toFixed(2)}px`)
  hero.style.setProperty('--surface-y', `${y.toFixed(2)}px`)
  hero.style.setProperty('--visual-x', `${(-x * 0.42).toFixed(2)}px`)
  hero.style.setProperty('--visual-y', `${(-y * 0.42).toFixed(2)}px`)
}

function resetSurface(event) {
  event.currentTarget.style.setProperty('--surface-x', '0px')
  event.currentTarget.style.setProperty('--surface-y', '0px')
  event.currentTarget.style.setProperty('--visual-x', '0px')
  event.currentTarget.style.setProperty('--visual-y', '0px')
}
</script>

<style scoped>
.overview-hero {
  --hero-accent: #2e6fe0;
  --hero-accent-dark: #245bb7;
  --hero-accent-soft: rgba(46, 111, 224, .08);
  --surface-x: 0px;
  --surface-y: 0px;
  --visual-x: 0px;
  --visual-y: 0px;
  --hero-ease: cubic-bezier(.22, 1, .36, 1);
  position: relative;
  isolation: isolate;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 46px;
  min-height: 272px;
  padding: 40px 48px;
  overflow: hidden;
  color: #10253f;
  background: #fff;
  border: 1px solid rgba(111, 153, 194, .22);
  box-shadow: 0 16px 38px rgba(42, 96, 145, .11);
}
.overview-hero.is-points {
  --hero-accent: #b77914;
  --hero-accent-dark: #8d5b0b;
  --hero-accent-soft: rgba(183, 121, 20, .08);
}
.overview-hero.is-admin {
  --hero-accent: #3976a3;
  --hero-accent-dark: #285a7f;
  --hero-accent-soft: rgba(57, 118, 163, .08);
}
.hero-grid {
  position: absolute;
  inset: -26px;
  z-index: -2;
  pointer-events: none;
  opacity: 1;
  background-image:
    linear-gradient(rgba(80, 139, 196, .075) 1px, transparent 1px),
    linear-gradient(90deg, rgba(80, 139, 196, .075) 1px, transparent 1px);
  background-size: 44px 44px;
  transform: translate3d(var(--surface-x), var(--surface-y), 0);
  transition: transform .7s var(--hero-ease);
  -webkit-mask-image: linear-gradient(90deg, transparent 4%, #000 42%, #000);
  mask-image: linear-gradient(90deg, transparent 4%, #000 42%, #000);
}
.hero-scan {
  position: absolute;
  inset: 0;
  z-index: -1;
  pointer-events: none;
  background: linear-gradient(106deg, transparent 28%, rgba(69, 143, 211, .075) 49%, transparent 69%);
  transform: translate3d(-115%, 0, 0);
}
.hero-copy { position: relative; z-index: 1; flex: 1 1 620px; min-width: 0; }
.eyebrow { margin: 0 0 8px; color: var(--hero-accent); font-size: 11px; font-weight: 800; letter-spacing: .18em; }
h1 { margin: 0; font-size: 42px; letter-spacing: 0; }
.hero-description { max-width: 660px; margin: 14px 0 0; color: #617895; }
.hero-side {
  position: relative;
  z-index: 1;
  flex: 0 1 440px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
  min-width: 320px;
  transform: translate3d(var(--visual-x), var(--visual-y), 0);
  transition: transform .7s var(--hero-ease);
}
.hero-cta.el-button {
  min-width: 158px;
  height: 48px;
  border: 1px solid var(--hero-accent);
  background: var(--hero-accent);
  color: #fff;
  box-shadow: 0 10px 22px color-mix(in srgb, var(--hero-accent) 24%, transparent);
}
.hero-cta.el-button:hover { border-color: var(--hero-accent-dark); background: var(--hero-accent-dark); color: #fff; box-shadow: 0 14px 28px color-mix(in srgb, var(--hero-accent) 30%, transparent); }

/* Health visual */
.health-wave { width: 100%; height: 118px; }
.health-wave svg { display: block; width: 100%; height: 100%; overflow: visible; }
.wave-track, .wave-line { fill: none; stroke-linecap: round; stroke-linejoin: round; }
.wave-track { stroke: #e6f0fa; stroke-width: 8; }
.wave-line { stroke: url(#overviewHealthWave); stroke-width: 2.5; stroke-dasharray: 130 680; filter: drop-shadow(0 0 6px rgba(46, 111, 224, .24)); }

/* Points visual */
.points-visual { position: relative; width: 100%; height: 158px; }
.coin-stage { position: absolute; left: 12px; bottom: 2px; width: 178px; height: 150px; perspective: 720px; }
.coin-flight { position: absolute; left: 45px; bottom: 18px; width: 88px; height: 88px; transform-style: preserve-3d; }
.coin { position: relative; width: 88px; height: 88px; transform-style: preserve-3d; }
.coin-face {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  overflow: hidden;
  border: 1px solid rgba(183, 121, 20, .82);
  border-radius: 50%;
  background: rgba(255, 245, 223, .18);
  box-shadow:
    inset 0 0 0 1px rgba(183, 121, 20, .05),
    0 5px 14px rgba(183, 121, 20, .08);
  backface-visibility: hidden;
}
.coin-front { transform: translateZ(4px); }
.coin-back { transform: rotateY(180deg) translateZ(4px); }
.coin-face::before,
.coin-face::after {
  content: "";
  position: absolute;
  border-radius: 50%;
}
.coin-face::before { inset: 7px; border: 1px solid rgba(183, 121, 20, .46); }
.coin-face::after { inset: 14px; border: 1px dashed rgba(183, 121, 20, .28); }
.coin-star {
  position: relative;
  z-index: 1;
  width: 34px;
  height: 34px;
  color: #b77914;
  font-size: 34px;
  filter: drop-shadow(0 2px 4px rgba(183, 121, 20, .16));
}
.coin-star :deep(svg) { width: 100%; height: 100%; stroke-width: 1.5; }
.coin-edge {
  position: absolute;
  left: 2px;
  top: 39px;
  width: 84px;
  height: 9px;
  border-top: 1px solid rgba(183, 121, 20, .68);
  border-bottom: 1px solid rgba(183, 121, 20, .32);
  border-radius: 50%;
  background: transparent;
  transform: rotateX(90deg);
  box-shadow: 0 2px 6px rgba(183, 121, 20, .08);
}
.coin-shadow {
  position: absolute;
  left: 50px;
  bottom: 3px;
  width: 80px;
  height: 17px;
  border: 1px solid rgba(183, 121, 20, .2);
  border-radius: 50%;
  background: transparent;
  box-shadow: 0 3px 8px rgba(183, 121, 20, .06);
  transform: scaleX(1);
}
.balance-readout { position: absolute; right: 2px; bottom: 20px; min-width: 148px; padding-left: 22px; border-left: 1px solid rgba(183,121,20,.3); }
.balance-readout span, .balance-readout strong { display: block; }
.balance-readout span { color: #7c6b4f; font-size: 13px; }
.balance-readout strong { margin-top: 5px; color: #9a650f; font-size: 38px; line-height: 1; font-variant-numeric: tabular-nums; text-shadow: 0 3px 14px rgba(183,121,20,.13); }

/* Admin visual */
.system-hub { position: relative; width: 340px; height: 158px; margin-right: 20px; }
.hub-core, .hub-node { position: absolute; display: grid; place-items: center; border: 1px solid rgba(57,118,163,.32); background: #f4f9fc; color: #3976a3; box-shadow: 0 8px 20px rgba(57,118,163,.1); }
.hub-core { left: 50%; top: 50%; z-index: 2; width: 68px; height: 68px; transform: translate(-50%, -50%); background: #eaf4fa; font-size: 30px; }
.core-ring { position: absolute; inset: -9px; border: 1px solid rgba(57,118,163,.2); }
.hub-node { z-index: 2; width: 42px; height: 42px; font-size: 20px; }
.node-top { left: 50%; top: 0; transform: translateX(-50%); }
.node-right { right: 0; top: 50%; transform: translateY(-50%); }
.node-bottom { left: 50%; bottom: 0; transform: translateX(-50%); }
.node-left { left: 0; top: 50%; transform: translateY(-50%); }
.hub-link { position: absolute; z-index: 1; overflow: hidden; background: rgba(57,118,163,.2); }
.hub-link::after { content: ""; position: absolute; background: #3976a3; box-shadow: 0 0 7px rgba(57,118,163,.34); }
.link-top { left: calc(50% - .5px); top: 40px; width: 1px; height: 25px; }
.link-top::after { left: -1px; top: -8px; width: 3px; height: 8px; }
.link-right { left: calc(50% + 34px); top: calc(50% - .5px); width: 115px; height: 1px; }
.link-right::after { right: -10px; top: -1px; width: 10px; height: 3px; }
.link-bottom { left: calc(50% - .5px); top: calc(50% + 34px); width: 1px; height: 25px; }
.link-bottom::after { left: -1px; bottom: -8px; width: 3px; height: 8px; }
.link-left { right: calc(50% + 34px); top: calc(50% - .5px); width: 115px; height: 1px; }
.link-left::after { left: -10px; top: -1px; width: 10px; height: 3px; }

@media (prefers-reduced-motion: no-preference) {
  .hero-scan { animation: heroSweep 8s var(--hero-ease) infinite 1.5s; }
  .wave-line { animation: waveTravel 4.6s linear infinite; }
  .coin-flight { animation: coinFlight 4.6s cubic-bezier(.45,.05,.25,1) infinite; }
  .coin { animation: coinFlip 4.6s cubic-bezier(.45,.05,.25,1) infinite; }
  .coin-shadow { animation: coinShadow 4.6s ease-in-out infinite; }
  .hub-core { animation: coreBreathe 3.6s var(--hero-ease) infinite; }
  .core-ring { animation: coreRing 3.6s var(--hero-ease) infinite; }
  .hub-node { animation: nodeSignal 3.6s var(--hero-ease) infinite; }
  .node-right { animation-delay: .45s; }
  .node-bottom { animation-delay: .9s; }
  .node-left { animation-delay: 1.35s; }
  .link-top::after { animation: dataDown 3.6s ease-in-out infinite; }
  .link-right::after { animation: dataLeft 3.6s ease-in-out infinite .45s; }
  .link-bottom::after { animation: dataUp 3.6s ease-in-out infinite .9s; }
  .link-left::after { animation: dataRight 3.6s ease-in-out infinite 1.35s; }
}
@keyframes heroSweep { 0%,60% { transform: translate3d(-115%,0,0); } 88%,100% { transform: translate3d(115%,0,0); } }
@keyframes waveTravel { to { stroke-dashoffset: -810; } }
@keyframes coinFlight { 0%,14%,82%,100% { transform: translateY(0); } 46% { transform: translateY(-62px); } }
@keyframes coinFlip { 0%,14% { transform: rotateX(0deg) rotateY(0deg); } 46% { transform: rotateX(540deg) rotateY(36deg); } 82%,100% { transform: rotateX(1080deg) rotateY(0deg); } }
@keyframes coinShadow { 0%,14%,82%,100% { opacity: .68; transform: scaleX(1); } 46% { opacity: .18; transform: scaleX(.46); } }
@keyframes coreBreathe { 0%,100% { box-shadow: 0 8px 20px rgba(57,118,163,.1); } 50% { box-shadow: 0 10px 28px rgba(57,118,163,.2); } }
@keyframes coreRing { 0%,60%,100% { opacity: .35; transform: scale(.82); } 78% { opacity: .72; transform: scale(1.18); } }
@keyframes nodeSignal { 0%,28%,100% { color: #3976a3; background: #f4f9fc; } 40%,58% { color: #fff; background: #4a88b4; } }
@keyframes dataDown { 0%,25% { transform: translateY(0); opacity: 0; } 38%,60% { opacity: 1; } 75%,100% { transform: translateY(33px); opacity: 0; } }
@keyframes dataLeft { 0%,25% { transform: translateX(0); opacity: 0; } 38%,60% { opacity: 1; } 75%,100% { transform: translateX(-125px); opacity: 0; } }
@keyframes dataUp { 0%,25% { transform: translateY(0); opacity: 0; } 38%,60% { opacity: 1; } 75%,100% { transform: translateY(-33px); opacity: 0; } }
@keyframes dataRight { 0%,25% { transform: translateX(0); opacity: 0; } 38%,60% { opacity: 1; } 75%,100% { transform: translateX(125px); opacity: 0; } }

@media (max-width: 1100px) {
  .overview-hero { gap: 28px; padding: 36px; }
  .hero-side { flex-basis: 360px; min-width: 280px; }
  .system-hub { width: 300px; margin-right: 0; }
  .link-right { width: 95px; }
  .link-left { width: 95px; }
}
@media (max-width: 760px) {
  .overview-hero { align-items: flex-start; flex-direction: column; gap: 16px; min-height: auto; padding: 28px 24px; }
  h1 { font-size: 34px; }
  .hero-side { width: 100%; min-width: 0; align-items: flex-start; }
  .health-wave { height: 84px; }
  .points-visual { max-width: 390px; }
  .system-hub { width: min(100%, 340px); align-self: center; }
}
@media (prefers-reduced-motion: reduce) {
  .overview-hero, .overview-hero *, .overview-hero *::before, .overview-hero *::after { animation: none !important; transition: none !important; }
}
</style>

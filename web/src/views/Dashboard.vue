<template>
  <div class="dash">
    <!-- 欢迎横幅：渐变网格 + 颗粒 + 漂浮光环 -->
    <section class="hero hda-mesh hda-grain hda-enter">
      <div class="hero-text">
        <p class="hi">{{ greeting }}，{{ nickname }}</p>
        <h1>祝您<span class="hl">健康</span>每一天</h1>
        <p class="sub">{{ today }}</p>
        <div class="chips">
          <span class="chip" @click="go('/health/metric')">＋ 记录体征</span>
          <span class="chip ghost" @click="go('/consult')">向 AI 咨询</span>
        </div>
      </div>
      <div class="hero-art">
        <div class="ring r1"></div><div class="ring r2"></div>
        <div class="orb"><el-icon :size="60"><FirstAidKit /></el-icon></div>
      </div>
    </section>

    <!-- 统计卡：数字滚动 + 悬浮抬升 -->
    <section class="stats">
      <div class="stat" v-for="(s, i) in stats" :key="s.label" v-reveal="i" :style="{ '--c': s.color }">
        <div class="ic"><el-icon :size="26"><component :is="s.icon" /></el-icon></div>
        <div class="meta">
          <div class="num" v-countup="s.value">0</div>
          <div class="lb">{{ s.label }}</div>
        </div>
        <div class="spark"></div>
      </div>
    </section>

    <!-- 快捷入口 -->
    <section class="quick">
      <h3 class="sec-title" v-reveal>快捷入口</h3>
      <div class="tiles">
        <div class="tile" v-for="(q, i) in quicks" :key="q.path" v-reveal="i"
             @click="go(q.path)" @mousemove="magnet" @mouseleave="reset" :style="{ '--c': q.color }">
          <div class="tile-ic"><el-icon :size="30"><component :is="q.icon" /></el-icon></div>
          <div class="tile-name">{{ q.name }}</div>
          <el-icon class="arr"><ArrowRightBold /></el-icon>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, markRaw } from 'vue'
import { useRouter } from 'vue-router'
import { getPointBalance, pageMetric, pageReport, pageAlerts } from '@/api'
import { GoldMedal, TrendCharts, Document, BellFilled, FirstAidKit,
  Notebook, ChatDotRound, Avatar, ArrowRightBold } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const nickname = computed(() => userStore.userInfo.nickname || userStore.userInfo.username || '您')

const hour = new Date().getHours()
const greeting = hour < 6 ? '凌晨好' : hour < 11 ? '早上好' : hour < 14 ? '中午好' : hour < 18 ? '下午好' : '晚上好'
const today = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })

const raw = ref({ balance: 0, metric: 0, report: 0, alert: 0 })
const stats = computed(() => [
  { label: '我的积分', value: raw.value.balance, icon: markRaw(GoldMedal), color: '#E8933B' },
  { label: '体征记录', value: raw.value.metric, icon: markRaw(TrendCharts), color: '#2FA37C' },
  { label: '健康报告', value: raw.value.report, icon: markRaw(Document), color: '#3E8ED0' },
  { label: '健康预警', value: raw.value.alert, icon: markRaw(BellFilled), color: '#E5654B' }
])

const quicks = [
  { name: '健康档案', path: '/health/profile', icon: markRaw(Notebook), color: '#2FA37C' },
  { name: '体征数据', path: '/health/metric', icon: markRaw(TrendCharts), color: '#37A86B' },
  { name: '积分商城', path: '/point/mall', icon: markRaw(GoldMedal), color: '#E8933B' },
  { name: '医生专家库', path: '/doctor', icon: markRaw(Avatar), color: '#3E8ED0' },
  { name: '健康咨询', path: '/consult', icon: markRaw(ChatDotRound), color: '#7C6FD6' },
  { name: '健康预警', path: '/alert', icon: markRaw(BellFilled), color: '#E5654B' }
]

function go(p) { router.push(p) }
// 磁吸悬浮：卡片随鼠标轻微倾斜
function magnet(e) {
  const el = e.currentTarget
  const r = el.getBoundingClientRect()
  const x = (e.clientX - r.left) / r.width - 0.5
  const y = (e.clientY - r.top) / r.height - 0.5
  el.style.transform = `translateY(-6px) rotateX(${-y * 6}deg) rotateY(${x * 6}deg)`
}
function reset(e) { e.currentTarget.style.transform = '' }

onMounted(async () => {
  try {
    raw.value.balance = (await getPointBalance()).data
    raw.value.metric = (await pageMetric({ pageNum: 1, pageSize: 1 })).data.total
    raw.value.report = (await pageReport({ pageNum: 1, pageSize: 1 })).data.total
    raw.value.alert = (await pageAlerts({ pageNum: 1, pageSize: 1 })).data.total
  } catch (e) {}
})
</script>

<style scoped>
.dash { display: flex; flex-direction: column; gap: 28px; }

/* 欢迎横幅 */
.hero {
  display: flex; justify-content: space-between; align-items: center;
  padding: 40px 44px; border-radius: 28px; overflow: hidden; position: relative;
  background: #ffffff; box-shadow: var(--hda-shadow);
}
.hero-text { position: relative; z-index: 2; }
.hero-text .hi { font-size: 18px; color: var(--hda-ink-soft); margin: 0 0 6px; font-weight: 500; }
.hero-text h1 { font-size: clamp(30px, 3.4vw, 46px); margin: 0 0 10px; color: var(--hda-ink); }
.hero-text h1 .hl { color: var(--el-color-primary); position: relative; }
.hero-text h1 .hl::after {
  content: ""; position: absolute; left: 0; right: 0; bottom: 4px; height: 10px;
  background: var(--el-color-primary-light-7); z-index: -1; border-radius: 4px;
}
.hero-text .sub { font-size: 16px; color: var(--hda-ink-soft); margin: 0 0 22px; }
.chips { display: flex; gap: 12px; }
.chip {
  padding: 10px 20px; border-radius: 999px; font-size: 16px; font-weight: 600; cursor: pointer;
  background: var(--el-color-primary); color: #fff; transition: transform .22s var(--ease-spring), box-shadow .25s;
  box-shadow: 0 8px 18px rgba(47,163,124,.28);
}
.chip:hover { transform: translateY(-3px); }
.chip.ghost { background: #fff; color: var(--el-color-primary); box-shadow: inset 0 0 0 2px var(--el-color-primary-light-5); }

.hero-art { position: relative; width: 190px; height: 160px; display: grid; place-items: center; z-index: 2; }
.ring { position: absolute; border-radius: 50%; border: 2px dashed rgba(47,163,124,.35); }
.r1 { width: 150px; height: 150px; animation: hdaSpin 20s linear infinite; }
.r2 { width: 110px; height: 110px; border-style: dotted; animation: hdaSpin 14s linear infinite reverse; }
.orb {
  width: 96px; height: 96px; border-radius: 50%; display: grid; place-items: center; color: #fff;
  background: linear-gradient(135deg, #37B389, #279470); box-shadow: 0 16px 36px rgba(39,148,112,.42);
  animation: hdaFloat 5s ease-in-out infinite;
}

/* 统计卡 */
.stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; }
.stat {
  position: relative; overflow: hidden; display: flex; align-items: center; gap: 16px;
  background: var(--hda-card); border-radius: 22px; padding: 24px;
  box-shadow: var(--hda-shadow-sm); transition: transform .4s var(--ease-out), box-shadow .4s var(--ease-out);
}
.stat:hover { transform: translateY(-6px); box-shadow: var(--hda-shadow); }
.stat .ic {
  width: 60px; height: 60px; border-radius: 18px; display: grid; place-items: center;
  color: var(--c); background: color-mix(in srgb, var(--c) 14%, white);
}
.stat .num { font-size: 34px; font-weight: 800; color: var(--hda-ink); line-height: 1.05; }
.stat .lb { font-size: 16px; color: var(--hda-ink-soft); }
.stat .spark {
  position: absolute; right: -20px; bottom: -20px; width: 90px; height: 90px; border-radius: 50%;
  background: radial-gradient(circle, color-mix(in srgb, var(--c) 22%, transparent), transparent 70%);
}

/* 快捷入口 */
.sec-title { font-size: 22px; margin: 4px 0 16px; color: var(--hda-ink); }
.tiles { display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; perspective: 900px; }
.tile {
  position: relative; display: flex; align-items: center; gap: 16px;
  background: var(--hda-card); border-radius: 20px; padding: 24px;
  box-shadow: var(--hda-shadow-sm); cursor: pointer; transform-style: preserve-3d;
  transition: transform .3s var(--ease-out), box-shadow .3s;
}
.tile:hover { box-shadow: var(--hda-shadow); }
.tile-ic {
  width: 58px; height: 58px; border-radius: 17px; display: grid; place-items: center; color: #fff;
  background: var(--c); box-shadow: 0 10px 20px color-mix(in srgb, var(--c) 42%, transparent);
}
.tile-name { font-size: 19px; font-weight: 600; color: var(--hda-ink); }
.tile .arr { margin-left: auto; color: var(--hda-ink-soft); transition: transform .3s var(--ease-spring); }
.tile:hover .arr { transform: translateX(7px); color: var(--c); }

@media (max-width: 1100px) {
  .stats { grid-template-columns: repeat(2, 1fr); }
  .tiles { grid-template-columns: repeat(2, 1fr); }
  .hero-art { display: none; }
}
</style>

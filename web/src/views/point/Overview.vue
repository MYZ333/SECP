<template>
  <div class="overview-page hda-enter">
    <OverviewHero
      variant="points"
      eyebrow="POINT CENTER"
      title="积分中心"
      description="通过签到、完善档案、记录体征和健康咨询获取积分，并在积分商城兑换健康服务与用品。"
      cta-label="去兑换"
      metric-label="可用积分"
      :metric-value="balance"
      @action="go('/point/mall')"
    />

    <section class="module-grid two">
      <article
        v-for="(item, index) in modules"
        :key="item.path"
        class="module-card"
        role="button"
        tabindex="0"
        :style="{ '--index': index }"
        @click="go(item.path)"
        @keyup.enter="go(item.path)"
        @keyup.space.prevent="go(item.path)"
      >
        <span class="module-accent" aria-hidden="true"></span>
        <div class="module-icon"><el-icon><component :is="item.icon" /></el-icon></div>
        <div class="module-copy">
          <h2>{{ item.title }}</h2>
          <p>{{ item.desc }}</p>
        </div>
        <span class="module-action">进入<el-icon><ArrowRight /></el-icon></span>
      </article>
    </section>

    <section class="content-grid">
      <article class="preview-panel wide">
        <div class="panel-head"><h3>积分任务</h3><el-button link type="primary" @click="go('/point/mall')">查看</el-button></div>
        <el-skeleton v-if="loading" :rows="4" animated />
        <div v-else class="task-list">
          <div v-for="task in tasks.slice(0, 4)" :key="task.type" class="task-row">
            <div><strong>{{ task.name }}</strong><span>{{ task.description }}</span></div>
            <el-tag :type="task.status === 'DONE' ? 'success' : 'warning'" effect="plain">{{ task.status === 'DONE' ? '已完成' : `+${task.points}` }}</el-tag>
          </div>
          <el-empty v-if="!tasks.length" description="暂无积分任务" :image-size="70" />
        </div>
      </article>

      <article class="preview-panel">
        <div class="panel-head"><h3>最近明细</h3><el-button link type="primary" @click="go('/point/record')">查看</el-button></div>
        <el-skeleton v-if="loading" :rows="4" animated />
        <template v-else>
          <div v-for="record in records.slice(0, 4)" :key="record.id || record.createTime" class="record-row">
            <span>{{ record.description || record.type }}</span>
            <strong :class="{ minus: record.changePoints < 0 }">{{ record.changePoints >= 0 ? '+' : '' }}{{ record.changePoints }}</strong>
          </div>
          <el-empty v-if="!records.length" description="暂无积分明细" :image-size="70" />
        </template>
      </article>
    </section>

    <section class="product-strip">
      <div class="panel-head"><h3>积分商城精选</h3><el-button link type="primary" @click="go('/point/mall')">更多商品</el-button></div>
      <el-skeleton v-if="loading" :rows="4" animated />
      <div v-else class="product-grid">
        <article v-for="(product, index) in products" :key="product.id" class="product-card" role="button" tabindex="0" :style="{ '--index': index }" @click="go('/point/mall')" @keyup.enter="go('/point/mall')">
          <div class="product-img">
            <el-image :src="resolveServerUrl(product.image)" fit="cover">
              <template #error><el-icon><Goods /></el-icon></template>
            </el-image>
          </div>
          <h4>{{ product.name }}</h4>
          <p>{{ product.description || '健康积分兑换商品' }}</p>
          <strong>{{ product.pointsCost }} 积分</strong>
        </article>
        <el-empty v-if="!products.length" description="暂无可兑换商品" :image-size="70" />
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Goods, Tickets } from '@element-plus/icons-vue'
import { getPointBalance, getPointTasks, pagePointProducts, pagePointRecords } from '@/api'
import OverviewHero from '@/components/OverviewHero.vue'
import { resolveServerUrl } from '@/config/server'

const router = useRouter()
const balance = ref(0)
const tasks = ref([])
const records = ref([])
const products = ref([])
const loading = ref(true)

const modules = [
  { title: '积分商城', desc: '浏览可兑换商品，使用积分兑换健康用品和服务。', path: '/point/mall', icon: Goods },
  { title: '积分明细', desc: '查看积分获取、消耗和兑换记录。', path: '/point/record', icon: Tickets },
]

function go(path) { router.push(path) }

async function load() {
  try {
    const [balanceRes, taskRes, productRes, recordRes] = await Promise.allSettled([
      getPointBalance(),
      getPointTasks(),
      pagePointProducts({ pageNum: 1, pageSize: 4 }),
      pagePointRecords({ pageNum: 1, pageSize: 4 }),
    ])
    if (balanceRes.status === 'fulfilled') balance.value = balanceRes.value.data || 0
    if (taskRes.status === 'fulfilled') tasks.value = taskRes.value.data || []
    if (productRes.status === 'fulfilled') products.value = productRes.value.data.records || []
    if (recordRes.status === 'fulfilled') records.value = recordRes.value.data.records || []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.overview-page { --point-accent: #c2871d; --point-soft: #fff5df; --point-ease: cubic-bezier(.22,1,.36,1); display: flex; flex-direction: column; gap: 18px; max-width: 1380px; margin: 0 auto; }
.module-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.module-card { position: relative; display: grid; grid-template-columns: 46px 1fr auto; align-items: center; gap: 14px; min-height: 128px; padding: 22px; overflow: hidden; cursor: pointer; background: rgba(255,255,255,.8); border: 1px solid var(--hda-line); box-shadow: var(--hda-shadow-sm); transition: transform .4s var(--point-ease), box-shadow .4s ease, border-color .4s ease, background-color .4s ease; }
.module-accent { position: absolute; inset: 0 auto 0 0; width: 3px; background: linear-gradient(180deg, #e6b64c, #a86a0d); transform: scaleY(0); transition: transform .45s var(--point-ease); }
.module-card:hover { transform: translateY(-6px); border-color: #e8c983; background: #fff; box-shadow: var(--hda-shadow); }
.module-card:hover .module-accent { transform: scaleY(1); }
.module-card:active { transform: translateY(-1px) scale(.98); transition-duration: .12s; }
.module-card:focus-visible, .product-card:focus-visible { outline: 3px solid #efd99d; outline-offset: 2px; }
.module-icon { width: 46px; height: 46px; display: grid; place-items: center; color: var(--point-accent); background: var(--point-soft); font-size: 24px; transition: transform .45s var(--ease-spring), color .35s ease, background-color .35s ease; }
.module-card:hover .module-icon { transform: translateY(-3px) rotate(-4deg) scale(1.08); color: #fff; background: linear-gradient(145deg, #dcaa39, #a86a0d); }
.module-copy { min-width: 0; }
.module-card h2 { margin: 0 0 6px; font-size: 20px; letter-spacing: 0; }
.module-card p { margin: 0; color: var(--hda-ink-soft); font-size: 14px; line-height: 1.55; }
.module-action { display: inline-flex; align-items: center; gap: 5px; color: #9a6411; font-size: 15px; font-weight: 700; white-space: nowrap; }
.module-action .el-icon { transition: transform .4s var(--point-ease); }
.module-card:hover .module-action .el-icon { transform: translateX(5px); }
.content-grid { display: grid; grid-template-columns: 1.25fr .75fr; gap: 16px; }
.preview-panel, .product-strip { padding: 22px; background: rgba(255,255,255,.8); border: 1px solid var(--hda-line); box-shadow: var(--hda-shadow-sm); transition: transform .38s var(--point-ease), box-shadow .38s ease, background-color .38s ease; }
.preview-panel:hover, .product-strip:hover { transform: translateY(-3px); background: rgba(255,255,255,.97); box-shadow: var(--hda-shadow); }
.panel-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.panel-head h3 { margin: 0; font-size: 18px; }
.task-list { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.task-row { display: flex; justify-content: space-between; gap: 12px; padding: 14px; background: #f8fafd; transition: transform .32s var(--point-ease), background-color .32s ease; }
.task-row:hover { transform: translateX(3px); background: var(--point-soft); }
.task-row strong, .task-row span { display: block; }
.task-row span { margin-top: 3px; color: var(--hda-ink-soft); font-size: 13px; }
.record-row { display: flex; justify-content: space-between; gap: 16px; padding: 12px 8px; border-bottom: 1px solid var(--hda-line); transition: padding-left .3s var(--point-ease), background-color .3s ease; }
.record-row:hover { padding-left: 13px; background: #fffaf0; }
.record-row span { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.record-row strong { color: var(--el-color-success); }
.record-row strong.minus { color: var(--el-color-danger); }
.product-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.product-card { min-height: 240px; padding: 14px; cursor: pointer; background: #fff; border: 1px solid var(--hda-line); transition: transform .4s var(--point-ease), box-shadow .4s ease, border-color .4s ease; }
.product-card:hover { transform: translateY(-6px); border-color: #e8c983; box-shadow: var(--hda-shadow); }
.product-card:active { transform: translateY(-1px) scale(.98); }
.product-img { height: 104px; display: grid; place-items: center; overflow: hidden; color: #d97612; background: #f8fafd; font-size: 26px; }
.product-img .el-image { width: 100%; height: 100%; display: grid; place-items: center; }
.product-card h4 { margin: 12px 0 4px; font-size: 17px; }
.product-card p { height: 44px; margin: 0 0 8px; overflow: hidden; color: var(--hda-ink-soft); font-size: 13px; line-height: 1.65; }
.product-card strong { color: #d97612; }
@media (prefers-reduced-motion: no-preference) {
  .module-card, .preview-panel, .product-strip, .product-card { animation: pointRise .58s var(--point-ease) both; animation-delay: calc(var(--index, 0) * 70ms + 100ms); }
}
@keyframes pointRise { from { opacity: 0; transform: translateY(16px); } to { opacity: 1; transform: translateY(0); } }
@media (max-width: 1000px) { .content-grid, .task-list, .product-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 760px) { .module-grid, .content-grid, .task-list, .product-grid { grid-template-columns: 1fr; } }
@media (prefers-reduced-motion: reduce) {
  .overview-page *, .overview-page *::before, .overview-page *::after { animation: none !important; transition: none !important; }
  .module-card:hover, .preview-panel:hover, .product-strip:hover, .product-card:hover, .task-row:hover { transform: none; }
}
</style>

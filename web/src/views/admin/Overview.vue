<template>
  <div class="admin-overview hda-enter">
    <OverviewHero
      variant="admin"
      eyebrow="ADMIN CONSOLE"
      title="系统管理"
      description="统一维护平台用户、积分商品、医生专家和智能体知识库，快速进入常用后台管理任务。"
      cta-label="进入用户管理"
      @action="go('/admin/user')"
    />

    <section class="summary-strip">
      <div v-for="(item, index) in summaryItems" :key="item.label" class="summary-item" :style="{ '--index': index }">
        <span>{{ item.label }}</span><strong>{{ item.value }}</strong>
      </div>
    </section>

    <section class="module-grid">
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

    <section class="preview-grid">
      <article class="preview-panel" style="--index: 0">
        <div class="panel-head"><h3>用户概览</h3><el-button link type="primary" @click="go('/admin/user')">管理</el-button></div>
        <el-skeleton v-if="loading" :rows="4" animated />
        <template v-else>
          <div v-for="user in users" :key="user.id" class="line-row">
            <span>{{ user.nickname || user.username }}</span>
            <el-tag size="small" effect="plain">{{ user.role }}</el-tag>
          </div>
          <el-empty v-if="!users.length" description="暂无用户数据" :image-size="70" />
        </template>
      </article>

      <article class="preview-panel" style="--index: 1">
        <div class="panel-head"><h3>商品维护</h3><el-button link type="primary" @click="go('/admin/product')">管理</el-button></div>
        <el-skeleton v-if="loading" :rows="4" animated />
        <template v-else>
          <div v-for="product in products" :key="product.id" class="line-row">
            <span>{{ product.name }}</span>
            <strong>{{ product.pointsCost }} 分</strong>
          </div>
          <el-empty v-if="!products.length" description="暂无商品数据" :image-size="70" />
        </template>
      </article>

      <article class="preview-panel" style="--index: 2">
        <div class="panel-head"><h3>医生专家</h3><el-button link type="primary" @click="go('/admin/doctor')">管理</el-button></div>
        <el-skeleton v-if="loading" :rows="4" animated />
        <template v-else>
          <div v-for="doctor in doctors" :key="doctor.id" class="line-row">
            <span>{{ doctor.name }} · {{ doctor.department || '未填写科室' }}</span>
            <el-tag size="small" :type="doctor.auditStatus === 'PENDING' ? 'warning' : 'success'" effect="plain">{{ auditText(doctor.auditStatus) }}</el-tag>
          </div>
          <el-empty v-if="!doctors.length" description="暂无医生数据" :image-size="70" />
        </template>
      </article>

      <article class="preview-panel" style="--index: 3">
        <div class="panel-head"><h3>知识库</h3><el-button link type="primary" @click="go('/admin/knowledge')">管理</el-button></div>
        <el-skeleton v-if="loading" :rows="4" animated />
        <template v-else>
          <div v-for="doc in knowledge" :key="doc.id" class="line-row">
            <span>{{ doc.title }}</span>
            <el-tag size="small" :type="doc.status === 'PUBLISHED' ? 'success' : 'warning'" effect="plain">{{ knowledgeStatus(doc.status) }}</el-tag>
          </div>
          <el-empty v-if="!knowledge.length" description="暂无知识资料" :image-size="70" />
        </template>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Box, Document, Goods, User } from '@element-plus/icons-vue'
import { adminPageDoctors, adminPageKnowledge, adminPageProducts, adminPageUsers } from '@/api'
import OverviewHero from '@/components/OverviewHero.vue'

const router = useRouter()
const totals = ref({ user: 0, product: 0, doctor: 0, knowledge: 0 })
const users = ref([])
const products = ref([])
const doctors = ref([])
const knowledge = ref([])
const loading = ref(true)

const modules = [
  { title: '用户管理', desc: '查看平台用户，维护状态并调整积分。', path: '/admin/user', icon: User },
  { title: '商品管理', desc: '新增、编辑和上下架积分商城商品。', path: '/admin/product', icon: Goods },
  { title: '专家管理', desc: '审核医生注册，维护专家资料与启用状态。', path: '/admin/doctor', icon: Box },
  { title: '智能体知识库', desc: '上传、预览、发布健康助手与应用助手资料。', path: '/admin/knowledge', icon: Document },
]

const summaryItems = computed(() => [
  { label: '用户总数', value: totals.value.user },
  { label: '商品总数', value: totals.value.product },
  { label: '医生总数', value: totals.value.doctor },
  { label: '知识资料', value: totals.value.knowledge },
])

function go(path) { router.push(path) }
function auditText(status) { return ({ PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝' })[status] || '已通过' }
function knowledgeStatus(status) { return ({ DRAFT: '待审核', INDEXING: '索引中', PUBLISHED: '已发布', FAILED: '异常', INACTIVE: '停用' })[status] || status }

async function load() {
  try {
    const [userRes, productRes, doctorRes, knowledgeRes] = await Promise.allSettled([
      adminPageUsers({ pageNum: 1, pageSize: 4 }),
      adminPageProducts({ pageNum: 1, pageSize: 4 }),
      adminPageDoctors({ pageNum: 1, pageSize: 4 }),
      adminPageKnowledge({ pageNum: 1, pageSize: 4 }),
    ])
    if (userRes.status === 'fulfilled') {
      totals.value.user = userRes.value.data.total || 0
      users.value = userRes.value.data.records || []
    }
    if (productRes.status === 'fulfilled') {
      totals.value.product = productRes.value.data.total || 0
      products.value = productRes.value.data.records || []
    }
    if (doctorRes.status === 'fulfilled') {
      totals.value.doctor = doctorRes.value.data.total || 0
      doctors.value = doctorRes.value.data.records || []
    }
    if (knowledgeRes.status === 'fulfilled') {
      totals.value.knowledge = knowledgeRes.value.data.total || 0
      knowledge.value = knowledgeRes.value.data.records || []
    }
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.admin-overview { --admin-ease: cubic-bezier(.22,1,.36,1); display: flex; flex-direction: column; gap: 18px; max-width: 1380px; margin: 0 auto; }
.summary-strip { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1px; overflow: hidden; border: 1px solid var(--hda-line); background: var(--hda-line); }
.summary-strip div { display: flex; align-items: baseline; justify-content: space-between; padding: 20px 24px; background: rgba(255,255,255,.86); }
.summary-item { position: relative; transition: transform .35s var(--admin-ease), background-color .35s ease; }
.summary-item::after { content: ""; position: absolute; left: 24px; right: 24px; bottom: 0; height: 2px; background: linear-gradient(90deg, #5d99c4, #b7d7eb); transform: scaleX(0); transform-origin: left; transition: transform .45s var(--admin-ease); }
.summary-item:hover { z-index: 1; transform: translateY(-2px); background: #fff; }
.summary-item:hover::after { transform: scaleX(1); }
.summary-strip span { color: var(--hda-ink-soft); font-size: 14px; }
.summary-strip strong { color: var(--hda-ink); font-size: 30px; font-variant-numeric: tabular-nums; }
.module-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.module-card { position: relative; display: grid; grid-template-columns: 46px 1fr auto; align-items: center; gap: 14px; min-height: 140px; padding: 22px; overflow: hidden; cursor: pointer; background: rgba(255,255,255,.8); border: 1px solid var(--hda-line); box-shadow: var(--hda-shadow-sm); transition: transform .4s var(--admin-ease), box-shadow .4s ease, border-color .4s ease, background-color .4s ease; }
.module-accent { position: absolute; inset: 0 auto 0 0; width: 3px; background: linear-gradient(180deg, #5d99c4, #b7d7eb); transform: scaleY(0); transition: transform .45s var(--admin-ease); }
.module-card:hover { transform: translateY(-6px); border-color: #a9cbe1; background: #fff; box-shadow: var(--hda-shadow); }
.module-card:hover .module-accent { transform: scaleY(1); }
.module-card:active { transform: translateY(-1px) scale(.98); transition-duration: .12s; }
.module-card:focus-visible { outline: 3px solid #bad9ec; outline-offset: 2px; }
.module-icon { width: 46px; height: 46px; display: grid; place-items: center; color: #3976a3; background: #edf6fb; font-size: 24px; transition: transform .45s var(--ease-spring), color .35s ease, background-color .35s ease; }
.module-card:hover .module-icon { transform: translateY(-3px) scale(1.08); color: #fff; background: linear-gradient(145deg, #477fa8, #80b6d5); }
.module-copy { min-width: 0; }
.module-card h2 { margin: 0 0 6px; font-size: 20px; letter-spacing: 0; }
.module-card p { margin: 0; color: var(--hda-ink-soft); font-size: 14px; line-height: 1.55; }
.module-action { display: inline-flex; align-items: center; gap: 5px; color: #3976a3; font-size: 15px; font-weight: 700; white-space: nowrap; }
.module-action .el-icon { transition: transform .4s var(--admin-ease); }
.module-card:hover .module-action .el-icon { transform: translateX(5px); }
.preview-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.preview-panel { min-height: 250px; padding: 22px; background: rgba(255,255,255,.8); border: 1px solid var(--hda-line); box-shadow: var(--hda-shadow-sm); transition: transform .38s var(--admin-ease), box-shadow .38s ease, background-color .38s ease; }
.preview-panel:hover { transform: translateY(-3px); background: rgba(255,255,255,.97); box-shadow: var(--hda-shadow); }
.panel-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.panel-head h3 { margin: 0; font-size: 18px; }
.line-row { display: flex; justify-content: space-between; align-items: center; gap: 12px; min-height: 42px; padding: 10px 8px; border-bottom: 1px solid var(--hda-line); transition: padding-left .3s var(--admin-ease), background-color .3s ease; }
.line-row:hover { padding-left: 13px; background: #f2f8fc; }
.line-row span { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.line-row strong { color: var(--el-color-primary); font-size: 14px; }
@media (prefers-reduced-motion: no-preference) {
  .summary-item, .module-card, .preview-panel { animation: adminRise .58s var(--admin-ease) both; animation-delay: calc(var(--index) * 70ms + 100ms); }
}
@keyframes adminRise { from { opacity: 0; transform: translateY(16px); } to { opacity: 1; transform: translateY(0); } }
@media (max-width: 1100px) { .module-grid, .preview-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 760px) { .summary-strip, .module-grid, .preview-grid { grid-template-columns: 1fr; } }
@media (prefers-reduced-motion: reduce) {
  .admin-overview *, .admin-overview *::before, .admin-overview *::after { animation: none !important; transition: none !important; }
  .summary-item:hover, .module-card:hover, .preview-panel:hover { transform: none; }
}
</style>

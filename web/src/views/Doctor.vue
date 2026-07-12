<template>
  <div class="doctor-page">
    <!-- 搜索 + 职称筛选 -->
    <div class="search-bar hda-enter">
      <el-input v-model="query.keyword" placeholder="搜索医生姓名 / 医院 / 擅长领域（如：高血压）" :prefix-icon="Search"
                clearable @keyup.enter="reload" @clear="reload" />
      <el-select v-model="query.title" placeholder="职称" clearable style="width: 160px" @change="reload">
        <el-option v-for="t in titles" :key="t" :label="t" :value="t" />
      </el-select>
      <el-button type="primary" @click="reload">搜索</el-button>
    </div>

    <!-- 科室 Tab 栏（来自后端去重列表） -->
    <div class="dept-tabs hda-enter">
      <button :class="{ on: !query.department }" @click="pickDept('')">全部科室</button>
      <button v-for="d in departments" :key="d" :class="{ on: query.department === d }" @click="pickDept(d)">
        {{ d }}
      </button>
    </div>

    <!-- 医生卡片 -->
    <div class="grid">
      <div class="doc" v-for="(d, i) in list" :key="d.id" v-reveal="i % 3" @click="openDetail(d)">
        <div class="top">
          <el-avatar :size="66" :src="d.avatar" class="ava">{{ d.name?.[0] }}</el-avatar>
          <div class="meta">
            <div class="name-row"><span class="name">{{ d.name }}</span><el-tag round effect="light">{{ d.title }}</el-tag></div>
            <div class="place">{{ d.hospital }} · {{ d.department }}</div>
          </div>
        </div>
        <div class="spec"><span class="k">擅长</span>{{ d.speciality || '—' }}</div>
        <p class="intro">{{ d.introduction }}</p>
        <div class="actions">
          <el-button size="default" @click.stop="openDetail(d)">查看详情</el-button>
          <el-button size="default" type="primary" plain @click.stop="askDoctor(d)">向TA咨询</el-button>
        </div>
      </div>
    </div>

    <el-empty v-if="!list.length" description="没有找到符合条件的医生" />
    <div class="pager">
      <el-pagination background layout="prev, pager, next" :total="total"
                     :page-size="query.pageSize" :current-page="query.pageNum" @current-change="onPage" />
    </div>

    <!-- 医生详情抽屉 -->
    <el-drawer v-model="drawer" size="440px" :with-header="false" class="doc-drawer" append-to-body>
      <div v-if="current" class="detail">
        <div class="d-head">
          <el-avatar :size="92" :src="current.avatar" class="d-ava">{{ current.name?.[0] }}</el-avatar>
          <h3>{{ current.name }}</h3>
          <p class="d-title">{{ current.title }}</p>
          <p class="d-place">{{ current.hospital }} · {{ current.department }}</p>
        </div>
        <div class="d-sec">
          <h4>擅长领域</h4>
          <div class="d-tags">
            <span v-for="s in specTags(current.speciality)" :key="s" class="d-tag">{{ s }}</span>
          </div>
        </div>
        <div class="d-sec">
          <h4>医生简介</h4>
          <p class="d-intro">{{ current.introduction || '暂无简介' }}</p>
        </div>
        <el-button type="primary" size="large" class="d-ask" @click="askDoctor(current)">向TA咨询健康问题</el-button>
        <p class="d-note">咨询由 AI 健康助手基于医生擅长领域应答</p>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { pageDoctors, getDoctorDepartments } from '@/api'

const router = useRouter()
const list = ref([]), total = ref(0)
const departments = ref([])
const titles = ['主任医师', '副主任医师', '主治医师']
const query = ref({ pageNum: 1, pageSize: 9, keyword: '', department: '', title: '' })
const drawer = ref(false)
const current = ref(null)

async function load() {
  const res = await pageDoctors(query.value)
  list.value = res.data.records
  total.value = res.data.total
}
function reload() { query.value.pageNum = 1; load() }
function onPage(p) { query.value.pageNum = p; load() }
function pickDept(d) { query.value.department = d; reload() }

function openDetail(d) { current.value = d; drawer.value = true }

/** 擅长领域拆成标签（顿号/逗号分隔） */
function specTags(s) {
  return (s || '').split(/[、，,;；]/).map(x => x.trim()).filter(Boolean)
}

/** 跳转健康咨询页，预填问题（只填入输入框，不自动发送） */
function askDoctor(d) {
  const spec = specTags(d.speciality)[0] || d.department
  const q = `我想咨询${d.department}的健康问题，主要是关于${spec}方面的，请给我一些建议。`
  router.push({ path: '/consult', query: { q } })
}

onMounted(() => {
  load()
  getDoctorDepartments().then(res => { departments.value = res.data || [] }).catch(() => {})
})
</script>

<style scoped>
.doctor-page {
  --ease: cubic-bezier(0.22, 1, 0.36, 1);
  --r: 12px;
  --shadow: 0 4px 16px rgba(64, 128, 255, 0.08);
}

/* 搜索栏 */
.search-bar { display: flex; gap: 12px; margin-bottom: 16px; }
.search-bar .el-input { max-width: 420px; }

/* 科室 Tab 栏 */
.dept-tabs { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 24px; }
.dept-tabs button {
  min-height: 44px; padding: 0 20px; cursor: pointer; font-size: 16px;
  background: rgba(255, 255, 255, 0.72); color: var(--hda-ink-soft);
  border: 1px solid rgba(255, 255, 255, 0.65); border-radius: var(--r);
  backdrop-filter: blur(14px); box-shadow: var(--shadow);
  transition: color 0.4s var(--ease), background-color 0.4s var(--ease), transform 0.4s var(--ease);
}
.dept-tabs button:hover { color: var(--el-color-primary); transform: translateY(-2px); }
.dept-tabs button.on {
  background: var(--el-color-primary); color: #fff; border-color: transparent; font-weight: 600;
}

/* 卡片网格 */
.grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.doc {
  display: flex; flex-direction: column; padding: 24px; cursor: pointer;
  background: rgba(255, 255, 255, 0.72); backdrop-filter: blur(14px);
  border: 1px solid rgba(255, 255, 255, 0.65); border-radius: var(--r);
  box-shadow: var(--shadow);
  transition: transform 0.4s var(--ease), box-shadow 0.4s var(--ease);
}
.doc:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(64, 128, 255, 0.12); }
.top { display: flex; gap: 14px; align-items: center; }
.ava { background: linear-gradient(135deg, #3E86EC, #2E6FE0); color: #fff; font-size: 24px; font-weight: 700; flex-shrink: 0; }
.name-row { display: flex; align-items: center; gap: 8px; }
.name { font-size: 20px; font-weight: 700; color: var(--hda-ink); }
.place { color: var(--hda-ink-soft); font-size: 15px; margin-top: 4px; }
.spec { margin: 16px 0 8px; font-size: 15px; color: var(--hda-ink); }
.spec .k {
  display: inline-block; background: var(--el-color-primary-light-9); color: var(--el-color-primary);
  font-weight: 600; padding: 2px 10px; border-radius: 6px; margin-right: 8px; font-size: 13px;
}
.intro {
  color: var(--hda-ink-soft); font-size: 14px; line-height: 1.6; margin: 0 0 16px;
  display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.actions { margin-top: auto; display: flex; gap: 8px; }
.actions .el-button { flex: 1; border-radius: 8px; min-height: 40px; }

.pager { display: flex; justify-content: center; margin-top: 24px; }

/* 详情抽屉 */
.detail { padding: 8px 8px 24px; }
.d-head { text-align: center; padding: 24px 0 16px; }
.d-ava { background: linear-gradient(135deg, #3E86EC, #2E6FE0); color: #fff; font-size: 36px; font-weight: 700; }
.d-head h3 { margin: 16px 0 4px; font-size: 26px; color: var(--hda-ink); }
.d-title { margin: 0 0 4px; font-size: 16px; color: var(--el-color-primary); font-weight: 600; }
.d-place { margin: 0; font-size: 15px; color: var(--hda-ink-soft); }
.d-sec { margin-top: 24px; }
.d-sec h4 { margin: 0 0 12px; font-size: 17px; color: var(--hda-ink); }
.d-tags { display: flex; flex-wrap: wrap; gap: 8px; }
.d-tag {
  padding: 6px 14px; font-size: 15px; border-radius: 8px;
  background: var(--el-color-primary-light-9); color: var(--el-color-primary);
}
.d-intro { margin: 0; font-size: 16px; line-height: 1.8; color: var(--hda-ink-soft); }
.d-ask { width: 100%; margin-top: 32px; border-radius: var(--r); }
.d-note { margin: 10px 0 0; text-align: center; font-size: 13px; color: #93a2b5; }

@media (max-width: 1100px) { .grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 640px) { .grid { grid-template-columns: 1fr; } }
@media (prefers-reduced-motion: reduce) {
  .doctor-page *, .doctor-page *::before, .doctor-page *::after { animation: none !important; transition: none !important; }
}
</style>

<template>
  <div class="hda-enter">
    <div class="search-bar">
      <el-input v-model="query.keyword" placeholder="搜索医生姓名" :prefix-icon="Search"
                style="max-width:320px" clearable @keyup.enter="reload" @clear="reload" />
      <el-button type="primary" @click="reload">搜索</el-button>
    </div>

    <div class="grid">
      <div class="doc" v-for="(d, i) in list" :key="d.id" v-reveal="i % 3">
        <div class="top">
          <el-avatar :size="66" :src="d.avatar" class="ava">{{ d.name?.[0] }}</el-avatar>
          <div class="meta">
            <div class="name-row"><span class="name">{{ d.name }}</span><el-tag round effect="light">{{ d.title }}</el-tag></div>
            <div class="place">{{ d.hospital }} · {{ d.department }}</div>
          </div>
        </div>
        <div class="spec"><span class="k">擅长</span>{{ d.speciality || '—' }}</div>
        <p class="intro">{{ d.introduction }}</p>
      </div>
    </div>

    <el-empty v-if="!list.length" description="没有找到医生" />
    <div class="pager">
      <el-pagination background layout="prev, pager, next" :total="total" :page-size="query.pageSize" @current-change="onPage" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { pageDoctors } from '@/api'
const list = ref([]), total = ref(0)
const query = ref({ pageNum: 1, pageSize: 9, keyword: '' })
async function load() { const res = await pageDoctors(query.value); list.value = res.data.records; total.value = res.data.total }
function reload() { query.value.pageNum = 1; load() }
function onPage(p) { query.value.pageNum = p; load() }
onMounted(load)
</script>

<style scoped>
.search-bar { display: flex; gap: 12px; margin-bottom: 22px; }
.grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
.doc {
  background: var(--hda-card); border-radius: 20px; padding: 22px;
  box-shadow: var(--hda-shadow-sm); transition: transform .3s cubic-bezier(.2,.8,.2,1), box-shadow .3s;
  border-top: 4px solid var(--el-color-primary);
}
.doc:hover { transform: translateY(-6px); box-shadow: var(--hda-shadow); }
.top { display: flex; gap: 14px; align-items: center; }
.ava { background: var(--el-color-primary); color: #fff; font-size: 24px; font-weight: 700; flex-shrink: 0; }
.name-row { display: flex; align-items: center; gap: 8px; }
.name { font-size: 20px; font-weight: 700; color: var(--hda-ink); }
.place { color: var(--hda-ink-soft); font-size: 15px; margin-top: 4px; }
.spec { margin: 16px 0 8px; font-size: 15px; color: var(--hda-ink); }
.spec .k { display: inline-block; background: var(--el-color-primary-light-9); color: var(--el-color-primary);
  font-weight: 600; padding: 2px 10px; border-radius: 8px; margin-right: 8px; font-size: 13px; }
.intro { color: var(--hda-ink-soft); font-size: 14px; line-height: 1.6; margin: 0;
  display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.pager { display: flex; justify-content: center; margin-top: 26px; }

@media (max-width: 1100px) { .grid { grid-template-columns: repeat(2, 1fr); } }
</style>

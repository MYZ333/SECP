<template>
  <div class="hda-enter">
    <!-- 积分横幅 -->
    <div class="pt-banner">
      <div class="pt-left">
        <el-icon :size="30"><GoldMedal /></el-icon>
        <div>
          <div class="pt-lb">我的可用积分</div>
          <div class="pt-num" :class="{ bump: bumping }">{{ balance }}</div>
        </div>
        <span v-if="fly !== null" :key="flyKey" class="fly-pts">+{{ fly }}</span>
      </div>
      <div class="pt-right">
        <span v-if="checkinTask" class="pt-streak">已连续签到 {{ checkinTask.streakDays }} 天</span>
        <el-button round :disabled="checkinTask?.done" class="ck-btn" @click="doCheckIn">
          {{ checkinTask?.done ? '今日已签到' : `签到 +${checkinTask?.points ?? 2}` }}
        </el-button>
      </div>
    </div>

    <!-- 积分任务(获取方式) -->
    <div class="tasks">
      <div class="task" v-for="t in taskList" :key="t.type">
        <div class="t-info">
          <div class="t-head">
            <span class="t-name">{{ t.name }}</span>
            <span class="t-pts">+{{ t.points }}</span>
            <el-tag size="small" :type="t.daily ? 'warning' : 'success'" effect="plain">
              {{ t.daily ? '每日' : '一次性' }}
            </el-tag>
          </div>
          <div class="t-desc">{{ t.description }}</div>
        </div>
        <el-tag v-if="t.status === 'DONE'" type="success" effect="light" round>已完成</el-tag>
        <el-button v-else-if="t.status === 'CLAIMABLE'" type="primary" size="small" round
                   class="claim-btn" :loading="claiming === t.type" @click="claim(t)">
          待领取
        </el-button>
        <el-button v-else size="small" round @click="goTask(t)">去完成</el-button>
      </div>
    </div>

    <!-- 类别筛选 -->
    <div class="cats">
      <el-radio-group v-model="category" @change="onCategory">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button v-for="c in categories" :key="c" :label="c">{{ c }}</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 商品网格 -->
    <div class="grid">
      <div class="prod" v-for="(p, i) in list" :key="p.id" v-reveal="i % 4">
        <div class="cover">
          <el-image :src="p.image" fit="cover" style="width:100%;height:100%">
            <template #error><div class="img-ph"><el-icon :size="30"><Present /></el-icon></div></template>
          </el-image>
          <span class="badge">{{ p.pointsCost }} 积分</span>
        </div>
        <div class="body">
          <h4>{{ p.name }} <span v-if="p.category" class="cat-tag">{{ p.category }}</span></h4>
          <p class="desc">{{ p.description }}</p>
          <div class="foot">
            <span class="stock">库存 {{ p.stock }}</span>
            <el-button type="primary" round :disabled="balance < p.pointsCost || p.stock < 1" @click="exchange(p)">
              {{ balance < p.pointsCost ? '积分不足' : '立即兑换' }}
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <el-empty v-if="!list.length" description="暂无可兑换商品" />
    <div class="pager">
      <el-pagination background layout="prev, pager, next" :total="total" :page-size="query.pageSize" @current-change="onPage" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { GoldMedal, Present } from '@element-plus/icons-vue'
import { pagePointProducts, exchangeProduct, getPointBalance, checkIn, getPointTasks, claimTask, getProductCategories } from '@/api'
const router = useRouter()
const list = ref([]), total = ref(0), balance = ref(0), tasks = ref([]), categories = ref([]), category = ref('')
const query = ref({ pageNum: 1, pageSize: 8 })
const checkinTask = computed(() => tasks.value.find(t => t.type === 'CHECKIN'))
// 任务列表排除签到（签到用顶部横幅的即时按钮）
const taskList = computed(() => tasks.value.filter(t => t.type !== 'CHECKIN'))
const TASK_ROUTES = { METRIC: '/health/metric', CONSULT: '/consult', PROFILE: '/health/profile' }
const claiming = ref('')   // 正在领取的任务类型
const fly = ref(null)      // 领取时飘出的 +N
const flyKey = ref(0)      // 重触发飘分动画
const bumping = ref(false) // 余额数字弹跳
async function load() {
  const res = await pagePointProducts({ ...query.value, category: category.value || undefined })
  list.value = res.data.records; total.value = res.data.total
  balance.value = (await getPointBalance()).data
  tasks.value = (await getPointTasks()).data
  categories.value = (await getProductCategories()).data
}
function onCategory() { query.value.pageNum = 1; load() }
async function doCheckIn() {
  const res = await checkIn()
  ElMessage.success(`签到成功，积分 +${res.data.points}，已连续签到 ${res.data.streakDays} 天`)
  load()
}
function goTask(t) {
  const path = TASK_ROUTES[t.type]
  if (path) router.push(path)
}
async function claim(t) {
  if (claiming.value) return
  claiming.value = t.type
  try {
    const res = await claimTask(t.type)
    balance.value = res.data.balance
    fly.value = res.data.points
    flyKey.value++
    bumping.value = true
    ElMessage.success(`领取成功，积分 +${res.data.points}`)
    setTimeout(() => { fly.value = null }, 1200)
    setTimeout(() => { bumping.value = false }, 500)
  } catch (e) {
    // 失败提示已由响应拦截器统一弹出，这里只需重新同步状态
  } finally {
    claiming.value = ''
    tasks.value = (await getPointTasks()).data  // 重新同步任务状态（→ 已完成 / 保持待领取）
  }
}
function onPage(p) { query.value.pageNum = p; load() }
async function exchange(p) {
  await ElMessageBox.confirm(`确认用 ${p.pointsCost} 积分兑换「${p.name}」吗？`, '兑换确认', { confirmButtonText: '确认兑换', cancelButtonText: '再想想' })
  await exchangeProduct({ productId: p.id, quantity: 1 })
  ElMessage.success('兑换成功，可在积分明细查看'); load()
}
onMounted(load)
</script>

<style scoped>
.pt-banner {
  display: flex; justify-content: space-between; align-items: center;
  padding: 22px 28px; border-radius: var(--hda-radius-lg); margin-bottom: 24px; color: #fff;
  background: linear-gradient(120deg, #F2A85E, #E8933B); box-shadow: 0 12px 28px rgba(232,147,59,.28);
}
.pt-left { display: flex; align-items: center; gap: 16px; position: relative; }
.pt-lb { font-size: 15px; opacity: .92; }
/* 领取动画：飘出的 +N */
.fly-pts {
  position: absolute; left: 46px; top: 8px;
  font-size: 24px; font-weight: 800; color: #FFF3D0;
  text-shadow: 0 2px 10px rgba(0,0,0,.25); pointer-events: none;
  animation: flyUp 1.2s ease-out forwards;
}
@keyframes flyUp {
  0%   { opacity: 0; transform: translateY(8px) scale(.7); }
  25%  { opacity: 1; transform: translateY(-6px) scale(1.2); }
  100% { opacity: 0; transform: translateY(-46px) scale(1); }
}
/* 余额数字弹跳 */
.pt-num.bump { animation: numBump .5s ease; }
@keyframes numBump {
  0%,100% { transform: scale(1); }
  40%     { transform: scale(1.28); color: #FFF7E0; }
}
/* 待领取按钮呼吸光圈，提示可领取 */
.claim-btn { animation: claimPulse 1.6s ease-in-out infinite; }
.claim-btn:hover { animation: none; }
@keyframes claimPulse {
  0%,100% { box-shadow: 0 0 0 0 rgba(62,134,236,.5); }
  50%     { box-shadow: 0 0 0 7px rgba(62,134,236,0); }
}
.pt-num { font-size: 34px; font-weight: 800; line-height: 1.1; font-variant-numeric: tabular-nums; }
.pt-right { display: flex; align-items: center; gap: 14px; }
.pt-streak { font-size: 14px; opacity: .92; }
.ck-btn { font-weight: 700; color: #E8933B; border: none; }

.tasks {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 14px;
  margin-bottom: 24px;
}
.task {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  background: var(--hda-card); padding: 14px 18px; box-shadow: var(--hda-shadow-sm);
  border-radius: var(--hda-radius-lg);
}
.t-info { min-width: 0; }
.t-head { display: flex; align-items: center; gap: 8px; }
.t-name { font-weight: 700; color: var(--hda-ink); font-size: 15px; }
.t-pts { color: #E8933B; font-weight: 800; font-size: 14px; }
.t-desc { color: var(--hda-ink-soft); font-size: 13px; margin-top: 4px; }

.cats { margin-bottom: 18px; }
.cat-tag {
  font-size: 12px; font-weight: 600; color: var(--hda-accent);
  background: rgba(232,147,59,.12); padding: 2px 8px; border-radius: 999px; vertical-align: 2px;
}

.grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; }
.prod {
  background: var(--hda-card); border-radius: 0; overflow: hidden;
  box-shadow: var(--hda-shadow-sm); transition: transform .3s cubic-bezier(.2,.8,.2,1), box-shadow .3s;
}
.prod:hover { transform: translateY(-6px); box-shadow: var(--hda-shadow); }
.cover { position: relative; height: 150px; background: #F4F8F5; }
.badge {
  position: absolute; top: 12px; left: 12px; background: rgba(255,255,255,.92);
  color: var(--hda-accent); font-weight: 700; padding: 4px 12px; border-radius: 0; font-size: 14px;
}
.img-ph { width: 100%; height: 100%; display: grid; place-items: center; color: #B9C9C1; }
.body { padding: 16px 18px 18px; }
.body h4 { margin: 0 0 6px; font-size: 19px; color: var(--hda-ink); }
.desc { color: var(--hda-ink-soft); font-size: 14px; height: 40px; overflow: hidden; margin: 0 0 12px; }
.foot { display: flex; justify-content: space-between; align-items: center; }
.stock { font-size: 14px; color: var(--hda-ink-soft); }
.pager { display: flex; justify-content: center; margin-top: 26px; }

@media (max-width: 1100px) { .grid { grid-template-columns: repeat(2, 1fr); } }
</style>

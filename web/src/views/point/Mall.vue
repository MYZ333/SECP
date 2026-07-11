<template>
  <div class="hda-enter">
    <!-- 积分横幅 -->
    <div class="pt-banner">
      <div class="pt-left">
        <el-icon :size="30"><GoldMedal /></el-icon>
        <div>
          <div class="pt-lb">我的可用积分</div>
          <div class="pt-num">{{ balance }}</div>
        </div>
      </div>
      <span class="pt-hint">完善档案、每日登录都能攒积分哦</span>
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
          <h4>{{ p.name }}</h4>
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
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { GoldMedal, Present } from '@element-plus/icons-vue'
import { pagePointProducts, exchangeProduct, getPointBalance } from '@/api'
const list = ref([]), total = ref(0), balance = ref(0)
const query = ref({ pageNum: 1, pageSize: 8 })
async function load() {
  const res = await pagePointProducts(query.value); list.value = res.data.records; total.value = res.data.total
  balance.value = (await getPointBalance()).data
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
.pt-left { display: flex; align-items: center; gap: 16px; }
.pt-lb { font-size: 15px; opacity: .92; }
.pt-num { font-size: 34px; font-weight: 800; line-height: 1.1; font-variant-numeric: tabular-nums; }
.pt-hint { font-size: 15px; opacity: .9; }

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

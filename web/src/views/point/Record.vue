<template>
  <el-card>
    <el-tabs v-model="tab">
      <el-tab-pane label="积分明细" name="record">
        <el-table :data="records" border>
          <el-table-column prop="createTime" label="时间" />
          <el-table-column prop="type" label="类型" />
          <el-table-column prop="description" label="描述" />
          <el-table-column label="变动"><template #default="{ row }">
            <span :style="{ color: row.changePoints >= 0 ? '#67c23a' : '#f56c6c' }">
              {{ row.changePoints >= 0 ? '+' : '' }}{{ row.changePoints }}</span>
          </template></el-table-column>
          <el-table-column prop="balance" label="余额" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="兑换记录" name="exchange">
        <el-table :data="exchanges" border>
          <el-table-column prop="createTime" label="时间" />
          <el-table-column prop="productName" label="商品" />
          <el-table-column prop="quantity" label="数量" />
          <el-table-column prop="pointsCost" label="消耗积分" />
          <el-table-column label="状态"><template #default="{ row }">
            <el-tag>{{ ['待发货','已发货','已完成','已取消'][row.status] }}</el-tag>
          </template></el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { pagePointRecords, pageMyExchanges } from '@/api'
const tab = ref('record'), records = ref([]), exchanges = ref([])
onMounted(async () => {
  records.value = (await pagePointRecords({ pageNum: 1, pageSize: 50 })).data.records
  exchanges.value = (await pageMyExchanges({ pageNum: 1, pageSize: 50 })).data.records
})
</script>

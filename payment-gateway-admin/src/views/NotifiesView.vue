<template>
  <section>
    <el-card shadow="never" class="toolbar-card">
      <el-form inline>
        <el-form-item label="网关订单号">
          <el-input v-model="filters.gatewayOrderNo" clearable />
        </el-form-item>
        <el-form-item label="商户订单号">
          <el-input v-model="filters.merchantOrderNo" clearable />
        </el-form-item>
        <el-form-item label="交易状态">
          <el-select v-model="filters.tradeStatus" clearable placeholder="全部状态" class="status-select">
            <el-option v-for="item in tradeStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="验签结果">
          <el-select v-model="filters.verified" clearable placeholder="全部" class="small-select">
            <el-option label="通过" :value="true" />
            <el-option label="失败" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理结果">
          <el-select v-model="filters.result" clearable placeholder="全部" class="status-select">
            <el-option v-for="item in notifyResultOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="更新时间">
          <el-date-picker
            v-model="filters.updatedAtRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            :default-time="defaultTime"
            class="date-range"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button :icon="RefreshLeft" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="records" border>
        <el-table-column prop="gateway_order_no" label="网关订单号" min-width="190" />
        <el-table-column prop="merchant_order_no" label="商户订单号" min-width="170" />
        <el-table-column prop="channel_trade_no" label="支付宝交易号" min-width="180" show-overflow-tooltip />
        <el-table-column label="交易状态" width="120">
          <template #default="{ row }">{{ labelOf(tradeStatusOptions, row.trade_status) }}</template>
        </el-table-column>
        <el-table-column label="验签" width="90">
          <template #default="{ row }">
            <el-tag :type="row.verified ? 'success' : 'danger'">{{ row.verified ? "通过" : "失败" }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理结果" width="120">
          <template #default="{ row }">
            <el-tag :type="resultTagType(row.result)">{{ labelOf(notifyResultOptions, row.result) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="创建时间" min-width="170" />
        <el-table-column prop="updated_at" label="更新时间" min-width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          layout="total, prev, pager, next"
          :total="page.total"
          :page-size="page.size"
          :current-page="page.current + 1"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <el-drawer v-model="detailVisible" :size="drawerSize" title="通知详情">
      <el-descriptions v-if="detail" :column="descriptionColumns" border>
        <el-descriptions-item label="网关订单号">{{ detail.gateway_order_no || "-" }}</el-descriptions-item>
        <el-descriptions-item label="商户订单号">{{ detail.merchant_order_no || "-" }}</el-descriptions-item>
        <el-descriptions-item label="支付宝交易号">{{ detail.channel_trade_no || "-" }}</el-descriptions-item>
        <el-descriptions-item label="交易状态">{{ labelOf(tradeStatusOptions, detail.trade_status) }}</el-descriptions-item>
        <el-descriptions-item label="验签结果">{{ detail.verified ? "通过" : "失败" }}</el-descriptions-item>
        <el-descriptions-item label="处理结果">{{ labelOf(notifyResultOptions, detail.result) }}</el-descriptions-item>
        <el-descriptions-item label="失败原因">{{ detail.failure_reason || "-" }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detail.updated_at }}</el-descriptions-item>
      </el-descriptions>
      <h3>通知参数</h3>
      <pre>{{ formatJson(detail?.notify_payload) }}</pre>
    </el-drawer>
  </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { RefreshLeft, Search } from "@element-plus/icons-vue";
import { fetchNotifies } from "../api/gatewayAdminApi";
import { labelOf, notifyResultOptions, resultTagType, tradeStatusOptions } from "../utils/status";

const defaultTime = [
  new Date(2000, 0, 1, 0, 0, 0),
  new Date(2000, 0, 1, 23, 59, 59)
];
const filters = reactive({
  gatewayOrderNo: "",
  merchantOrderNo: "",
  tradeStatus: "",
  verified: "",
  result: "",
  updatedAtRange: []
});
const page = reactive({ current: 0, size: 20, total: 0 });
const records = ref([]);
const loading = ref(false);
const detailVisible = ref(false);
const detail = ref(null);
const viewportWidth = ref(window.innerWidth);
const drawerSize = computed(() => (viewportWidth.value <= 760 ? "92%" : "58%"));
const descriptionColumns = computed(() => (viewportWidth.value <= 760 ? 1 : 2));

onMounted(() => {
  window.addEventListener("resize", updateViewportWidth);
  load();
});
onUnmounted(() => window.removeEventListener("resize", updateViewportWidth));

function updateViewportWidth() {
  viewportWidth.value = window.innerWidth;
}

function formatJson(value) {
  if (!value) return "-";
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

async function load() {
  loading.value = true;
  try {
    const data = await fetchNotifies({
      gatewayOrderNo: filters.gatewayOrderNo || undefined,
      merchantOrderNo: filters.merchantOrderNo || undefined,
      tradeStatus: filters.tradeStatus || undefined,
      verified: filters.verified === "" ? undefined : filters.verified,
      result: filters.result || undefined,
      updatedAtStart: filters.updatedAtRange?.[0] || undefined,
      updatedAtEnd: filters.updatedAtRange?.[1] || undefined,
      page: page.current,
      size: page.size
    });
    records.value = data.content || [];
    page.total = Number(data.totalElements || 0);
  } catch (error) {
    ElMessage.error(error.message || "加载通知记录失败");
  } finally {
    loading.value = false;
  }
}

function search() {
  page.current = 0;
  load();
}

function reset() {
  Object.assign(filters, {
    gatewayOrderNo: "",
    merchantOrderNo: "",
    tradeStatus: "",
    verified: "",
    result: "",
    updatedAtRange: []
  });
  search();
}

function handlePageChange(pageNo) {
  page.current = pageNo - 1;
  load();
}

function openDetail(row) {
  detail.value = row;
  detailVisible.value = true;
}
</script>

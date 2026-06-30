<template>
  <section>
    <el-card shadow="never" class="toolbar-card notify-toolbar">
      <el-form inline>
        <el-form-item label="订单号">
          <el-input v-model="filters.orderNo" clearable placeholder="按订单号查询" />
        </el-form-item>
        <el-form-item label="验签结果">
          <el-select v-model="filters.verified" clearable placeholder="全部" class="status-select">
            <el-option label="验签通过" value="true" />
            <el-option label="验签失败" value="false" />
          </el-select>
        </el-form-item>
        <el-form-item label="交易状态">
          <el-select v-model="filters.tradeStatus" clearable placeholder="全部状态" class="status-select">
            <el-option
              v-for="item in tradeStatuses"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
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
            class="notify-date-range"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="searchNotifies">查询</el-button>
          <el-button :icon="RefreshLeft" @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="records" border>
        <el-table-column prop="orderNo" label="订单号" min-width="210" />
        <el-table-column label="验签" width="110">
          <template #default="{ row }">
            <el-tag :type="row.verified ? 'success' : 'danger'">{{ row.verified ? "通过" : "失败" }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理结果" width="120">
          <template #default="{ row }">{{ notifyResultText(row.result) }}</template>
        </el-table-column>
        <el-table-column label="交易状态" width="160">
          <template #default="{ row }">{{ tradeStatusText(row.tradeStatus) }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column prop="updatedAt" label="更新时间" width="180" />
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看通知</el-button>
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

    <el-drawer v-model="detailVisible" :size="drawerSize" title="回调记录">
      <el-descriptions v-if="detail" :column="descriptionColumns" border>
        <el-descriptions-item label="订单号">{{ detail.orderNo || "-" }}</el-descriptions-item>
        <el-descriptions-item label="验签">{{ detail.verified ? "通过" : "失败" }}</el-descriptions-item>
        <el-descriptions-item label="处理结果">{{ notifyResultText(detail.result) }}</el-descriptions-item>
        <el-descriptions-item label="失败原因">{{ detail.failureReason || "-" }}</el-descriptions-item>
        <el-descriptions-item label="交易状态">{{ tradeStatusText(detail.tradeStatus) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detail.updatedAt || "-" }}</el-descriptions-item>
      </el-descriptions>

      <h3>通知参数</h3>
      <pre>{{ formatJson(detail?.notifyPayload) }}</pre>
    </el-drawer>
  </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { RefreshLeft, Search } from "@element-plus/icons-vue";
import { fetchNotifies } from "../api/adminApi";
import {
  notifyResultText,
  tradeStatusOptions as tradeStatuses,
  tradeStatusText
} from "../utils/status";

const defaultTime = [
  new Date(2000, 0, 1, 0, 0, 0),
  new Date(2000, 0, 1, 23, 59, 59)
];
const filters = reactive({
  orderNo: "",
  verified: "",
  tradeStatus: "",
  updatedAtRange: []
});
const page = reactive({ current: 0, size: 20, total: 0 });
const records = ref([]);
const loading = ref(false);
const detailVisible = ref(false);
const detail = ref(null);
const viewportWidth = ref(window.innerWidth);
const isMobile = computed(() => viewportWidth.value <= 760);
const drawerSize = computed(() => (isMobile.value ? "92%" : "58%"));
const descriptionColumns = computed(() => (isMobile.value ? 1 : 2));

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

async function loadNotifies() {
  loading.value = true;
  try {
    const data = await fetchNotifies({
      orderNo: filters.orderNo || undefined,
      verified: filters.verified || undefined,
      tradeStatus: filters.tradeStatus || undefined,
      updatedAtStart: filters.updatedAtRange?.[0] || undefined,
      updatedAtEnd: filters.updatedAtRange?.[1] || undefined,
      page: page.current,
      size: page.size
    });
    records.value = data.content;
    page.total = data.totalElements;
  } catch (error) {
    ElMessage.error(error.message || "加载回调记录失败");
  } finally {
    loading.value = false;
  }
}

function searchNotifies() {
  page.current = 0;
  loadNotifies();
}

function resetFilters() {
  Object.assign(filters, {
    orderNo: "",
    verified: "",
    tradeStatus: "",
    updatedAtRange: []
  });
  page.current = 0;
  loadNotifies();
}

function openDetail(row) {
  detail.value = row;
  detailVisible.value = true;
}

function handlePageChange(value) {
  page.current = value - 1;
  loadNotifies();
}

onMounted(() => {
  window.addEventListener("resize", updateViewportWidth);
  loadNotifies();
});

onUnmounted(() => {
  window.removeEventListener("resize", updateViewportWidth);
});
</script>

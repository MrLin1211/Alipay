<template>
  <section>
    <el-card shadow="never" class="toolbar-card">
      <el-form inline>
        <el-form-item label="订单号">
          <el-input v-model="filters.orderNo" clearable placeholder="按订单号查询" />
        </el-form-item>
        <el-form-item label="退款状态">
          <el-select v-model="filters.status" clearable placeholder="全部状态" class="status-select">
            <el-option
              v-for="item in refundStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="提交时间">
          <el-date-picker
            v-model="filters.createdAtRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            :default-time="defaultTime"
            class="order-date-range"
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
        <el-table-column prop="requestNo" label="退款单号" min-width="210" />
        <el-table-column prop="orderNo" label="订单号" min-width="210" />
        <el-table-column prop="refundAmount" label="退款金额" width="110" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ refundStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="refundReason" label="退款原因" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createdBy" label="操作账号" width="120" />
        <el-table-column prop="createdAt" label="提交时间" width="180" />
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看详情</el-button>
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

    <el-drawer v-model="detailVisible" :size="drawerSize" title="退款详情">
      <el-descriptions v-if="detail" :column="descriptionColumns" border>
        <el-descriptions-item label="退款单号">{{ detail.requestNo }}</el-descriptions-item>
        <el-descriptions-item label="商户订单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="平台单号">{{ detail.platTradeNo }}</el-descriptions-item>
        <el-descriptions-item label="退款金额">{{ detail.refundAmount }}</el-descriptions-item>
        <el-descriptions-item label="退款状态">{{ refundStatusText(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="平台交易状态">{{ tradeStatusText(detail.tradeStatus) }}</el-descriptions-item>
        <el-descriptions-item label="操作账号">{{ detail.createdBy }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ detail.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="退款原因" :span="descriptionColumns">
          {{ detail.refundReason }}
        </el-descriptions-item>
      </el-descriptions>

      <h3>平台原始响应</h3>
      <pre>{{ formatJson(detail?.platformResponse) }}</pre>
    </el-drawer>
  </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { RefreshLeft, Search } from "@element-plus/icons-vue";
import { fetchRefunds } from "../api/adminApi";
import {
  refundStatusOptions,
  refundStatusText,
  tradeStatusText
} from "../utils/status";

const filters = reactive({ orderNo: "", status: "", createdAtRange: [] });
const defaultTime = [
  new Date(2000, 0, 1, 0, 0, 0),
  new Date(2000, 0, 1, 23, 59, 59)
];
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

function statusType(status) {
  if (status === "SUCCESS") return "success";
  if (status === "FAILED") return "danger";
  return "warning";
}

function formatJson(value) {
  if (!value) return "-";
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

async function loadRefunds() {
  loading.value = true;
  try {
    const data = await fetchRefunds({
      orderNo: filters.orderNo || undefined,
      status: filters.status || undefined,
      createdAtStart: filters.createdAtRange?.[0] || undefined,
      createdAtEnd: filters.createdAtRange?.[1] || undefined,
      page: page.current,
      size: page.size
    });
    records.value = data.content;
    page.total = data.totalElements;
  } catch (error) {
    ElMessage.error(error.response?.data?.message || "退款记录加载失败");
  } finally {
    loading.value = false;
  }
}

function search() {
  page.current = 0;
  loadRefunds();
}

function reset() {
  Object.assign(filters, { orderNo: "", status: "", createdAtRange: [] });
  search();
}

function openDetail(row) {
  detail.value = row;
  detailVisible.value = true;
}

function handlePageChange(value) {
  page.current = value - 1;
  loadRefunds();
}

onMounted(() => {
  window.addEventListener("resize", updateViewportWidth);
  loadRefunds();
});

onUnmounted(() => {
  window.removeEventListener("resize", updateViewportWidth);
});
</script>

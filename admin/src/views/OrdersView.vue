<template>
  <section>
    <el-card shadow="never" class="toolbar-card order-toolbar">
      <el-form inline>
        <el-form-item label="订单号">
          <el-input v-model="filters.orderNo" clearable placeholder="按订单号查询" />
        </el-form-item>
        <el-form-item label="支付状态">
          <el-select v-model="filters.status" clearable placeholder="全部状态" class="status-select">
            <el-option
              v-for="item in orderStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="平台单号">
          <el-input v-model="filters.platTradeNo" clearable placeholder="按平台单号查询" />
        </el-form-item>
        <el-form-item label="第三方单号">
          <el-input v-model="filters.thirdOutTradeNo" clearable placeholder="按第三方单号查询" />
        </el-form-item>
        <el-form-item label="创建时间">
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
          <el-button type="primary" :icon="Search" @click="searchOrders">查询</el-button>
          <el-button :icon="RefreshLeft" @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="orders" border>
        <el-table-column prop="orderNo" label="订单号" min-width="210" />
        <el-table-column prop="totalAmount" label="金额" width="100" />
        <el-table-column label="状态" width="150">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ orderStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="platTradeNo" label="平台单号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="thirdOutTradeNo" label="第三方单号" min-width="190" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.orderNo)">查看详情</el-button>
            <el-button
              v-if="canRefund(row)"
              link
              type="danger"
              @click="openRefund(row)"
            >
              退款
            </el-button>
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

    <el-drawer v-model="detailVisible" :size="drawerSize" title="订单详情">
      <el-descriptions v-if="detail" :column="descriptionColumns" border>
        <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ detail.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="本地状态">{{ orderStatusText(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="交易状态">{{ tradeStatusText(detail.tradeStatus) }}</el-descriptions-item>
        <el-descriptions-item label="平台单号">{{ detail.platTradeNo || "-" }}</el-descriptions-item>
        <el-descriptions-item label="第三方单号">{{ detail.thirdOutTradeNo || "-" }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ detail.paidAt || "-" }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detail.updatedAt || "-" }}</el-descriptions-item>
      </el-descriptions>

      <h3>平台原始响应</h3>
      <pre>{{ formatJson(detail?.platformCreateResponse) }}</pre>

      <h3>支付通知内容</h3>
      <pre>{{ formatJson(detail?.notifyPayload) }}</pre>

      <h3>退款记录</h3>
      <el-table :data="detailRefunds" size="small" border>
        <el-table-column prop="requestNo" label="退款单号" min-width="190" />
        <el-table-column prop="refundAmount" label="金额" width="90" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">{{ refundStatusText(row.status) }}</template>
        </el-table-column>
        <el-table-column prop="refundReason" label="原因" min-width="140" />
        <el-table-column prop="createdAt" label="提交时间" min-width="170" />
      </el-table>
    </el-drawer>

    <el-dialog v-model="refundVisible" title="提交退款" :width="refundDialogWidth">
      <el-alert
        title="退款请求提交平台后不可在本系统撤销，请核对金额。"
        type="warning"
        show-icon
        :closable="false"
      />
      <el-descriptions v-if="refundOrder" :column="1" border class="refund-summary">
        <el-descriptions-item label="订单号">{{ refundOrder.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="订单金额">{{ refundOrder.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="已申请退款">{{ occupiedRefundAmount.toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="剩余可退">{{ refundableAmount.toFixed(2) }}</el-descriptions-item>
      </el-descriptions>
      <el-form :model="refundForm" label-position="top">
        <el-form-item label="退款金额" required>
          <el-input-number
            v-model="refundForm.refundAmount"
            :min="0.01"
            :max="refundableAmount"
            :precision="2"
            :step="0.01"
            controls-position="right"
          />
        </el-form-item>
        <el-form-item label="退款原因" required>
          <el-input
            v-model="refundForm.refundReason"
            type="textarea"
            :rows="3"
            maxlength="256"
            show-word-limit
            placeholder="请输入退款原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundVisible = false">取消</el-button>
        <el-button type="danger" :loading="refunding" @click="submitRefund">确认退款</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { RefreshLeft, Search } from "@element-plus/icons-vue";
import {
  createOrderRefund,
  fetchOrderDetail,
  fetchOrderRefunds,
  fetchOrders
} from "../api/adminApi";
import {
  orderStatusOptions,
  orderStatusText,
  refundStatusText,
  tradeStatusText
} from "../utils/status";

const defaultTime = [
  new Date(2000, 0, 1, 0, 0, 0),
  new Date(2000, 0, 1, 23, 59, 59)
];
const filters = reactive({
  orderNo: "",
  status: "",
  platTradeNo: "",
  thirdOutTradeNo: "",
  createdAtRange: []
});
const page = reactive({ current: 0, size: 20, total: 0 });
const orders = ref([]);
const loading = ref(false);
const detailVisible = ref(false);
const detail = ref(null);
const detailRefunds = ref([]);
const refundVisible = ref(false);
const refunding = ref(false);
const refundOrder = ref(null);
const refundRecords = ref([]);
const refundForm = reactive({ refundAmount: 0.01, refundReason: "" });
const viewportWidth = ref(window.innerWidth);
const isMobile = computed(() => viewportWidth.value <= 760);
const drawerSize = computed(() => (isMobile.value ? "92%" : "58%"));
const descriptionColumns = computed(() => (isMobile.value ? 1 : 2));
const refundDialogWidth = computed(() => (isMobile.value ? "calc(100% - 24px)" : "520px"));
const occupiedRefundAmount = computed(() =>
  refundRecords.value
    .filter((item) => item.status === "PROCESSING" || item.status === "SUCCESS")
    .reduce((sum, item) => sum + Number(item.refundAmount || 0), 0)
);
const refundableAmount = computed(() =>
  Math.max(0, Number(refundOrder.value?.totalAmount || 0) - occupiedRefundAmount.value)
);

function updateViewportWidth() {
  viewportWidth.value = window.innerWidth;
}

function statusType(status) {
  if (status === "SUCCESS") return "success";
  if (status === "CREATE_FAILED" || status === "FINISHED" || status === "CLOSED") return "danger";
  if (status === "CREATE_SUCCESS") return "warning";
  return "info";
}

function canRefund(order) {
  return order.status === "SUCCESS" && order.platTradeNo;
}

function formatJson(value) {
  if (!value) return "-";
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

async function loadOrders() {
  loading.value = true;
  try {
    const data = await fetchOrders({
      orderNo: filters.orderNo || undefined,
      status: filters.status || undefined,
      platTradeNo: filters.platTradeNo || undefined,
      thirdOutTradeNo: filters.thirdOutTradeNo || undefined,
      createdAtStart: filters.createdAtRange?.[0] || undefined,
      createdAtEnd: filters.createdAtRange?.[1] || undefined,
      page: page.current,
      size: page.size
    });
    orders.value = data.content;
    page.total = data.totalElements;
  } catch (error) {
    ElMessage.error(error.message || "加载订单失败");
  } finally {
    loading.value = false;
  }
}

function searchOrders() {
  page.current = 0;
  loadOrders();
}

function resetFilters() {
  Object.assign(filters, {
    orderNo: "",
    status: "",
    platTradeNo: "",
    thirdOutTradeNo: "",
    createdAtRange: []
  });
  page.current = 0;
  loadOrders();
}

async function openDetail(orderNo) {
  try {
    const [orderDetail, refunds] = await Promise.all([
      fetchOrderDetail(orderNo),
      fetchOrderRefunds(orderNo)
    ]);
    detail.value = orderDetail;
    detailRefunds.value = refunds;
    detailVisible.value = true;
  } catch (error) {
    ElMessage.error(error.message || "加载订单详情失败");
  }
}

async function openRefund(order) {
  try {
    refundOrder.value = order;
    refundRecords.value = await fetchOrderRefunds(order.orderNo);
    refundForm.refundAmount = Math.max(0.01, refundableAmount.value);
    refundForm.refundReason = "";
    if (refundableAmount.value <= 0) {
      ElMessage.warning("该订单已无剩余可退金额");
      return;
    }
    refundVisible.value = true;
  } catch (error) {
    ElMessage.error(error.response?.data?.message || "退款信息加载失败");
  }
}

async function submitRefund() {
  if (!refundForm.refundReason.trim()) {
    ElMessage.warning("请输入退款原因");
    return;
  }
  if (refundForm.refundAmount <= 0 || refundForm.refundAmount > refundableAmount.value) {
    ElMessage.warning("退款金额不合法");
    return;
  }

  try {
    await ElMessageBox.confirm(
      `确认对订单 ${refundOrder.value.orderNo} 退款 ${Number(refundForm.refundAmount).toFixed(2)} 元吗？`,
      "确认退款",
      { type: "warning", confirmButtonText: "确认退款", cancelButtonText: "取消" }
    );
  } catch {
    return;
  }

  refunding.value = true;
  try {
    const result = await createOrderRefund(refundOrder.value.orderNo, {
      refundAmount: refundForm.refundAmount,
      refundReason: refundForm.refundReason.trim()
    });
    if (result.status === "SUCCESS") {
      ElMessage.success("退款请求成功");
    } else {
      ElMessage.error("平台退款失败，请查看退款记录");
    }
    refundVisible.value = false;
    await loadOrders();
  } catch (error) {
    ElMessage.error(error.response?.data?.message || "退款提交失败");
  } finally {
    refunding.value = false;
  }
}

function handlePageChange(value) {
  page.current = value - 1;
  loadOrders();
}

onMounted(() => {
  window.addEventListener("resize", updateViewportWidth);
  loadOrders();
});

onUnmounted(() => {
  window.removeEventListener("resize", updateViewportWidth);
});
</script>

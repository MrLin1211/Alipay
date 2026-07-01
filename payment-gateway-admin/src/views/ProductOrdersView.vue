<template>
  <section>
    <el-card shadow="never" class="toolbar-card">
      <el-form inline>
        <el-form-item label="订单编号">
          <el-input v-model="filters.orderNo" clearable placeholder="按订单编号查询" style="width:180px" @keyup.enter="load" />
        </el-form-item>
        <el-form-item label="商家编号">
          <el-input v-model="filters.merchantNo" clearable placeholder="商家编号" style="width:180px" @keyup.enter="load" />
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="filters.keyword" clearable placeholder="买家手机 / 商家名称" style="width:180px" @keyup.enter="load" />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="filters.status" clearable placeholder="全部状态" class="status-select" style="width:160px">
            <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
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
            class="date-range"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="load">查询</el-button>
          <el-button :icon="RefreshLeft" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="orders" border>
        <el-table-column label="主图" width="88" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.items?.[0]?.productImage"
              class="table-product-image"
              :src="row.items[0].productImage"
              :preview-src-list="[row.items[0].productImage]"
              preview-teleported
              fit="cover"
            />
            <div v-else class="table-product-image empty">无图</div>
          </template>
        </el-table-column>
        <el-table-column prop="orderNo" label="订单编号" min-width="150" />
        <el-table-column label="商品标题" min-width="140">
          <template #default="{ row }">
            <template v-if="row.items?.length">
              <span>{{ row.items[0].productName }}</span>
              <span v-if="row.items.length > 1" style="color:#909399"> 等{{ row.items.length }}件</span>
            </template>
            <span v-else style="color:#c0c4cc">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="userDisplayName" label="买家昵称" width="110" />
        <el-table-column prop="userPhone" label="买家手机" width="130" />
        <el-table-column label="收货信息" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ shippingSummary(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="merchantName" label="商家" width="140" />
        <el-table-column prop="merchantNo" label="商家编号" width="180" />
        <el-table-column prop="totalAmount" label="金额" width="90" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="paidAt" label="支付时间" min-width="170" />
        <el-table-column prop="createdAt" label="创建时间" min-width="170" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page.current"
        v-model:page-size="page.size"
        :total="page.total"
        layout="total, prev, pager, next, sizes"
        :page-sizes="[10, 20, 50]"
        @current-change="load"
        @size-change="load"
      />
    </el-card>

    <el-drawer v-model="drawer.show" title="订单详情" :size="drawerSize" class="order-detail-drawer">
      <template v-if="drawer.order">
        <div class="order-detail-summary">
          <div class="order-detail-hero">
            <span>订单金额</span>
            <strong>¥{{ drawer.order.totalAmount }}</strong>
            <el-tag :type="statusType(drawer.order.status)">{{ statusLabel(drawer.order.status) }}</el-tag>
          </div>
          <div class="order-detail-meta">
            <div>
              <span>订单编号</span>
              <strong>{{ drawer.order.orderNo }}</strong>
            </div>
            <div>
              <span>买家</span>
              <strong>{{ drawer.order.userDisplayName || '-' }} / {{ drawer.order.userPhone || '-' }}</strong>
            </div>
            <div>
              <span>商家</span>
              <strong>{{ drawer.order.merchantName || '-' }}</strong>
            </div>
            <div>
              <span>商家编号</span>
              <strong>{{ drawer.order.merchantNo || '-' }}</strong>
            </div>
            <div>
              <span>优惠金额</span>
              <strong>¥{{ drawer.order.discountAmount }}</strong>
            </div>
            <div>
              <span>创建时间</span>
              <strong>{{ drawer.order.createdAt }}</strong>
            </div>
            <div>
              <span>支付时间</span>
              <strong>{{ drawer.order.paidAt || '-' }}</strong>
            </div>
            <div>
              <span>发货时间</span>
              <strong>{{ drawer.order.shippedAt || '-' }}</strong>
            </div>
          </div>
        </div>

        <div class="order-detail-address">
          <div>
            <span>收货信息</span>
            <strong>{{ drawer.order.shippingName || '-' }} {{ drawer.order.shippingPhone || '' }}</strong>
            <p>{{ drawer.order.shippingAddress || '-' }}</p>
          </div>
        </div>

        <h4 class="detail-section-title">商品明细</h4>
        <div class="detail-table-wrap">
          <el-table :data="drawer.order.items" size="small" border>
            <el-table-column label="主图" width="70" align="center">
              <template #default="{ row }">
                <el-image
                  v-if="row.productImage"
                  :src="row.productImage"
                  style="width:44px;height:44px;border-radius:4px"
                  fit="cover"
                  :preview-src-list="[row.productImage]"
                  preview-teleported
                />
                <span v-else style="color:#c0c4cc;font-size:12px">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="productName" label="商品标题" min-width="140" />
            <el-table-column label="SKU" width="120">
              <template #default="{ row }">
                <span>{{ row.skuName || row.skuCode || '默认规格' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="unitPrice" label="单价" width="80" />
            <el-table-column prop="quantity" label="数量" width="60" />
            <el-table-column prop="subtotal" label="小计" width="90" />
          </el-table>
        </div>

        <h4 class="detail-section-title">关联支付单</h4>
        <div class="detail-table-wrap">
          <el-table :data="drawer.order.payments" size="small" border>
            <el-table-column prop="orderNo" label="支付单号" min-width="180" />
            <el-table-column prop="status" label="状态" width="100" />
            <el-table-column prop="totalAmount" label="金额" width="80" />
            <el-table-column prop="createdAt" label="创建时间" min-width="170" />
          </el-table>
        </div>
      </template>
    </el-drawer>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { Search, RefreshLeft } from "@element-plus/icons-vue";
import { fetchProductOrders, fetchProductOrderDetail } from "../api/gatewayAdminApi";
import { ElMessage } from "element-plus";

const filters = reactive({ orderNo: "", status: "", keyword: "", merchantNo: "", createdAtRange: [] });
const defaultTime = [
  new Date(2000, 0, 1, 0, 0, 0),
  new Date(2000, 0, 1, 23, 59, 59)
];
const page = reactive({ current: 1, size: 20, total: 0 });
const orders = ref([]);
const loading = ref(false);
const viewportWidth = ref(window.innerWidth);
const isMobile = computed(() => viewportWidth.value <= 760);
const drawerSize = computed(() => (isMobile.value ? "100%" : "680px"));

function updateViewportWidth() {
  viewportWidth.value = window.innerWidth;
}

const statusOptions = [
  { label: "待支付", value: "PENDING" },
  { label: "已支付", value: "PAID" },
  { label: "已发货", value: "SHIPPED" },
  { label: "已签收", value: "DELIVERED" },
  { label: "已完成", value: "COMPLETED" },
  { label: "已取消", value: "CANCELLED" },
  { label: "退款中", value: "REFUNDING" },
  { label: "已退款", value: "REFUNDED" }
];

function statusLabel(s) {
  const opt = statusOptions.find(o => o.value === s);
  return opt ? opt.label : (s || "-");
}

function statusType(s) {
  const map = { PAID: "success", COMPLETED: "success", CANCELLED: "danger", REFUNDING: "warning", REFUNDED: "info" };
  return map[s] || "info";
}

function shippingSummary(row) {
  const parts = [row.shippingName, row.shippingPhone, row.shippingAddress].filter(Boolean);
  return parts.length ? parts.join(" / ") : "-";
}

async function load() {
  loading.value = true;
  try {
    const data = await fetchProductOrders({
      orderNo: filters.orderNo || undefined,
      status: filters.status || undefined,
      keyword: filters.keyword || undefined,
      merchantNo: filters.merchantNo || undefined,
      createdAtStart: filters.createdAtRange?.[0] || undefined,
      createdAtEnd: filters.createdAtRange?.[1] || undefined,
      page: page.current - 1,
      size: page.size
    });
    orders.value = data.content || [];
    page.total = Number(data.totalElements || 0);
  } finally {
    loading.value = false;
  }
}

function reset() {
  filters.orderNo = "";
  filters.status = "";
  filters.keyword = "";
  filters.merchantNo = "";
  filters.createdAtRange = [];
  load();
}

const drawer = reactive({ show: false, order: null });

async function openDetail(row) {
  try {
    drawer.order = await fetchProductOrderDetail(row.orderNo);
    drawer.show = true;
  } catch {
    ElMessage.error("加载订单详情失败");
  }
}

onMounted(() => window.addEventListener("resize", updateViewportWidth));
onBeforeUnmount(() => window.removeEventListener("resize", updateViewportWidth));

load();
</script>

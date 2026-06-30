<template>
  <section class="dashboard-page">
    <div class="dashboard-hero admin-dashboard-hero">
      <div>
        <span class="dashboard-kicker">Platform Console</span>
        <h2>管理后台</h2>
        <p>从平台视角管理商家、用户、商品、订单和支付配置。</p>
      </div>
      <el-button type="primary" size="large" @click="$emit('navigate', 'merchants')">管理商家</el-button>
    </div>

    <div class="metric-grid">
      <div v-for="item in metrics" :key="item.label" class="metric-card">
        <span>{{ item.label }}</span>
        <strong>{{ loading ? "..." : item.value }}</strong>
        <small>{{ item.hint }}</small>
      </div>
    </div>

    <div class="dashboard-columns">
      <el-card shadow="never" class="dashboard-card">
        <template #header>
          <strong>平台运营</strong>
        </template>
        <div class="quick-actions">
          <button type="button" @click="$emit('navigate', 'merchants')">
            <strong>商家管理</strong>
            <span>审核注册账号，维护接入应用</span>
          </button>
          <button type="button" @click="$emit('navigate', 'products')">
            <strong>商品管理</strong>
            <span>跨商家查看商品、分类、SKU</span>
          </button>
          <button type="button" @click="$emit('navigate', 'mallUsers')">
            <strong>用户运营</strong>
            <span>查看商城用户和收货地址</span>
          </button>
        </div>
      </el-card>

      <el-card shadow="never" class="dashboard-card">
        <template #header>
          <strong>支付监控</strong>
        </template>
        <div class="quick-actions">
          <button type="button" @click="$emit('navigate', 'productOrders')">
            <strong>商品订单</strong>
            <span>按商家、买家、状态追踪履约</span>
          </button>
          <button type="button" @click="$emit('navigate', 'orders')">
            <strong>支付订单</strong>
            <span>查看网关订单和支付宝响应</span>
          </button>
          <button type="button" @click="$emit('navigate', 'notifies')">
            <strong>通知记录</strong>
            <span>排查异步通知和验签结果</span>
          </button>
        </div>
      </el-card>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { fetchMallUsers, fetchMerchants, fetchOrders, fetchProductOrders, fetchProducts } from "../api/gatewayAdminApi";

defineEmits(["navigate"]);

const loading = ref(false);
const overview = ref({
  merchants: "-",
  products: "-",
  users: "-",
  orders: "-"
});

const metrics = computed(() => [
  { label: "商家", value: overview.value.merchants, hint: "账号和接入应用统一维护" },
  { label: "商品", value: overview.value.products, hint: "分类、主图、SKU 集中管理" },
  { label: "用户", value: overview.value.users, hint: "商城用户和收货地址运营" },
  { label: "订单", value: overview.value.orders, hint: "商品订单和支付订单追踪" }
]);

onMounted(loadOverview);

async function loadOverview() {
  loading.value = true;
  try {
    const [merchants, products, users, productOrders, payOrders] = await Promise.all([
      fetchMerchants({ page: 0, size: 1 }),
      fetchProducts({ page: 0, size: 1 }),
      fetchMallUsers({ page: 0, size: 1 }),
      fetchProductOrders({ page: 0, size: 1 }),
      fetchOrders({ page: 0, size: 1 })
    ]);
    overview.value = {
      merchants: String(merchants.totalElements ?? 0),
      products: String(products.totalElements ?? 0),
      users: String(users.totalElements ?? 0),
      orders: String((productOrders.totalElements ?? 0) + (payOrders.totalElements ?? 0))
    };
  } catch {
    overview.value = {
      merchants: "-",
      products: "-",
      users: "-",
      orders: "-"
    };
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <section class="dashboard-page">
    <div class="dashboard-hero">
      <div>
        <span class="dashboard-kicker">Merchant Console</span>
        <h2>{{ merchantName }}，关注今天的经营状态</h2>
        <p>商品、订单和支付配置集中在一个工作台，减少日常操作跳转。</p>
      </div>
      <el-button type="primary" size="large" @click="$emit('navigate', 'products')">管理商品</el-button>
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
          <strong>常用操作</strong>
        </template>
        <div class="quick-actions">
          <button type="button" @click="$emit('navigate', 'products')">
            <strong>新增或编辑商品</strong>
            <span>维护图片、SKU、价格和库存</span>
          </button>
          <button type="button" @click="$emit('navigate', 'productOrders')">
            <strong>处理商品订单</strong>
            <span>查看买家、收货地址和商品明细</span>
          </button>
          <button type="button" @click="$emit('navigate', 'payConfig')">
            <strong>检查支付配置</strong>
            <span>确认商家支付通道可正常下单</span>
          </button>
        </div>
      </el-card>

      <el-card shadow="never" class="dashboard-card">
        <template #header>
          <strong>运营提醒</strong>
        </template>
        <ul class="dashboard-tips">
          <li>商品列表库存建议展示 SKU 总库存，价格建议展示最低价。</li>
          <li>新增商品后先确认分类、主图和至少一个启用 SKU。</li>
          <li>支付配置变更后，建议用商城创建一笔小额订单测试链路。</li>
        </ul>
      </el-card>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { fetchMerchantProducts, fetchNotifies, fetchOrders, fetchProductOrders } from "../api/adminApi";

const props = defineProps({
  user: {
    type: Object,
    default: () => ({})
  }
});

defineEmits(["navigate"]);

const merchantName = computed(() => props.user?.merchantName || props.user?.merchantNo || "商家");

const loading = ref(false);
const overview = ref({
  products: "-",
  productOrders: "-",
  payOrders: "-",
  notifies: "-"
});

const metrics = computed(() => [
  { label: "商品总数", value: overview.value.products, hint: "当前商家维护的商品数量" },
  { label: "商品订单", value: overview.value.productOrders, hint: "最近商品订单总量" },
  { label: "支付订单", value: overview.value.payOrders, hint: "网关侧支付订单数量" },
  { label: "通知记录", value: overview.value.notifies, hint: "支付通知和验签记录" }
]);

onMounted(loadOverview);

async function loadOverview() {
  loading.value = true;
  try {
    const [products, productOrders, payOrders, notifies] = await Promise.all([
      fetchMerchantProducts({ page: 0, size: 1 }),
      fetchProductOrders({ page: 0, size: 1 }),
      fetchOrders({ page: 0, size: 1 }),
      fetchNotifies({ page: 0, size: 1 })
    ]);
    overview.value = {
      products: String(products.totalElements ?? 0),
      productOrders: String(productOrders.totalElements ?? 0),
      payOrders: String(payOrders.totalElements ?? 0),
      notifies: String(notifies.totalElements ?? 0)
    };
  } catch {
    overview.value = {
      products: "-",
      productOrders: "-",
      payOrders: "-",
      notifies: "-"
    };
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <LoginView v-if="!currentUser" @logged-in="handleLoggedIn" />
  <el-container v-else class="admin-layout">
    <el-aside width="220px" class="admin-aside">
      <div class="brand">商家后台</div>
      <el-menu :default-active="activeView" class="admin-menu" @select="activeView = $event">
        <el-menu-item index="home">
          <el-icon><DataBoard /></el-icon>
          <span>首页</span>
        </el-menu-item>
        <el-sub-menu index="goods">
          <template #title>
            <el-icon><Shop /></el-icon>
            <span>商品运营</span>
          </template>
          <el-menu-item index="products">
            <el-icon><Goods /></el-icon>
            <span>商品管理</span>
          </el-menu-item>
          <el-menu-item index="productOrders">
            <el-icon><Tickets /></el-icon>
            <span>商品订单</span>
          </el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="payment">
          <template #title>
            <el-icon><Wallet /></el-icon>
            <span>支付中心</span>
          </template>
          <el-menu-item index="orders">
            <el-icon><Tickets /></el-icon>
            <span>支付订单</span>
          </el-menu-item>
          <el-menu-item index="notifies">
            <el-icon><Bell /></el-icon>
            <span>通知记录</span>
          </el-menu-item>
          <el-menu-item index="payConfig">
            <el-icon><Wallet /></el-icon>
            <span>支付配置</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container class="admin-content">
      <div class="mobile-topbar">
        <div class="mobile-brand">商家后台</div>
        <el-dropdown>
          <el-button size="small">
            {{ displayUser }}
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="handleLogout">
                <el-icon><SwitchButton /></el-icon>
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <div class="mobile-tabs">
        <button
          v-for="item in navItems"
          :key="item.index"
          :class="{ active: activeView === item.index }"
          type="button"
          @click="activeView = item.index"
        >
          {{ item.label }}
        </button>
      </div>

      <el-header class="admin-header" height="88px">
        <div>
          <h1>{{ pageMeta.title }}</h1>
          <p>{{ pageMeta.desc }}</p>
        </div>
        <div class="header-actions">
          <el-dropdown>
            <el-button>
              {{ displayUser }}
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  {{ currentUser.merchantName || currentUser.merchantNo }}
                </el-dropdown-item>
                <el-dropdown-item @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="admin-main">
        <DashboardView v-if="activeView === 'home'" :user="currentUser" @navigate="activeView = $event" />
        <MerchantProductsView v-if="activeView === 'products'" />
        <PayConfigView v-if="activeView === 'payConfig'" />
        <OrdersView v-if="activeView === 'orders'" />
        <ProductOrdersView v-if="activeView === 'productOrders'" />
        <NotifyRecordsView v-if="activeView === 'notifies'" />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { ArrowDown, Bell, DataBoard, Goods, Shop, SwitchButton, Tickets, Wallet } from "@element-plus/icons-vue";
import { clearMerchantSession, getMerchantUser } from "./api/client";
import { fetchCurrentAdmin, logoutAdmin } from "./api/adminApi";
import LoginView from "./views/LoginView.vue";
import DashboardView from "./views/DashboardView.vue";
import MerchantProductsView from "./views/MerchantProductsView.vue";
import PayConfigView from "./views/PayConfigView.vue";
import OrdersView from "./views/OrdersView.vue";
import ProductOrdersView from "./views/ProductOrdersView.vue";
import NotifyRecordsView from "./views/NotifyRecordsView.vue";

const activeView = ref("home");
const currentUser = ref(getMerchantUser());
const navItems = [
  { index: "home", label: "首页" },
  { index: "products", label: "商品" },
  { index: "orders", label: "订单" },
  { index: "productOrders", label: "商品订单" },
  { index: "notifies", label: "通知" },
  { index: "payConfig", label: "配置" }
];

const meta = {
  home: {
    title: "商家工作台",
    desc: "关注今日订单、商品状态和支付配置，快速进入常用运营功能"
  },
  products: {
    title: "商品管理",
    desc: "维护当前商家的商品资料、库存、价格和上下架状态"
  },
  payConfig: {
    title: "支付配置",
    desc: "配置当前商家的支付通道，商城下单会按这里的配置发起支付"
  },
  orders: {
    title: "支付订单",
    desc: "查看当前商家的支付订单、支付状态和平台响应"
  },
  productOrders: {
    title: "商品订单",
    desc: "管理当前商家的商品订单，查看明细、变更发货/签收状态"
  },
  notifies: {
    title: "通知记录",
    desc: "查看当前商家的支付通知、验签结果和处理记录"
  }
};

const pageMeta = computed(() => meta[activeView.value]);
const displayUser = computed(() => currentUser.value?.displayName || currentUser.value?.username || "商家账号");

onMounted(async () => {
  window.addEventListener("merchant-session-expired", handleSessionExpired);
  if (currentUser.value) {
    try {
      currentUser.value = await fetchCurrentAdmin();
    } catch {
      handleSessionExpired();
    }
  }
});

onUnmounted(() => {
  window.removeEventListener("merchant-session-expired", handleSessionExpired);
});

function handleLoggedIn(user) {
  currentUser.value = user;
}

async function handleLogout() {
  try {
    await logoutAdmin();
  } finally {
    clearMerchantSession();
    currentUser.value = null;
    activeView.value = "home";
  }
}

function handleSessionExpired() {
  clearMerchantSession();
  currentUser.value = null;
  ElMessage.warning("登录已失效，请重新登录");
}
</script>

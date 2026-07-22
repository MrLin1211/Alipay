<template>
  <LoginView v-if="!currentUser" @logged-in="handleLoggedIn" />
  <el-container v-else class="admin-layout">
    <el-aside width="220px" class="admin-aside">
      <div class="brand">商家后台</div>
      <el-menu :default-active="route.path" class="admin-menu" router>
        <el-menu-item index="/dashboard">
          <el-icon><DataBoard /></el-icon>
          <span>首页</span>
        </el-menu-item>
        <el-sub-menu index="goods">
          <template #title>
            <el-icon><Shop /></el-icon>
            <span>商品运营</span>
          </template>
          <el-menu-item index="/goods/products">
            <el-icon><Goods /></el-icon>
            <span>商品管理</span>
          </el-menu-item>
          <el-menu-item index="/goods/product-orders">
            <el-icon><Tickets /></el-icon>
            <span>商品订单</span>
          </el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="payment">
          <template #title>
            <el-icon><Wallet /></el-icon>
            <span>支付中心</span>
          </template>
          <el-menu-item index="/payment/orders">
            <el-icon><Tickets /></el-icon>
            <span>支付订单</span>
          </el-menu-item>
          <el-menu-item index="/payment/notifications">
            <el-icon><Bell /></el-icon>
            <span>通知记录</span>
          </el-menu-item>
          <el-menu-item index="/payment/config">
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
          :key="item.path"
          :class="{ active: route.path === item.path }"
          type="button"
          @click="router.push(item.path)"
        >
          {{ item.label }}
        </button>
      </div>

      <el-header class="admin-header" height="88px">
        <div>
          <h1>{{ route.meta.title }}</h1>
          <p>{{ route.meta.desc }}</p>
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
        <router-view v-slot="{ Component }">
          <component
            :is="Component"
            v-if="route.name === 'dashboard'"
            :user="currentUser"
            @navigate="handleDashboardNavigate"
          />
          <component :is="Component" v-else />
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { ArrowDown, Bell, DataBoard, Goods, Shop, SwitchButton, Tickets, Wallet } from "@element-plus/icons-vue";
import { clearMerchantSession, getMerchantUser } from "./api/client";
import { fetchCurrentAdmin, logoutAdmin } from "./api/adminApi";
import LoginView from "./views/LoginView.vue";

const currentUser = ref(getMerchantUser());
const route = useRoute();
const router = useRouter();
const navItems = [
  { path: "/dashboard", label: "首页" },
  { path: "/goods/products", label: "商品" },
  { path: "/payment/orders", label: "支付订单" },
  { path: "/goods/product-orders", label: "商品订单" },
  { path: "/payment/notifications", label: "通知" },
  { path: "/payment/config", label: "配置" }
];
const dashboardPaths = {
  products: "/goods/products",
  productOrders: "/goods/product-orders",
  payConfig: "/payment/config"
};

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

function handleDashboardNavigate(target) {
  const path = dashboardPaths[target];
  if (path) router.push(path);
}

async function handleLogout() {
  try {
    await logoutAdmin();
  } finally {
    clearMerchantSession();
    currentUser.value = null;
    router.replace("/dashboard");
  }
}

function handleSessionExpired() {
  clearMerchantSession();
  currentUser.value = null;
  ElMessage.warning("登录已失效，请重新登录");
}
</script>

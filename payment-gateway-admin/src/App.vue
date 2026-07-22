<template>
  <LoginView v-if="!currentUser" @logged-in="handleLoggedIn" />
  <el-container v-else class="admin-layout">
    <el-aside width="220px" class="admin-aside">
      <div class="brand">管理后台</div>
      <el-menu :default-active="route.path" class="admin-menu" router>
        <el-menu-item index="/dashboard">
          <el-icon><DataBoard /></el-icon>
          <span>首页</span>
        </el-menu-item>
        <el-sub-menu index="merchant">
          <template #title>
            <el-icon><Shop /></el-icon>
            <span>商家运营</span>
          </template>
          <el-menu-item index="/merchant/merchants">
            <el-icon><User /></el-icon>
            <span>商家管理</span>
          </el-menu-item>
          <el-menu-item index="/merchant/products">
            <el-icon><Goods /></el-icon>
            <span>商品管理</span>
          </el-menu-item>
          <el-menu-item index="/merchant/categories">
            <el-icon><Collection /></el-icon>
            <span>商品分类</span>
          </el-menu-item>
          <el-menu-item index="/merchant/product-orders">
            <el-icon><Tickets /></el-icon>
            <span>商品订单</span>
          </el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="user">
          <template #title>
            <el-icon><UserFilled /></el-icon>
            <span>用户运营</span>
          </template>
          <el-menu-item index="/users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/addresses">
            <el-icon><Location /></el-icon>
            <span>收货地址</span>
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
          <el-menu-item index="/payment/apps">
            <el-icon><Connection /></el-icon>
            <span>接入应用</span>
          </el-menu-item>
          <el-menu-item index="/payment/alipay-config">
            <el-icon><Setting /></el-icon>
            <span>支付宝配置</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container class="admin-content">
      <div class="mobile-topbar">
        <strong>管理后台</strong>
        <el-button size="small" @click="handleLogout">退出</el-button>
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
              {{ currentUser.display_name || currentUser.username }}
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
      </el-header>

      <el-main class="admin-main">
        <router-view v-slot="{ Component }">
          <component
            :is="Component"
            v-if="route.name === 'dashboard'"
            @navigate="handleDashboardNavigate"
          />
          <component :is="Component" v-else />
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { ArrowDown, Bell, Collection, Connection, DataBoard, Goods, Location, Setting, Shop, SwitchButton, Tickets, User, UserFilled, Wallet } from "@element-plus/icons-vue";
import { clearAdminSession, getAdminUser } from "./api/client";
import LoginView from "./views/LoginView.vue";

const currentUser = ref(getAdminUser());
const route = useRoute();
const router = useRouter();
const navItems = [
  { path: "/dashboard", label: "首页" },
  { path: "/merchant/merchants", label: "商家" },
  { path: "/users", label: "用户" },
  { path: "/addresses", label: "地址" },
  { path: "/merchant/products", label: "商品" },
  { path: "/merchant/categories", label: "分类" },
  { path: "/payment/orders", label: "支付订单" },
  { path: "/merchant/product-orders", label: "商品订单" },
  { path: "/payment/notifications", label: "通知" },
  { path: "/payment/apps", label: "应用" },
  { path: "/payment/alipay-config", label: "配置" }
];
const dashboardPaths = {
  merchants: "/merchant/merchants",
  products: "/merchant/products",
  mallUsers: "/users",
  productOrders: "/merchant/product-orders",
  orders: "/payment/orders",
  notifies: "/payment/notifications"
};

onMounted(() => {
  window.addEventListener("gateway-admin-session-expired", handleSessionExpired);
});

onUnmounted(() => {
  window.removeEventListener("gateway-admin-session-expired", handleSessionExpired);
});

function handleLoggedIn(user) {
  currentUser.value = user;
}

function handleDashboardNavigate(target) {
  const path = dashboardPaths[target];
  if (path) router.push(path);
}

function handleLogout() {
  clearAdminSession();
  currentUser.value = null;
}

function handleSessionExpired() {
  clearAdminSession();
  currentUser.value = null;
  ElMessage.warning("登录已失效，请重新登录");
}
</script>

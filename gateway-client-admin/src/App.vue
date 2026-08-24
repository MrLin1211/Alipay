<template>
  <LoginView v-if="!currentUser" @logged-in="handleLoggedIn" />
  <el-container v-else class="admin-layout">
    <el-aside width="220px" class="admin-aside">
      <div class="brand">接入方支付后台</div>
      <el-menu :default-active="route.path" class="admin-menu" router>
        <el-menu-item index="/payment/orders">
          <el-icon><Tickets /></el-icon>
          <span>支付订单</span>
        </el-menu-item>
        <el-menu-item index="/payment/refunds">
          <el-icon><Refresh /></el-icon>
          <span>退款记录</span>
        </el-menu-item>
        <el-menu-item index="/payment/notifications">
          <el-icon><Bell /></el-icon>
          <span>通知记录</span>
        </el-menu-item>
        <el-menu-item index="/payment/app-config">
          <el-icon><Connection /></el-icon>
          <span>接入配置</span>
        </el-menu-item>
        <el-menu-item index="/payment/openapi">
          <el-icon><Document /></el-icon>
          <span>OpenAPI 文档</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container class="admin-content">
      <div class="mobile-topbar">
        <strong>接入方支付后台</strong>
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
              {{ currentUser.clientName || currentUser.displayName || currentUser.username }}
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>{{ currentUser.appId }}</el-dropdown-item>
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
          <component :is="Component" />
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { ArrowDown, Bell, Connection, Document, Refresh, SwitchButton, Tickets } from "@element-plus/icons-vue";
import { clearAdminSession, getAdminUser } from "./api/client";
import LoginView from "./views/LoginView.vue";

const currentUser = ref(getAdminUser());
const route = useRoute();
const router = useRouter();
const navItems = [
  { path: "/payment/orders", label: "订单" },
  { path: "/payment/refunds", label: "退款" },
  { path: "/payment/notifications", label: "通知" },
  { path: "/payment/app-config", label: "配置" },
  { path: "/payment/openapi", label: "文档" }
];

onMounted(() => {
  window.addEventListener("gateway-client-session-expired", handleSessionExpired);
});

onUnmounted(() => {
  window.removeEventListener("gateway-client-session-expired", handleSessionExpired);
});

function handleLoggedIn(user) {
  currentUser.value = user;
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

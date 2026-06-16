<template>
  <LoginView v-if="!currentUser" @logged-in="handleLoggedIn" />
  <el-container v-else class="admin-layout">
    <el-aside width="220px" class="admin-aside">
      <div class="brand">支付网关</div>
      <el-menu :default-active="activeView" class="admin-menu" @select="activeView = $event">
        <el-menu-item index="config">
          <el-icon><Setting /></el-icon>
          <span>支付宝配置</span>
        </el-menu-item>
        <el-menu-item index="apps">
          <el-icon><Connection /></el-icon>
          <span>接入应用</span>
        </el-menu-item>
        <el-menu-item index="orders">
          <el-icon><Tickets /></el-icon>
          <span>支付订单</span>
        </el-menu-item>
        <el-menu-item index="notifies">
          <el-icon><Bell /></el-icon>
          <span>通知记录</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container class="admin-content">
      <div class="mobile-topbar">
        <strong>支付网关</strong>
        <el-button size="small" @click="handleLogout">退出</el-button>
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
          <el-form-item label="网关地址" class="backend-url">
            <el-input v-model="gatewayUrl" @change="applyGatewayUrl" />
          </el-form-item>
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
        <AlipayConfigView v-if="activeView === 'config'" />
        <AppsView v-if="activeView === 'apps'" />
        <OrdersView v-if="activeView === 'orders'" />
        <NotifiesView v-if="activeView === 'notifies'" />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { ArrowDown, Bell, Connection, Setting, SwitchButton, Tickets } from "@element-plus/icons-vue";
import { clearAdminSession, gatewayBaseUrl, getAdminUser, setGatewayBaseUrl } from "./api/client";
import LoginView from "./views/LoginView.vue";
import AlipayConfigView from "./views/AlipayConfigView.vue";
import AppsView from "./views/AppsView.vue";
import OrdersView from "./views/OrdersView.vue";
import NotifiesView from "./views/NotifiesView.vue";

const currentUser = ref(getAdminUser());
const activeView = ref("config");
const gatewayUrl = ref(gatewayBaseUrl.value);
const navItems = [
  { index: "config", label: "配置" },
  { index: "apps", label: "应用" },
  { index: "orders", label: "订单" },
  { index: "notifies", label: "通知" }
];
const meta = {
  config: { title: "支付宝配置", desc: "维护支付宝 AppId、网关、公钥、私钥和回调地址" },
  apps: { title: "接入应用", desc: "管理调用支付网关的业务系统账号和回调地址" },
  orders: { title: "支付订单", desc: "查看网关侧订单、渠道响应和支付通知内容" },
  notifies: { title: "通知记录", desc: "查看支付宝异步通知、验签结果和处理结果" }
};
const pageMeta = computed(() => meta[activeView.value]);

onMounted(() => {
  window.addEventListener("gateway-admin-session-expired", handleSessionExpired);
});

onUnmounted(() => {
  window.removeEventListener("gateway-admin-session-expired", handleSessionExpired);
});

function applyGatewayUrl() {
  setGatewayBaseUrl(gatewayUrl.value);
  gatewayUrl.value = gatewayBaseUrl.value;
}

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

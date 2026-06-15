<template>
  <LoginView v-if="!currentUser" @logged-in="handleLoggedIn" />
  <el-container v-else class="admin-layout">
    <el-aside width="220px" class="admin-aside">
      <div class="brand">支付后台</div>
      <el-menu :default-active="activeView" class="admin-menu" @select="activeView = $event">
        <el-menu-item index="orders">
          <el-icon><Tickets /></el-icon>
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="config">
          <el-icon><Setting /></el-icon>
          <span>支付配置</span>
        </el-menu-item>
        <el-menu-item index="notifies">
          <el-icon><Bell /></el-icon>
          <span>回调记录</span>
        </el-menu-item>
        <el-menu-item index="refunds">
          <el-icon><RefreshLeft /></el-icon>
          <span>退款记录</span>
        </el-menu-item>
        <el-menu-item index="users">
          <el-icon><User /></el-icon>
          <span>账号管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container class="admin-content">
      <div class="mobile-topbar">
        <div class="mobile-brand">支付后台</div>
        <el-dropdown>
          <el-button size="small">
            {{ currentUser.displayName || currentUser.username }}
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
          <el-form-item label="后端地址" class="backend-url">
            <el-input v-model="backendUrl" @change="applyBackendUrl" />
          </el-form-item>
          <el-dropdown>
            <el-button>
              {{ currentUser.displayName || currentUser.username }}
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
        <OrdersView v-if="activeView === 'orders'" />
        <PayConfigView v-if="activeView === 'config'" />
        <NotifyRecordsView v-if="activeView === 'notifies'" />
        <RefundRecordsView v-if="activeView === 'refunds'" />
        <AdminUsersView v-if="activeView === 'users'" />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { ArrowDown, Bell, RefreshLeft, Setting, SwitchButton, Tickets, User } from "@element-plus/icons-vue";
import { backendBaseUrl, clearAdminSession, getAdminUser, setBackendBaseUrl } from "./api/client";
import { fetchCurrentAdmin, logoutAdmin } from "./api/adminApi";
import OrdersView from "./views/OrdersView.vue";
import PayConfigView from "./views/PayConfigView.vue";
import NotifyRecordsView from "./views/NotifyRecordsView.vue";
import RefundRecordsView from "./views/RefundRecordsView.vue";
import AdminUsersView from "./views/AdminUsersView.vue";
import LoginView from "./views/LoginView.vue";

const activeView = ref("orders");
const backendUrl = ref(backendBaseUrl.value);
const currentUser = ref(getAdminUser());
const navItems = [
  { index: "orders", label: "订单" },
  { index: "config", label: "配置" },
  { index: "notifies", label: "回调" },
  { index: "refunds", label: "退款" },
  { index: "users", label: "账号" }
];

const meta = {
  orders: {
    title: "订单管理",
    desc: "查看订单状态、平台响应和支付通知内容"
  },
  config: {
    title: "支付配置",
    desc: "查看当前后端正在使用的支付配置"
  },
  notifies: {
    title: "回调记录",
    desc: "查看平台通知参数、验签结果和处理时间"
  },
  refunds: {
    title: "退款记录",
    desc: "查看全部退款请求、处理状态和平台原始响应"
  },
  users: {
    title: "账号管理",
    desc: "新增账号、修改密码、启用或禁用后台管理员"
  }
};

const pageMeta = computed(() => meta[activeView.value]);

onMounted(async () => {
  window.addEventListener("admin-session-expired", handleSessionExpired);
  if (currentUser.value) {
    try {
      currentUser.value = await fetchCurrentAdmin();
    } catch {
      handleSessionExpired();
    }
  }
});

onUnmounted(() => {
  window.removeEventListener("admin-session-expired", handleSessionExpired);
});

function applyBackendUrl() {
  setBackendBaseUrl(backendUrl.value);
  backendUrl.value = backendBaseUrl.value;
}

function handleLoggedIn(user) {
  currentUser.value = user;
}

async function handleLogout() {
  try {
    await logoutAdmin();
  } finally {
    clearAdminSession();
    currentUser.value = null;
    activeView.value = "orders";
  }
}

function handleSessionExpired() {
  clearAdminSession();
  currentUser.value = null;
  ElMessage.warning("登录已失效，请重新登录");
}
</script>

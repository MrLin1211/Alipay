<template>
  <LoginView v-if="!currentUser" @logged-in="handleLoggedIn" />
  <el-container v-else class="admin-layout">
    <el-aside width="220px" class="admin-aside">
      <div class="brand">管理后台</div>
      <el-menu :default-active="activeView" class="admin-menu" @select="activeView = $event">
        <el-menu-item index="home">
          <el-icon><DataBoard /></el-icon>
          <span>首页</span>
        </el-menu-item>
        <el-sub-menu index="merchant">
          <template #title>
            <el-icon><Shop /></el-icon>
            <span>商家运营</span>
          </template>
          <el-menu-item index="merchants">
            <el-icon><User /></el-icon>
            <span>商家管理</span>
          </el-menu-item>
          <el-menu-item index="products">
            <el-icon><Goods /></el-icon>
            <span>商品管理</span>
          </el-menu-item>
          <el-menu-item index="categories">
            <el-icon><Collection /></el-icon>
            <span>商品分类</span>
          </el-menu-item>
          <el-menu-item index="productOrders">
            <el-icon><Tickets /></el-icon>
            <span>商品订单</span>
          </el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="user">
          <template #title>
            <el-icon><UserFilled /></el-icon>
            <span>用户运营</span>
          </template>
          <el-menu-item index="mallUsers">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="addresses">
            <el-icon><Location /></el-icon>
            <span>收货地址</span>
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
          <el-menu-item index="apps">
            <el-icon><Connection /></el-icon>
            <span>接入应用</span>
          </el-menu-item>
          <el-menu-item index="config">
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
        <HomeView v-if="activeView === 'home'" @navigate="activeView = $event" />
        <AlipayConfigView v-if="activeView === 'config'" />
        <AppsView v-if="activeView === 'apps'" />
        <MerchantsView v-if="activeView === 'merchants'" />
        <MallUsersView v-if="activeView === 'mallUsers'" />
        <AddressesView v-if="activeView === 'addresses'" />
        <ProductsView v-if="activeView === 'products'" />
        <CategoriesView v-if="activeView === 'categories'" />
        <OrdersView v-if="activeView === 'orders'" />
        <ProductOrdersView v-if="activeView === 'productOrders'" />
        <NotifiesView v-if="activeView === 'notifies'" />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { ArrowDown, Bell, Collection, Connection, DataBoard, Goods, Location, Setting, Shop, SwitchButton, Tickets, User, UserFilled, Wallet } from "@element-plus/icons-vue";
import { clearAdminSession, getAdminUser } from "./api/client";
import LoginView from "./views/LoginView.vue";
import HomeView from "./views/HomeView.vue";
import AlipayConfigView from "./views/AlipayConfigView.vue";
import AppsView from "./views/AppsView.vue";
import MerchantsView from "./views/MerchantsView.vue";
import MallUsersView from "./views/MallUsersView.vue";
import AddressesView from "./views/AddressesView.vue";
import ProductsView from "./views/ProductsView.vue";
import CategoriesView from "./views/CategoriesView.vue";
import OrdersView from "./views/OrdersView.vue";
import ProductOrdersView from "./views/ProductOrdersView.vue";
import NotifiesView from "./views/NotifiesView.vue";

const currentUser = ref(getAdminUser());
const activeView = ref("home");
const navItems = [
  { index: "home", label: "首页" },
  { index: "merchants", label: "商家" },
  { index: "mallUsers", label: "用户" },
  { index: "addresses", label: "地址" },
  { index: "products", label: "商品" },
  { index: "categories", label: "分类" },
  { index: "orders", label: "订单" },
  { index: "productOrders", label: "商品订单" },
  { index: "notifies", label: "通知" },
  { index: "apps", label: "应用" },
  { index: "config", label: "配置" }
];
const meta = {
  home: { title: "平台总览", desc: "集中查看商家、商品、用户、订单和支付链路的运营入口" },
  merchants: { title: "商家管理", desc: "统一管理商家注册账号，并维护商家的支付网关接入应用" },
  mallUsers: { title: "用户管理", desc: "展示和管理商城注册用户，支持按手机号、昵称和状态筛选" },
  addresses: { title: "收货地址", desc: "统一管理商城用户收货地址，支持查询、编辑、删除和设置默认地址" },
  products: { title: "商品管理", desc: "管理商家商品，支持新增、编辑、上下架和按商家筛选" },
  categories: { title: "商品分类", desc: "维护商城统一商品分类，管理后台和商家后台新增商品共用同一套分类" },
  config: { title: "支付宝配置", desc: "维护支付宝 AppId、网关、公钥、私钥和回调地址" },
  apps: { title: "接入应用", desc: "管理调用支付网关的业务系统账号和回调地址" },
  orders: { title: "支付订单", desc: "查看网关侧订单、支付宝请求参数和支付通知内容" },
  productOrders: { title: "商品订单", desc: "全平台商品订单，支持按商家、买家、状态筛选和查看明细，与支付订单关联追踪" },
  notifies: { title: "通知记录", desc: "查看支付宝异步通知、验签结果和处理结果" }
};
const pageMeta = computed(() => meta[activeView.value]);

onMounted(() => {
  window.addEventListener("gateway-admin-session-expired", handleSessionExpired);
});

onUnmounted(() => {
  window.removeEventListener("gateway-admin-session-expired", handleSessionExpired);
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

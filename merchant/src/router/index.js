import { createRouter, createWebHistory } from "vue-router";

const routes = [
  {
    path: "/",
    redirect: "/dashboard"
  },
  {
    path: "/dashboard",
    name: "dashboard",
    component: () => import("../views/DashboardView.vue"),
    meta: { title: "商家工作台", desc: "关注今日订单、商品状态和支付配置，快速进入常用运营功能" }
  },
  {
    path: "/goods/products",
    name: "products",
    component: () => import("../views/MerchantProductsView.vue"),
    meta: { title: "商品管理", desc: "维护当前商家的商品资料、库存、价格和上下架状态" }
  },
  {
    path: "/goods/product-orders",
    name: "product-orders",
    component: () => import("../views/ProductOrdersView.vue"),
    meta: { title: "商品订单", desc: "管理当前商家的商品订单，查看明细、变更发货和签收状态" }
  },
  {
    path: "/payment/orders",
    name: "payment-orders",
    component: () => import("../views/OrdersView.vue"),
    meta: { title: "支付订单", desc: "查看当前商家的支付订单、支付状态和平台响应" }
  },
  {
    path: "/payment/notifications",
    name: "payment-notifications",
    component: () => import("../views/NotifyRecordsView.vue"),
    meta: { title: "通知记录", desc: "查看当前商家的支付通知、验签结果和处理记录" }
  },
  {
    path: "/payment/config",
    name: "payment-config",
    component: () => import("../views/PayConfigView.vue"),
    meta: { title: "支付配置", desc: "配置当前商家的支付通道，商城下单会按这里的配置发起支付" }
  },
  {
    path: "/:pathMatch(.*)*",
    redirect: "/dashboard"
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
});

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - 商家后台` : "商家后台";
});

export default router;

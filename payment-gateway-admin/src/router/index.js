import { createRouter, createWebHistory } from "vue-router";

const routes = [
  {
    path: "/",
    redirect: "/payment/orders"
  },
  {
    path: "/payment/orders",
    name: "payment-orders",
    component: () => import("../views/OrdersView.vue"),
    meta: { title: "支付订单", desc: "查看网关侧订单、平台请求响应和支付通知内容" }
  },
  {
    path: "/payment/notifications",
    name: "payment-notifications",
    component: () => import("../views/NotifiesView.vue"),
    meta: { title: "通知记录", desc: "查看平台异步通知、验签结果和处理结果" }
  },
  {
    path: "/payment/refunds",
    name: "payment-refunds",
    component: () => import("../views/RefundsView.vue"),
    meta: { title: "退款记录", desc: "查看全平台退款流水、退款请求参数和平台退款响应" }
  },
  {
    path: "/payment/apps",
    name: "payment-apps",
    component: () => import("../views/AppsView.vue"),
    meta: { title: "接入应用", desc: "管理调用支付网关的业务系统账号和回调地址" }
  },
  {
    path: "/payment/alipay-config",
    name: "channel-config",
    component: () => import("../views/AlipayConfigView.vue"),
    meta: { title: "通道配置", desc: "维护珍宝阁支付平台网关、商户号、密钥和回调地址" }
  },
  {
    path: "/:pathMatch(.*)*",
    redirect: "/payment/orders"
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
});

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - 支付网关后台` : "支付网关后台";
});

export default router;

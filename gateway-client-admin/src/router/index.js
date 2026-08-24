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
    meta: { title: "支付订单", desc: "查看本应用发起的支付订单、平台请求响应和通知内容" }
  },
  {
    path: "/payment/refunds",
    name: "payment-refunds",
    component: () => import("../views/RefundsView.vue"),
    meta: { title: "退款记录", desc: "查看本应用提交的退款请求和平台退款响应" }
  },
  {
    path: "/payment/notifications",
    name: "payment-notifications",
    component: () => import("../views/NotifiesView.vue"),
    meta: { title: "通知记录", desc: "查看本应用订单相关的平台通知、验签结果和处理结果" }
  },
  {
    path: "/payment/app-config",
    name: "app-config",
    component: () => import("../views/AppConfigView.vue"),
    meta: { title: "接入配置", desc: "查看 AppId，维护业务通知地址和 IP 白名单" }
  },
  {
    path: "/payment/openapi",
    name: "openapi-docs",
    component: () => import("../views/OpenApiDocsView.vue"),
    meta: { title: "OpenAPI 文档", desc: "第三方后端调用支付网关接口的签名规则和请求示例" }
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
  document.title = to.meta.title ? `${to.meta.title} - 接入方支付后台` : "接入方支付后台";
});

export default router;

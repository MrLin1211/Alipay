import { createRouter, createWebHistory } from "vue-router";

const routes = [
  {
    path: "/",
    redirect: "/dashboard"
  },
  {
    path: "/dashboard",
    name: "dashboard",
    component: () => import("../views/HomeView.vue"),
    meta: { title: "平台总览", desc: "集中查看商家、商品、用户、订单和支付链路的运营入口" }
  },
  {
    path: "/merchant/merchants",
    name: "merchants",
    component: () => import("../views/MerchantsView.vue"),
    meta: { title: "商家管理", desc: "统一管理商家注册账号，并维护商家的支付网关接入应用" }
  },
  {
    path: "/merchant/products",
    name: "products",
    component: () => import("../views/ProductsView.vue"),
    meta: { title: "商品管理", desc: "管理商家商品，支持新增、编辑、上下架和按商家筛选" }
  },
  {
    path: "/merchant/categories",
    name: "categories",
    component: () => import("../views/CategoriesView.vue"),
    meta: { title: "商品分类", desc: "维护商城统一商品分类，管理后台和商家后台新增商品共用同一套分类" }
  },
  {
    path: "/merchant/product-orders",
    name: "product-orders",
    component: () => import("../views/ProductOrdersView.vue"),
    meta: { title: "商品订单", desc: "全平台商品订单，支持按商家、买家、状态筛选和查看明细，与支付订单关联追踪" }
  },
  {
    path: "/users",
    name: "users",
    component: () => import("../views/MallUsersView.vue"),
    meta: { title: "用户管理", desc: "展示和管理商城注册用户，支持按手机号、昵称和状态筛选" }
  },
  {
    path: "/addresses",
    name: "addresses",
    component: () => import("../views/AddressesView.vue"),
    meta: { title: "收货地址", desc: "统一管理商城用户收货地址，支持查询、编辑、删除和设置默认地址" }
  },
  {
    path: "/payment/orders",
    name: "payment-orders",
    component: () => import("../views/OrdersView.vue"),
    meta: { title: "支付订单", desc: "查看网关侧订单、支付宝请求参数和支付通知内容" }
  },
  {
    path: "/payment/notifications",
    name: "payment-notifications",
    component: () => import("../views/NotifiesView.vue"),
    meta: { title: "通知记录", desc: "查看支付宝异步通知、验签结果和处理结果" }
  },
  {
    path: "/payment/apps",
    name: "payment-apps",
    component: () => import("../views/AppsView.vue"),
    meta: { title: "接入应用", desc: "管理调用支付网关的业务系统账号和回调地址" }
  },
  {
    path: "/payment/alipay-config",
    name: "alipay-config",
    component: () => import("../views/AlipayConfigView.vue"),
    meta: { title: "支付宝配置", desc: "维护支付宝 AppId、网关、公钥、私钥和回调地址" }
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
  document.title = to.meta.title ? `${to.meta.title} - 管理后台` : "管理后台";
});

export default router;

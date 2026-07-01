<template>
  <div class="mobile-shell">
    <template v-if="isResultPage">
      <header class="app-header">
        <div class="brand-row">
          <div>
            <span class="eyebrow">Payment Result</span>
            <h1>支付结果</h1>
          </div>
          <van-button icon="wap-home-o" round class="icon-action" @click="goHome" />
        </div>
      </header>

      <main class="content result-content">
        <section class="result-panel">
          <van-icon :name="resultIcon" :class="['result-icon', resultState]" />
          <h2>{{ resultTitle }}</h2>
          <p>{{ resultDescription }}</p>
        </section>

        <van-cell-group inset>
          <van-cell title="商户订单号" :value="resultOrderNo || '-'" />
          <van-cell title="支付状态" :value="orderStatusText(payResult?.status)" />
          <van-cell title="订单金额" :value="payResult?.totalAmount ? money(payResult.totalAmount) : '-'" />
          <van-cell title="平台订单号" :value="payResult?.platTradeNo || '-'" />
          <van-cell title="更新时间" :value="payResult?.updatedAt || '-'" />
        </van-cell-group>

        <div class="result-actions">
          <van-button block round color="#0f766e" :loading="resultLoading" @click="loadPayResult">刷新状态</van-button>
          <van-button block round @click="goHome">返回商城</van-button>
        </div>
      </main>
    </template>

    <template v-else>
    <header ref="appHeader" class="app-header">
      <div class="brand-row">
        <div>
          <span class="eyebrow">商城</span>
          <h1>商城</h1>
        </div>
        <div class="header-buttons">
          <van-button
            :icon="customerUser ? 'user-circle-o' : 'contact-o'"
            round
            class="icon-action"
            @click="customerUser ? showUserMenu = true : openAuth('login')"
          />
          <van-badge :content="cartCount || ''" :show-zero="false">
            <van-button icon="shopping-cart-o" round class="icon-action" @click="openCart()" />
          </van-badge>
        </div>
      </div>
      <van-search
        v-model="query"
        shape="round"
        background="transparent"
        placeholder="搜索手机、家电、生鲜、运动..."
      />
    </header>

    <main class="content">
      <section class="hero-card">
        <div class="hero-bg"></div>
        <div class="hero-copy">
          <span>品质生活 · 每日优选</span>
          <strong>全品类好物，一站购齐</strong>
          <small>精选商家直供，支持 SKU 规格下单</small>
          <div class="hero-actions">
            <van-button size="small" round color="#0f766e" @click="scrollToGoods">立即选购</van-button>
            <span>{{ products.length || defaultProducts.length }} 款在售</span>
          </div>
        </div>
        <div class="hero-metrics">
          <div>
            <strong>48h</strong>
            <span>快速履约</span>
          </div>
          <div>
            <strong>SKU</strong>
            <span>规格下单</span>
          </div>
        </div>
      </section>

      <div ref="tabsSticky" class="tabs-sticky">
        <van-tabs ref="categoryTabs" v-model:active="activeCategory" swipeable class="category-tabs">
          <van-tab v-for="category in categories" :key="category.id" :title="category.name" :name="category.id" />
        </van-tabs>
      </div>

      <section ref="goodsSection" class="goods-section">
        <div class="section-head">
          <div>
            <span class="eyebrow">Catalog</span>
            <h2>{{ catalogTitle }}</h2>
          </div>
          <van-button size="small" icon="filter-o" round @click="showFilter = true">筛选</van-button>
        </div>

        <van-empty v-if="!catalogLoading && filteredProducts.length === 0" description="没有找到商品，试试放宽筛选条件" />
        <van-loading v-else-if="catalogLoading" class="catalog-loading" color="#0f766e">加载商品中...</van-loading>
        <div v-else class="goods-grid">
          <article v-for="product in filteredProducts" :key="product.id" class="goods-card" @click="openProduct(product)">
            <div class="goods-image">
              <img :src="product.image" :alt="product.name" />
              <van-tag v-if="product.fast" round type="success">极速达</van-tag>
            </div>
            <div class="goods-body">
              <div class="tag-row">
                <van-tag v-for="tag in product.tags" :key="tag" plain type="warning">{{ tag }}</van-tag>
              </div>
              <strong>{{ product.name }}</strong>
              <small class="goods-merchant">{{ product.merchantName || product.merchantNo || '精选商家' }}</small>
              <div class="price-row">
                <span>{{ money(product.price) }}</span>
                <del>{{ money(product.originPrice) }}</del>
              </div>
              <div class="goods-meta">
                <span>{{ product.skus?.length > 1 ? product.skus.length + '规格' : '默认规格' }}</span>
                <span>库存 {{ product.stock }}</span>
              </div>
            </div>
          </article>
        </div>
      </section>

    </main>

    <van-action-bar class="mall-action-bar" placeholder safe-area-inset-bottom>
      <van-action-bar-icon icon="wap-home-o" text="首页" @click="scrollToTop" />
      <van-action-bar-icon icon="apps-o" text="分类" @click="scrollToGoods" />
      <van-action-bar-icon icon="shopping-cart-o" text="购物车" :badge="cartCount || ''" @click="openCart()" />
      <van-action-bar-button color="#0f766e" type="primary" text="我的订单" @click="openMyOrders" />
    </van-action-bar>

    <van-popup v-model:show="showFilter" position="bottom" round closeable class="filter-popup">
      <h2>筛选商品</h2>
      <van-field label="价格区间">
        <template #input>
          <van-radio-group v-model="priceRange" direction="horizontal">
            <van-radio name="all">全部</van-radio>
            <van-radio name="0-99">99以下</van-radio>
            <van-radio name="100-499">100-499</van-radio>
            <van-radio name="500-1999">500-1999</van-radio>
            <van-radio name="2000-99999">2000+</van-radio>
          </van-radio-group>
        </template>
      </van-field>
      <van-field label="排序">
        <template #input>
          <van-radio-group v-model="sortType" direction="horizontal">
            <van-radio name="featured">推荐</van-radio>
            <van-radio name="sales">销量</van-radio>
            <van-radio name="priceAsc">低价</van-radio>
            <van-radio name="priceDesc">高价</van-radio>
          </van-radio-group>
        </template>
      </van-field>
      <van-cell title="只看极速达">
        <template #right-icon>
          <van-switch v-model="onlyFast" size="22" />
        </template>
      </van-cell>
      <van-cell title="只看优惠商品">
        <template #right-icon>
          <van-switch v-model="onlyDiscount" size="22" />
        </template>
      </van-cell>
      <div class="popup-actions">
        <van-button block round @click="resetFilters">重置</van-button>
        <van-button block round color="#0f766e" @click="showFilter = false">完成</van-button>
      </div>
    </van-popup>

    <van-popup v-model:show="showAuth" position="bottom" round closeable class="auth-popup">
      <h2>{{ authMode === 'login' ? '登录商城' : '注册商城账号' }}</h2>
      <van-tabs v-model:active="authMode">
        <van-tab title="登录" name="login" />
        <van-tab title="注册" name="register" />
      </van-tabs>
      <van-cell-group inset>
        <van-field v-model="authForm.phone" label="手机号" placeholder="请输入11位手机号" maxlength="11" type="tel" clearable />
        <van-field v-model="authForm.password" label="密码" placeholder="至少6位" type="password" clearable />
        <van-field v-if="authMode === 'register'" v-model="authForm.displayName" label="昵称" placeholder="选填" clearable />
      </van-cell-group>
      <div class="popup-actions">
        <van-button block round color="#0f766e" :loading="authLoading" @click="submitAuth">
          {{ authMode === 'login' ? '登录' : '注册并登录' }}
        </van-button>
      </div>
    </van-popup>

    <van-popup v-model:show="showUserMenu" position="bottom" round closeable class="auth-popup">
      <h2>账号</h2>
      <van-cell-group inset>
        <van-cell title="手机号" :value="customerUser?.phone || '-'" />
        <van-cell title="昵称" :value="customerUser?.displayName || '-'" />
      </van-cell-group>
      <div class="popup-actions">
        <van-button block round color="#0f766e" @click="openMyOrders">我的订单</van-button>
        <van-button block round plain color="#0f766e" @click="openAddresses">地址管理</van-button>
        <van-button block round @click="logoutCustomer">退出登录</van-button>
      </div>
    </van-popup>

    <van-popup v-model:show="showAddresses" position="bottom" round closeable class="cart-popup" :style="{ height: '70vh' }">
      <h2>地址管理</h2>
      <van-loading v-if="addressesLoading" class="catalog-loading" color="#0f766e">加载中...</van-loading>
      <van-empty v-else-if="addresses.length === 0 && !showAddressForm" description="暂无地址" />
      <div v-else class="cart-list">
        <div v-if="showAddressForm" class="address-form-card">
          <van-field v-model="addressForm.receiverName" label="收件人" placeholder="请输入收件人姓名" required />
          <van-field v-model="addressForm.phone" label="手机号" placeholder="请输入手机号" type="tel" maxlength="11" required />
          <van-field :model-value="addressForm.province" is-link readonly label="省份" placeholder="请选择省份" required @click="openRegionPicker('province')" />
          <van-field :model-value="addressForm.city" is-link readonly label="城市" placeholder="请选择城市" required @click="openRegionPicker('city')" />
          <van-field :model-value="addressForm.district" is-link readonly label="区县" placeholder="请选择区县" required @click="openRegionPicker('district')" />
          <van-field v-model="addressForm.detailAddress" label="详细地址" placeholder="街道、门牌号等" required />
          <van-popup v-model:show="showRegionPicker" position="bottom" round>
            <van-picker :columns="regionOptions" :title="regionPickerTitle" @confirm="onRegionConfirm" @cancel="showRegionPicker = false" />
          </van-popup>
          <div class="popup-actions">
            <van-button block round color="#0f766e" @click="saveAddress" :loading="savingAddress">保存</van-button>
            <van-button block round @click="resetAddressForm">取消</van-button>
          </div>
        </div>
        <div v-for="addr in addresses" :key="addr.id" class="address-card" :class="{ 'is-default': addr.isDefault, 'is-selected': isSelectedAddress(addr) }">
          <div class="address-card-body" @click="selectCheckoutAddress(addr)">
            <van-icon :name="isSelectedAddress(addr) ? 'checked' : 'circle'" :color="isSelectedAddress(addr) ? '#0f766e' : '#c0c4cc'" size="20" />
            <div class="address-copy">
              <div class="address-name-row">
                <strong>{{ addr.receiverName }} {{ addr.phone }}</strong>
                <span v-if="addr.isDefault" class="address-badge">默认</span>
                <span v-if="isSelectedAddress(addr)" class="address-badge selected">已选</span>
              </div>
              <small>{{ addr.province }} {{ addr.city }} {{ addr.district }} {{ addr.detailAddress }}</small>
            </div>
          </div>
          <div class="address-card-actions">
            <van-button size="small" plain type="success" :disabled="addr.isDefault" @click="setDefaultAddress(addr)">
              {{ addr.isDefault ? '默认地址' : '设为默认' }}
            </van-button>
            <van-button size="small" plain type="primary" @click="editAddress(addr)">编辑</van-button>
            <van-button size="small" plain type="danger" @click="deleteAddress(addr)">删除</van-button>
          </div>
        </div>
      </div>
      <div v-if="!showAddressForm" class="address-add-bar">
        <van-button block round color="#0f766e" @click="openAddressForm">新增地址</van-button>
      </div>
    </van-popup>

    <van-popup v-model:show="showOrders" position="bottom" round closeable class="cart-popup" :style="{ height: '70vh' }">
      <h2>我的订单</h2>
      <van-loading v-if="ordersLoading" class="catalog-loading" color="#0f766e">加载订单中...</van-loading>
      <van-empty v-else-if="myOrders.length === 0" description="暂无订单" />
      <div v-else class="cart-list order-list">
        <div v-for="po in myOrders" :key="po.id" class="order-card">
          <div class="order-card-header">
            <div>
              <span class="order-merchant">{{ po.merchantName || '商家' }}</span>
              <span class="order-no">{{ po.orderNo }}</span>
            </div>
            <van-tag :type="orderStatusTag(po.status)" size="medium">{{ orderStatusLabel(po.status) }}</van-tag>
          </div>
          <div class="order-items">
            <div v-for="item in visibleOrderItems(po)" :key="item.id" class="order-item-row">
              <img :src="item.productImage" :alt="item.productName" />
              <div>
                <strong>{{ item.productName }}</strong>
                <span>{{ item.skuName || item.skuCode || '默认规格' }}</span>
                <em>{{ money(item.unitPrice) }}</em>
              </div>
              <small>x{{ item.quantity }}</small>
            </div>
            <div v-if="hiddenOrderItemCount(po)" class="order-more-row">
              共 {{ po.items.length }} 件商品，已收起 {{ hiddenOrderItemCount(po) }} 件
            </div>
          </div>
          <div class="order-shipping">
            <van-icon name="location-o" />
            <div>
              <strong>{{ po.shippingName || '-' }} {{ po.shippingPhone || '' }}</strong>
              <span>{{ po.shippingAddress || '暂无收货地址' }}</span>
            </div>
          </div>
          <div class="order-card-footer">
            <div class="order-time-block">
              <span>下单时间</span>
              <strong>{{ po.createdAt }}</strong>
            </div>
            <div class="order-total">
              <span>合计</span>
              <strong>{{ money(po.totalAmount) }}</strong>
            </div>
          </div>
          <div v-if="hasPendingPayment(po)" class="order-pay-bar">
            <button class="order-pay-button" type="button" @click="openPayForOrder(po)">去支付</button>
          </div>
        </div>
      </div>
    </van-popup>

    <van-popup v-model:show="showDetail" position="bottom" round closeable class="detail-popup">
      <template v-if="selectedProduct">
        <van-swipe class="detail-image-swipe" indicator-color="#0f766e">
          <van-swipe-item v-for="image in selectedProduct.images" :key="image">
            <img class="detail-image" :src="image" :alt="selectedProduct.name" />
          </van-swipe-item>
        </van-swipe>
        <div class="detail-content">
          <div class="detail-title-block">
            <span class="eyebrow">{{ categoryName(selectedProduct.category) }}</span>
            <h2>{{ selectedProduct.name }}</h2>
          </div>
          <div class="detail-price-panel">
            <div class="detail-price">
              <strong>{{ money(selectedSku?.price || selectedProduct.price) }}</strong>
              <del>{{ money(selectedProduct.originPrice) }}</del>
            </div>
            <span>库存 {{ selectedSku?.stock ?? selectedProduct.stock }}</span>
          </div>
          <div class="detail-shop">
            <van-icon name="shop-o" />
            <div>
              <strong>{{ selectedProduct.merchantName || selectedProduct.merchantNo || '精选商家' }}</strong>
              <span>由商家提供商品和售后服务</span>
            </div>
          </div>
          <section v-if="selectedProduct.skus?.length" class="detail-section">
            <div class="detail-section-head">
              <strong>选择规格</strong>
              <span>{{ selectedProduct.skus.length }} 个规格</span>
            </div>
            <div class="sku-card-grid">
              <button
                v-for="sku in selectedProduct.skus"
                :key="sku.id"
                :class="{ active: String(selectedSkuId || 'default') === String(sku.id || 'default') }"
                type="button"
                @click="selectedSkuId = sku.id || null"
              >
                <strong>{{ sku.skuName }}</strong>
                <span>
                  <em>{{ money(sku.price) }}</em>
                  <small>库存 {{ sku.stock }}</small>
                </span>
              </button>
            </div>
          </section>
          <p>{{ selectedProduct.desc }}</p>
          <div class="service-strip">
            <span><van-icon name="passed" />7天无理由</span>
            <span><van-icon name="logistics" />{{ selectedProduct.fast ? '同城极速达' : '次日达' }}</span>
            <span><van-icon name="certificate" />正品保障</span>
          </div>
        </div>
        <div class="detail-actions">
          <button class="detail-cart-button" type="button" @click="addToCart(selectedProduct)">加入购物车</button>
          <button class="detail-buy-button" type="button" @click="buyNow(selectedProduct)">立即购买</button>
        </div>
      </template>
    </van-popup>

    <van-popup v-model:show="showCart" position="bottom" round closeable class="cart-popup">
      <div class="popup-title">
        <div>
          <span class="eyebrow">Cart</span>
          <h2>购物车</h2>
        </div>
        <small>{{ cartCount }} 件商品</small>
      </div>
      <van-empty v-if="cartRows.length === 0" description="购物车为空" />
      <div v-else class="cart-list">
        <div v-for="row in cartRows" :key="row.key" class="cart-item-card">
          <img :src="row.product.image" :alt="row.product.name" />
          <div class="cart-item-main">
            <strong>{{ row.product.name }}</strong>
            <span class="cart-merchant-name">{{ cartMerchantName(row) }}</span>
            <span class="cart-sku-name">{{ cartSkuName(row) }}</span>
          </div>
          <div class="cart-item-bottom">
            <div>
              <em>{{ money(row.sku.price) }}</em>
              <del>{{ money(row.product.originPrice) }}</del>
            </div>
            <van-stepper v-model="cart[row.key]" min="0" integer @change="syncCart" />
          </div>
        </div>
      </div>
      <div v-if="cartRows.length" class="cart-summary">
        <span>商品金额</span>
        <strong>{{ money(cartTotal.subtotal) }}</strong>
        <span>已优惠</span>
        <strong class="discount">{{ money(cartTotal.discount) }}</strong>
      </div>
      <div class="cart-bottom-bar">
        <div>
          <span>合计</span>
          <strong>{{ money(cartTotal.total) }}</strong>
        </div>
        <button class="checkout-primary-button" type="button" :disabled="cartRows.length === 0" @click="openCheckout">去结算</button>
      </div>
    </van-popup>

    <van-popup v-model:show="showCheckout" position="bottom" round closeable class="checkout-popup">
      <div class="popup-title">
        <div>
          <span class="eyebrow">Checkout</span>
          <h2>确认订单</h2>
        </div>
        <small>{{ checkoutMerchantName || '待确认商家' }}</small>
      </div>
      <van-steps :active="checkoutDone ? 2 : 1">
        <van-step>地址</van-step>
        <van-step>支付</van-step>
        <van-step>完成</van-step>
      </van-steps>
      <section class="checkout-section address-section" @click="openAddresses">
        <van-icon name="location-o" />
        <div>
          <strong>{{ selectedAddress ? selectedAddress.receiverName : '请选择收货地址' }}</strong>
          <span>{{ selectedAddress ? formatAddress(selectedAddress) : '下单前需要选择真实收货地址' }}</span>
        </div>
        <van-icon name="arrow" />
      </section>
      <section class="checkout-section">
        <div class="checkout-row">
          <span>支付方式</span>
          <strong>支付宝</strong>
        </div>
        <div class="checkout-row">
          <span>配送方式</span>
          <strong>{{ cartTotal.shipping > 0 ? '普通配送' : '包邮极速达' }}</strong>
        </div>
        <div class="checkout-row">
          <span>登录账号</span>
          <strong>{{ customerUser?.phone || '未登录' }}</strong>
        </div>
        <div class="checkout-row">
          <span>收款商家</span>
          <strong>{{ checkoutMerchantName || '-' }}</strong>
        </div>
      </section>
      <section class="checkout-section">
        <div class="checkout-section-head">
          <strong>商品明细</strong>
          <span>{{ cartCount }} 件</span>
        </div>
        <div v-for="row in cartRows" :key="row.key" class="checkout-goods-row">
          <img :src="row.product.image" :alt="row.product.name" />
          <div>
            <strong>{{ row.product.name }}</strong>
            <span class="checkout-merchant-name">{{ cartMerchantName(row) }}</span>
            <span class="checkout-sku-name">{{ cartSkuName(row) }}</span>
          </div>
          <em>{{ money(row.sku.price) }} ×{{ row.qty }}</em>
        </div>
      </section>
      <section v-if="createdOrder" class="pay-result-card">
        <span class="eyebrow">Pay Order</span>
        <strong>{{ createdOrder.orderNo }}</strong>
        <p>{{ orderStatusText(createdOrder.status) }} · {{ createdOrder.platTradeNo || "等待平台单号" }}</p>
        <div class="pay-actions">
          <van-button size="small" round color="#0f766e" @click="openPayPage">打开支付页</van-button>
          <van-button size="small" round @click="openResultPage">查看结果</van-button>
        </div>
      </section>
      <div class="cart-bottom-bar checkout-bottom-bar">
        <div>
          <span>应付</span>
          <strong>{{ money(cartTotal.total) }}</strong>
        </div>
        <button class="checkout-primary-button" type="button" :disabled="cartRows.length === 0 || payCreating" @click="placeOrder">
          {{ payCreating ? '处理中' : (createdOrder ? '重新支付' : '去支付') }}
        </button>
      </div>
      <van-notice-bar v-if="checkoutDone" mode="closeable" type="success" text="支付订单创建成功，可打开支付页完成付款。" />
    </van-popup>
    </template>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { showToast } from "vant";

const defaultCategories = [
  { id: "all", name: "全部", icon: "⌘", desc: "综合推荐" },
  { id: "digital", name: "数码家电", icon: "▣", desc: "手机、电脑、家电" },
  { id: "fashion", name: "服饰美妆", icon: "◇", desc: "穿搭、护肤、香氛" },
  { id: "fresh", name: "生鲜食品", icon: "◌", desc: "水果、肉禽、烘焙" },
  { id: "home", name: "家居日用", icon: "□", desc: "收纳、清洁、家纺" },
  { id: "sports", name: "运动户外", icon: "△", desc: "健身、露营、骑行" },
  { id: "baby", name: "母婴玩具", icon: "◎", desc: "纸尿裤、玩具、童装" }
];

const defaultProducts = [
  {
    id: 1,
    category: "digital",
    name: "Pro X 折叠屏手机",
    price: 6999,
    originPrice: 7499,
    sales: 12890,
    rating: 4.9,
    fast: true,
    image: "https://images.unsplash.com/photo-1598327105666-5b89351aff97?auto=format&fit=crop&w=900&q=80",
    tags: ["新品", "24期免息"],
    desc: "旗舰影像、轻薄折叠机身和全天续航。"
  },
  {
    id: 2,
    category: "fashion",
    name: "轻奢通勤羊毛外套",
    price: 899,
    originPrice: 1299,
    sales: 8430,
    rating: 4.8,
    fast: false,
    image: "https://images.unsplash.com/photo-1496747611176-843222e1e57c?auto=format&fit=crop&w=900&q=80",
    tags: ["满减", "热卖"],
    desc: "利落剪裁和温润材质，适合通勤穿搭。"
  },
  {
    id: 3,
    category: "fresh",
    name: "云南高山蓝莓礼盒",
    price: 89,
    originPrice: 119,
    sales: 32200,
    rating: 4.7,
    fast: true,
    image: "https://images.unsplash.com/photo-1498557850523-fd3d118b962e?auto=format&fit=crop&w=900&q=80",
    tags: ["2小时达", "产地直发"],
    desc: "高山产区直采，冷链鲜配到家。"
  },
  {
    id: 4,
    category: "home",
    name: "智能分区扫拖机器人",
    price: 1899,
    originPrice: 2399,
    sales: 15600,
    rating: 4.9,
    fast: true,
    image: "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?auto=format&fit=crop&w=900&q=80",
    tags: ["家电补贴", "以旧换新"],
    desc: "自动集尘、分区清洁，适合全屋日常维护。"
  },
  {
    id: 5,
    category: "sports",
    name: "城市轻量跑步鞋",
    price: 459,
    originPrice: 599,
    sales: 21400,
    rating: 4.8,
    fast: false,
    image: "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=900&q=80",
    tags: ["专业缓震", "新品色"],
    desc: "轻量缓震中底，适合日常训练和通勤。"
  },
  {
    id: 6,
    category: "baby",
    name: "婴儿柔护纸尿裤箱装",
    price: 169,
    originPrice: 219,
    sales: 48900,
    rating: 4.9,
    fast: true,
    image: "https://images.unsplash.com/photo-1522771930-78848d9293e8?auto=format&fit=crop&w=900&q=80",
    tags: ["囤货装", "极速达"],
    desc: "柔软透气，适合高频复购和家庭囤货。"
  },
  {
    id: 7,
    category: "digital",
    name: "27英寸 4K 专业显示器",
    price: 2199,
    originPrice: 2499,
    sales: 7600,
    rating: 4.7,
    fast: false,
    image: "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=900&q=80",
    tags: ["设计师精选", "低蓝光"],
    desc: "高色准屏幕，适合办公、修图和多任务。"
  },
  {
    id: 8,
    category: "fresh",
    name: "低温鲜牛乳 950ml",
    price: 19.9,
    originPrice: 26.9,
    sales: 62700,
    rating: 4.8,
    fast: true,
    image: "https://images.unsplash.com/photo-1563636619-e9143da7973b?auto=format&fit=crop&w=900&q=80",
    tags: ["每日鲜配", "第二件半价"],
    desc: "低价高频商品，适合测试购物车和凑单。"
  }
];

const activeCategory = ref("all");
const categories = ref(defaultCategories);
const products = ref([]);
const catalogLoading = ref(false);
const query = ref("");
const priceRange = ref("all");
const sortType = ref("featured");
const onlyFast = ref(false);
const onlyDiscount = ref(false);
const showFilter = ref(false);
const showDetail = ref(false);
const showCart = ref(false);
const showCheckout = ref(false);
const selectedProduct = ref(null);
const selectedSkuId = ref(null);
const checkoutDone = ref(false);

const storageKeys = {
  customerToken: "mall_customer_token",
  customerUser: "mall_customer_user",
  loginPhone: "mall_login_phone",
  lastOrderNo: "mall_last_order_no"
};

const legacyStorageKeys = {
  customerToken: "frontend_customer_token",
  customerUser: "frontend_customer_user",
  loginPhone: "frontend_login_phone",
  lastOrderNo: "frontend_last_order_no"
};

const defaultBackendBaseUrl = String(import.meta.env.VITE_API_BASE_URL || "").trim().replace(/\/$/, "");

function migrateStorageValue(key, legacyKey) {
  const current = localStorage.getItem(key);
  const legacy = localStorage.getItem(legacyKey);
  if (!current && legacy) {
    localStorage.setItem(key, legacy);
  }
  if (legacy !== null) {
    localStorage.removeItem(legacyKey);
  }
  return localStorage.getItem(key) || "";
}

localStorage.removeItem("mall_backend_base_url");
localStorage.removeItem("frontend_backend_base_url");

const backendBaseUrl = ref(defaultBackendBaseUrl);
const customerToken = ref(migrateStorageValue(storageKeys.customerToken, legacyStorageKeys.customerToken));
const customerUser = ref(readStoredCustomer());
const showAuth = ref(false);
const showUserMenu = ref(false);
const authMode = ref("login");
const authLoading = ref(false);
const authForm = ref({
  phone: migrateStorageValue(storageKeys.loginPhone, legacyStorageKeys.loginPhone),
  password: "",
  displayName: ""
});
const payCreating = ref(false);
const createdOrder = ref(null);
const payResult = ref(null);
const resultLoading = ref(false);
const cart = ref({});
const lastSyncedCart = ref({});
const pendingAddToCartProduct = ref(null);
const pendingBuyNow = ref(false);
const savedCartBeforeBuyNow = ref(null);
const showOrders = ref(false);
const myOrders = ref([]);

const showAddresses = ref(false);
const addresses = ref([]);
const selectedAddressId = ref(null);
const addressesLoading = ref(false);
const savingAddress = ref(false);
const showAddressForm = ref(false);
const editingAddressId = ref(null);
const addressForm = reactive({ receiverName: "", phone: "", province: "", city: "", district: "", detailAddress: "" });
const addressCodes = reactive({ province: "", city: "" });
const showRegionPicker = ref(false);
const regionPickerTitle = ref("");
const regionOptions = ref([]);
const regionPickerType = ref("");
const regionsCache = reactive({ province: [], city: {}, district: {} });
const ordersLoading = ref(false);
const appHeader = ref(null);
const categoryTabs = ref(null);
const goodsSection = ref(null);
const tabsSticky = ref(null);
const headerHeight = ref(0);
let timer = null;
let stickyOffsetFrame = 0;

const isResultPage = window.location.pathname === "/pay-result";
const resultOrderNo = new URLSearchParams(window.location.search).get("orderNo")
  || migrateStorageValue(storageKeys.lastOrderNo, legacyStorageKeys.lastOrderNo)
  || "";

const orderStatusLabels = {
  CREATED: "订单已创建",
  CREATE_SUCCESS: "支付单已创建",
  CREATE_FAILED: "创建失败",
  SUCCESS: "支付成功",
  FINISHED: "交易结束",
  CLOSED: "交易关闭",
  UNKNOWN_NOTIFY: "未知通知"
};

const filteredProducts = computed(() => {
  const keyword = query.value.trim();
  const [min, max] = priceRange.value === "all" ? [0, Infinity] : priceRange.value.split("-").map(Number);
  const list = products.value.filter((product) => {
    const matchesCategory = activeCategory.value === "all" || product.category === activeCategory.value;
    const matchesQuery = !keyword || product.name.includes(keyword) || product.desc.includes(keyword);
    const matchesPrice = product.price >= min && product.price <= max;
    const matchesFast = !onlyFast.value || product.fast;
    const matchesDiscount = !onlyDiscount.value || product.originPrice > product.price;
    return matchesCategory && matchesQuery && matchesPrice && matchesFast && matchesDiscount;
  });

  return [...list].sort((a, b) => {
    if (sortType.value === "sales") return b.sales - a.sales;
    if (sortType.value === "priceAsc") return a.price - b.price;
    if (sortType.value === "priceDesc") return b.price - a.price;
    return b.rating - a.rating;
  });
});

const catalogTitle = computed(() => {
  if (query.value.trim()) return `搜索：${query.value.trim()}`;
  return activeCategory.value === "all" ? "为你推荐" : categoryName(activeCategory.value);
});

const cartRows = computed(() =>
  Object.entries(cart.value)
    .map(([key, qty]) => {
      const { productId, skuId } = parseCartKey(key);
      const product = products.value.find((item) => String(item.id) === String(productId));
      const sku = findSku(product, skuId);
      return { key, product, sku, qty };
    })
    .filter((row) => row.product && row.qty > 0)
);

const cartCount = computed(() => cartRows.value.reduce((sum, row) => sum + row.qty, 0));

const checkoutMerchantNo = computed(() => {
  const merchantNos = uniqueCartMerchantNos();
  return merchantNos.length === 1 ? merchantNos[0] : "";
});

const checkoutMerchantName = computed(() => {
  if (cartRows.value.length === 0) return "";
  return cartRows.value[0].product?.merchantName || checkoutMerchantNo.value;
});

const selectedAddress = computed(() => {
  if (addresses.value.length === 0) return null;
  return addresses.value.find((address) => String(address.id) === String(selectedAddressId.value))
    || addresses.value.find((address) => address.isDefault)
    || addresses.value[0];
});

const cartTotal = computed(() => {
  const subtotal = cartRows.value.reduce((sum, row) => sum + row.product.originPrice * row.qty, 0);
  const payable = cartRows.value.reduce((sum, row) => sum + row.sku.price * row.qty, 0);
  const shipping = 0;
  return {
    subtotal,
    discount: subtotal - payable,
    shipping,
    total: payable
  };
});

const selectedSku = computed(() => findSku(selectedProduct.value, selectedSkuId.value));

const resultState = computed(() => {
  if (payResult.value?.status === "SUCCESS") return "success";
  if (["CREATE_FAILED", "FINISHED", "CLOSED", "UNKNOWN_NOTIFY"].includes(payResult.value?.status)) return "failed";
  return "processing";
});

const resultIcon = computed(() => {
  if (resultState.value === "success") return "checked";
  if (resultState.value === "failed") return "warning-o";
  return "clock-o";
});

const resultTitle = computed(() => {
  if (resultState.value === "success") return "支付成功";
  if (resultState.value === "failed") return "支付未完成";
  return resultLoading.value ? "正在查询支付结果" : "支付处理中";
});

const resultDescription = computed(() => {
  if (!resultOrderNo) return "没有找到商户订单号，请返回商城重新下单。";
  if (resultState.value === "success") return "后端已收到支付成功状态，订单链路已闭环。";
  if (resultState.value === "failed") return "当前订单未完成支付，可以返回商城重新创建订单。";
  return "如果已经完成付款，稍后刷新状态即可看到最新结果。";
});

function money(value) {
  return `¥${formatAmount(value)}`;
}

function formatAmount(value) {
  const amount = Number(value || 0);
  return amount.toLocaleString("zh-CN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}

function cartKey(product, sku = findSku(product)) {
  return `${product?.id || ""}:${sku?.id || "default"}`;
}

function parseCartKey(key) {
  const [productId, skuId] = String(key).split(":");
  return { productId, skuId: skuId === "default" ? null : skuId };
}

function findSku(product, skuId = null) {
  const skus = product?.skus?.length
    ? product.skus
    : [{ id: null, skuCode: "", skuName: "默认规格", price: Number(product?.price || 0), stock: Number(product?.stock || 0) }];
  return skus.find((sku) => String(sku.id || "default") === String(skuId || "default")) || skus[0];
}

function cartSkuDesc(row) {
  return `${row.product.merchantName || ""}${row.sku?.skuName ? " · " + row.sku.skuName : ""}`;
}

function cartMerchantName(row) {
  return row?.product?.merchantName || row?.product?.merchantNo || "精选商家";
}

function cartSkuName(row) {
  return row?.sku?.skuName || row?.sku?.skuCode || "默认规格";
}

function normalizedBackendBaseUrl() {
  const value = backendBaseUrl.value.trim().replace(/\/$/, "");
  return value;
}

function backendPublicBaseUrl() {
  return normalizedBackendBaseUrl() || window.location.origin;
}

function readStoredCustomer() {
  try {
    return JSON.parse(migrateStorageValue(storageKeys.customerUser, legacyStorageKeys.customerUser) || "null");
  } catch {
    return null;
  }
}

function orderStatusText(status) {
  return orderStatusLabels[status] || status || "-";
}

function categoryName(id) {
  return categories.value.find((category) => category.id === id)?.name || "全部";
}

async function loadCatalog() {
  catalogLoading.value = true;
  try {
    const backend = normalizedBackendBaseUrl();
    const [categoryResponse, productResponse] = await Promise.all([
      fetch(`${backend}/api/mall/catalog/categories`),
      fetch(`${backend}/api/mall/catalog/products?size=100`)
    ]);
    const remoteCategories = await readResponseJson(categoryResponse);
    const remoteProducts = await readResponseJson(productResponse);
    categories.value = [
      defaultCategories[0],
      ...remoteCategories.map((item) => ({
        id: item.category_code,
        name: item.category_name,
        icon: categoryIcon(item.category_code),
        desc: "精选好物"
      }))
    ];
    products.value = (remoteProducts.content || []).map(normalizeProduct);
  } catch (error) {
    categories.value = defaultCategories;
    products.value = defaultProducts;
    showToast(error instanceof Error ? `商品接口不可用，已显示演示商品：${error.message}` : "商品接口不可用，已显示演示商品");
  } finally {
    catalogLoading.value = false;
  }
}

function categoryIcon(code) {
  const fallback = defaultCategories.find((item) => item.id === code);
  return fallback?.icon || "□";
}

function normalizeProduct(product) {
  const images = normalizeImages(product);
  const skus = normalizeSkus(product);
  const defaultSku = skus[0] || { id: null, skuName: "默认规格", price: Number(product.price || 0), stock: Number(product.stock || 0) };
  return {
    id: product.id,
    merchantNo: product.merchantNo || product.merchant_no || product.appId || product.app_id || "biz-demo",
    merchantName: product.merchantName || product.merchant_name || product.appName || product.app_name || "",
    productCode: product.productCode || product.product_code || "",
    category: product.category || "all",
    name: product.name || product.productName || "未命名商品",
    price: Number(defaultSku.price || product.price || 0),
    originPrice: Number(product.originPrice || defaultSku.price || product.price || 0),
    stock: Number(defaultSku.stock || product.stock || 0),
    skus,
    sales: Number(product.sales || 0),
    rating: Number(product.rating || 4.8),
    fast: Boolean(product.fast),
    image: images[0],
    images,
    tags: [product.merchantName || product.merchant_name || product.appName || product.app_name || "商家"],
    desc: product.desc || product.description || ""
  };
}

function normalizeSkus(product) {
  const rawSkus = Array.isArray(product.skus) ? product.skus : [];
  if (rawSkus.length === 0) {
    return [{ id: null, skuCode: "", skuName: "默认规格", price: Number(product.price || 0), stock: Number(product.stock || 0) }];
  }
  return rawSkus
    .filter((sku) => sku.enabled !== false)
    .map((sku) => ({
      id: sku.id,
      skuCode: sku.skuCode || sku.sku_code || "",
      skuName: sku.skuName || sku.sku_name || "默认规格",
      price: Number(sku.price || 0),
      stock: Number(sku.stock || 0)
    }));
}

function normalizeImages(product) {
  const images = Array.isArray(product.images)
    ? product.images
    : Array.isArray(product.imageUrls)
      ? product.imageUrls
      : [];
  const normalized = images.filter((url) => typeof url === "string" && url.trim()).slice(0, 5);
  if (normalized.length > 0) return normalized;
  if (product.image) return [product.image];
  return ["https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?auto=format&fit=crop&w=900&q=80"];
}

function openAuth(mode) {
  authMode.value = mode;
  showAuth.value = true;
}

async function submitAuth() {
  const phone = authForm.value.phone.trim();
  const password = authForm.value.password;
  if (!/^1\d{10}$/.test(phone)) {
    showToast("请输入11位手机号");
    return;
  }
  if (!password || password.length < 6) {
    showToast("密码至少6位");
    return;
  }
  authLoading.value = true;
  try {
    const backend = normalizedBackendBaseUrl();
    const path = authMode.value === "login" ? "login" : "register";
    const response = await fetch(`${backend}/api/mall/customer/auth/${path}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        phone,
        password,
        displayName: authForm.value.displayName
      })
    });
    const data = await readResponseJson(response);
    customerToken.value = data.token;
    customerUser.value = data.user;
    localStorage.setItem(storageKeys.customerToken, data.token);
    localStorage.setItem(storageKeys.customerUser, JSON.stringify(data.user));
    showAuth.value = false;

    // 登录成功保留手机号，注册成功清空表单
    if (authMode.value === "register") {
      authForm.value = { phone: "", password: "", displayName: "" };
      localStorage.removeItem(storageKeys.loginPhone);
    } else {
      localStorage.setItem(storageKeys.loginPhone, phone);
    }

    // 登录后：如果有待加购/立即购买商品则自动执行，否则合并本地购物车
    try {
      if (pendingAddToCartProduct.value) {
        const product = pendingAddToCartProduct.value;
        pendingAddToCartProduct.value = null;
        if (pendingBuyNow.value) {
          pendingBuyNow.value = false;
          buyNow(product);
        } else {
          addToCart(product);
        }
      } else if (Object.keys(cart.value).length > 0) {
        await mergeCartToServer();
      } else {
        const saved = await loadCartFromServer();
        cart.value = saved;
        lastSyncedCart.value = { ...saved };
      }
    } catch { /* 非关键 */ }

    showToast(authMode.value === "login" ? "登录成功" : "注册成功");
  } catch (error) {
    showToast(error instanceof Error ? error.message : String(error));
  } finally {
    authLoading.value = false;
  }
}

async function logoutCustomer() {
  try {
    const backend = normalizedBackendBaseUrl();
    await fetch(`${backend}/api/mall/customer/auth/logout`, {
      method: "POST",
      headers: customerToken.value ? { Authorization: `Bearer ${customerToken.value}` } : {}
    });
  } finally {
    customerToken.value = "";
    customerUser.value = null;
    localStorage.removeItem(storageKeys.customerToken);
    localStorage.removeItem(storageKeys.customerUser);
    cart.value = {};
    lastSyncedCart.value = {};
    pendingAddToCartProduct.value = null;
    showUserMenu.value = false;
    // 清密码，保留手机号方便下次登录
    authForm.value.password = "";
    showToast("已退出登录");
  }
}

async function openMyOrders() {
  if (!customerUser.value) {
    openAuth("login");
    showToast("请先登录");
    return;
  }
  showUserMenu.value = false;
  showOrders.value = true;
  ordersLoading.value = true;
  try {
    const backend = normalizedBackendBaseUrl();
    const response = await fetch(`${backend}/api/mall/product-orders`, { headers: authHeaders() });
    myOrders.value = await readResponseJson(response);
  } catch {
    showToast("加载订单失败");
  } finally {
    ordersLoading.value = false;
  }
}

async function openAddresses() {
  if (!customerUser.value) { openAuth("login"); return; }
  showUserMenu.value = false;
  showAddresses.value = true;
  resetAddressForm();
  await loadAddresses();
}

async function loadAddresses() {
  addressesLoading.value = true;
  try {
    const backend = normalizedBackendBaseUrl();
    const response = await fetch(`${backend}/api/mall/addresses`, { headers: authHeaders() });
    addresses.value = await readResponseJson(response);
    syncSelectedAddress();
  } catch {
    showToast("加载地址失败");
  } finally {
    addressesLoading.value = false;
  }
}

function syncSelectedAddress() {
  if (addresses.value.length === 0) {
    selectedAddressId.value = null;
    return;
  }
  const exists = addresses.value.some((address) => String(address.id) === String(selectedAddressId.value));
  if (!exists) {
    selectedAddressId.value = (addresses.value.find((address) => address.isDefault) || addresses.value[0]).id;
  }
}

function openAddressForm() {
  editingAddressId.value = null;
  addressForm.receiverName = "";
  addressForm.phone = "";
  addressForm.province = "";
  addressForm.city = "";
  addressForm.district = "";
  addressForm.detailAddress = "";
  addressCodes.province = "";
  addressCodes.city = "";
  showAddressForm.value = true;
}

function editAddress(addr) {
  editingAddressId.value = addr.id;
  addressForm.receiverName = addr.receiverName;
  addressForm.phone = addr.phone;
  addressForm.province = addr.province;
  addressForm.city = addr.city;
  addressForm.district = addr.district;
  addressForm.detailAddress = addr.detailAddress;
  showAddressForm.value = true;
}

function resetAddressForm() {
  showAddressForm.value = false;
  editingAddressId.value = null;
  addressCodes.province = "";
  addressCodes.city = "";
}

async function saveAddress() {
  savingAddress.value = true;
  try {
    const backend = normalizedBackendBaseUrl();
    const payload = {
      receiverName: addressForm.receiverName,
      phone: addressForm.phone,
      province: addressForm.province,
      city: addressForm.city,
      district: addressForm.district,
      detailAddress: addressForm.detailAddress
    };
    if (editingAddressId.value) {
      await fetch(`${backend}/api/mall/addresses/${editingAddressId.value}`, {
        method: "PUT", headers: authHeaders(), body: JSON.stringify(payload)
      });
    } else {
      await fetch(`${backend}/api/mall/addresses`, {
        method: "POST", headers: authHeaders(), body: JSON.stringify(payload)
      });
    }
    resetAddressForm();
    await loadAddresses();
  } catch {
    showToast("保存失败");
  } finally {
    savingAddress.value = false;
  }
}

async function deleteAddress(addr) {
  try {
    const backend = normalizedBackendBaseUrl();
    await fetch(`${backend}/api/mall/addresses/${addr.id}`, { method: "DELETE", headers: authHeaders() });
    await loadAddresses();
  } catch {
    showToast("删除失败");
  }
}

async function fetchRegions(parentCode) {
  const backend = normalizedBackendBaseUrl();
  const params = parentCode ? `?parentCode=${parentCode}` : "";
  const response = await fetch(`${backend}/api/mall/regions${params}`);
  return (await readResponseJson(response)).map((r) => ({ text: r.name, value: r.code }));
}

async function openRegionPicker(type) {
  regionPickerType.value = type;
  if (type === "province") {
    if (regionsCache.province.length === 0) regionsCache.province = await fetchRegions(null);
    regionOptions.value = regionsCache.province;
    regionPickerTitle.value = "选择省份";
  } else if (type === "city") {
    if (!addressCodes.province) { showToast("请先选择省份"); return; }
    if (!regionsCache.city[addressCodes.province]) regionsCache.city[addressCodes.province] = await fetchRegions(addressCodes.province);
    regionOptions.value = regionsCache.city[addressCodes.province];
    regionPickerTitle.value = "选择城市";
  } else {
    if (!addressCodes.city) { showToast("请先选择城市"); return; }
    if (!regionsCache.district[addressCodes.city]) regionsCache.district[addressCodes.city] = await fetchRegions(addressCodes.city);
    regionOptions.value = regionsCache.district[addressCodes.city];
    regionPickerTitle.value = "选择区县";
  }
  showRegionPicker.value = true;
}

function onRegionConfirm({ selectedOptions }) {
  const code = selectedOptions[0]?.value;
  const name = selectedOptions[0]?.text;
  if (!code) return;
  const type = regionPickerType.value;
  if (type === "province") {
    addressForm.province = name;
    addressCodes.province = code;
    addressForm.city = "";
    addressCodes.city = "";
    addressForm.district = "";
  } else if (type === "city") {
    addressForm.city = name;
    addressCodes.city = code;
    addressForm.district = "";
  } else {
    addressForm.district = name;
  }
  showRegionPicker.value = false;
}

async function setDefaultAddress(addr) {
  if (addr.isDefault) return;
  try {
    const backend = normalizedBackendBaseUrl();
    await fetch(`${backend}/api/mall/addresses/${addr.id}/default`, { method: "PUT", headers: authHeaders() });
    await loadAddresses();
  } catch {
    showToast("操作失败");
  }
}

function isSelectedAddress(addr) {
  return selectedAddress.value && String(selectedAddress.value.id) === String(addr.id);
}

function selectCheckoutAddress(addr) {
  selectedAddressId.value = addr.id;
  showAddresses.value = false;
}

function formatAddress(addr) {
  if (!addr) return "";
  return [addr.phone, addr.province, addr.city, addr.district, addr.detailAddress]
    .filter(Boolean)
    .join(" ");
}

function orderStatusLabel(status) {
  const map = {
    PENDING: "待支付", PAID: "已支付", SHIPPED: "已发货",
    DELIVERED: "已签收", COMPLETED: "已完成", CANCELLED: "已取消",
    REFUNDING: "退款中", REFUNDED: "已退款"
  };
  return map[status] || status || "-";
}

function orderStatusTag(status) {
  const map = {
    PAID: "success", SHIPPED: "primary", DELIVERED: "success",
    COMPLETED: "success", CANCELLED: "danger", REFUNDING: "warning",
    REFUNDED: "warning", PENDING: "danger"
  };
  return map[status] || "default";
}

function visibleOrderItems(order) {
  return (order?.items || []).slice(0, 2);
}

function hiddenOrderItemCount(order) {
  return Math.max((order?.items?.length || 0) - 2, 0);
}

function openPayForOrder(order) {
  if (!order) {
    showToast("订单数据异常");
    return;
  }
  const payment = order.payments?.[0];
  if (!payment?.orderNo) {
    showToast("支付单不存在，请重新下单");
    return;
  }
  const payPageUrl = `${backendPublicBaseUrl()}/api/mall/pay-orders/${encodeURIComponent(payment.orderNo)}/pay-page`;
  window.open(payPageUrl, "_blank", "noreferrer");
}

function hasPendingPayment(order) {
  return order?.status === "PENDING" && Array.isArray(order.payments) && order.payments.length > 0;
}

function scrollToTop() {
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function scrollToGoods() {
  const el = tabsSticky.value || goodsSection.value;
  if (!el) return;
  updateStickyHeaderOffset();
  const h = headerHeight.value || appHeader.value?.offsetHeight || 0;
  const top = el.getBoundingClientRect().top + window.scrollY - h;
  window.scrollTo({ top, behavior: "smooth" });
}

function updateStickyHeaderOffset() {
  const nextHeight = appHeader.value?.offsetHeight || 0;
  headerHeight.value = nextHeight;
  document.documentElement.style.setProperty("--mall-header-height", `${nextHeight}px`);
  if (tabsSticky.value) {
    tabsSticky.value.style.top = `${nextHeight}px`;
  }
}

function scheduleStickyHeaderOffset() {
  window.cancelAnimationFrame(stickyOffsetFrame);
  stickyOffsetFrame = window.requestAnimationFrame(updateStickyHeaderOffset);
}

function openProduct(product) {
  selectedProduct.value = {
    ...product,
    images: product.images?.length ? product.images : [product.image]
  };
  selectedSkuId.value = findSku(product)?.id || null;
  showDetail.value = true;
}

function authHeaders() {
  return customerToken.value
    ? { Authorization: `Bearer ${customerToken.value}`, "Content-Type": "application/json" }
    : { "Content-Type": "application/json" };
}

async function loadCartFromServer() {
  const backend = normalizedBackendBaseUrl();
  const response = await fetch(`${backend}/api/mall/cart`, { headers: authHeaders() });
  const data = await readResponseJson(response);
  const cartObj = {};
  (data.items || []).forEach((item) => {
    cartObj[`${item.productId}:${item.skuId || "default"}`] = item.quantity;
  });
  return cartObj;
}

async function persistCartItem(productId, quantity, product, sku = findSku(product)) {
  const backend = normalizedBackendBaseUrl();
  const body = {
    productId: Number(productId),
    skuId: sku?.id || null,
    quantity,
    productCode: product?.productCode || "",
    skuCode: sku?.skuCode || "",
    skuName: sku?.skuName || "",
    productName: product?.name || "",
    productImage: product?.image || "",
    unitPrice: sku?.price || product?.price || 0
  };
  await fetch(`${backend}/api/mall/cart/items`, {
    method: "POST",
    headers: authHeaders(),
    body: JSON.stringify(body)
  });
}

async function mergeCartToServer() {
  const backend = normalizedBackendBaseUrl();
  const items = cartRows.value.map((row) => ({
    productId: row.product.id,
    skuId: row.sku?.id || null,
    quantity: row.qty,
    productCode: row.product.productCode || "",
    skuCode: row.sku?.skuCode || "",
    skuName: row.sku?.skuName || "",
    productName: row.product.name || "",
    productImage: row.product.image || "",
    unitPrice: row.sku?.price || row.product.price || 0
  }));
  if (items.length === 0) {
    const saved = await loadCartFromServer();
    cart.value = saved;
    lastSyncedCart.value = { ...saved };
    return;
  }
  const response = await fetch(`${backend}/api/mall/cart/merge`, {
    method: "POST",
    headers: authHeaders(),
    body: JSON.stringify({ items })
  });
  const data = await readResponseJson(response);
  const cartObj = {};
  (data.items || []).forEach((item) => {
    cartObj[`${item.productId}:${item.skuId || "default"}`] = item.quantity;
  });
  cart.value = cartObj;
  lastSyncedCart.value = { ...cartObj };
}

async function addToCart(product) {
  const sku = product === selectedProduct.value ? selectedSku.value : findSku(product);
  if (!customerUser.value) {
    pendingAddToCartProduct.value = product;
    openAuth("login");
    showToast("请先登录后添加购物车");
    return;
  }
  const key = cartKey(product, sku);
  const newQty = (cart.value[key] || 0) + 1;
  cart.value = { ...cart.value, [key]: newQty };
  try {
    await persistCartItem(product.id, newQty, product, sku);
    lastSyncedCart.value[key] = newQty;
    showToast("已加入购物车");
  } catch {
    // 回滚本地
    if (newQty <= 1) {
      delete cart.value[key];
    } else {
      cart.value[key] = newQty - 1;
    }
    showToast("购物车同步失败，请重试");
  }
}

async function buyNow(product) {
  const sku = product === selectedProduct.value ? selectedSku.value : findSku(product);
  if (!customerUser.value) {
    pendingAddToCartProduct.value = product;
    pendingBuyNow.value = true;
    openAuth("login");
    showToast("请先登录");
    return;
  }
  // 保存现有购物车，用单个商品替换
  savedCartBeforeBuyNow.value = { ...cart.value };
  const key = cartKey(product, sku);
  cart.value = { [key]: 1 };
  lastSyncedCart.value = { [key]: 1 };
  if (customerUser.value) {
    persistCartItem(product.id, 1, product, sku).catch(() => {});
  }
  const checkoutAddress = await ensureCheckoutAddress();
  if (!checkoutAddress) return;
  showCheckout.value = true;
}

async function syncCart() {
  const entries = Object.entries(cart.value);
  for (const [key, qty] of entries) {
    const lastQty = lastSyncedCart.value[key] || 0;
    const { productId, skuId } = parseCartKey(key);
    if (qty <= 0) {
      if (customerUser.value) {
        try { await persistCartItem(productId, 0, null, { id: skuId }); } catch { /* ignore */ }
      }
      delete cart.value[key];
      delete lastSyncedCart.value[key];
    } else if (qty !== lastQty && customerUser.value) {
      try {
        const product = products.value.find((p) => String(p.id) === String(productId));
        await persistCartItem(productId, qty, product, findSku(product, skuId));
        lastSyncedCart.value[key] = qty;
      } catch {
        showToast("同步失败");
      }
    }
  }
}

function resetFilters() {
  priceRange.value = "all";
  sortType.value = "featured";
  onlyFast.value = false;
  onlyDiscount.value = false;
}

function openCart() {
  if (!customerUser.value) {
    openAuth("login");
    showToast("请先登录");
    return;
  }
  showCart.value = true;
}

async function openCheckout() {
  if (cartRows.value.length === 0) {
    showToast("请先添加商品");
    return;
  }
  if (!customerUser.value) {
    openAuth("login");
    showToast("请先登录");
    return;
  }
  const merchantNos = uniqueCartMerchantNos();
  if (merchantNos.length === 0) {
    showToast("商品缺少商家归属，无法发起支付");
    return;
  }
  if (merchantNos.length > 1) {
    showToast("请先结算同一商家的商品");
    return;
  }
  showCart.value = false;
  const checkoutAddress = await ensureCheckoutAddress();
  if (!checkoutAddress) return;
  showCheckout.value = true;
}

async function ensureCheckoutAddress() {
  if (!customerUser.value) return null;
  if (addresses.value.length === 0) {
    await loadAddresses();
  } else {
    syncSelectedAddress();
  }
  if (!selectedAddress.value) {
    showToast("请先新增收货地址");
    showAddresses.value = true;
  }
  return selectedAddress.value;
}

function frontendResultUrl(orderNo) {
  const url = new URL("/pay-result", window.location.origin);
  url.searchParams.set("orderNo", orderNo || "");
  return url.toString();
}

function buildSubject() {
  const first = cartRows.value[0]?.product?.name || "商城订单";
  const count = cartRows.value.reduce((sum, row) => sum + row.qty, 0);
  return count > 1 ? `${first} 等 ${count} 件商品` : first;
}

function uniqueCartMerchantNos() {
  return [...new Set(
    cartRows.value
      .map((row) => row.product?.merchantNo)
      .filter((merchantNo) => merchantNo && String(merchantNo).trim())
      .map((merchantNo) => String(merchantNo).trim())
  )];
}

async function readResponseJson(response) {
  const text = await response.text();
  if (!response.ok) {
    try {
      const body = JSON.parse(text);
      throw new Error(body.message || body.error || text || `HTTP ${response.status}`);
    } catch (error) {
      if (error instanceof Error && !error.message.includes("Unexpected")) throw error;
      throw new Error(text || `HTTP ${response.status}`);
    }
  }
  return text ? JSON.parse(text) : {};
}

function parsePlatformResult(order) {
  if (!order?.rawResponse) return {};
  try {
    return JSON.parse(order.rawResponse);
  } catch {
    return {};
  }
}

async function placeOrder() {
  if (cartRows.value.length === 0) {
    showToast("请先添加商品");
    return;
  }

  payCreating.value = true;
  checkoutDone.value = false;
  createdOrder.value = null;

  try {
    const backend = normalizedBackendBaseUrl();
    const checkoutAddress = await ensureCheckoutAddress();
    if (!checkoutAddress) {
      throw new Error("请先新增收货地址");
    }
    const merchantNos = uniqueCartMerchantNos();
    if (merchantNos.length === 0) {
      throw new Error("商品缺少商家归属，无法发起支付");
    }
    if (merchantNos.length > 1) {
      throw new Error("请先结算同一商家的商品");
    }
    const merchantNo = merchantNos[0];
    const merchantName = cartRows.value[0]?.product?.merchantName || merchantNo;

    // 1. 创建商品订单
    const orderItems = cartRows.value.map((row) => ({
      productId: row.product.id,
      skuId: row.sku?.id || null,
      productCode: row.product.productCode || "",
      skuCode: row.sku?.skuCode || "",
      skuName: row.sku?.skuName || "",
      productName: row.product.name,
      productImage: row.product.image || "",
      unitPrice: row.sku?.price || row.product.price,
      quantity: row.qty
    }));
    const productOrderPayload = {
      merchantNo,
      merchantName,
      totalAmount: Number(cartTotal.value.total.toFixed(2)),
      discountAmount: Number(cartTotal.value.discount.toFixed(2)),
      items: orderItems,
      addressId: checkoutAddress.id
    };
    const poResponse = await fetch(`${backend}/api/mall/product-orders`, {
      method: "POST",
      headers: authHeaders(),
      body: JSON.stringify(productOrderPayload)
    });
    const productOrder = await readResponseJson(poResponse);

    // 2. 创建支付订单，关联商品订单
    const payload = {
      productOrderId: productOrder.id,
      merchantNo,
      totalAmount: Number(cartTotal.value.total.toFixed(2)),
      subject: buildSubject(),
      typeIndex: 1,
      goodsType: 1,
      payMethodType: "ALIPAY_CN",
      attachInfo: JSON.stringify({
        source: "mobile-storefront",
        merchantNo,
        itemCount: cartCount.value,
        productIds: cartRows.value.map((row) => row.product.id),
        skuIds: cartRows.value.map((row) => row.sku?.id || null)
      }),
      returnUrl: frontendResultUrl("")
    };

    const response = await fetch(`${backend}/api/mall/pay-orders`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...(customerToken.value ? { Authorization: `Bearer ${customerToken.value}` } : {})
      },
      body: JSON.stringify(payload)
    });
    const order = await readResponseJson(response);
    const platformResult = parsePlatformResult(order);

    if (platformResult.code !== undefined && platformResult.code !== 0) {
      throw new Error(platformResult.msg || platformResult.message || "平台创建支付订单失败");
    }

    createdOrder.value = order;
    localStorage.setItem(storageKeys.lastOrderNo, order.orderNo);
    // 下单成功清空购物车
    cart.value = {};
    lastSyncedCart.value = {};
    if (customerUser.value) {
      try {
        const backend = normalizedBackendBaseUrl();
        await fetch(`${backend}/api/mall/cart`, { method: "DELETE", headers: authHeaders() });
      } catch { /* 非关键 */ }
    }
    showCheckout.value = false;
    // 直接跳转到支付页面
    const payUrl = `${backendPublicBaseUrl()}/api/mall/pay-orders/${encodeURIComponent(order.orderNo)}/pay-page`;
    window.location.href = payUrl;
  } catch (error) {
    showToast(error instanceof Error ? error.message : String(error));
  } finally {
    payCreating.value = false;
  }
}

function openPayPage() {
  if (!createdOrder.value?.orderNo) {
    showToast("订单号不存在");
    return;
  }
  const payPageUrl = `${backendPublicBaseUrl()}/api/mall/pay-orders/${encodeURIComponent(createdOrder.value.orderNo)}/pay-page`;
  window.open(payPageUrl, "_blank", "noreferrer");
}

function openResultPage() {
  if (!createdOrder.value?.orderNo) {
    showToast("订单号不存在");
    return;
  }
  window.location.href = frontendResultUrl(createdOrder.value.orderNo);
}

async function loadPayResult() {
  if (!resultOrderNo) return;
  resultLoading.value = true;
  try {
    const backend = normalizedBackendBaseUrl();
    const response = await fetch(`${backend}/api/mall/pay-orders/${encodeURIComponent(resultOrderNo)}`, { headers: authHeaders() });
    payResult.value = await readResponseJson(response);
  } catch (error) {
    showToast(error instanceof Error ? error.message : String(error));
  } finally {
    resultLoading.value = false;
  }
}

function goHome() {
  window.location.href = "/";
}

// 立即购买模式下关闭结算弹窗时恢复原购物车
watch(showCheckout, (visible) => {
  if (!visible && savedCartBeforeBuyNow.value) {
    if (!checkoutDone.value) {
      cart.value = savedCartBeforeBuyNow.value;
      lastSyncedCart.value = { ...savedCartBeforeBuyNow.value };
    }
    savedCartBeforeBuyNow.value = null;
  }
});

onMounted(() => {
  if (isResultPage) {
    loadPayResult();
    timer = window.setInterval(loadPayResult, 5000);
  } else {
    loadCatalog();
    // Header 高度会受移动端地址栏、搜索框渲染和字体加载影响，分类吸顶位置需要重新测量。
    updateStickyHeaderOffset();
    scheduleStickyHeaderOffset();
    window.addEventListener("resize", scheduleStickyHeaderOffset);
    window.addEventListener("orientationchange", scheduleStickyHeaderOffset);
    window.addEventListener("load", scheduleStickyHeaderOffset);
    // 已登录用户加载服务端购物车
    if (customerToken.value && customerUser.value) {
      loadCartFromServer().then((saved) => {
        cart.value = saved;
        lastSyncedCart.value = { ...saved };
      }).catch(() => {});
    }
  }
});

onBeforeUnmount(() => {
  window.clearInterval(timer);
  window.cancelAnimationFrame(stickyOffsetFrame);
  window.removeEventListener("resize", scheduleStickyHeaderOffset);
  window.removeEventListener("orientationchange", scheduleStickyHeaderOffset);
  window.removeEventListener("load", scheduleStickyHeaderOffset);
});
</script>

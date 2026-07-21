<template>
	<view class="cart-page">
		<view v-if="!loggedIn" class="state-panel full-state">
			<view class="state-icon">人</view>
			<text class="state-title">登录后查看购物车</text>
			<text class="state-desc">登录账号后，购物车可以在不同设备间同步</text>
			<button class="primary-button" @click="openAuth">登录 / 注册</button>
		</view>

		<view v-else-if="loading" class="state-panel full-state">
			<view class="loading-dot"></view>
			<text class="state-desc">正在加载购物车...</text>
		</view>

		<view v-else-if="errorMessage" class="state-panel full-state">
			<text class="state-title">购物车加载失败</text>
			<text class="state-desc">{{ errorMessage }}</text>
			<button class="primary-button" @click="loadCart">重新加载</button>
		</view>

		<view v-else-if="cartItems.length === 0" class="state-panel full-state">
			<view class="state-icon warm">袋</view>
			<text class="state-title">购物车还是空的</text>
			<text class="state-desc">去逛逛精选商品，把喜欢的好物加入购物车</text>
			<button class="primary-button" @click="goShopping">去选购</button>
		</view>

		<template v-else>
			<view class="cart-header">
				<view>
					<text class="cart-title">购物车</text>
					<text class="cart-count">共 {{ totalQuantity }} 件商品</text>
				</view>
				<text class="clear-button" @click="confirmClear">清空</text>
			</view>

			<view class="cart-content">
				<view v-for="group in merchantGroups" :key="group.key" class="merchant-card">
					<view class="merchant-heading">
						<view class="merchant-mark">店</view>
						<text class="merchant-name">{{ group.name }}</text>
						<text class="merchant-count">{{ group.items.length }} 种商品</text>
					</view>

					<view v-for="item in group.items" :key="item.key" class="cart-item">
						<view class="product-main" @click="openProduct(item.productId)">
							<view class="product-visual">
								<image v-if="item.image && !failedImages[item.key]" class="product-image" :src="item.image" mode="aspectFill" @error="failedImages[item.key] = true" />
								<view v-else class="image-fallback"><text>{{ item.productName.slice(0, 2) }}</text></view>
							</view>
							<view class="product-copy">
								<text class="product-name">{{ item.productName }}</text>
								<text class="cart-merchant-name">{{ item.merchantName }}</text>
								<text class="cart-sku-name">规格：{{ item.skuName || '默认规格' }}</text>
								<text v-if="item.unavailable" class="unavailable-tip">商品或规格已下架</text>
							</view>
						</view>

						<view class="item-footer">
							<view class="price-row"><text class="price-symbol">¥</text><text class="price-value">{{ formatMoney(item.unitPrice) }}</text></view>
							<view class="item-actions">
								<text class="delete-button" @click="removeItem(item)">删除</text>
								<view class="stepper">
									<button class="stepper-button" :disabled="item.quantity <= 1 || updating[item.key]" @click="changeQuantity(item, -1)">−</button>
									<text class="quantity-value">{{ item.quantity }}</text>
									<button class="stepper-button" :disabled="item.quantity >= item.stock || item.unavailable || updating[item.key]" @click="changeQuantity(item, 1)">＋</button>
								</view>
							</view>
						</view>
					</view>
				</view>
			</view>

			<view class="checkout-bar">
				<view class="total-copy">
					<text class="total-label">合计</text>
					<view class="total-price"><text class="price-symbol">¥</text><text>{{ formatMoney(totalAmount) }}</text></view>
				</view>
				<button class="checkout-button" @click="checkout">去结算（{{ totalQuantity }}）</button>
			</view>
		</template>
	</view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { clearCart, fetchCart, saveCartItem } from '@/api/cart.js'
import { fetchProducts } from '@/api/catalog.js'
import { formatMoney, normalizeImageUrl, normalizeProduct } from '@/utils/catalog.js'
import { cartItemPayload, updateCartTabBadge } from '@/utils/cart.js'
import { getStoredToken } from '@/utils/auth.js'

const loggedIn = ref(false)
const loading = ref(false)
const errorMessage = ref('')
const cartItems = ref([])
const updating = reactive({})
const failedImages = reactive({})

const totalQuantity = computed(() => cartItems.value.reduce((sum, item) => sum + item.quantity, 0))
const totalAmount = computed(() => cartItems.value.reduce((sum, item) => sum + item.unitPrice * item.quantity, 0))
const merchantGroups = computed(() => {
	const groups = new Map()
	cartItems.value.forEach((item) => {
		const key = item.merchantNo || item.merchantName || 'merchant-default'
		if (!groups.has(key)) groups.set(key, { key, name: item.merchantName || '精选商家', items: [] })
		groups.get(key).items.push(item)
	})
	return [...groups.values()]
})

function enrichCartItems(cart, catalog) {
	const productMap = new Map(catalog.map((product) => [String(product.id), product]))
	return (cart?.items || []).map((snapshot) => {
		const product = productMap.get(String(snapshot.productId))
		const sku = product?.skus?.find((item) => String(item.id || '') === String(snapshot.skuId || ''))
		const unavailable = !product || (snapshot.skuId && !sku)
		return {
			key: `${snapshot.productId}:${snapshot.skuId || 'default'}`,
			id: snapshot.id,
			productId: snapshot.productId,
			skuId: snapshot.skuId || null,
			productCode: product?.productCode || snapshot.productCode || '',
			skuCode: sku?.skuCode || snapshot.skuCode || '',
			skuName: sku?.skuName || snapshot.skuName || '默认规格',
			productName: product?.name || snapshot.productName || '商品',
			image: product?.image || normalizeImageUrl(snapshot.productImage),
			merchantNo: product?.merchantNo || '',
			merchantName: product?.merchantName || '精选商家',
			unitPrice: Number(sku?.price ?? product?.price ?? snapshot.unitPrice ?? 0),
			stock: unavailable ? 0 : Number(sku?.stock ?? product?.stock ?? 0),
			quantity: Number(snapshot.quantity || 0),
			unavailable
		}
	})
}

async function loadCart(showLoading = true) {
	loggedIn.value = Boolean(getStoredToken())
	if (!loggedIn.value) {
		cartItems.value = []
		updateCartTabBadge({ items: [] })
		return
	}
	if (showLoading) loading.value = true
	errorMessage.value = ''
	try {
		const [cart, catalogData] = await Promise.all([fetchCart(), fetchProducts({ page: 0, size: 100 })])
		const catalog = (catalogData?.content || []).map(normalizeProduct)
		cartItems.value = enrichCartItems(cart, catalog)
		updateCartTabBadge(cart)
	} catch (error) {
		errorMessage.value = error instanceof Error ? error.message : '购物车加载失败'
		if (!getStoredToken()) loggedIn.value = false
	} finally {
		loading.value = false
	}
}

function openAuth() {
	uni.navigateTo({ url: '/pages/auth/auth?mode=login' })
}

function goShopping() {
	uni.switchTab({ url: '/pages/category/category' })
}

function openProduct(productId) {
	uni.navigateTo({ url: `/pages/product/detail?id=${encodeURIComponent(productId)}` })
}

function toPayloadProduct(item) {
	return {
		id: item.productId,
		productCode: item.productCode,
		name: item.productName,
		image: item.image,
		price: item.unitPrice
	}
}

function toPayloadSku(item) {
	return { id: item.skuId, skuCode: item.skuCode, skuName: item.skuName, price: item.unitPrice }
}

async function changeQuantity(item, step) {
	const nextQuantity = item.quantity + step
	if (nextQuantity < 1 || nextQuantity > item.stock || updating[item.key]) return
	await persistQuantity(item, nextQuantity)
}

async function persistQuantity(item, nextQuantity) {
	updating[item.key] = true
	const previous = item.quantity
	item.quantity = nextQuantity
	try {
		await saveCartItem(cartItemPayload(toPayloadProduct(item), toPayloadSku(item), nextQuantity))
		updateCartTabBadge({ items: cartItems.value })
	} catch (error) {
		item.quantity = previous
		uni.showToast({ title: error instanceof Error ? error.message : '购物车更新失败', icon: 'none' })
	} finally {
		updating[item.key] = false
	}
}

function removeItem(item) {
	uni.showModal({
		title: '删除商品',
		content: `确认从购物车删除“${item.productName}”吗？`,
		confirmText: '删除',
		confirmColor: '#d9480f',
		success: async ({ confirm }) => {
			if (!confirm) return
			updating[item.key] = true
			try {
				await saveCartItem(cartItemPayload(toPayloadProduct(item), toPayloadSku(item), 0))
				cartItems.value = cartItems.value.filter((row) => row.key !== item.key)
				updateCartTabBadge({ items: cartItems.value })
				uni.showToast({ title: '已删除', icon: 'success' })
			} catch (error) {
				uni.showToast({ title: error instanceof Error ? error.message : '删除失败', icon: 'none' })
			} finally {
				updating[item.key] = false
			}
		}
	})
}

function confirmClear() {
	uni.showModal({
		title: '清空购物车',
		content: '确认删除购物车中的全部商品吗？',
		confirmText: '清空',
		confirmColor: '#d9480f',
		success: async ({ confirm }) => {
			if (!confirm) return
			try {
				await clearCart()
				cartItems.value = []
				updateCartTabBadge({ items: [] })
				uni.showToast({ title: '购物车已清空', icon: 'success' })
			} catch (error) {
				uni.showToast({ title: error instanceof Error ? error.message : '清空失败', icon: 'none' })
			}
		}
	})
}

function checkout() {
	if (cartItems.value.some((item) => item.unavailable || item.quantity > item.stock)) {
		uni.showToast({ title: '请先处理已失效或库存不足的商品', icon: 'none' })
		return
	}
	if (merchantGroups.value.length === 1) {
		openCheckout(merchantGroups.value[0])
		return
	}
	uni.showActionSheet({
		itemList: merchantGroups.value.map((group) => `${group.name}（${group.items.length}种）`),
		success: ({ tapIndex }) => openCheckout(merchantGroups.value[tapIndex])
	})
}

function openCheckout(group) {
	if (!group) return
	uni.navigateTo({ url: `/pages/checkout/checkout?merchantNo=${encodeURIComponent(group.key)}` })
}

onShow(() => loadCart())

onPullDownRefresh(async () => {
	await loadCart(false)
	uni.stopPullDownRefresh()
})
</script>

<style scoped>
.cart-page { min-height: 100vh; padding-bottom: calc(130rpx + var(--window-bottom, 0px) + env(safe-area-inset-bottom)); background: #f3f5f7; color: #17202a; }
.cart-header { display: flex; align-items: flex-end; justify-content: space-between; padding: 26rpx 28rpx 22rpx; background: #f8faf9; }
.cart-title, .cart-count { display: block; }
.cart-title { font-size: 40rpx; font-weight: 800; }
.cart-count { margin-top: 7rpx; color: #8a949b; font-size: 21rpx; }
.clear-button { padding: 12rpx 0; color: #d9480f; font-size: 23rpx; }
.cart-content { padding: 0 22rpx 32rpx; }
.merchant-card { margin-top: 20rpx; overflow: hidden; border-radius: 28rpx; background: #fff; box-shadow: 0 8rpx 25rpx rgba(28, 48, 56, .05); }
.merchant-heading { display: flex; align-items: center; padding: 23rpx 25rpx; border-bottom: 1rpx solid #edf0f2; }
.merchant-mark { display: flex; align-items: center; justify-content: center; width: 44rpx; height: 44rpx; border-radius: 14rpx; background: #e5f2ef; color: #0f766e; font-size: 19rpx; font-weight: 800; }
.merchant-name { flex: 1; margin-left: 13rpx; color: #364149; font-size: 26rpx; font-weight: 800; word-break: break-all; }
.merchant-count { margin-left: 14rpx; color: #929ba2; font-size: 19rpx; }
.cart-item { padding: 24rpx; border-bottom: 1rpx solid #edf0f2; }
.cart-item:last-child { border-bottom: 0; }
.product-main { display: flex; align-items: flex-start; }
.product-visual { flex: 0 0 176rpx; width: 176rpx; height: 176rpx; overflow: hidden; border-radius: 22rpx; background: #e8edef; }
.product-image, .image-fallback { width: 100%; height: 100%; }
.image-fallback { display: flex; align-items: center; justify-content: center; color: #69757d; font-size: 28rpx; font-weight: 800; }
.product-copy { flex: 1; min-width: 0; margin-left: 20rpx; }
.product-name, .cart-merchant-name, .cart-sku-name, .unavailable-tip { display: block; word-break: break-all; }
.product-name { color: #26323a; font-size: 26rpx; font-weight: 800; line-height: 1.45; }
.cart-merchant-name { margin-top: 10rpx; color: #68747c; font-size: 21rpx; line-height: 1.45; }
.cart-sku-name { margin-top: 7rpx; color: #89939a; font-size: 21rpx; line-height: 1.5; }
.unavailable-tip { margin-top: 8rpx; color: #d9480f; font-size: 20rpx; }
.item-footer { display: flex; align-items: center; margin-top: 20rpx; }
.price-row { color: #d9480f; }
.price-symbol { font-size: 20rpx; font-weight: 800; }
.price-value { font-size: 32rpx; font-weight: 800; }
.item-actions { display: flex; align-items: center; margin-left: auto; }
.delete-button { margin-right: 20rpx; padding: 10rpx 0; color: #8c969d; font-size: 21rpx; }
.stepper { display: flex; align-items: center; overflow: hidden; border: 1rpx solid #dce2e5; border-radius: 16rpx; }
.stepper-button { width: 58rpx; height: 54rpx; padding: 0; border: 0; border-radius: 0; background: #f4f6f7; color: #344049; font-size: 26rpx; line-height: 54rpx; }
.stepper-button[disabled] { color: #c4cbd0; }
.quantity-value { width: 58rpx; color: #26323a; font-size: 23rpx; font-weight: 700; text-align: center; }
.checkout-bar { position: fixed; right: 0; bottom: var(--window-bottom, 0px); left: 0; z-index: 20; display: flex; align-items: center; padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom)); border-top: 1rpx solid #e5e9eb; background: rgba(255,255,255,.97); }
.total-copy { flex: 1; }
.total-label { color: #747f87; font-size: 20rpx; }
.total-price { display: inline-flex; align-items: baseline; margin-left: 10rpx; color: #d9480f; font-size: 36rpx; font-weight: 800; }
.checkout-button { flex: 0 0 320rpx; margin-left: 20rpx; border: 0; border-radius: 42rpx; background: #0f766e; color: #fff; font-size: 26rpx; font-weight: 800; line-height: 84rpx; }
.state-panel { display: flex; align-items: center; flex-direction: column; justify-content: center; padding: 70rpx 48rpx; text-align: center; }
.full-state { min-height: 70vh; }
.state-icon { display: flex; align-items: center; justify-content: center; width: 136rpx; height: 136rpx; margin-bottom: 30rpx; border-radius: 44rpx; background: #dff1ed; color: #0f766e; font-size: 42rpx; font-weight: 800; }
.state-icon.warm { border-radius: 50%; background: #fff0e3; color: #d9480f; }
.state-title { color: #17202a; font-size: 36rpx; font-weight: 800; }
.state-desc { max-width: 520rpx; margin-top: 17rpx; color: #7d8891; font-size: 25rpx; line-height: 1.65; }
.primary-button { margin-top: 38rpx; padding: 0 42rpx; border: 0; border-radius: 38rpx; background: #0f766e; color: #fff; font-size: 25rpx; line-height: 76rpx; }
.loading-dot { width: 38rpx; height: 38rpx; margin-bottom: 18rpx; border: 5rpx solid #d8e9e6; border-top-color: #0f766e; border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>

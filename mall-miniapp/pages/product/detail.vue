<template>
	<view class="detail-page">
		<view v-if="loading" class="state-panel full-state">
			<view class="loading-dot"></view>
			<text class="state-desc">正在加载商品...</text>
		</view>
		<view v-else-if="errorMessage" class="state-panel full-state">
			<text class="state-title">商品加载失败</text>
			<text class="state-desc">{{ errorMessage }}</text>
			<button class="retry-button" @click="loadProduct">重新加载</button>
		</view>

		<template v-else-if="product">
			<swiper class="image-swiper" circular :indicator-dots="product.images.length > 1" indicator-color="rgba(255,255,255,.55)" indicator-active-color="#0f766e">
				<swiper-item v-for="(image, index) in product.images" :key="image || index">
					<image v-if="image && !failedImages[index]" class="product-image" :src="image" mode="aspectFill" @error="failedImages[index] = true" />
					<view v-else class="image-fallback"><text>{{ product.name.slice(0, 2) }}</text></view>
				</swiper-item>
			</swiper>

			<view class="detail-content">
				<view class="product-card">
					<view class="price-row">
						<text class="price-symbol">¥</text>
						<text class="price-value">{{ formatMoney(selectedSku.price) }}</text>
						<text class="stock-text">库存 {{ selectedSku.stock }}</text>
					</view>
					<text class="product-name">{{ product.name }}</text>
					<view class="merchant-row">
						<view class="merchant-mark">店</view>
						<text class="merchant-name">{{ product.merchantName }}</text>
						<text class="merchant-label">商家直供</text>
					</view>
				</view>

				<view class="section-card">
					<view class="section-heading">
						<text class="section-title">选择规格</text>
						<text class="section-tip">已选：{{ selectedSku.skuName }}</text>
					</view>
					<view class="sku-list">
						<view
							v-for="sku in skuOptions"
							:key="sku.id || 'default'"
							:class="['sku-option', { active: selectedSkuId === sku.id, disabled: sku.stock <= 0 }]"
							@click="selectSku(sku)"
						>
							<view class="sku-copy">
								<text class="sku-name">{{ sku.skuName }}</text>
								<text class="sku-meta">库存 {{ sku.stock }}</text>
							</view>
							<text class="sku-price">¥{{ formatMoney(sku.price) }}</text>
						</view>
					</view>
				</view>

				<view class="section-card quantity-card">
					<view>
						<text class="section-title">购买数量</text>
						<text class="quantity-tip">单次最多购买 {{ maxQuantity }} 件</text>
					</view>
					<view class="stepper">
						<button class="stepper-button" :disabled="quantity <= 1" @click="changeQuantity(-1)">−</button>
						<text class="quantity-value">{{ quantity }}</text>
						<button class="stepper-button" :disabled="quantity >= maxQuantity" @click="changeQuantity(1)">＋</button>
					</view>
				</view>

				<view class="section-card description-card">
					<text class="section-title">商品说明</text>
					<text class="description-text">{{ product.desc || '商品详情以实际收到的商品为准。' }}</text>
				</view>
			</view>

			<view class="action-bar">
				<view class="cart-entry" @click="goCart">
					<text class="cart-icon">袋</text>
					<text class="cart-label">购物车</text>
				</view>
				<button class="add-button" :disabled="adding || selectedSku.stock <= 0" @click="addToCart">
					{{ selectedSku.stock <= 0 ? '暂时缺货' : (adding ? '正在加入...' : '加入购物车') }}
				</button>
			</view>
		</template>
	</view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { fetchProductById } from '@/api/catalog.js'
import { fetchCart, saveCartItem } from '@/api/cart.js'
import { formatMoney, normalizeProduct } from '@/utils/catalog.js'
import { cartItemPayload, updateCartTabBadge } from '@/utils/cart.js'
import { getStoredToken } from '@/utils/auth.js'

const productId = ref('')
const product = ref(null)
const selectedSkuId = ref(null)
const quantity = ref(1)
const loading = ref(false)
const adding = ref(false)
const errorMessage = ref('')
const failedImages = reactive({})

const skuOptions = computed(() => {
	if (product.value?.skus?.length) return product.value.skus
	return [{ id: null, skuCode: '', skuName: '默认规格', price: product.value?.price || 0, stock: product.value?.stock || 0 }]
})

const selectedSku = computed(() => skuOptions.value.find((sku) => sku.id === selectedSkuId.value) || skuOptions.value[0])
const maxQuantity = computed(() => Math.max(1, Math.min(Number(selectedSku.value?.stock || 0), 99)))

async function loadProduct() {
	loading.value = true
	errorMessage.value = ''
	try {
		product.value = normalizeProduct(await fetchProductById(productId.value))
		const firstAvailable = skuOptions.value.find((sku) => sku.stock > 0) || skuOptions.value[0]
		selectedSkuId.value = firstAvailable?.id ?? null
		quantity.value = 1
		uni.setNavigationBarTitle({ title: product.value.name })
	} catch (error) {
		errorMessage.value = error instanceof Error ? error.message : '商品加载失败'
	} finally {
		loading.value = false
	}
}

function selectSku(sku) {
	if (sku.stock <= 0) return
	selectedSkuId.value = sku.id
	quantity.value = Math.min(quantity.value, Math.max(1, Number(sku.stock)))
}

function changeQuantity(step) {
	quantity.value = Math.min(maxQuantity.value, Math.max(1, quantity.value + step))
}

function goCart() {
	uni.switchTab({ url: '/pages/cart/cart' })
}

async function addToCart() {
	if (!getStoredToken()) {
		uni.showToast({ title: '请先登录后加入购物车', icon: 'none' })
		setTimeout(() => uni.navigateTo({ url: '/pages/auth/auth?mode=login' }), 350)
		return
	}
	if (selectedSku.value.stock <= 0) return

	adding.value = true
	try {
		const cart = await fetchCart()
		const existing = (cart?.items || []).find((item) =>
			String(item.productId) === String(product.value.id) &&
			String(item.skuId || '') === String(selectedSku.value.id || '')
		)
		const nextQuantity = Number(existing?.quantity || 0) + quantity.value
		if (nextQuantity > selectedSku.value.stock) throw new Error(`库存仅剩 ${selectedSku.value.stock} 件`)
		await saveCartItem(cartItemPayload(product.value, selectedSku.value, nextQuantity))
		const nextCart = await fetchCart()
		updateCartTabBadge(nextCart)
		uni.showToast({ title: '已加入购物车', icon: 'success' })
	} catch (error) {
		uni.showToast({ title: error instanceof Error ? error.message : '加入购物车失败', icon: 'none' })
	} finally {
		adding.value = false
	}
}

onLoad((options) => {
	productId.value = options?.id || ''
	if (!productId.value) {
		errorMessage.value = '缺少商品编号'
		return
	}
	loadProduct()
})
</script>

<style scoped>
.detail-page { min-height: 100vh; padding-bottom: calc(126rpx + env(safe-area-inset-bottom)); background: #f3f5f7; color: #17202a; }
.image-swiper { height: 720rpx; background: #e5eaed; }
.product-image, .image-fallback { width: 100%; height: 100%; }
.image-fallback { display: flex; align-items: center; justify-content: center; color: #68757e; font-size: 46rpx; font-weight: 800; }
.detail-content { padding: 20rpx 22rpx 34rpx; }
.product-card, .section-card { padding: 28rpx; border-radius: 28rpx; background: #fff; box-shadow: 0 8rpx 24rpx rgba(27, 46, 55, .05); }
.section-card { margin-top: 20rpx; }
.price-row { display: flex; align-items: baseline; color: #d9480f; }
.price-symbol { font-size: 26rpx; font-weight: 800; }
.price-value { font-size: 48rpx; font-weight: 800; }
.stock-text { margin-left: auto; color: #8e989f; font-size: 22rpx; }
.product-name { display: block; margin-top: 17rpx; color: #202b33; font-size: 36rpx; font-weight: 800; line-height: 1.45; word-break: break-all; }
.merchant-row { display: flex; align-items: center; margin-top: 24rpx; padding-top: 22rpx; border-top: 1rpx solid #edf0f2; }
.merchant-mark { display: flex; align-items: center; justify-content: center; width: 48rpx; height: 48rpx; border-radius: 15rpx; background: #e5f2ef; color: #0f766e; font-size: 20rpx; font-weight: 800; }
.merchant-name { flex: 1; margin-left: 14rpx; color: #455159; font-size: 25rpx; font-weight: 700; word-break: break-all; }
.merchant-label { margin-left: 12rpx; color: #0f766e; font-size: 20rpx; }
.section-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 20rpx; }
.section-title { color: #26323a; font-size: 29rpx; font-weight: 800; }
.section-tip { max-width: 440rpx; color: #7d8890; font-size: 21rpx; line-height: 1.45; text-align: right; word-break: break-all; }
.sku-list { display: flex; flex-direction: column; gap: 16rpx; margin-top: 24rpx; }
.sku-option { display: flex; align-items: center; padding: 22rpx; border: 2rpx solid #e4e8ea; border-radius: 22rpx; background: #fafbfb; }
.sku-option.active { border-color: #0f766e; background: #edf7f5; }
.sku-option.disabled { opacity: .42; }
.sku-copy { flex: 1; min-width: 0; }
.sku-name, .sku-meta { display: block; }
.sku-name { color: #344049; font-size: 25rpx; font-weight: 700; line-height: 1.5; word-break: break-all; }
.sku-meta { margin-top: 7rpx; color: #939ca2; font-size: 20rpx; }
.sku-price { margin-left: 20rpx; color: #d9480f; font-size: 25rpx; font-weight: 800; }
.quantity-card { display: flex; align-items: center; justify-content: space-between; }
.quantity-tip { display: block; margin-top: 8rpx; color: #929ba2; font-size: 20rpx; }
.stepper { display: flex; align-items: center; overflow: hidden; border: 1rpx solid #dce2e5; border-radius: 18rpx; }
.stepper-button { width: 66rpx; height: 62rpx; padding: 0; border: 0; border-radius: 0; background: #f4f6f7; color: #344049; font-size: 30rpx; line-height: 62rpx; }
.stepper-button[disabled] { color: #c5cbd0; }
.quantity-value { width: 66rpx; color: #253139; font-size: 25rpx; font-weight: 700; text-align: center; }
.description-text { display: block; margin-top: 20rpx; color: #66727a; font-size: 25rpx; line-height: 1.75; word-break: break-all; }
.action-bar { position: fixed; right: 0; bottom: 0; left: 0; z-index: 20; display: flex; align-items: center; gap: 20rpx; padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom)); border-top: 1rpx solid #e5e9eb; background: rgba(255,255,255,.97); }
.cart-entry { display: flex; align-items: center; flex: 0 0 100rpx; flex-direction: column; }
.cart-icon { color: #0f766e; font-size: 28rpx; font-weight: 800; }
.cart-label { margin-top: 5rpx; color: #66727a; font-size: 19rpx; }
.add-button { flex: 1; border: 0; border-radius: 43rpx; background: #0f766e; color: #fff; font-size: 28rpx; font-weight: 800; line-height: 86rpx; }
.add-button[disabled] { background: #aab6b4; }
.state-panel { display: flex; align-items: center; flex-direction: column; justify-content: center; padding: 70rpx 36rpx; text-align: center; }
.full-state { min-height: 70vh; }
.state-title { color: #26323a; font-size: 30rpx; font-weight: 800; }
.state-desc { margin-top: 14rpx; color: #879199; font-size: 24rpx; line-height: 1.55; }
.retry-button { margin-top: 28rpx; padding: 0 36rpx; border: 0; border-radius: 34rpx; background: #0f766e; color: #fff; font-size: 23rpx; line-height: 66rpx; }
.loading-dot { width: 38rpx; height: 38rpx; margin-bottom: 18rpx; border: 5rpx solid #d8e9e6; border-top-color: #0f766e; border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>

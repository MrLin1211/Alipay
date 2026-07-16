<template>
	<view class="home-page">
		<view class="home-header">
			<view class="brand-row">
				<view>
					<text class="eyebrow">品质生活 · 每日优选</text>
					<text class="brand-title">商城</text>
				</view>
			</view>
		</view>

		<view class="page-content">
			<view class="hero-card">
				<view class="hero-orb hero-orb-one"></view>
				<view class="hero-orb hero-orb-two"></view>
				<view class="hero-copy">
					<text class="hero-label">SUMMER PICKS</text>
					<text class="hero-title">全品类好物\n一站购齐</text>
					<text class="hero-subtitle">精选商家直供 · 放心选购</text>
					<view class="hero-button" @click="goCategory('all')">
						<text>立即选购</text>
						<text class="hero-button-arrow">›</text>
					</view>
				</view>
				<view class="hero-badge">
					<text class="hero-badge-main">48h</text>
					<text class="hero-badge-text">快速履约</text>
				</view>
			</view>

			<view class="category-panel">
				<view
					v-for="category in categories"
					:key="category.name"
					class="category-item"
					@click="goCategory(category.id)"
				>
					<view class="category-icon" :style="{ background: category.background }">
						<text>{{ category.icon }}</text>
					</view>
					<text class="category-name">{{ category.name }}</text>
				</view>
			</view>

			<view class="benefit-strip">
				<view v-for="benefit in benefits" :key="benefit.title" class="benefit-item">
					<text class="benefit-icon">{{ benefit.icon }}</text>
					<view>
						<text class="benefit-title">{{ benefit.title }}</text>
						<text class="benefit-desc">{{ benefit.desc }}</text>
					</view>
				</view>
			</view>

			<view class="section-heading">
				<view>
					<text class="section-kicker">RECOMMENDED</text>
					<text class="section-title">为你推荐</text>
				</view>
				<view class="section-more" @click="goCategory('all')">
					<text>查看全部</text>
					<text>›</text>
				</view>
			</view>

			<view v-if="loading" class="state-panel compact">
				<view class="loading-dot"></view>
				<text>正在加载商品...</text>
			</view>
			<view v-else-if="errorMessage" class="state-panel compact">
				<text class="state-title">商品加载失败</text>
				<text class="state-desc">{{ errorMessage }}</text>
				<button class="retry-button" @click="loadCatalog">重新加载</button>
			</view>
			<view v-else-if="products.length === 0" class="state-panel compact">
				<text class="state-title">暂时没有在售商品</text>
				<text class="state-desc">商家上架商品后会在这里展示</text>
			</view>
			<view v-else class="goods-grid">
				<view v-for="product in products" :key="product.id" class="goods-card" @click="openProduct(product)">
					<view class="goods-visual">
						<image v-if="product.image && !failedImages[product.id]" class="goods-image" :src="product.image" mode="aspectFill" @error="markImageFailed(product.id)" />
						<view v-else class="goods-image-fallback"><text>{{ product.name.slice(0, 2) }}</text></view>
						<text v-if="product.fast" class="goods-badge">极速达</text>
					</view>
					<view class="goods-body">
						<text class="goods-name">{{ product.name }}</text>
						<text class="goods-merchant">{{ product.merchantName }}</text>
						<view class="price-row">
							<text class="price-symbol">¥</text>
							<text class="price-value">{{ formatMoney(product.price) }}</text>
							<text class="stock-text">库存 {{ product.stock }}</text>
						</view>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app'
import { fetchCategories, fetchProducts } from '@/api/catalog.js'
import { formatMoney, normalizeCategory, normalizeProduct } from '@/utils/catalog.js'

const categories = ref([])
const products = ref([])
const loading = ref(false)
const errorMessage = ref('')
const failedImages = reactive({})

const benefits = [
	{ icon: '✓', title: '品质保障', desc: '精选正品' },
	{ icon: '↯', title: '快速发货', desc: '高效履约' },
	{ icon: '安', title: '售后无忧', desc: '放心购买' }
]

async function loadCatalog(showLoading = true) {
	if (showLoading) loading.value = true
	errorMessage.value = ''
	try {
		const [categoryData, productData] = await Promise.all([
			fetchCategories(),
			fetchProducts({ page: 0, size: 6 })
		])
		categories.value = (Array.isArray(categoryData) ? categoryData : []).slice(0, 5).map(normalizeCategory)
		products.value = (productData?.content || []).map(normalizeProduct)
	} catch (error) {
		errorMessage.value = error instanceof Error ? error.message : '网络连接失败，请稍后重试'
	} finally {
		loading.value = false
	}
}

function goCategory(categoryId = 'all') {
	const normalizedCategoryId = typeof categoryId === 'string' && categoryId ? categoryId : 'all'
	uni.setStorageSync('mall_selected_category', normalizedCategoryId)
	uni.switchTab({ url: '/pages/category/category' })
}

function openProduct(product) {
	uni.navigateTo({ url: `/pages/product/detail?id=${encodeURIComponent(product.id)}` })
}

function markImageFailed(productId) {
	failedImages[productId] = true
}

onLoad(() => loadCatalog())

onPullDownRefresh(async () => {
	await loadCatalog(false)
	uni.stopPullDownRefresh()
})
</script>

<style scoped>
.home-page { min-height: 100vh; background: #f3f5f7; color: #17202a; }
.home-header { padding: calc(28rpx + env(safe-area-inset-top)) 28rpx 22rpx; background: linear-gradient(180deg, #f8faf9 0%, #f3f5f7 100%); }
.brand-row { display: flex; align-items: center; justify-content: space-between; }
.eyebrow { display: block; margin-bottom: 5rpx; color: #d9480f; font-size: 20rpx; font-weight: 700; letter-spacing: 2rpx; }
.brand-title { display: block; font-size: 48rpx; font-weight: 800; line-height: 1.1; }
.page-content { padding: 0 24rpx 38rpx; }
.hero-card { position: relative; height: 330rpx; overflow: hidden; padding: 38rpx; border-radius: 32rpx; background: linear-gradient(135deg, #0a5f5a 0%, #0f8178 55%, #17a193 100%); box-shadow: 0 18rpx 40rpx rgba(15, 118, 110, .22); }
.hero-orb { position: absolute; border-radius: 50%; background: rgba(255,255,255,.08); }
.hero-orb-one { top: -90rpx; right: -40rpx; width: 300rpx; height: 300rpx; }
.hero-orb-two { right: 130rpx; bottom: -170rpx; width: 360rpx; height: 360rpx; }
.hero-copy { position: relative; z-index: 2; display: flex; align-items: flex-start; flex-direction: column; }
.hero-label { margin-bottom: 12rpx; color: #facc15; font-size: 20rpx; font-weight: 800; letter-spacing: 3rpx; }
.hero-title { color: #fff; font-size: 48rpx; font-weight: 800; line-height: 1.22; }
.hero-subtitle { margin-top: 10rpx; color: rgba(255,255,255,.74); font-size: 23rpx; }
.hero-button { display: flex; align-items: center; gap: 12rpx; margin-top: 24rpx; padding: 14rpx 24rpx; border-radius: 28rpx; background: #fff; color: #0f766e; font-size: 24rpx; font-weight: 700; }
.hero-button-arrow { font-size: 34rpx; line-height: 1; }
.hero-badge { position: absolute; right: 30rpx; bottom: 30rpx; z-index: 2; display: flex; align-items: center; flex-direction: column; justify-content: center; width: 124rpx; height: 124rpx; border: 1rpx solid rgba(255,255,255,.24); border-radius: 28rpx; background: rgba(255,255,255,.12); }
.hero-badge-main { color: #fff; font-size: 34rpx; font-weight: 800; }
.hero-badge-text { margin-top: 4rpx; color: rgba(255,255,255,.76); font-size: 20rpx; }
.category-panel { display: grid; grid-template-columns: repeat(5, 1fr); gap: 8rpx; margin-top: 26rpx; padding: 28rpx 14rpx 24rpx; border-radius: 28rpx; background: #fff; box-shadow: 0 10rpx 30rpx rgba(30, 50, 60, .06); }
.category-item { display: flex; align-items: center; flex-direction: column; }
.category-icon { display: flex; align-items: center; justify-content: center; width: 82rpx; height: 82rpx; margin-bottom: 12rpx; border-radius: 26rpx; color: #315e59; font-size: 28rpx; font-weight: 800; }
.category-name { color: #37424b; font-size: 23rpx; font-weight: 600; }
.benefit-strip { display: grid; grid-template-columns: repeat(3, 1fr); margin-top: 22rpx; padding: 22rpx 10rpx; border-radius: 24rpx; background: #fff8f0; }
.benefit-item { display: flex; align-items: center; justify-content: center; gap: 10rpx; border-right: 1rpx solid #f0dfcf; }
.benefit-item:last-child { border-right: 0; }
.benefit-icon { display: flex; align-items: center; justify-content: center; width: 36rpx; height: 36rpx; border-radius: 50%; background: #0f766e; color: #fff; font-size: 20rpx; font-weight: 800; }
.benefit-title, .benefit-desc { display: block; }
.benefit-title { color: #39434b; font-size: 21rpx; font-weight: 700; }
.benefit-desc { margin-top: 3rpx; color: #92999e; font-size: 17rpx; }
.section-heading { display: flex; align-items: flex-end; justify-content: space-between; margin: 38rpx 4rpx 22rpx; }
.section-kicker { display: block; margin-bottom: 5rpx; color: #d9480f; font-size: 18rpx; font-weight: 800; letter-spacing: 2rpx; }
.section-title { display: block; font-size: 36rpx; font-weight: 800; }
.section-more { display: flex; align-items: center; gap: 8rpx; padding-bottom: 4rpx; color: #75808a; font-size: 23rpx; }
.goods-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18rpx; }
.goods-card { overflow: hidden; border-radius: 26rpx; background: #fff; box-shadow: 0 10rpx 26rpx rgba(27, 46, 55, .06); }
.goods-visual { position: relative; height: 250rpx; overflow: hidden; background: linear-gradient(145deg, #e7ecef, #d3dce1); }
.goods-image { width: 100%; height: 100%; }
.goods-image-fallback { display: flex; align-items: center; justify-content: center; width: 100%; height: 100%; color: #627079; font-size: 34rpx; font-weight: 800; }
.goods-badge { position: absolute; top: 16rpx; left: 16rpx; padding: 6rpx 13rpx; border-radius: 18rpx; background: #d9480f; color: #fff; font-size: 18rpx; font-weight: 700; }
.goods-body { padding: 20rpx; }
.goods-name { display: -webkit-box; min-height: 70rpx; overflow: hidden; color: #222d35; font-size: 25rpx; font-weight: 700; line-height: 1.4; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.goods-merchant { display: block; margin-top: 7rpx; overflow: hidden; color: #8a949b; font-size: 20rpx; text-overflow: ellipsis; white-space: nowrap; }
.price-row { display: flex; align-items: baseline; margin-top: 14rpx; color: #d9480f; }
.price-symbol { font-size: 21rpx; font-weight: 800; }
.price-value { font-size: 34rpx; font-weight: 800; }
.stock-text { margin-left: auto; color: #929ba1; font-size: 18rpx; font-weight: 400; }
.state-panel { display: flex; align-items: center; flex-direction: column; justify-content: center; padding: 70rpx 34rpx; border-radius: 26rpx; background: #fff; text-align: center; }
.state-panel.compact { min-height: 230rpx; }
.state-title { color: #26323a; font-size: 28rpx; font-weight: 800; }
.state-desc { max-width: 520rpx; margin-top: 12rpx; color: #879199; font-size: 23rpx; line-height: 1.55; }
.retry-button { margin-top: 24rpx; padding: 0 34rpx; border: 0; border-radius: 34rpx; background: #0f766e; color: #fff; font-size: 23rpx; line-height: 64rpx; }
.loading-dot { width: 34rpx; height: 34rpx; margin-bottom: 18rpx; border: 5rpx solid #d8e9e6; border-top-color: #0f766e; border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>

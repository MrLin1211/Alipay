<template>
	<view class="catalog-page">
		<view class="search-wrap">
			<view class="search-box">
				<text class="search-icon">⌕</text>
				<input
					v-model="keyword"
					class="search-input"
					confirm-type="search"
					placeholder="搜索商品名称"
					placeholder-class="search-placeholder"
					@confirm="searchProducts"
				/>
				<text v-if="keyword" class="clear-button" @click="clearSearch">×</text>
			</view>
		</view>

		<scroll-view class="category-scroll" scroll-x :show-scrollbar="false">
			<view class="category-list">
				<view
					v-for="category in categories"
					:key="category.id"
					:class="['category-pill', { active: activeCategory === category.id }]"
					@click="selectCategory(category.id)"
				>
					<text>{{ category.name }}</text>
				</view>
			</view>
		</scroll-view>

		<view class="catalog-content">
			<view class="catalog-heading">
				<view>
					<text class="heading-kicker">CATALOG</text>
					<text class="heading-title">{{ activeCategoryName }}</text>
				</view>
				<text class="goods-count">{{ total }} 件商品</text>
			</view>

			<view v-if="loading && products.length === 0" class="state-panel">
				<view class="loading-dot"></view>
				<text class="state-desc">正在加载商品...</text>
			</view>
			<view v-else-if="errorMessage && products.length === 0" class="state-panel">
				<text class="state-title">加载失败</text>
				<text class="state-desc">{{ errorMessage }}</text>
				<button class="retry-button" @click="loadProducts(true)">重新加载</button>
			</view>
			<view v-else-if="products.length === 0" class="state-panel">
				<text class="state-icon">空</text>
				<text class="state-title">没有找到商品</text>
				<text class="state-desc">换个分类或搜索词试试看</text>
				<button v-if="keyword || activeCategory !== 'all'" class="retry-button" @click="resetFilters">查看全部商品</button>
			</view>

			<view v-else class="goods-list">
				<view v-for="product in products" :key="product.id" class="goods-card" @click="openProduct(product)">
					<view class="goods-visual">
						<image v-if="product.image && !failedImages[product.id]" class="goods-image" :src="product.image" mode="aspectFill" @error="markImageFailed(product.id)" />
						<view v-else class="goods-image-fallback"><text>{{ product.name.slice(0, 2) }}</text></view>
						<text v-if="product.fast" class="goods-badge">极速达</text>
					</view>
					<view class="goods-body">
						<text class="goods-name">{{ product.name }}</text>
						<text class="goods-merchant">{{ product.merchantName }}</text>
						<text class="goods-sku">{{ product.skus.length > 1 ? `${product.skus.length} 种规格可选` : (product.skus[0]?.skuName || '默认规格') }}</text>
						<view class="goods-footer">
							<view class="price-row"><text class="price-symbol">¥</text><text class="price-value">{{ formatMoney(product.price) }}</text></view>
							<text class="stock-text">库存 {{ product.stock }}</text>
						</view>
					</view>
				</view>
			</view>

			<view v-if="products.length > 0" class="load-more">
				<text v-if="loading">正在加载更多...</text>
				<text v-else-if="hasMore">继续上拉加载</text>
				<text v-else>已经到底了</text>
			</view>
		</view>
	</view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { fetchCategories, fetchProducts } from '@/api/catalog.js'
import { formatMoney, normalizeCategory, normalizeProduct } from '@/utils/catalog.js'

const PAGE_SIZE = 20
const categories = ref([{ id: 'all', name: '全部' }])
const activeCategory = ref('all')
const keyword = ref('')
const products = ref([])
const page = ref(0)
const total = ref(0)
const loading = ref(false)
const errorMessage = ref('')
const failedImages = reactive({})

const activeCategoryName = computed(() => categories.value.find((item) => item.id === activeCategory.value)?.name || '全部商品')
const hasMore = computed(() => products.value.length < total.value)

async function loadCategories() {
	try {
		const data = await fetchCategories()
		categories.value = [
			{ id: 'all', name: '全部' },
			...(Array.isArray(data) ? data.map(normalizeCategory) : [])
		]
	} catch (error) {
		if (!errorMessage.value) errorMessage.value = error instanceof Error ? error.message : '分类加载失败'
	}
}

async function loadProducts(reset = false) {
	if (loading.value) return
	if (!reset && !hasMore.value) return
	if (reset) {
		page.value = 0
		products.value = []
		total.value = 0
	}
	loading.value = true
	errorMessage.value = ''
	try {
		const data = await fetchProducts({
			category: activeCategory.value,
			keyword: keyword.value.trim(),
			page: page.value,
			size: PAGE_SIZE
		})
		const nextProducts = (data?.content || []).map(normalizeProduct)
		products.value = reset ? nextProducts : [...products.value, ...nextProducts]
		total.value = Number(data?.totalElements || products.value.length)
		page.value += 1
	} catch (error) {
		errorMessage.value = error instanceof Error ? error.message : '商品加载失败，请稍后重试'
	} finally {
		loading.value = false
	}
}

async function refreshCatalog() {
	await Promise.all([loadCategories(), loadProducts(true)])
}

function selectCategory(categoryId) {
	if (activeCategory.value === categoryId) return
	activeCategory.value = categoryId
	uni.setStorageSync('mall_selected_category', categoryId)
	loadProducts(true)
}

function searchProducts() {
	uni.removeStorageSync('mall_catalog_keyword')
	loadProducts(true)
}

function clearSearch() {
	keyword.value = ''
	loadProducts(true)
}

function resetFilters() {
	keyword.value = ''
	activeCategory.value = 'all'
	uni.setStorageSync('mall_selected_category', 'all')
	loadProducts(true)
}

function openProduct(product) {
	uni.navigateTo({ url: `/pages/product/detail?id=${encodeURIComponent(product.id)}` })
}

function markImageFailed(productId) {
	failedImages[productId] = true
}

onLoad(() => loadCategories())

onShow(() => {
	const storedValue = uni.getStorageSync('mall_selected_category')
	const storedCategory = typeof storedValue === 'string' && storedValue ? storedValue : 'all'
	const storedKeyword = uni.getStorageSync('mall_catalog_keyword') || ''
	uni.removeStorageSync('mall_catalog_keyword')
	activeCategory.value = storedCategory
	keyword.value = storedKeyword
	loadProducts(true)
})

onReachBottom(() => loadProducts(false))

onPullDownRefresh(async () => {
	await refreshCatalog()
	uni.stopPullDownRefresh()
})
</script>

<style scoped>
.catalog-page { min-height: 100vh; background: #f3f5f7; color: #17202a; }
.search-wrap { padding: 20rpx 24rpx 16rpx; background: #f8faf9; }
.search-box { display: flex; align-items: center; height: 74rpx; padding: 0 24rpx; border: 1rpx solid #e2e7ea; border-radius: 38rpx; background: #fff; }
.search-icon { margin-right: 14rpx; color: #65727b; font-size: 40rpx; transform: rotate(-20deg); }
.search-input { flex: 1; height: 70rpx; color: #303b43; font-size: 26rpx; }
.search-placeholder { color: #929ba2; }
.clear-button { padding: 10rpx; color: #9aa3aa; font-size: 36rpx; }
.category-scroll { width: 100%; padding: 0 0 18rpx; background: #f8faf9; white-space: nowrap; }
.category-list { display: flex; align-items: center; flex-direction: row; width: max-content; padding: 0 24rpx; }
.category-pill { flex: 0 0 auto; flex-shrink: 0; margin-right: 14rpx; padding: 13rpx 26rpx; border: 1rpx solid #e0e5e8; border-radius: 30rpx; background: #fff; color: #6d7880; font-size: 24rpx; line-height: 1.4; text-align: center; white-space: nowrap; }
.category-pill:last-child { margin-right: 0; }
.category-pill.active { border-color: #0f766e; background: #0f766e; color: #fff; font-weight: 700; }
/* #ifdef APP */
.category-list { min-width: 100%; }
.category-pill { min-width: 112rpx; }
/* #endif */
.catalog-content { padding: 28rpx 24rpx 40rpx; }
.catalog-heading { display: flex; align-items: flex-end; justify-content: space-between; margin: 0 4rpx 22rpx; }
.heading-kicker { display: block; color: #d9480f; font-size: 18rpx; font-weight: 800; letter-spacing: 2rpx; }
.heading-title { display: block; margin-top: 6rpx; font-size: 36rpx; font-weight: 800; }
.goods-count { padding-bottom: 4rpx; color: #8a949b; font-size: 22rpx; }
.goods-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18rpx; }
.goods-card { overflow: hidden; border-radius: 26rpx; background: #fff; box-shadow: 0 10rpx 26rpx rgba(27, 46, 55, .06); }
.goods-visual { position: relative; height: 250rpx; overflow: hidden; background: linear-gradient(145deg, #e7ecef, #d3dce1); }
.goods-image { width: 100%; height: 100%; }
.goods-image-fallback { display: flex; align-items: center; justify-content: center; width: 100%; height: 100%; color: #627079; font-size: 34rpx; font-weight: 800; }
.goods-badge { position: absolute; top: 16rpx; left: 16rpx; padding: 6rpx 13rpx; border-radius: 18rpx; background: #0f766e; color: #fff; font-size: 18rpx; font-weight: 700; }
.goods-body { padding: 19rpx; }
.goods-name { display: block; min-height: 70rpx; color: #222d35; font-size: 25rpx; font-weight: 700; line-height: 1.4; word-break: break-all; }
.goods-merchant { display: block; margin-top: 8rpx; color: #7d8890; font-size: 20rpx; line-height: 1.4; word-break: break-all; }
.goods-sku { display: block; margin-top: 7rpx; color: #9aa2a8; font-size: 19rpx; line-height: 1.45; word-break: break-all; }
.goods-footer { display: flex; align-items: baseline; margin-top: 14rpx; }
.price-row { color: #d9480f; }
.price-symbol { font-size: 20rpx; font-weight: 800; }
.price-value { font-size: 33rpx; font-weight: 800; }
.stock-text { margin-left: auto; color: #929ba1; font-size: 18rpx; }
.state-panel { display: flex; align-items: center; flex-direction: column; justify-content: center; min-height: 400rpx; padding: 60rpx 36rpx; border-radius: 26rpx; background: #fff; text-align: center; }
.state-icon { display: flex; align-items: center; justify-content: center; width: 110rpx; height: 110rpx; margin-bottom: 24rpx; border-radius: 34rpx; background: #e6f2ef; color: #0f766e; font-size: 34rpx; font-weight: 800; }
.state-title { color: #26323a; font-size: 30rpx; font-weight: 800; }
.state-desc { max-width: 520rpx; margin-top: 14rpx; color: #879199; font-size: 24rpx; line-height: 1.55; }
.retry-button { margin-top: 28rpx; padding: 0 36rpx; border: 0; border-radius: 34rpx; background: #0f766e; color: #fff; font-size: 23rpx; line-height: 66rpx; }
.loading-dot { width: 38rpx; height: 38rpx; margin-bottom: 18rpx; border: 5rpx solid #d8e9e6; border-top-color: #0f766e; border-radius: 50%; animation: spin .8s linear infinite; }
.load-more { padding: 32rpx 0 8rpx; color: #939ba1; font-size: 22rpx; text-align: center; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>

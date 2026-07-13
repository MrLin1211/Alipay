<template>
	<view class="home-page">
		<view class="home-header">
			<view class="brand-row">
				<view>
					<text class="eyebrow">品质生活 · 每日优选</text>
					<text class="brand-title">商城</text>
				</view>
				<view class="header-action" @click="goProfile">
					<text class="header-action-icon">人</text>
				</view>
			</view>

			<view class="search-box" @click="openSearch">
				<text class="search-icon">⌕</text>
				<text class="search-placeholder">搜索手机、家电、生鲜、运动...</text>
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
					<view class="hero-button" @click="goCategory">
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
					@click="goCategory"
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
				<view class="section-more" @click="goCategory">
					<text>查看全部</text>
					<text>›</text>
				</view>
			</view>

			<view class="goods-grid">
				<view v-for="product in products" :key="product.id" class="goods-card" @click="openProduct(product)">
					<view class="goods-visual" :style="{ background: product.background }">
						<text class="goods-visual-icon">{{ product.icon }}</text>
						<text v-if="product.badge" class="goods-badge">{{ product.badge }}</text>
					</view>
					<view class="goods-body">
						<text class="goods-name">{{ product.name }}</text>
						<text class="goods-merchant">{{ product.merchant }}</text>
						<view class="price-row">
							<text class="price-symbol">¥</text>
							<text class="price-value">{{ product.price }}</text>
							<text class="price-origin">¥{{ product.originPrice }}</text>
						</view>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script setup>
const categories = [
	{ name: '数码', icon: '⌁', background: 'linear-gradient(135deg, #e8f7f4, #ccece7)' },
	{ name: '家电', icon: '⌂', background: 'linear-gradient(135deg, #fff3e6, #ffe0bd)' },
	{ name: '生鲜', icon: '叶', background: 'linear-gradient(135deg, #eef8e8, #d7edca)' },
	{ name: '美妆', icon: '✦', background: 'linear-gradient(135deg, #fff0f2, #ffd8dd)' },
	{ name: '运动', icon: '跑', background: 'linear-gradient(135deg, #eef1ff, #d8defd)' }
]

const benefits = [
	{ icon: '✓', title: '品质保障', desc: '精选正品' },
	{ icon: '↯', title: '快速发货', desc: '高效履约' },
	{ icon: '安', title: '售后无忧', desc: '放心购买' }
]

const products = [
	{ id: 1, icon: '耳机', badge: '热卖', name: '无线降噪蓝牙耳机 长续航版', merchant: '数码优选店', price: '399', originPrice: '499', background: 'linear-gradient(145deg, #e8eef4, #cfd9e4)' },
	{ id: 2, icon: '咖啡', badge: '新品', name: '精品挂耳咖啡 轻盈果香组合装', merchant: '每日鲜享', price: '69', originPrice: '89', background: 'linear-gradient(145deg, #f4eadc, #dbc7ac)' },
	{ id: 3, icon: '音箱', badge: '', name: '智能桌面音箱 沉浸式立体声', merchant: '智慧生活馆', price: '259', originPrice: '329', background: 'linear-gradient(145deg, #e6e4ee, #cbc7d7)' },
	{ id: 4, icon: '跑鞋', badge: '优惠', name: '轻量缓震运动跑鞋 日常训练款', merchant: '活力运动', price: '329', originPrice: '429', background: 'linear-gradient(145deg, #e7eee8, #cbd8cd)' }
]

function goCategory() {
	uni.switchTab({ url: '/pages/category/category' })
}

function goProfile() {
	uni.switchTab({ url: '/pages/profile/profile' })
}

function openSearch() {
	uni.showToast({ title: '商品搜索即将接入', icon: 'none' })
}

function openProduct(product) {
	uni.showToast({ title: `即将查看：${product.name}`, icon: 'none' })
}
</script>

<style scoped>
.home-page { min-height: 100vh; background: #f3f5f7; color: #17202a; }
.home-header { padding: calc(28rpx + env(safe-area-inset-top)) 28rpx 18rpx; background: linear-gradient(180deg, #f8faf9 0%, #f3f5f7 100%); }
.brand-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 22rpx; }
.eyebrow { display: block; margin-bottom: 5rpx; color: #d9480f; font-size: 20rpx; font-weight: 700; letter-spacing: 2rpx; }
.brand-title { display: block; font-size: 48rpx; font-weight: 800; line-height: 1.1; }
.header-action { display: flex; align-items: center; justify-content: center; width: 76rpx; height: 76rpx; border-radius: 50%; background: #fff; box-shadow: 0 10rpx 30rpx rgba(25, 45, 55, .09); }
.header-action-icon { display: flex; align-items: center; justify-content: center; width: 42rpx; height: 42rpx; border: 3rpx solid #0f766e; border-radius: 50%; color: #0f766e; font-size: 19rpx; font-weight: 700; }
.search-box { display: flex; align-items: center; height: 76rpx; padding: 0 26rpx; border: 1rpx solid #e2e7ea; border-radius: 38rpx; background: #fff; box-shadow: 0 8rpx 24rpx rgba(32, 52, 60, .05); }
.search-icon { margin-right: 16rpx; color: #687681; font-size: 42rpx; line-height: 1; transform: rotate(-20deg); }
.search-placeholder { color: #8a959e; font-size: 27rpx; }
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
.goods-visual { position: relative; display: flex; align-items: center; justify-content: center; height: 250rpx; }
.goods-visual-icon { color: rgba(35, 48, 56, .68); font-size: 38rpx; font-weight: 800; letter-spacing: 2rpx; }
.goods-badge { position: absolute; top: 16rpx; left: 16rpx; padding: 6rpx 13rpx; border-radius: 18rpx; background: #d9480f; color: #fff; font-size: 18rpx; font-weight: 700; }
.goods-body { padding: 20rpx; }
.goods-name { display: -webkit-box; min-height: 70rpx; overflow: hidden; color: #222d35; font-size: 25rpx; font-weight: 700; line-height: 1.4; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.goods-merchant { display: block; margin-top: 7rpx; overflow: hidden; color: #8a949b; font-size: 20rpx; text-overflow: ellipsis; white-space: nowrap; }
.price-row { display: flex; align-items: baseline; margin-top: 14rpx; color: #d9480f; }
.price-symbol { font-size: 21rpx; font-weight: 800; }
.price-value { font-size: 34rpx; font-weight: 800; }
.price-origin { margin-left: 9rpx; color: #aeb5ba; font-size: 19rpx; text-decoration: line-through; }
</style>

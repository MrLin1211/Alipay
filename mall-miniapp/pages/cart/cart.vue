<template>
	<view class="placeholder-page">
		<view class="placeholder-icon">袋</view>
		<text class="placeholder-title">购物车还是空的</text>
		<text class="placeholder-desc">去逛逛精选商品，把喜欢的好物加入购物车</text>
		<button class="primary-button" @click="goShopping">去选购</button>
	</view>
</template>

<script setup>
function goShopping() { uni.switchTab({ url: '/pages/category/category' }) }
</script>

<style scoped>
.placeholder-page { display: flex; align-items: center; flex-direction: column; padding: 180rpx 60rpx 80rpx; text-align: center; }
.placeholder-icon { display: flex; align-items: center; justify-content: center; width: 140rpx; height: 140rpx; border-radius: 50%; background: #fff0e3; color: #d9480f; font-size: 44rpx; font-weight: 800; }
.placeholder-title { margin-top: 34rpx; color: #17202a; font-size: 38rpx; font-weight: 800; }
.placeholder-desc { max-width: 520rpx; margin-top: 18rpx; color: #7d8891; font-size: 26rpx; line-height: 1.7; }
.primary-button { margin-top: 42rpx; padding: 0 44rpx; border: 0; border-radius: 40rpx; background: #0f766e; color: #fff; font-size: 26rpx; }
</style>

<template>
	<view class="placeholder-page">
		<view class="placeholder-icon">品</view>
		<text class="placeholder-title">商品分类</text>
		<text class="placeholder-desc">分类导航和商品筛选将在下一阶段接入真实数据</text>
		<button class="primary-button" @click="backHome">返回首页</button>
	</view>
</template>

<script setup>
function backHome() { uni.switchTab({ url: '/pages/index/index' }) }
</script>

<style scoped>
.placeholder-page { display: flex; align-items: center; flex-direction: column; padding: 180rpx 60rpx 80rpx; text-align: center; }
.placeholder-icon { display: flex; align-items: center; justify-content: center; width: 140rpx; height: 140rpx; border-radius: 44rpx; background: #dff1ed; color: #0f766e; font-size: 52rpx; font-weight: 800; }
.placeholder-title { margin-top: 34rpx; color: #17202a; font-size: 38rpx; font-weight: 800; }
.placeholder-desc { max-width: 520rpx; margin-top: 18rpx; color: #7d8891; font-size: 26rpx; line-height: 1.7; }
.primary-button { margin-top: 42rpx; padding: 0 44rpx; border: 0; border-radius: 40rpx; background: #0f766e; color: #fff; font-size: 26rpx; }
</style>

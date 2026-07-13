<template>
	<view class="orders-page">
		<view class="order-tabs">
			<text v-for="(tab, index) in tabs" :key="tab" :class="['order-tab', { active: index === 0 }]">{{ tab }}</text>
		</view>
		<view class="placeholder-content">
			<view class="placeholder-icon">单</view>
			<text class="placeholder-title">暂无订单</text>
			<text class="placeholder-desc">完成购物后，订单状态会在这里展示</text>
		</view>
	</view>
</template>

<script setup>
const tabs = ['全部', '待支付', '待发货', '待收货']
</script>

<style scoped>
.orders-page { min-height: 100vh; }
.order-tabs { display: grid; grid-template-columns: repeat(4, 1fr); padding: 0 18rpx; background: #fff; }
.order-tab { position: relative; padding: 28rpx 0; color: #7d8790; font-size: 25rpx; text-align: center; }
.order-tab.active { color: #0f766e; font-weight: 800; }
.order-tab.active::after { position: absolute; right: 34rpx; bottom: 12rpx; left: 34rpx; height: 5rpx; border-radius: 5rpx; background: #0f766e; content: ''; }
.placeholder-content { display: flex; align-items: center; flex-direction: column; padding: 170rpx 60rpx; }
.placeholder-icon { display: flex; align-items: center; justify-content: center; width: 140rpx; height: 140rpx; border-radius: 44rpx; background: #e4f0ee; color: #0f766e; font-size: 44rpx; font-weight: 800; }
.placeholder-title { margin-top: 32rpx; color: #17202a; font-size: 36rpx; font-weight: 800; }
.placeholder-desc { margin-top: 16rpx; color: #8a949b; font-size: 25rpx; }
</style>

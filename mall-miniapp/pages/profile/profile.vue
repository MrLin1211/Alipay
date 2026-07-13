<template>
	<view class="profile-page">
		<view class="profile-card">
			<view class="avatar">人</view>
			<view class="profile-copy">
				<text class="profile-title">登录商城</text>
				<text class="profile-desc">登录后查看订单和收货地址</text>
			</view>
			<text class="profile-arrow">›</text>
		</view>

		<view class="menu-card">
			<view v-for="item in menuItems" :key="item.title" class="menu-item" @click="showComingSoon(item.title)">
				<view class="menu-icon">{{ item.icon }}</view>
				<text class="menu-title">{{ item.title }}</text>
				<text class="menu-arrow">›</text>
			</view>
		</view>
	</view>
</template>

<script setup>
const menuItems = [
	{ icon: '单', title: '我的订单' },
	{ icon: '址', title: '收货地址' },
	{ icon: '服', title: '联系客服' },
	{ icon: '设', title: '设置' }
]

function showComingSoon(title) {
	uni.showToast({ title: `${title}即将开放`, icon: 'none' })
}
</script>

<style scoped>
.profile-page { min-height: 100vh; padding: 30rpx 24rpx; background: linear-gradient(180deg, #eaf4f1 0, #f3f5f7 420rpx); }
.profile-card { display: flex; align-items: center; padding: 40rpx 32rpx; border-radius: 30rpx; background: #fff; box-shadow: 0 12rpx 34rpx rgba(25, 50, 55, .07); }
.avatar { display: flex; align-items: center; justify-content: center; width: 108rpx; height: 108rpx; border-radius: 50%; background: #dff1ed; color: #0f766e; font-size: 36rpx; font-weight: 800; }
.profile-copy { flex: 1; margin-left: 24rpx; }
.profile-title, .profile-desc { display: block; }
.profile-title { color: #17202a; font-size: 34rpx; font-weight: 800; }
.profile-desc { margin-top: 9rpx; color: #8a949b; font-size: 23rpx; }
.profile-arrow, .menu-arrow { color: #a9b0b5; font-size: 42rpx; }
.menu-card { margin-top: 26rpx; overflow: hidden; border-radius: 28rpx; background: #fff; }
.menu-item { display: flex; align-items: center; min-height: 104rpx; margin-left: 28rpx; padding-right: 28rpx; border-bottom: 1rpx solid #eef1f3; }
.menu-item:last-child { border-bottom: 0; }
.menu-icon { display: flex; align-items: center; justify-content: center; width: 54rpx; height: 54rpx; border-radius: 17rpx; background: #ecf5f3; color: #0f766e; font-size: 22rpx; font-weight: 800; }
.menu-title { flex: 1; margin-left: 20rpx; color: #303b43; font-size: 27rpx; font-weight: 600; }
</style>

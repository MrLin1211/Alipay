<template>
	<view class="profile-page">
		<view class="profile-card" @click="handleProfileCard">
			<view class="avatar">{{ avatarText }}</view>
			<view class="profile-copy">
				<text class="profile-title">{{ user ? (user.displayName || '商城用户') : '登录 / 注册' }}</text>
				<text class="profile-desc">{{ user ? maskPhone(user.phone) : '登录后同步订单、购物车和收货地址' }}</text>
			</view>
			<text v-if="!user" class="profile-arrow">›</text>
			<view v-else class="login-badge"><text>已登录</text></view>
		</view>

		<view v-if="user" class="account-summary">
			<view class="summary-item" @click="goOrders">
				<text class="summary-value">订单</text>
				<text class="summary-label">查看全部</text>
			</view>
			<view class="summary-divider"></view>
			<view class="summary-item" @click="showComingSoon('收货地址')">
				<text class="summary-value">地址</text>
				<text class="summary-label">收货管理</text>
			</view>
			<view class="summary-divider"></view>
			<view class="summary-item" @click="goCart">
				<text class="summary-value">购物车</text>
				<text class="summary-label">已同步</text>
			</view>
		</view>

		<view class="menu-card">
			<view v-for="item in menuItems" :key="item.key" class="menu-item" @click="handleMenu(item)">
				<view class="menu-icon">{{ item.icon }}</view>
				<text class="menu-title">{{ item.title }}</text>
				<text class="menu-arrow">›</text>
			</view>
		</view>

		<button v-if="user" class="logout-button" :disabled="loggingOut" @click="confirmLogout">
			{{ loggingOut ? '正在退出...' : '退出登录' }}
		</button>
		<text class="session-tip">账号登录状态会安全保存在当前设备</text>
	</view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow, onUnload } from '@dcloudio/uni-app'
import { fetchCurrentUser, logoutCurrentUser } from '@/api/auth.js'
import { clearAuthSession, getStoredToken, getStoredUser } from '@/utils/auth.js'

const user = ref(getStoredUser())
const loggingOut = ref(false)

const menuItems = [
	{ key: 'orders', icon: '单', title: '我的订单', auth: true },
	{ key: 'address', icon: '址', title: '收货地址', auth: true },
	{ key: 'service', icon: '服', title: '联系客服', auth: false },
	{ key: 'settings', icon: '设', title: '设置', auth: false }
]

const avatarText = computed(() => {
	if (!user.value) return '人'
	return String(user.value.displayName || user.value.phone || '用').slice(0, 1)
})

function maskPhone(phone) {
	const value = String(phone || '')
	return /^1\d{10}$/.test(value) ? `${value.slice(0, 3)}****${value.slice(7)}` : value
}

function openAuth(mode = 'login') {
	uni.navigateTo({ url: `/pages/auth/auth?mode=${mode}` })
}

function handleProfileCard() {
	if (!user.value) openAuth('login')
}

function requireLogin() {
	if (user.value) return true
	uni.showToast({ title: '请先登录商城', icon: 'none' })
	setTimeout(() => openAuth('login'), 350)
	return false
}

function goOrders() {
	if (!requireLogin()) return
	uni.switchTab({ url: '/pages/orders/orders' })
}

function goCart() {
	if (!requireLogin()) return
	uni.switchTab({ url: '/pages/cart/cart' })
}

function handleMenu(item) {
	if (item.auth && !requireLogin()) return
	if (item.key === 'orders') {
		goOrders()
		return
	}
	showComingSoon(item.title)
}

function showComingSoon(title) {
	uni.showToast({ title: `${title}即将开放`, icon: 'none' })
}

async function refreshUser() {
	if (!getStoredToken()) {
		user.value = null
		return
	}
	user.value = getStoredUser()
	try {
		const currentUser = await fetchCurrentUser()
		user.value = currentUser
		uni.setStorageSync('mall_customer_user', currentUser)
	} catch (error) {
		if (!getStoredToken()) user.value = null
	}
}

function confirmLogout() {
	if (loggingOut.value) return
	uni.showModal({
		title: '退出登录',
		content: '退出后仍会保留登录手机号，方便下次登录。',
		confirmText: '退出',
		confirmColor: '#d9480f',
		success: ({ confirm }) => { if (confirm) logout() }
	})
}

async function logout() {
	loggingOut.value = true
	try {
		await logoutCurrentUser()
	} catch {
		// 服务端不可用时仍清理本地登录态。
	} finally {
		clearAuthSession()
		user.value = null
		loggingOut.value = false
		uni.$emit('mall-auth-changed', null)
		uni.showToast({ title: '已退出登录', icon: 'success' })
	}
}

function handleAuthChanged(nextUser) {
	user.value = nextUser || null
}

function handleAuthExpired() {
	user.value = null
}

onLoad(() => {
	uni.$on('mall-auth-changed', handleAuthChanged)
	uni.$on('mall-auth-expired', handleAuthExpired)
})

onShow(() => refreshUser())

onUnload(() => {
	uni.$off('mall-auth-changed', handleAuthChanged)
	uni.$off('mall-auth-expired', handleAuthExpired)
})
</script>

<style scoped>
.profile-page { min-height: 100vh; padding: 30rpx 24rpx calc(50rpx + env(safe-area-inset-bottom)); background: linear-gradient(180deg, #eaf4f1 0, #f3f5f7 420rpx); }
.profile-card { display: flex; align-items: center; padding: 40rpx 32rpx; border-radius: 30rpx; background: #fff; box-shadow: 0 12rpx 34rpx rgba(25, 50, 55, .07); }
.avatar { display: flex; align-items: center; justify-content: center; width: 108rpx; height: 108rpx; border-radius: 50%; background: linear-gradient(145deg, #dff1ed, #cce8e2); color: #0f766e; font-size: 36rpx; font-weight: 800; }
.profile-copy { flex: 1; min-width: 0; margin-left: 24rpx; }
.profile-title, .profile-desc { display: block; }
.profile-title { overflow: hidden; color: #17202a; font-size: 34rpx; font-weight: 800; text-overflow: ellipsis; white-space: nowrap; }
.profile-desc { margin-top: 9rpx; color: #8a949b; font-size: 23rpx; line-height: 1.45; }
.profile-arrow, .menu-arrow { color: #a9b0b5; font-size: 42rpx; }
.login-badge { padding: 8rpx 16rpx; border-radius: 22rpx; background: #e7f3f0; color: #0f766e; font-size: 20rpx; font-weight: 700; }
.account-summary { display: flex; align-items: center; margin-top: 24rpx; padding: 28rpx 16rpx; border-radius: 28rpx; background: #fff; }
.summary-item { display: flex; align-items: center; flex: 1; flex-direction: column; }
.summary-value, .summary-label { display: block; }
.summary-value { color: #26323a; font-size: 26rpx; font-weight: 800; }
.summary-label { margin-top: 7rpx; color: #929ba2; font-size: 20rpx; }
.summary-divider { width: 1rpx; height: 52rpx; background: #e7ebed; }
.menu-card { margin-top: 26rpx; overflow: hidden; border-radius: 28rpx; background: #fff; }
.menu-item { display: flex; align-items: center; min-height: 104rpx; margin-left: 28rpx; padding-right: 28rpx; border-bottom: 1rpx solid #eef1f3; }
.menu-item:last-child { border-bottom: 0; }
.menu-icon { display: flex; align-items: center; justify-content: center; width: 54rpx; height: 54rpx; border-radius: 17rpx; background: #ecf5f3; color: #0f766e; font-size: 22rpx; font-weight: 800; }
.menu-title { flex: 1; margin-left: 20rpx; color: #303b43; font-size: 27rpx; font-weight: 600; }
.logout-button { margin-top: 30rpx; border: 1rpx solid #f0d8d0; border-radius: 42rpx; background: #fff; color: #d9480f; font-size: 27rpx; line-height: 82rpx; }
.logout-button[disabled] { opacity: .58; }
.session-tip { display: block; margin-top: 22rpx; color: #9ca4aa; font-size: 20rpx; text-align: center; }
</style>

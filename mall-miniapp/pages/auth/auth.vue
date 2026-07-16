<template>
	<view class="auth-page">
		<view class="auth-hero">
			<view class="brand-mark">商</view>
			<text class="hero-title">欢迎来到商城</text>
			<text class="hero-desc">登录后同步购物车、地址和订单信息</text>
		</view>

		<view class="auth-card">
			<view class="mode-tabs">
				<view :class="['mode-tab', { active: mode === 'login' }]" @click="switchMode('login')">登录</view>
				<view :class="['mode-tab', { active: mode === 'register' }]" @click="switchMode('register')">注册</view>
			</view>

			<view v-if="mode === 'register'" class="field-group">
				<text class="field-label">昵称</text>
				<input v-model="form.displayName" class="field-input" maxlength="30" placeholder="选填，未填写时自动生成" placeholder-class="field-placeholder" />
			</view>

			<view class="field-group">
				<text class="field-label">手机号</text>
				<view class="phone-input-wrap">
					<text class="phone-prefix">+86</text>
					<input v-model="form.phone" class="field-input phone-input" type="number" maxlength="11" placeholder="请输入11位手机号" placeholder-class="field-placeholder" />
				</view>
			</view>

			<view class="field-group">
				<text class="field-label">密码</text>
				<view class="password-input-wrap">
					<input v-model="form.password" class="field-input password-input" :password="!showPassword" maxlength="64" placeholder="至少6位密码" placeholder-class="field-placeholder" @confirm="submit" />
					<text class="password-toggle" @click="showPassword = !showPassword">{{ showPassword ? '隐藏' : '显示' }}</text>
				</view>
			</view>

			<text v-if="errorMessage" class="form-error">{{ errorMessage }}</text>

			<button class="submit-button" :disabled="submitting" @click="submit">
				{{ submitting ? '处理中...' : (mode === 'login' ? '登录' : '注册并登录') }}
			</button>

			<view class="mode-tip">
				<text>{{ mode === 'login' ? '还没有商城账号？' : '已经注册过账号？' }}</text>
				<text class="mode-link" @click="switchMode(mode === 'login' ? 'register' : 'login')">
					{{ mode === 'login' ? '立即注册' : '返回登录' }}
				</text>
			</view>
		</view>

		<view class="future-login">
			<view class="future-line"></view>
			<text>后续将支持微信快捷登录</text>
			<view class="future-line"></view>
		</view>
	</view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { loginByPhone, registerByPhone } from '@/api/auth.js'
import { getRememberedPhone, saveAuthSession } from '@/utils/auth.js'

const mode = ref('login')
const showPassword = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const form = reactive({ phone: '', password: '', displayName: '' })

function switchMode(nextMode) {
	mode.value = nextMode
	errorMessage.value = ''
	form.password = ''
	uni.setNavigationBarTitle({ title: nextMode === 'login' ? '登录商城' : '注册商城账号' })
}

function validate() {
	const phone = form.phone.trim()
	if (!/^1\d{10}$/.test(phone)) return '请输入正确的11位手机号'
	if (!form.password || form.password.length < 6) return '密码至少需要6位'
	if (mode.value === 'register' && form.displayName.trim().length > 30) return '昵称不能超过30个字符'
	return ''
}

async function submit() {
	if (submitting.value) return
	errorMessage.value = validate()
	if (errorMessage.value) return

	submitting.value = true
	try {
		const payload = {
			phone: form.phone.trim(),
			password: form.password,
			displayName: form.displayName.trim()
		}
		const session = mode.value === 'login'
			? await loginByPhone(payload)
			: await registerByPhone(payload)
		saveAuthSession(session, payload.phone)
		uni.$emit('mall-auth-changed', session.user)
		uni.showToast({ title: mode.value === 'login' ? '登录成功' : '注册成功', icon: 'success' })
		setTimeout(() => {
			const pages = getCurrentPages()
			if (pages.length > 1) uni.navigateBack()
			else uni.switchTab({ url: '/pages/profile/profile' })
		}, 500)
	} catch (error) {
		errorMessage.value = error instanceof Error ? error.message : '操作失败，请稍后重试'
	} finally {
		submitting.value = false
	}
}

onLoad((options) => {
	form.phone = getRememberedPhone()
	if (options?.mode === 'register') switchMode('register')
})
</script>

<style scoped>
.auth-page { min-height: 100vh; padding: 58rpx 28rpx calc(60rpx + env(safe-area-inset-bottom)); background: linear-gradient(180deg, #e8f4f1 0, #f3f5f7 520rpx); }
.auth-hero { display: flex; align-items: center; flex-direction: column; text-align: center; }
.brand-mark { display: flex; align-items: center; justify-content: center; width: 104rpx; height: 104rpx; border-radius: 32rpx; background: linear-gradient(145deg, #0f766e, #14968a); box-shadow: 0 16rpx 34rpx rgba(15, 118, 110, .22); color: #fff; font-size: 42rpx; font-weight: 800; }
.hero-title { margin-top: 26rpx; color: #17202a; font-size: 42rpx; font-weight: 800; }
.hero-desc { margin-top: 12rpx; color: #77838c; font-size: 24rpx; }
.auth-card { margin-top: 44rpx; padding: 32rpx 30rpx 36rpx; border-radius: 32rpx; background: #fff; box-shadow: 0 14rpx 38rpx rgba(30, 51, 58, .08); }
.mode-tabs { display: grid; grid-template-columns: repeat(2, 1fr); padding: 6rpx; border-radius: 26rpx; background: #f0f3f4; }
.mode-tab { padding: 17rpx 0; border-radius: 22rpx; color: #7b858d; font-size: 27rpx; font-weight: 600; text-align: center; }
.mode-tab.active { background: #fff; box-shadow: 0 5rpx 14rpx rgba(32, 50, 58, .08); color: #0f766e; font-weight: 800; }
.field-group { margin-top: 28rpx; }
.field-label { display: block; margin: 0 4rpx 12rpx; color: #354049; font-size: 24rpx; font-weight: 700; }
.field-input, .phone-input-wrap, .password-input-wrap { height: 86rpx; border: 1rpx solid #e0e5e8; border-radius: 22rpx; background: #f9fbfb; }
.field-input { width: 100%; padding: 0 24rpx; color: #243039; font-size: 27rpx; }
.field-placeholder { color: #a4acb2; }
.phone-input-wrap, .password-input-wrap { display: flex; align-items: center; overflow: hidden; }
.phone-prefix { padding: 0 22rpx; border-right: 1rpx solid #dfe4e7; color: #53616a; font-size: 26rpx; font-weight: 700; }
.phone-input, .password-input { flex: 1; height: 82rpx; border: 0; background: transparent; }
.password-toggle { flex: 0 0 auto; padding: 22rpx; color: #0f766e; font-size: 23rpx; font-weight: 700; }
.form-error { display: block; margin: 20rpx 4rpx 0; color: #d9480f; font-size: 23rpx; line-height: 1.5; }
.submit-button { margin-top: 34rpx; border: 0; border-radius: 43rpx; background: #0f766e; color: #fff; font-size: 28rpx; font-weight: 800; line-height: 86rpx; }
.submit-button[disabled] { opacity: .58; }
.mode-tip { display: flex; justify-content: center; gap: 10rpx; margin-top: 28rpx; color: #8b949b; font-size: 23rpx; }
.mode-link { color: #0f766e; font-weight: 700; }
.future-login { display: flex; align-items: center; gap: 20rpx; margin: 46rpx 30rpx 0; color: #9aa2a8; font-size: 21rpx; }
.future-line { flex: 1; height: 1rpx; background: #dce2e5; }
</style>

<template>
	<view class="page">
		<view v-if="loading" class="state">正在加载收货地址...</view>
		<view v-else-if="!addresses.length" class="empty">
			<view class="empty-icon">址</view><text class="empty-title">还没有收货地址</text>
			<text class="empty-desc">新增地址后即可提交商品订单</text>
		</view>
		<view v-else class="list">
			<view v-for="item in addresses" :key="item.id" class="card" @click="selectAddress(item)">
				<view class="info">
					<view class="person"><text class="name">{{ item.receiverName }}</text><text class="phone">{{ item.phone }}</text><text v-if="item.isDefault" class="tag">默认</text></view>
					<text class="address">{{ fullAddress(item) }}</text>
				</view>
				<view class="actions" @click.stop>
					<text v-if="!item.isDefault" @click="makeDefault(item)">设为默认</text>
					<text @click="edit(item)">编辑</text><text class="danger" @click="remove(item)">删除</text>
				</view>
			</view>
		</view>
		<view class="bottom"><button class="primary" @click="add">新增收货地址</button></view>
	</view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { deleteAddress, fetchAddresses, setDefaultAddress } from '@/api/address.js'

const addresses = ref([])
const loading = ref(false)
const selecting = ref(false)

function fullAddress(item) { return [item.province, item.city, item.district, item.detailAddress].filter(Boolean).join(' ') }

async function load() {
	loading.value = true
	try { addresses.value = await fetchAddresses() || [] }
	catch (error) { uni.showToast({ title: error.message || '地址加载失败', icon: 'none' }) }
	finally { loading.value = false }
}
function add() { uni.removeStorageSync('mall_edit_address'); uni.navigateTo({ url: '/pages/address/edit' }) }
function edit(item) { uni.setStorageSync('mall_edit_address', item); uni.navigateTo({ url: '/pages/address/edit?id=' + item.id }) }
function selectAddress(item) {
	if (!selecting.value) return
	uni.setStorageSync('mall_checkout_address_id', item.id)
	uni.navigateBack()
}
async function makeDefault(item) {
	try { await setDefaultAddress(item.id); await load(); uni.showToast({ title: '已设为默认', icon: 'success' }) }
	catch (error) { uni.showToast({ title: error.message || '设置失败', icon: 'none' }) }
}
function remove(item) {
	uni.showModal({ title: '删除地址', content: `确认删除 ${item.receiverName} 的收货地址吗？`, confirmText: '删除', confirmColor: '#d9480f', success: async ({ confirm }) => {
		if (!confirm) return
		try { await deleteAddress(item.id); await load(); uni.showToast({ title: '已删除', icon: 'success' }) }
		catch (error) { uni.showToast({ title: error.message || '删除失败', icon: 'none' }) }
	} })
}
onLoad((options) => { selecting.value = options?.select === '1' })
onShow(load)
</script>

<style scoped>
.page{min-height:100vh;padding:20rpx 22rpx 150rpx;background:#f3f5f7;color:#26323a}.list{display:flex;flex-direction:column;gap:18rpx}.card{overflow:hidden;border-radius:26rpx;background:#fff;box-shadow:0 8rpx 24rpx rgba(28,48,56,.05)}.info{padding:28rpx}.person{display:flex;align-items:center;gap:18rpx}.name{font-size:29rpx;font-weight:800}.phone{color:#69757d;font-size:24rpx}.tag{padding:4rpx 12rpx;border-radius:16rpx;background:#e5f2ef;color:#0f766e;font-size:19rpx}.address{display:block;margin-top:17rpx;font-size:25rpx;line-height:1.6;word-break:break-all}.actions{display:flex;justify-content:flex-end;gap:32rpx;padding:20rpx 28rpx;border-top:1rpx solid #edf0f2;color:#68747c;font-size:22rpx}.danger{color:#d9480f}.bottom{position:fixed;right:0;bottom:0;left:0;padding:16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom));background:#fff}.primary{border:0;border-radius:42rpx;background:#0f766e;color:#fff;font-size:27rpx;line-height:84rpx}.state,.empty{display:flex;align-items:center;flex-direction:column;padding:170rpx 50rpx;color:#7d8891}.empty-icon{display:flex;align-items:center;justify-content:center;width:130rpx;height:130rpx;border-radius:44rpx;background:#e4f0ee;color:#0f766e;font-size:40rpx;font-weight:800}.empty-title{margin-top:28rpx;color:#17202a;font-size:34rpx;font-weight:800}.empty-desc{margin-top:14rpx;font-size:24rpx}
</style>

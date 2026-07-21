<template>
	<view class="page">
		<scroll-view v-if="loggedIn" scroll-x class="tabs"><view class="tabs-inner"><text v-for="tab in tabs" :key="tab.key" :class="['tab',{active:active===tab.key}]" @click="active=tab.key">{{tab.label}}</text></view></scroll-view>
		<view v-if="!loggedIn" class="state"><view class="state-icon">人</view><text class="state-title">登录后查看订单</text><text class="state-desc">手机号登录后可同步全部订单状态</text><button @click="openAuth">登录 / 注册</button></view>
		<view v-else-if="loading" class="state">正在加载订单...</view>
		<view v-else-if="errorMessage" class="state"><text>{{errorMessage}}</text><button @click="load">重新加载</button></view>
		<view v-else-if="!filtered.length" class="state"><view class="state-icon">单</view><text class="state-title">暂无相关订单</text><text class="state-desc">完成购物后，订单状态会在这里展示</text></view>
		<view v-else class="list">
			<view v-for="order in filtered" :key="order.orderNo" class="card" @click="openDetail(order.orderNo)">
				<view class="heading"><view class="merchant-mark">店</view><text class="merchant">{{order.merchantName||'精选商家'}}</text><text :class="['status',statusOf(order).tone]">{{statusOf(order).label}}</text></view>
				<view v-for="item in orderItems(order)" :key="item.id||`${item.productId}:${item.skuId}`" class="item">
					<image v-if="item.productImage" class="image" :src="item.productImage" mode="aspectFill"/><view v-else class="image fallback">{{String(item.productName||'商品').slice(0,2)}}</view>
					<view class="copy"><text class="name">{{item.productName}}</text><text class="merchant-line">{{order.merchantName||'精选商家'}}</text><text class="sku">规格：{{item.skuName||'默认规格'}}</text><view class="price"><text>¥{{formatMoney(item.unitPrice)}}</text><text>×{{item.quantity}}</text></view></view>
				</view>
				<view class="summary"><text>共 {{orderQuantity(order)}} 件</text><text>实付 </text><text class="amount">¥{{formatMoney(order.totalAmount)}}</text></view>
			</view>
		</view>
	</view>
</template>
<script setup>
import { computed, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { fetchOrders } from '@/api/orders.js'
import { formatMoney } from '@/utils/catalog.js'
import { getStoredToken } from '@/utils/auth.js'
import { orderItems, orderQuantity, orderStatus } from '@/utils/orders.js'
const tabs=[{key:'all',label:'全部'},{key:'PENDING',label:'待支付'},{key:'PAID',label:'待发货'},{key:'shipping',label:'待收货'},{key:'COMPLETED',label:'已完成'},{key:'CANCELLED',label:'已取消'}]
const active=ref('all');const loggedIn=ref(false);const loading=ref(false);const errorMessage=ref('');const orders=ref([])
const filtered=computed(()=>orders.value.filter(order=>active.value==='all'||(active.value==='shipping'?['SHIPPED','DELIVERED'].includes(order.status):order.status===active.value)))
function statusOf(order){return orderStatus(order.status)}
function openAuth(){uni.navigateTo({url:'/pages/auth/auth?mode=login'})}
function openDetail(orderNo){uni.navigateTo({url:`/pages/orders/detail?orderNo=${encodeURIComponent(orderNo)}`})}
async function load(show=true){loggedIn.value=Boolean(getStoredToken());if(!loggedIn.value){orders.value=[];return}if(show)loading.value=true;errorMessage.value='';try{orders.value=await fetchOrders()||[]}catch(error){errorMessage.value=error.message||'订单加载失败'}finally{loading.value=false}}
onShow(load);onPullDownRefresh(async()=>{await load(false);uni.stopPullDownRefresh()})
</script>
<style scoped>
.page{min-height:100vh;background:#f3f5f7;color:#26323a}.tabs{position:sticky;top:0;z-index:5;width:100%;white-space:nowrap;background:#fff}.tabs-inner{display:inline-flex;padding:0 15rpx}.tab{position:relative;padding:27rpx 25rpx;color:#7d8790;font-size:24rpx}.tab.active{color:#0f766e;font-weight:800}.tab.active:after{position:absolute;right:26rpx;bottom:10rpx;left:26rpx;height:5rpx;border-radius:5rpx;background:#0f766e;content:''}.list{padding:20rpx 22rpx}.card{margin-bottom:20rpx;overflow:hidden;border-radius:27rpx;background:#fff;box-shadow:0 8rpx 24rpx rgba(28,48,56,.05)}.heading{display:flex;align-items:center;padding:23rpx;border-bottom:1rpx solid #edf0f2}.merchant-mark{display:flex;align-items:center;justify-content:center;width:43rpx;height:43rpx;border-radius:14rpx;background:#e5f2ef;color:#0f766e;font-size:18rpx;font-weight:800}.merchant{flex:1;margin-left:13rpx;font-size:25rpx;font-weight:800}.status{font-size:22rpx}.status.warning{color:#d9480f}.status.primary{color:#0f766e}.status.success{color:#2b8a3e}.status.muted{color:#8a949b}.item{display:flex;padding:22rpx;border-bottom:1rpx solid #edf0f2}.image{flex:0 0 140rpx;width:140rpx;height:140rpx;border-radius:18rpx;background:#eef1f2}.fallback{display:flex;align-items:center;justify-content:center;color:#6f7b82;font-weight:800}.copy{flex:1;min-width:0;margin-left:18rpx}.name,.merchant-line,.sku{display:block;word-break:break-all}.name{font-size:24rpx;font-weight:800;line-height:1.45}.merchant-line{margin-top:6rpx;color:#68747c;font-size:20rpx}.sku{margin-top:5rpx;color:#89939a;font-size:20rpx;line-height:1.45}.price{display:flex;justify-content:space-between;margin-top:9rpx;font-size:22rpx}.summary{padding:22rpx;text-align:right;color:#68747c;font-size:21rpx}.amount{color:#d9480f;font-size:29rpx;font-weight:800}.state{display:flex;align-items:center;flex-direction:column;padding:170rpx 55rpx;color:#7d8891;text-align:center}.state-icon{display:flex;align-items:center;justify-content:center;width:135rpx;height:135rpx;border-radius:44rpx;background:#e4f0ee;color:#0f766e;font-size:42rpx;font-weight:800}.state-title{margin-top:30rpx;color:#17202a;font-size:35rpx;font-weight:800}.state-desc{margin-top:15rpx;font-size:24rpx}.state button{margin-top:32rpx;border:0;border-radius:38rpx;background:#0f766e;color:#fff;font-size:24rpx;line-height:76rpx}
</style>

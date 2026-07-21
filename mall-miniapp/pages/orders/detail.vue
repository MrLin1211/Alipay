<template>
	<view class="page">
		<view v-if="loading" class="state">正在加载订单详情...</view>
		<view v-else-if="errorMessage" class="state"><text>{{errorMessage}}</text><button @click="load">重新加载</button></view>
		<template v-else-if="order">
			<view :class="['status-card',status.tone]"><text class="status-title">{{status.label}}</text><text class="status-desc">{{statusDescription}}</text></view>
			<view class="address-card"><text class="section-title">收货信息</text><text class="person">{{order.shippingName}}　{{order.shippingPhone}}</text><text class="address">{{order.shippingAddress}}</text></view>
			<view class="merchant-card"><view class="heading"><view class="mark">店</view><text>{{order.merchantName||'精选商家'}}</text></view><view v-for="item in orderItems(order)" :key="item.id||`${item.productId}:${item.skuId}`" class="item"><image v-if="item.productImage" class="image" :src="item.productImage" mode="aspectFill"/><view v-else class="image fallback">{{String(item.productName||'商品').slice(0,2)}}</view><view class="copy"><text class="name">{{item.productName}}</text><text class="merchant-line">{{order.merchantName||'精选商家'}}</text><text class="sku">规格：{{item.skuName||'默认规格'}}</text><view class="price"><text>¥{{formatMoney(item.unitPrice)}}</text><text>×{{item.quantity}}</text></view></view></view></view>
			<view class="amount-card"><view><text>商品金额</text><text>¥{{formatMoney(itemsAmount)}}</text></view><view><text>优惠金额</text><text>-¥{{formatMoney(order.discountAmount||0)}}</text></view><view><text>配送费</text><text>¥{{formatMoney(order.shippingFee||0)}}</text></view><view class="total"><text>实付金额</text><text>¥{{formatMoney(order.totalAmount)}}</text></view></view>
			<view class="info-card"><text class="section-title">订单信息</text><view><text>订单编号</text><text selectable>{{order.orderNo}}</text></view><view><text>下单时间</text><text>{{formatDateTime(order.createdAt)}}</text></view><view v-if="order.remark"><text>订单备注</text><text>{{order.remark}}</text></view></view>
			<!-- #ifdef H5 -->
			<view v-if="order.status==='PENDING'" class="payment-tip"><text>订单尚未支付，可继续使用支付宝付款。</text><button :disabled="paying" @click="payOrder">{{paying?'正在发起支付...':'支付宝支付'}}</button></view>
			<!-- #endif -->
			<!-- #ifndef H5 -->
			<view v-if="order.status==='PENDING'" class="payment-tip">微信支付将在下一阶段接入，当前订单已成功创建。</view>
			<!-- #endif -->
		</template>
	</view>
</template>
<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { fetchOrderDetail } from '@/api/orders.js'
// #ifdef H5
import { createPaymentOrder } from '@/api/payment.js'
// #endif
import { formatMoney } from '@/utils/catalog.js'
import { formatDateTime, orderItems, orderStatus } from '@/utils/orders.js'
// #ifdef H5
import { ensurePaymentCreated, latestPayment, openH5PaymentPage, paymentPayload } from '@/utils/payment.js'
// #endif
const orderNo=ref('');const order=ref(null);const loading=ref(true);const errorMessage=ref('')
// #ifdef H5
const paying=ref(false)
// #endif
const status=computed(()=>orderStatus(order.value?.status));const itemsAmount=computed(()=>orderItems(order.value).reduce((sum,item)=>sum+Number(item.subtotal??Number(item.unitPrice||0)*Number(item.quantity||0)),0))
const statusDescription=computed(()=>({PENDING:'订单已提交，等待完成支付',PAID:'支付成功，等待商家发货',SHIPPED:'商品已发出，请留意物流信息',DELIVERED:'商品已送达，请确认收货',COMPLETED:'订单已完成',CANCELLED:'订单已取消',REFUNDING:'退款正在处理中',REFUNDED:'退款已完成'}[order.value?.status]||'订单状态已更新'))
async function load(){loading.value=true;errorMessage.value='';try{order.value=await fetchOrderDetail(orderNo.value)}catch(error){errorMessage.value=error.message||'订单详情加载失败'}finally{loading.value=false}}
// #ifdef H5
async function payOrder(){
	if(paying.value||order.value?.status!=='PENDING')return
	paying.value=true
	try{
		const current=latestPayment(order.value)
		if(current?.status==='CREATE_SUCCESS'&&current.orderNo){openH5PaymentPage(current.orderNo);return}
		if(['SUCCESS','FINISHED'].includes(current?.status)){uni.showToast({title:'支付已完成，正在刷新订单',icon:'none'});await load();return}
		if(['CREATED','UNKNOWN_NOTIFY'].includes(current?.status)){uni.showToast({title:'支付状态确认中，请稍后重试',icon:'none'});return}
		const payment=ensurePaymentCreated(await createPaymentOrder(paymentPayload(order.value,'miniapp-h5-repay')))
		openH5PaymentPage(payment.orderNo)
	}catch(error){uni.showToast({title:error.message||'支付发起失败',icon:'none',duration:2800})}finally{paying.value=false}
}
// #endif
onLoad(options=>{orderNo.value=decodeURIComponent(options?.orderNo||'');if(orderNo.value)load();else{loading.value=false;errorMessage.value='缺少订单编号'}})
</script>
<style scoped>
.page{min-height:100vh;padding:22rpx;background:#f3f5f7;color:#26323a}.status-card,.address-card,.merchant-card,.amount-card,.info-card,.payment-tip{margin-bottom:20rpx;border-radius:27rpx;background:#fff;box-shadow:0 8rpx 24rpx rgba(28,48,56,.04)}.status-card{padding:34rpx 28rpx;background:linear-gradient(135deg,#0f766e,#159487);color:#fff}.status-card.warning{background:linear-gradient(135deg,#d9480f,#ef7f36)}.status-card.muted{background:linear-gradient(135deg,#65727a,#8b969d)}.status-title{display:block;font-size:36rpx;font-weight:800}.status-desc{display:block;margin-top:10rpx;font-size:23rpx;opacity:.9}.address-card{padding:27rpx}.section-title{display:block;margin-bottom:18rpx;font-size:26rpx;font-weight:800}.person{display:block;font-size:24rpx;font-weight:700}.address{display:block;margin-top:10rpx;color:#68747c;font-size:22rpx;line-height:1.6;word-break:break-all}.heading{display:flex;align-items:center;gap:14rpx;padding:23rpx;border-bottom:1rpx solid #edf0f2;font-size:25rpx;font-weight:800}.mark{display:flex;align-items:center;justify-content:center;width:43rpx;height:43rpx;border-radius:14rpx;background:#e5f2ef;color:#0f766e;font-size:18rpx}.item{display:flex;padding:22rpx;border-bottom:1rpx solid #edf0f2}.item:last-child{border-bottom:0}.image{flex:0 0 140rpx;width:140rpx;height:140rpx;border-radius:18rpx;background:#eef1f2}.fallback{display:flex;align-items:center;justify-content:center;color:#6f7b82;font-weight:800}.copy{flex:1;min-width:0;margin-left:18rpx}.name,.merchant-line,.sku{display:block;word-break:break-all}.name{font-size:24rpx;font-weight:800;line-height:1.45}.merchant-line{margin-top:6rpx;color:#68747c;font-size:20rpx}.sku{margin-top:5rpx;color:#89939a;font-size:20rpx;line-height:1.45}.price{display:flex;justify-content:space-between;margin-top:9rpx;font-size:22rpx}.amount-card,.info-card{padding:21rpx 27rpx}.amount-card view,.info-card view{display:flex;justify-content:space-between;padding:10rpx 0;color:#68747c;font-size:22rpx}.amount-card .total{color:#26323a;font-weight:800}.amount-card .total text:last-child{color:#d9480f;font-size:29rpx}.info-card view text:last-child{max-width:470rpx;color:#26323a;text-align:right;word-break:break-all}.payment-tip{padding:24rpx;color:#a34a11;font-size:22rpx;line-height:1.6;background:#fff4e8}.payment-tip text{display:block}.payment-tip button{margin-top:20rpx;border:0;border-radius:36rpx;background:#1677ff;color:#fff;font-size:24rpx;font-weight:800;line-height:72rpx}.payment-tip button[disabled]{opacity:.55}.state{display:flex;align-items:center;flex-direction:column;padding:180rpx 50rpx;color:#7d8891;text-align:center}.state button{margin-top:28rpx;border:0;border-radius:36rpx;background:#0f766e;color:#fff;font-size:24rpx;line-height:72rpx}
</style>

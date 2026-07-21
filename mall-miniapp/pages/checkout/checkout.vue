<template>
	<view class="page">
		<view v-if="loading" class="state">正在核对商品和库存...</view>
		<template v-else-if="items.length">
			<view class="address-card" @click="chooseAddress">
				<template v-if="selectedAddress">
					<view class="address-icon">址</view><view class="address-copy">
						<view><text class="receiver">{{ selectedAddress.receiverName }}</text><text class="phone">{{ selectedAddress.phone }}</text><text v-if="selectedAddress.isDefault" class="tag">默认</text></view>
						<text class="address-text">{{ fullAddress(selectedAddress) }}</text>
					</view><text class="arrow">›</text>
				</template>
				<template v-else><view class="address-icon">＋</view><text class="empty-address">请选择或新增收货地址</text><text class="arrow">›</text></template>
			</view>

			<view class="merchant-card">
				<view class="merchant-heading"><view class="merchant-mark">店</view><text>{{ merchantName }}</text></view>
				<view v-for="item in items" :key="item.key" class="product-row">
					<image v-if="item.image" class="image" :src="item.image" mode="aspectFill" />
					<view v-else class="image fallback">{{ item.productName.slice(0,2) }}</view>
					<view class="product-copy"><text class="product-name">{{ item.productName }}</text><text class="merchant-line">{{ item.merchantName }}</text><text class="sku-line">规格：{{ item.skuName || '默认规格' }}</text><view class="price-line"><text>¥{{ formatMoney(item.unitPrice) }}</text><text class="quantity">×{{ item.quantity }}</text></view></view>
				</view>
			</view>

			<view class="remark-card"><text>订单备注</text><input v-model="remark" maxlength="100" placeholder="选填，可填写配送要求" /></view>
			<view class="amount-card"><view><text>商品金额</text><text>¥{{ formatMoney(totalAmount) }}</text></view><view><text>配送费</text><text>¥0.00</text></view><view class="total"><text>应付金额</text><text>¥{{ formatMoney(totalAmount) }}</text></view></view>
			<view class="submit-bar"><view class="submit-total"><text>合计：</text><text>¥{{ formatMoney(totalAmount) }}</text></view>
				<!-- #ifdef H5 -->
				<button :disabled="submitting" @click="submit">{{ submitting ? '处理中...' : '支付宝支付' }}</button>
				<!-- #endif -->
				<!-- #ifndef H5 -->
				<button :disabled="submitting" @click="submit">{{ submitting ? '提交中...' : '提交订单' }}</button>
				<!-- #endif -->
			</view>
		</template>
		<view v-else-if="errorMessage" class="state"><text>{{ errorMessage }}</text><button class="retry" @click="load">重新加载</button></view>
	</view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { fetchAddresses } from '@/api/address.js'
import { fetchCart, saveCartItem } from '@/api/cart.js'
import { fetchProducts } from '@/api/catalog.js'
import { createOrder } from '@/api/orders.js'
// #ifdef H5
import { createPaymentOrder } from '@/api/payment.js'
// #endif
import { formatMoney, normalizeProduct } from '@/utils/catalog.js'
import { cartItemPayload, updateCartTabBadge } from '@/utils/cart.js'
// #ifdef H5
import { ensurePaymentCreated, openH5PaymentPage, paymentPayload } from '@/utils/payment.js'
// #endif

const merchantKey=ref(''); const items=ref([]); const addresses=ref([]); const selectedAddress=ref(null); const loading=ref(true); const submitting=ref(false); const errorMessage=ref(''); const remark=ref('')
const totalAmount=computed(()=>items.value.reduce((sum,item)=>sum+item.unitPrice*item.quantity,0))
const merchantName=computed(()=>items.value[0]?.merchantName||'精选商家')
function fullAddress(item){return [item.province,item.city,item.district,item.detailAddress].filter(Boolean).join(' ')}
function productPayload(item){return{id:item.productId,productCode:item.productCode,name:item.productName,image:item.image,price:item.unitPrice}}
function skuPayload(item){return{id:item.skuId,skuCode:item.skuCode,skuName:item.skuName,price:item.unitPrice}}

async function load(){
	loading.value=true; errorMessage.value=''
	try{
		const [cart,catalogData,addressData]=await Promise.all([fetchCart(),fetchProducts({page:0,size:100}),fetchAddresses()])
		const catalog=(catalogData?.content||[]).map(normalizeProduct); const map=new Map(catalog.map(p=>[String(p.id),p]))
		items.value=(cart?.items||[]).map(snapshot=>{
			const product=map.get(String(snapshot.productId)); const sku=product?.skus?.find(row=>String(row.id||'')===String(snapshot.skuId||'')); const unavailable=!product||(snapshot.skuId&&!sku)
			return{key:`${snapshot.productId}:${snapshot.skuId||'default'}`,productId:snapshot.productId,skuId:snapshot.skuId||null,productCode:product?.productCode||snapshot.productCode||'',skuCode:sku?.skuCode||snapshot.skuCode||'',productName:product?.name||snapshot.productName||'商品',skuName:sku?.skuName||snapshot.skuName||'默认规格',image:product?.image||'',merchantNo:product?.merchantNo||'',merchantName:product?.merchantName||'精选商家',unitPrice:Number(sku?.price??product?.price??snapshot.unitPrice??0),stock:Number(sku?.stock??product?.stock??0),quantity:Number(snapshot.quantity||0),unavailable}
		}).filter(item=>(item.merchantNo||item.merchantName||'merchant-default')===merchantKey.value)
		if(!items.value.length) throw new Error('该商家的购物车商品不存在')
		if(items.value.some(item=>item.unavailable||item.quantity>item.stock)) throw new Error('部分商品已下架或库存不足，请返回购物车处理')
		addresses.value=addressData||[]; const stored=uni.getStorageSync('mall_checkout_address_id'); selectedAddress.value=addresses.value.find(a=>String(a.id)===String(stored))||addresses.value.find(a=>a.isDefault)||addresses.value[0]||null
	}catch(error){items.value=[];errorMessage.value=error.message||'确认订单加载失败'}finally{loading.value=false}
}
function chooseAddress(){uni.navigateTo({url:'/pages/address/list?select=1'})}
async function clearPurchasedItems(){
	await Promise.all(items.value.map(item=>saveCartItem(cartItemPayload(productPayload(item),skuPayload(item),0)))).catch(()=>{})
	const cart=await fetchCart().catch(()=>({items:[]}))
	updateCartTabBadge(cart)
	uni.removeStorageSync('mall_checkout_address_id')
}
function openOrderDetail(orderNo,delay=0){setTimeout(()=>uni.redirectTo({url:`/pages/orders/detail?orderNo=${encodeURIComponent(orderNo)}`}),delay)}
async function submit(){
	if(!selectedAddress.value){uni.showToast({title:'请先选择收货地址',icon:'none'});return}
	if(submitting.value)return; submitting.value=true
	try{
		const first=items.value[0]; const order=await createOrder({merchantNo:first.merchantNo,merchantName:first.merchantName,totalAmount:Number(totalAmount.value.toFixed(2)),discountAmount:0,addressId:selectedAddress.value.id,remark:remark.value.trim(),items:items.value.map(item=>({productId:item.productId,skuId:item.skuId,quantity:item.quantity}))})
		await clearPurchasedItems()
		// #ifdef H5
		try{
			const payment=ensurePaymentCreated(await createPaymentOrder(paymentPayload(order,'miniapp-h5-checkout')))
			openH5PaymentPage(payment.orderNo)
		}catch(error){
			uni.showToast({title:error.message||'订单已创建，支付发起失败',icon:'none',duration:2800})
			openOrderDetail(order.orderNo,900)
		}
		return
		// #endif
		// #ifndef H5
		uni.showToast({title:'订单提交成功',icon:'success'})
		openOrderDetail(order.orderNo,550)
		// #endif
	}catch(error){uni.showToast({title:error.message||'订单提交失败',icon:'none',duration:2800})}finally{submitting.value=false}
}
onLoad(options=>{merchantKey.value=decodeURIComponent(options?.merchantNo||'')})
onShow(()=>{if(merchantKey.value)load()})
</script>

<style scoped>
.page{min-height:100vh;padding:22rpx 22rpx calc(150rpx + env(safe-area-inset-bottom));background:#f3f5f7;color:#26323a}.address-card,.merchant-card,.remark-card,.amount-card{margin-bottom:20rpx;border-radius:27rpx;background:#fff;box-shadow:0 8rpx 24rpx rgba(28,48,56,.04)}.address-card{display:flex;align-items:center;min-height:130rpx;padding:25rpx}.address-icon,.merchant-mark{display:flex;align-items:center;justify-content:center;width:48rpx;height:48rpx;border-radius:15rpx;background:#e4f0ee;color:#0f766e;font-size:20rpx;font-weight:800}.address-copy{flex:1;min-width:0;margin-left:18rpx}.receiver{font-size:27rpx;font-weight:800}.phone{margin-left:18rpx;color:#68747c;font-size:23rpx}.tag{margin-left:12rpx;padding:3rpx 10rpx;border-radius:12rpx;background:#e5f2ef;color:#0f766e;font-size:18rpx}.address-text{display:block;margin-top:12rpx;font-size:23rpx;line-height:1.55;word-break:break-all}.empty-address{flex:1;margin-left:18rpx;font-size:25rpx;font-weight:700}.arrow{color:#a5adb2;font-size:40rpx}.merchant-heading{display:flex;align-items:center;gap:14rpx;padding:24rpx;border-bottom:1rpx solid #edf0f2;font-size:26rpx;font-weight:800}.product-row{display:flex;padding:24rpx;border-bottom:1rpx solid #edf0f2}.product-row:last-child{border-bottom:0}.image{flex:0 0 150rpx;width:150rpx;height:150rpx;border-radius:19rpx;background:#eef1f2}.fallback{display:flex;align-items:center;justify-content:center;color:#6f7b82;font-weight:800}.product-copy{flex:1;min-width:0;margin-left:18rpx}.product-name,.merchant-line,.sku-line{display:block;word-break:break-all}.product-name{font-size:25rpx;font-weight:800;line-height:1.45}.merchant-line{margin-top:7rpx;color:#68747c;font-size:20rpx}.sku-line{margin-top:5rpx;color:#89939a;font-size:20rpx;line-height:1.5}.price-line{display:flex;margin-top:12rpx;color:#d9480f;font-size:26rpx;font-weight:800}.quantity{margin-left:auto;color:#68747c;font-size:21rpx;font-weight:400}.remark-card{display:flex;align-items:center;padding:27rpx;font-size:24rpx}.remark-card input{flex:1;margin-left:24rpx;text-align:right}.amount-card{padding:18rpx 26rpx}.amount-card view{display:flex;justify-content:space-between;padding:12rpx 0;color:#68747c;font-size:23rpx}.amount-card .total{color:#26323a;font-weight:800}.amount-card .total text:last-child{color:#d9480f;font-size:28rpx}.submit-bar{position:fixed;right:0;bottom:0;left:0;display:flex;align-items:center;padding:15rpx 23rpx calc(15rpx + env(safe-area-inset-bottom));background:#fff}.submit-total{flex:1;font-size:23rpx}.submit-total text:last-child{color:#d9480f;font-size:34rpx;font-weight:800}.submit-bar button{width:280rpx;border:0;border-radius:42rpx;background:#0f766e;color:#fff;font-size:26rpx;font-weight:800;line-height:82rpx}.submit-bar button[disabled]{opacity:.55}.state{display:flex;align-items:center;flex-direction:column;padding:180rpx 45rpx;color:#7d8891;text-align:center;line-height:1.7}.retry{margin-top:28rpx;border:0;border-radius:36rpx;background:#0f766e;color:#fff;font-size:24rpx;line-height:72rpx}
</style>

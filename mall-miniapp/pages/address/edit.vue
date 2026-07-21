<template>
	<view class="page">
		<view class="form-card">
			<view class="field"><text class="label">收货人</text><input v-model="form.receiverName" maxlength="30" placeholder="请输入收货人姓名" /></view>
			<view class="field"><text class="label">手机号</text><input v-model="form.phone" type="number" maxlength="11" placeholder="请输入手机号" /></view>
			<picker mode="selector" :range="provinces" range-key="name" @change="chooseProvince"><view class="field"><text class="label">省份</text><text :class="['value',{muted:!form.province}]">{{ form.province || '请选择省份' }}</text><text>›</text></view></picker>
			<picker mode="selector" :range="cities" range-key="name" :disabled="!cities.length" @change="chooseCity"><view class="field"><text class="label">城市</text><text :class="['value',{muted:!form.city}]">{{ form.city || '请选择城市' }}</text><text>›</text></view></picker>
			<picker mode="selector" :range="districts" range-key="name" :disabled="!districts.length" @change="chooseDistrict"><view class="field"><text class="label">区县</text><text :class="['value',{muted:!form.district}]">{{ form.district || '请选择区县' }}</text><text>›</text></view></picker>
			<view class="detail-field"><text class="label">详细地址</text><textarea v-model="form.detailAddress" maxlength="120" placeholder="街道、门牌号、小区、楼栋、单元室等" /></view>
		</view>
		<button class="save" :disabled="saving" @click="submit">{{ saving ? '保存中...' : '保存地址' }}</button>
	</view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { createAddress, fetchRegions, updateAddress } from '@/api/address.js'

const form = reactive({ receiverName:'', phone:'', province:'', city:'', district:'', detailAddress:'' })
const id = ref(''); const provinces=ref([]); const cities=ref([]); const districts=ref([]); const saving=ref(false)

async function regions(parentCode) { try { return await fetchRegions(parentCode) || [] } catch { return [] } }
async function chooseProvince(e) { const item=provinces.value[Number(e.detail.value)]; if(!item)return; form.province=item.name; form.city=''; form.district=''; cities.value=await regions(item.code); districts.value=[] }
async function chooseCity(e) { const item=cities.value[Number(e.detail.value)]; if(!item)return; form.city=item.name; form.district=''; districts.value=await regions(item.code) }
function chooseDistrict(e) { const item=districts.value[Number(e.detail.value)]; if(item) form.district=item.name }
function validate() {
	if (!form.receiverName.trim()) return '请输入收货人姓名'
	if (!/^1\d{10}$/.test(form.phone)) return '请输入正确的手机号'
	if (!form.province || !form.city || !form.district) return '请选择完整省市区'
	if (!form.detailAddress.trim()) return '请输入详细地址'
	return ''
}
async function submit() {
	const message=validate(); if(message){uni.showToast({title:message,icon:'none'});return}
	saving.value=true
	try { const data={...form,receiverName:form.receiverName.trim(),detailAddress:form.detailAddress.trim()}; if(id.value) await updateAddress(id.value,data); else await createAddress(data); uni.removeStorageSync('mall_edit_address'); uni.showToast({title:'地址已保存',icon:'success'}); setTimeout(()=>uni.navigateBack(),450) }
	catch(error){uni.showToast({title:error.message||'保存失败',icon:'none'})} finally{saving.value=false}
}
onLoad(async(options)=>{
	id.value=options?.id||''
	const saved=uni.getStorageSync('mall_edit_address')
	if(saved&&String(saved.id)===String(id.value)) Object.assign(form,saved)
	provinces.value=await regions('0')
	if (!id.value) return
	const province=provinces.value.find(item=>item.name===form.province)
	if (province) cities.value=await regions(province.code)
	const city=cities.value.find(item=>item.name===form.city)
	if (city) districts.value=await regions(city.code)
})
</script>

<style scoped>
.page{min-height:100vh;padding:24rpx;background:#f3f5f7}.form-card{overflow:hidden;border-radius:28rpx;background:#fff}.field{display:flex;align-items:center;min-height:98rpx;padding:0 28rpx;border-bottom:1rpx solid #edf0f2;font-size:25rpx}.label{flex:0 0 150rpx;color:#344049;font-weight:700}.field input,.value{flex:1;min-width:0;color:#26323a}.muted{color:#a7afb4}.detail-field{padding:27rpx 28rpx}.detail-field .label{display:block;margin-bottom:18rpx}.detail-field textarea{box-sizing:border-box;width:100%;height:190rpx;padding:20rpx;border-radius:18rpx;background:#f6f8f9;font-size:25rpx;line-height:1.6}.save{margin-top:34rpx;border:0;border-radius:42rpx;background:#0f766e;color:#fff;font-size:27rpx;font-weight:800;line-height:86rpx}.save[disabled]{opacity:.55}
</style>

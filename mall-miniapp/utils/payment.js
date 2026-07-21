import { getApiBaseUrl } from '@/config/env.js'

export function paymentSubject(order) {
	const items = Array.isArray(order?.items) ? order.items : []
	const firstName = items[0]?.productName || `商城订单 ${order?.orderNo || ''}`
	const count = items.reduce((sum, item) => sum + Number(item.quantity || 0), 0)
	return (count > 1 ? `${firstName} 等 ${count} 件商品` : firstName).slice(0, 128)
}

export function h5ReturnUrl(productOrderNo) {
	let returnUrl = ''
	// #ifdef H5
	const base = `${window.location.origin}${window.location.pathname}`
	returnUrl = `${base}#/pages/orders/detail?orderNo=${encodeURIComponent(productOrderNo || '')}`
	// #endif
	return returnUrl
}

export function paymentPayload(order, source = 'miniapp-h5') {
	return {
		productOrderId: order.id,
		merchantNo: order.merchantNo,
		totalAmount: Number(Number(order.totalAmount || 0).toFixed(2)),
		subject: paymentSubject(order),
		typeIndex: 1,
		goodsType: 1,
		payMethodType: 'ALIPAY_CN',
		attachInfo: JSON.stringify({ source, productOrderNo: order.orderNo }),
		returnUrl: h5ReturnUrl(order.orderNo)
	}
}

export function latestPayment(order) {
	const payments = Array.isArray(order?.payments) ? order.payments : []
	return [...payments].sort((a, b) => String(b.createdAt || '').localeCompare(String(a.createdAt || '')))[0] || null
}

export function ensurePaymentCreated(payment) {
	if (payment?.status !== 'CREATE_SUCCESS' || !payment?.orderNo) {
		throw new Error('支付宝支付单创建失败，请稍后重试')
	}
	return payment
}

export function openH5PaymentPage(paymentOrderNo) {
	// #ifdef H5
	window.location.assign(`${getApiBaseUrl()}/api/mall/pay-orders/${encodeURIComponent(paymentOrderNo)}/pay-page`)
	// #endif
}

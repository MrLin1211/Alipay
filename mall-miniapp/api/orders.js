import { request } from './request.js'

export function fetchOrders() {
	return request({ url: '/api/mall/product-orders', auth: true })
}

export function fetchOrderDetail(orderNo) {
	return request({ url: `/api/mall/product-orders/${encodeURIComponent(orderNo)}`, auth: true })
}

export function createOrder(data) {
	return request({ url: '/api/mall/product-orders', method: 'POST', data, auth: true })
}

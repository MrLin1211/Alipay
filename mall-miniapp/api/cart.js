import { request } from './request.js'

export function fetchCart() {
	return request({ url: '/api/mall/cart', auth: true })
}

export function saveCartItem(item) {
	return request({
		url: '/api/mall/cart/items',
		method: 'POST',
		data: item,
		auth: true
	})
}

export function clearCart() {
	return request({ url: '/api/mall/cart', method: 'DELETE', auth: true })
}

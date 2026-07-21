import { request } from './request.js'

export function createPaymentOrder(data) {
	return request({ url: '/api/mall/pay-orders', method: 'POST', data, auth: true })
}

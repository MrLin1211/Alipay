import { request } from './request.js'

export function fetchAddresses() {
	return request({ url: '/api/mall/addresses', auth: true })
}

export function fetchRegions(parentCode = '0') {
	return request({ url: '/api/mall/regions', params: { parentCode } })
}

export function createAddress(data) {
	return request({ url: '/api/mall/addresses', method: 'POST', data, auth: true })
}

export function updateAddress(id, data) {
	return request({ url: `/api/mall/addresses/${id}`, method: 'PUT', data, auth: true })
}

export function deleteAddress(id) {
	return request({ url: `/api/mall/addresses/${id}`, method: 'DELETE', auth: true })
}

export function setDefaultAddress(id) {
	return request({ url: `/api/mall/addresses/${id}/default`, method: 'PUT', auth: true })
}

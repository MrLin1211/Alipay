import { request } from './request.js'

export function loginByPhone({ phone, password }) {
	return request({
		url: '/api/mall/customer/auth/login',
		method: 'POST',
		data: { phone, password }
	})
}

export function registerByPhone({ phone, password, displayName }) {
	return request({
		url: '/api/mall/customer/auth/register',
		method: 'POST',
		data: { phone, password, displayName }
	})
}

export function fetchCurrentUser() {
	return request({ url: '/api/mall/customer/auth/me', auth: true })
}

export function logoutCurrentUser() {
	return request({ url: '/api/mall/customer/auth/logout', method: 'POST', auth: true })
}

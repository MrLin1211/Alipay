import { getApiBaseUrl } from '@/config/env.js'
import { AUTH_STORAGE_KEYS, clearAuthSession } from '@/utils/auth.js'

function buildUrl(path, params) {
	const normalizedPath = path.startsWith('/') ? path : `/${path}`
	const query = Object.entries(params || {})
		.filter(([, value]) => value !== undefined && value !== null && value !== '')
		.map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
		.join('&')
	return `${getApiBaseUrl()}${normalizedPath}${query ? `?${query}` : ''}`
}

function errorMessage(data, statusCode) {
	if (typeof data === 'string' && data.trim()) return data
	return data?.message || data?.error || `请求失败（${statusCode || '网络异常'}）`
}

export function request({ url, method = 'GET', params, data, auth = false, timeout = 12000 }) {
	const token = uni.getStorageSync(AUTH_STORAGE_KEYS.token)
	return new Promise((resolve, reject) => {
		uni.request({
			url: buildUrl(url, params),
			method,
			data,
			timeout,
			header: {
				'Content-Type': 'application/json',
				...(auth && token ? { Authorization: `Bearer ${token}` } : {})
			},
			success(response) {
				if (response.statusCode >= 200 && response.statusCode < 300) {
					resolve(response.data)
					return
				}
				if (response.statusCode === 401 && auth) {
					clearAuthSession()
					uni.$emit('mall-auth-expired')
				}
				reject(new Error(errorMessage(response.data, response.statusCode)))
			},
			fail(error) {
				reject(new Error(error?.errMsg?.replace('request:fail ', '') || '网络连接失败，请稍后重试'))
			}
		})
	})
}

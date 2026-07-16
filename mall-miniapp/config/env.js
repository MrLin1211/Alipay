const DEFAULT_API_BASE_URL = 'https://api.linsy.online'

export function getApiBaseUrl() {
	const stored = uni.getStorageSync('mall_api_base_url')
	return String(stored || DEFAULT_API_BASE_URL).trim().replace(/\/$/, '')
}

export { DEFAULT_API_BASE_URL }

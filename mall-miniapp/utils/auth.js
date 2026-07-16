export const AUTH_STORAGE_KEYS = {
	token: 'mall_customer_token',
	user: 'mall_customer_user',
	loginPhone: 'mall_login_phone',
	expiresAt: 'mall_customer_expires_at'
}

export function saveAuthSession(session, rememberPhone = '') {
	uni.setStorageSync(AUTH_STORAGE_KEYS.token, session?.token || '')
	uni.setStorageSync(AUTH_STORAGE_KEYS.user, session?.user || null)
	uni.setStorageSync(AUTH_STORAGE_KEYS.expiresAt, session?.expiresAt || '')
	if (rememberPhone) uni.setStorageSync(AUTH_STORAGE_KEYS.loginPhone, rememberPhone)
}

export function clearAuthSession({ keepPhone = true } = {}) {
	uni.removeStorageSync(AUTH_STORAGE_KEYS.token)
	uni.removeStorageSync(AUTH_STORAGE_KEYS.user)
	uni.removeStorageSync(AUTH_STORAGE_KEYS.expiresAt)
	if (!keepPhone) uni.removeStorageSync(AUTH_STORAGE_KEYS.loginPhone)
}

export function getStoredToken() {
	return uni.getStorageSync(AUTH_STORAGE_KEYS.token) || ''
}

export function getStoredUser() {
	const user = uni.getStorageSync(AUTH_STORAGE_KEYS.user)
	if (user && typeof user === 'object') return user
	if (typeof user === 'string') {
		try { return JSON.parse(user) } catch { return null }
	}
	return null
}

export function getRememberedPhone() {
	return uni.getStorageSync(AUTH_STORAGE_KEYS.loginPhone) || ''
}

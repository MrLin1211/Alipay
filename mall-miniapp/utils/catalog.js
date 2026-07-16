import { getApiBaseUrl } from '@/config/env.js'

const categoryStyles = [
	{ icon: '数', background: 'linear-gradient(135deg, #e8f7f4, #ccece7)' },
	{ icon: '家', background: 'linear-gradient(135deg, #fff3e6, #ffe0bd)' },
	{ icon: '鲜', background: 'linear-gradient(135deg, #eef8e8, #d7edca)' },
	{ icon: '美', background: 'linear-gradient(135deg, #fff0f2, #ffd8dd)' },
	{ icon: '动', background: 'linear-gradient(135deg, #eef1ff, #d8defd)' },
	{ icon: '选', background: 'linear-gradient(135deg, #f2effa, #ded6f1)' }
]

export function normalizeCategory(item, index = 0) {
	const style = categoryStyles[index % categoryStyles.length]
	const name = item?.category_name || item?.name || '精选'
	return {
		id: item?.category_code || item?.id || 'all',
		name,
		icon: name.slice(0, 1) || style.icon,
		background: style.background
	}
}

export function normalizeImageUrl(url) {
	if (!url) return ''
	const value = String(url).trim()
	if (/^https?:\/\//i.test(value)) return value
	return `${getApiBaseUrl()}${value.startsWith('/') ? '' : '/'}${value}`
}

export function normalizeProduct(product) {
	const skus = (Array.isArray(product?.skus) ? product.skus : [])
		.filter((sku) => sku.enabled !== false)
		.map((sku) => ({
			id: sku.id,
			skuCode: sku.skuCode || sku.sku_code || '',
			skuName: sku.skuName || sku.sku_name || '默认规格',
			price: Number(sku.price || 0),
			stock: Number(sku.stock || 0)
		}))
	const defaultSku = skus[0]
	const images = (Array.isArray(product?.images) ? product.images : [product?.image])
		.filter(Boolean)
		.map(normalizeImageUrl)
	return {
		id: product?.id,
		merchantNo: product?.merchantNo || product?.merchant_no || '',
		merchantName: product?.merchantName || product?.merchant_name || '精选商家',
		productCode: product?.productCode || product?.product_code || '',
		category: product?.category || 'all',
		categoryName: product?.categoryName || product?.category_name || '',
		name: product?.name || product?.productName || '未命名商品',
		price: Number(defaultSku?.price || product?.price || 0),
		originPrice: Number(product?.originPrice || defaultSku?.price || product?.price || 0),
		stock: Number(defaultSku?.stock ?? product?.stock ?? 0),
		skus,
		image: images[0] || '',
		images,
		desc: product?.desc || product?.description || '',
		fast: Boolean(product?.fast),
		sales: Number(product?.sales || 0)
	}
}

export function formatMoney(value) {
	const amount = Number(value || 0)
	return Number.isInteger(amount) ? String(amount) : amount.toFixed(2)
}

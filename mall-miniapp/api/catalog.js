import { request } from './request.js'

export function fetchCategories() {
	return request({ url: '/api/mall/catalog/categories' })
}

export function fetchProducts({ category = '', keyword = '', page = 0, size = 20 } = {}) {
	return request({
		url: '/api/mall/catalog/products',
		params: { category: category === 'all' ? '' : category, keyword, page, size }
	})
}

export async function fetchProductById(productId) {
	const data = await fetchProducts({ page: 0, size: 100 })
	const product = (data?.content || []).find((item) => String(item.id) === String(productId))
	if (!product) throw new Error('商品不存在或已下架')
	return product
}

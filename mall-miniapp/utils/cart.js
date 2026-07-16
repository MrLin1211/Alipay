export function cartTotalQuantity(cart) {
	return (cart?.items || []).reduce((sum, item) => sum + Number(item.quantity || 0), 0)
}

export function updateCartTabBadge(cart) {
	const count = cartTotalQuantity(cart)
	if (count > 0) {
		uni.setTabBarBadge({ index: 2, text: count > 99 ? '99+' : String(count) })
	} else {
		uni.removeTabBarBadge({ index: 2, fail: () => {} })
	}
}

export function cartItemPayload(product, sku, quantity) {
	return {
		productId: Number(product.id),
		skuId: sku?.id || null,
		quantity: Number(quantity),
		productCode: product.productCode || '',
		skuCode: sku?.skuCode || '',
		skuName: sku?.skuName || '默认规格',
		productName: product.name || '',
		productImage: product.image || '',
		unitPrice: Number(sku?.price ?? product.price ?? 0)
	}
}

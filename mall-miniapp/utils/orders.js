export const ORDER_STATUS = {
	PENDING: { label: '待支付', tone: 'warning' },
	PAID: { label: '待发货', tone: 'primary' },
	SHIPPED: { label: '已发货', tone: 'primary' },
	DELIVERED: { label: '待确认收货', tone: 'primary' },
	COMPLETED: { label: '已完成', tone: 'success' },
	CANCELLED: { label: '已取消', tone: 'muted' },
	REFUNDING: { label: '退款中', tone: 'warning' },
	REFUNDED: { label: '已退款', tone: 'muted' }
}

export function orderStatus(status) {
	return ORDER_STATUS[status] || { label: status || '未知状态', tone: 'muted' }
}

export function orderItems(order) {
	return Array.isArray(order?.items) ? order.items : []
}

export function orderQuantity(order) {
	return orderItems(order).reduce((sum, item) => sum + Number(item.quantity || 0), 0)
}

export function formatDateTime(value) {
	if (!value) return '-'
	return String(value).replace('T', ' ').replace(/\.\d+([+-].*)?$/, '').replace(/Z$/, '')
}

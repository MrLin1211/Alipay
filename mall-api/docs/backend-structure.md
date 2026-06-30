# 后端目录分类

后端接口层按调用方分类，接口 URL 不因目录调整而变化。

## controller/frontend

给自己的前端页面调用的接口。

- `PayOrderController`：创建支付订单、查询本地订单
- `ClientIpController`：调试查看后端识别到的客户端 IP

## controller/admin

给管理后台调用的接口。

- `AdminController`：订单列表、订单详情、支付配置、回调记录

## controller/server

给外部平台或服务端系统调用的接口。

- `PayNotifyController`：接收支付平台异步通知

## controller/common

接口层公共能力。

- `GlobalExceptionHandler`：统一处理参数校验、业务参数和系统异常

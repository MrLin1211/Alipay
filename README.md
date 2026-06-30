# Alipay Project

项目目录已整理为前后端分离结构。

```text
.
├── mall-api/  # 商城后端，接口前缀 /api/mall/**
├── merchant-api/ # 商家后台后端，接口前缀 /api/merchant/**
├── payment-gateway-api/ # 独立支付网关服务
├── payment-gateway-admin/ # 管理后台
├── mall/  # 商城
├── merchant/     # 商家后台
├── ios/       # iOS SwiftUI App
└── README.md  # 项目总说明
```

支付网关说明见：[payment-gateway-api/README.md](payment-gateway-api/README.md)

支付网关后台说明见：[payment-gateway-admin/README.md](payment-gateway-admin/README.md)

本地前后端启动命令见：[docs/start-services.md](docs/start-services.md)

## 后端启动

```bash
sh mall-api/scripts/run-local.sh
sh merchant-api/scripts/run-local.sh
```

## 商城

```bash
cd mall
npm run dev
```

前端说明见：[mall/README.md](mall/README.md)

## 商家后台

```bash
cd merchant
npm run dev
```

后台说明见：[merchant/README.md](merchant/README.md)

## 支付网关后台

```bash
cd payment-gateway-admin
npm run dev
```

默认地址：`http://127.0.0.1:5175`

## iOS App

```bash
open ios/MallhomePayIOS/MallhomePayIOS.xcodeproj
```

iOS 说明见：[ios/MallhomePayIOS/README.md](ios/MallhomePayIOS/README.md)

# Alipay Project

项目目录已整理为前后端分离结构。

```text
.
├── backend/   # Spring Boot 后端项目
├── frontend/  # 原生 HTML/CSS/JS 前端项目
├── admin/     # 支付管理后台
├── ios/       # iOS SwiftUI App
└── README.md  # 项目总说明
```

后端说明见：[backend/README.md](backend/README.md)

本地前后端启动命令见：[docs/start-services.md](docs/start-services.md)

## 后端启动

```bash
sh backend/scripts/run-local.sh
```

## 后续前端

```bash
cd frontend
npm run dev
```

前端说明见：[frontend/README.md](frontend/README.md)

## 管理后台

```bash
cd admin
npm run dev
```

后台说明见：[admin/README.md](admin/README.md)

## iOS App

```bash
open ios/MallhomePayIOS/MallhomePayIOS.xcodeproj
```

iOS 说明见：[ios/MallhomePayIOS/README.md](ios/MallhomePayIOS/README.md)

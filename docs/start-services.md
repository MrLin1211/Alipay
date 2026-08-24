# 前后端服务启动文档

本文档用于本地开发时启动商城后端、商家后台后端、支付网关、商城和后台前端。

> **提示：** 所有服务已配置为同时支持本机访问和局域网访问。在局域网其他设备上访问时，请将 IP 替换为电脑的局域网 IP（如 `192.168.5.55`）。

## 服务地址

| 服务 | 目录 | 端口 | 本机访问 | 局域网访问 |
| --- | --- | --- | --- | --- |
| 商城后端 | `mall-api` | `8080` | `http://127.0.0.1:8080` | `http://192.168.5.55:8080` |
| 商家后台后端 | `merchant-api` | `8081` | `http://127.0.0.1:8081` | `http://192.168.5.55:8081` |
| 支付网关服务 | `payment-gateway-api` | `8090` | `http://127.0.0.1:8090` | `http://192.168.5.55:8090` |
| 商城 | `mall` | `5173` | `http://127.0.0.1:5173` | `http://192.168.5.55:5173` |
| 商家后台 | `merchant` | `5174` | `http://127.0.0.1:5174` | `http://192.168.5.55:5174` |
| 支付网关后台 | `payment-gateway-admin` | `5175` | `http://127.0.0.1:5175` | `http://192.168.5.55:5175` |
| 接入方支付后台 | `gateway-client-admin` | `5176` | `http://127.0.0.1:5176` | `http://192.168.5.55:5176` |
| 支付网关测试端 | `gateway-pay-test` | `5177` | `http://127.0.0.1:5177` | `http://192.168.5.55:5177` |

## 启动前检查

确保本地 MySQL 已启动，并且数据库配置可用：

```bash
mysql -h 127.0.0.1 -P 3306 -u alipay_user -p alipay_pay
```

默认数据库信息：

```text
Database: alipay_pay
Username: alipay_user
Password: alipay123456
```

商城后端本地环境变量配置文件：

```text
mall-api/.env
```

商家后台后端本地环境变量配置文件：

```text
merchant-api/.env
```

## 单独启动商城后端

从项目根目录执行：

```bash
sh mall-api/scripts/run-local.sh
```

或者进入商城后端目录执行：

```bash
cd mall-api
sh scripts/run-local.sh
```

商城后端默认监听所有网卡（`0.0.0.0:8080`），本机和局域网均可访问，商城接口统一为 `/api/mall/**`。

## 单独启动商家后台后端

```bash
sh merchant-api/scripts/run-local.sh
```

商家后台后端默认监听所有网卡（`0.0.0.0:8081`），商家后台接口统一为 `/api/merchant/**`。

## 单独启动支付网关

```bash
sh payment-gateway-api/scripts/run-local.sh
```

支付网关默认监听所有网卡（`0.0.0.0:8090`），本机和局域网均可访问。

## 单独启动商城

```bash
cd mall
npm run dev
```

或者显式指定局域网模式：

```bash
cd mall
npm run dev:lan
```

启动成功后访问：

- 本机：`http://127.0.0.1:5173`
- 局域网：`http://192.168.5.55:5173`

## 单独启动商家后台

```bash
cd merchant
npm run dev -- --host 0.0.0.0 --port 5174
```

启动成功后访问：

- 本机：`http://127.0.0.1:5174`
- 局域网：`http://192.168.5.55:5174`

## 单独启动支付网关后台

```bash
cd payment-gateway-admin
npm run dev
```

启动成功后访问：

- 本机：`http://127.0.0.1:5175`
- 局域网：`http://192.168.5.55:5175`

## 单独启动接入方支付后台

```bash
cd gateway-client-admin
npm run dev
```

启动成功后访问：

- 本机：`http://127.0.0.1:5176`
- 局域网：`http://192.168.5.55:5176`

## 单独启动支付网关测试端

```bash
cd gateway-pay-test
npm run dev
```

启动成功后访问：

- 本机：`http://127.0.0.1:5177`
- 局域网：`http://192.168.5.55:5177`

测试端用于本地联调 OpenAPI，会在浏览器里使用 AppSecret 生成签名。正式业务系统不能这样处理密钥，必须由第三方后端保存密钥并调用支付网关。

## 同时启动所有服务

需要打开八个终端窗口，分别执行：

```bash
# 终端 1 - 商城后端
sh mall-api/scripts/run-local.sh

# 终端 2 - 商家后台后端
sh merchant-api/scripts/run-local.sh

# 终端 3 - 支付网关
sh payment-gateway-api/scripts/run-local.sh

# 终端 4 - 商城
cd mall && npm run dev

# 终端 5 - 商家后台
cd merchant && npm run dev

# 终端 6 - 支付网关后台
cd payment-gateway-admin && npm run dev

# 终端 7 - 接入方支付后台
cd gateway-client-admin && npm run dev

# 终端 8 - 支付网关测试端
cd gateway-pay-test && npm run dev
```

## 局域网访问注意事项

从局域网其他设备（手机、平板等）访问时：

1. **前端页面** 和后端 API 地址要对应：
   - 手机浏览器打开 `http://192.168.5.55:5173`（商城）
   - 在页面设置中将"后端地址"改为 `http://192.168.5.55:8080`
   
2. **商家后台** 同样需要设置后端地址：
   - 打开 `http://192.168.5.55:5174`
   - 登录页的"后端地址"改为 `http://192.168.5.55:8081`

3. **支付网关后台**：
   - 打开 `http://192.168.5.55:5175`
   - 在设置中将网关地址改为 `http://192.168.5.55:8090`

4. **接入方支付后台**：
   - 打开 `http://192.168.5.55:5176`
   - 接入方支付后台调用支付网关服务，接口地址为 `http://192.168.5.55:8090`

5. **支付网关测试端**：
   - 打开 `http://192.168.5.55:5177`
   - 在页面中将网关地址改为 `http://192.168.5.55:8090`

6. 确保防火墙允许对应端口的入站连接（8080, 8081, 8090, 5173, 5174, 5175, 5176, 5177）

## 查看端口占用

```bash
lsof -nP -iTCP -sTCP:LISTEN | rg ':(8080|8081|8090|5173|5174|5175|5176|5177) '
```

正常运行时会看到：

```text
java  ... TCP *:8080 (LISTEN)
java  ... TCP *:8081 (LISTEN)
java  ... TCP *:8090 (LISTEN)
node  ... TCP *:5173 (LISTEN)
node  ... TCP *:5174 (LISTEN)
node  ... TCP *:5175 (LISTEN)
node  ... TCP *:5176 (LISTEN)
node  ... TCP *:5177 (LISTEN)
```

> `*:PORT` 表示监听所有网卡，局域网可访问。

## 停止服务

如果服务是在当前终端启动的，直接按：

```text
Control + C
```

如果需要按端口停止，先查 PID：

```bash
lsof -nP -iTCP -sTCP:LISTEN | rg ':(8080|8081|8090|5173|5174|5175|5176|5177) '
```

然后停止对应进程：

```bash
kill <PID>
```

示例：

```bash
kill 12345
```

## 常见问题

### 8080 或 8081 端口被占用

说明已有商城后端或商家后台后端服务在运行。先查 PID：

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
lsof -nP -iTCP:8081 -sTCP:LISTEN
```

再停止对应进程：

```bash
kill <PID>
```

### 前端请求后端失败

检查后端是否已启动：

```bash
curl http://127.0.0.1:8080/api/mall/catalog/categories
curl http://127.0.0.1:8081/api/merchant/categories
```

如果 iOS 模拟器/真机访问后端失败，把 App 里的后端地址改成电脑局域网 IP：

```text
http://192.168.5.55:8080
```

商家后台则使用：

```text
http://192.168.5.55:8081
```

### 商家后台端口不是 5174

Vite 默认可能自动切换端口。请使用固定端口启动命令：

```bash
cd merchant
npm run dev -- --host 0.0.0.0 --port 5174
```

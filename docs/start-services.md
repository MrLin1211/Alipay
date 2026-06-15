# 前后端服务启动文档

本文档用于本地开发时启动支付后端、前端支付页和管理后台。

## 服务地址

| 服务 | 目录 | 端口 | 访问地址 |
| --- | --- | --- | --- |
| 后端服务 | `backend` | `8080` | `http://127.0.0.1:8080` |
| 前端支付页 | `frontend` | `5173` | `http://127.0.0.1:5173` |
| 管理后台 | `admin` | `5174` | `http://127.0.0.1:5174` |

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

后端本地环境变量配置文件：

```text
backend/.env
```

## 单独启动后端

从项目根目录执行：

```bash
sh backend/scripts/run-local.sh
```

或者进入后端目录执行：

```bash
cd backend
sh scripts/run-local.sh
```

启动成功后，控制台会看到类似日志：

```text
Tomcat started on port 8080
Started MallhomePayApplication
```

## 单独启动前端支付页

```bash
cd frontend
npm run dev
```

启动成功后访问：

```text
http://127.0.0.1:5173
```

## 单独启动管理后台

为了固定管理后台端口为 `5174`，建议使用：

```bash
cd admin
npm run dev -- --host 127.0.0.1 --port 5174
```

启动成功后访问：

```text
http://127.0.0.1:5174
```

## 同时启动三个服务

需要打开三个终端窗口，分别执行：

```bash
sh backend/scripts/run-local.sh
```

```bash
cd frontend
npm run dev
```

```bash
cd admin
npm run dev -- --host 127.0.0.1 --port 5174
```

## 查看端口占用

```bash
lsof -nP -iTCP -sTCP:LISTEN | rg ':(8080|5173|5174) '
```

正常运行时会看到：

```text
java  ... TCP *:8080 (LISTEN)
node  ... TCP 127.0.0.1:5173 (LISTEN)
node  ... TCP 127.0.0.1:5174 (LISTEN)
```

## 停止服务

如果服务是在当前终端启动的，直接按：

```text
Control + C
```

如果需要按端口停止，先查 PID：

```bash
lsof -nP -iTCP -sTCP:LISTEN | rg ':(8080|5173|5174) '
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

### 8080 端口被占用

说明已有后端服务在运行。先查 PID：

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
```

再停止对应进程：

```bash
kill <PID>
```

### 前端请求后端失败

检查后端是否已启动：

```bash
curl http://127.0.0.1:8080/api/admin/pay-config
```

如果 iOS 模拟器访问后端失败，可以把 App 里的后端地址从：

```text
http://localhost:8080
```

改成电脑局域网 IP：

```text
http://192.168.x.x:8080
```

### 管理后台端口不是 5174

Vite 默认可能自动切换端口。请使用固定端口启动命令：

```bash
cd admin
npm run dev -- --host 127.0.0.1 --port 5174
```

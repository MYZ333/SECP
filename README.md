# 智慧医养大数据公共服务平台 · 个人健康档案系统

前后端分离项目。五个独立目录：

- `backend/` —— Java 后端（Spring Boot）
- `web/` —— Web 端（Vue3 + Element Plus，含管理端）
- `miniprogram/` —— 小程序端（uni-app，面向老人，登录后直达 AI 健康咨询）
- `doctor-web/` —— 医生 Web 端（Vue3 + Element Plus）
- `doctor-miniprogram/` —— 医生小程序端（uni-app）

## 技术栈

| 端 | 技术 |
|----|------|
| 后端 | JDK 21、Spring Boot 3.5.8、Spring Security + JWT、MyBatis-Plus、MySQL 8、Redis、Knife4j、Spring AI(骨架) |
| Web | Vue3 + Vite + Element Plus + Pinia + Vue Router + Axios |
| 小程序 | uni-app (Vue3)，可编译到微信小程序 / H5 |
| 医生 Web | Vue3 + Vite + Element Plus + Pinia + Vue Router + Axios |
| 医生小程序 | uni-app (Vue3)，可编译到微信小程序 / H5 |

后端包名：`com.medcare.hda`。演示账号密码统一为 `123456`：管理员 `admin`、普通用户 `user001`、医生 `doctor1` 至 `doctor20`。

---

# 快速启动（照着做即可）

启动顺序很重要：**先开 MySQL + Redis → 建数据库 → 启动后端 → 启动前端**。后端没起来，前端登录会报错。

## 第 0 步：装好环境并检查

打开命令行（Windows 用 PowerShell 或 CMD），逐条检查是否已安装：

```bash
java -version      # 需要 21
mvn -version       # 需要 3.9+（用 IDEA 启动可跳过）
node -v            # 需要 18+
```

还需要本机装好并**启动**：

- **MySQL 8**（默认端口 3306）
- **Redis**（默认端口 6379，登录时会用到）

检查 Redis 是否在跑：

```bash
redis-cli ping     # 返回 PONG 即正常
```

## 第 1 步：初始化数据库

用**命令行**执行建库脚本（脚本里已含 `CREATE DATABASE`，会自动建库、建表、插入初始数据）：

```powershell
mysql -u root -p --default-character-set=utf8mb4 -e "source backend/src/main/resources/schema.sql"
```

> 一定要加 `--default-character-set=utf8mb4`，否则中文会报 `Data too long`。
> 也可以用 Navicat 打开 `schema.sql` 全选执行。

验证：

```bash
mysql -u root -p -e "USE hda_db; SELECT username, role FROM sys_user;"
```

能看到 `admin`、`user001` 以及 `doctor1` 至 `doctor20` 即成功。

### 已有数据库升级

如果数据库已经由旧版 `schema.sql` 初始化，不要重新执行会清表的完整初始化脚本。先执行医生咨询结构升级脚本：

```powershell
mysql -u root -p --default-character-set=utf8mb4 -e "source backend/src/main/resources/doctor_consult_upgrade.sql"
```

如果之前已经执行过医生咨询升级，只需要补充或重置全部现有医生账号：

```powershell
mysql -u root -p --default-character-set=utf8mb4 -e "source backend/src/main/resources/doctor_seed_accounts.sql"
```

账号脚本会按 `doctor.id` 顺序自动生成 `doctor1`、`doctor2`……，统一密码 `123456`，并将账号关联到对应医生。脚本可重复执行；新增医生后再次执行，会继续按数字后缀分配。

## 第 2 步：改数据库连接配置

打开 `backend/src/main/resources/application.yml`，把密码改成**你自己的 MySQL 密码**：

```yaml
spring:
  datasource:
    username: root
    password: 你的MySQL密码      # ← 改这里
  data:
    redis:
      host: localhost
      port: 6379
      password:                  # Redis 没设密码就留空
```

## 第 3 步：启动后端

**方式 A —— IDEA（推荐）**

1. IDEA 里 `File → Open`，选 **`backend`** 文件夹（不是最外层，要选 backend）
2. 等右下角 Maven 依赖下载完（首次较慢）
3. 打开 `src/main/java/com/medcare/hda/HdaApplication.java`，点 `main` 左边绿色三角 ▶ 运行

**方式 B —— 命令行**

```bash
cd backend
mvn spring-boot:run
```

**成功标志**：控制台出现 `Started HdaApplication`。
- 服务地址：http://localhost:8080
- 在线接口文档（Knife4j）：http://localhost:8080/doc.html

## 第 4 步：启动 Web 端

新开一个终端（后端别关）：

```bash
cd web
npm install       # 首次装依赖，几分钟
npm run dev
```

浏览器打开 **http://localhost:5173/** ，用 `admin/123456` 或 `user001/123456` 登录。
（Vite 已配 `/api` 代理到后端 8080，后端也开了 CORS，无需额外设置。）

## 第 5 步：启动医生 Web 端

新开一个 PowerShell 终端：

```powershell
Set-Location doctor-web
npm install
npm run dev
```

浏览器打开 **http://localhost:5176/**，使用 `doctor1/123456` 至 `doctor20/123456` 登录。医生可查看工作台、患者列表、患者详情、实时咨询会话和个人资料。

医生自行注册后默认不能登录，需要管理员在 Web 管理端的“医生专家管理”页面审核通过。

## 第 6 步：启动用户小程序端

再新开一个终端：

```bash
cd miniprogram
npm install
npm run dev:h5            # 浏览器预览（最快）
```

浏览器打开 **http://localhost:5175/**（小程序端专用端口）。登录后会**直接进入 AI 健康咨询页**，点页面右上角「更多功能」进入其它页面。

要在**微信开发者工具**里看真实小程序效果：

```bash
npm run dev:mp-weixin    # 生成 dist/dev/mp-weixin
```

然后打开微信开发者工具 → 导入项目 → 选 `miniprogram/dist/dev/mp-weixin` 目录。
真机调试时，把 `miniprogram/src/api/request.js` 里的 `BASE_URL` 从 `localhost` 改成电脑局域网 IP。

## 第 7 步：启动医生小程序端

```powershell
Set-Location doctor-miniprogram
npm install
npm run dev:h5
```

构建微信小程序：

```powershell
npm run build:mp-weixin
```

使用微信开发者工具导入 `doctor-miniprogram/dist/build/mp-weixin`。真机调试前，需要把 `doctor-miniprogram/src/api/request.js` 以及咨询页面中的 WebSocket 地址由 `localhost` 改为电脑局域网 IP，并在微信小程序后台配置合法请求域名和 WebSocket 域名。

## 端口一览

| 服务 | 地址 |
|------|------|
| 后端 API | http://localhost:8080 |
| 接口文档 | http://localhost:8080/doc.html |
| Web 端 | http://localhost:5173 |
| 小程序端(H5) | http://localhost:5175 |
| 医生 Web 端 | http://localhost:5176 |
| 医生小程序端(H5) | http://localhost:5177 |

---

# 常见问题排查

**登录报 `Failed to obtain JDBC Connection`**
后端连不上 MySQL。检查 MySQL 是否启动、`application.yml` 里的密码是否正确，改完**重启后端**。

**登录报「用户名或密码错误」，但账号没错**
确认用的是登录**用户名**（不是昵称）；种子账号 `user001 / 123456`、`admin / 123456` 一定能登。

**建库报 `Data too long for column`**
中文编码问题，导入命令要带 `--default-character-set=utf8mb4`（见第 1 步）。

**后端编译报找不到 `PaginationInnerInterceptor`**
MyBatis-Plus 3.5.6+ 分页需单独依赖，`pom.xml` 已加 `mybatis-plus-jsqlparser`，Reload Maven 即可。

**小程序白屏、控制台报 `getEscapedCssVarName`**
Vue 版本冲突，`package.json` 已用 `overrides` 锁定 Vue 3.4.21。若仍报错，删掉 `miniprogram/node_modules` 和 `package-lock.json` 重新 `npm install`。

**小程序 `npm run dev` 报 Missing script**
小程序端没有 `dev`，用 `npm run dev:h5`。

**端口被占用**
谁先启动谁占端口。已固定：Web 5173、小程序 5175，可同时开。

---

# 功能模块

| 模块 | 说明 | Web 端 | 小程序端 |
|------|------|:---:|:---:|
| 登录 / 注册 | Spring Security + JWT | ✅ | ✅ |
| 健康档案 | 基本信息 / 体征 / 就诊 / 报告（CRUD） | ✅ | ✅ |
| 个人积分 | 余额 / 明细 / 商城 / 兑换 | ✅ | ✅ |
| 账户管理 | 资料 / 改密码 / 注销 | ✅ | ✅ |
| 医生专家库 | 专家查询 | ✅ | ✅ |
| 健康助手 | AI 对话（骨架） | ✅ | ✅ |
| 医生咨询 | 患者选择医生、WebSocket 实时对话、附件 | ✅ | — |
| 健康预警 | AI 分析预警（骨架） | ✅ | ✅ |
| 系统管理 | 用户 / 商品 / 专家管理 / 医生审核 | ✅ | — |

医生端提供工作台、患者列表、患者详情、咨询会话和个人资料。医生可查看所有状态正常的患者及其健康资料；咨询会话和消息仍严格限定为向当前医生发起的会话。患者详情包括昵称、性别、年龄、健康档案、体征、就诊记录、健康报告和预警记录。医生 Web 与医生小程序均支持实时消息和附件。

> **Spring AI 为骨架**：所有大模型交互统一收口在 `service/AiChatService`，`AiChatServiceImpl`
> 已注入 `ChatClient.Builder`，未配置有效模型时返回占位文案。接入具体模型（通义/OpenAI/智谱等）
> 只需在 `application.yml` 配置 `spring.ai.*` 并在 `AiChatServiceImpl.consult()` 放开调用。

# 后端目录结构

```
com.medcare.hda
├─ common        统一响应 Result / 分页 PageResult / 状态码
├─ config        MyBatis-Plus / Redis / Knife4j / 字段自动填充
├─ security      Spring Security + JWT（过滤器、UserDetails、配置、JSON 异常处理）
├─ exception     业务异常 + 全局异常处理
├─ entity        实体类
├─ mapper        MyBatis-Plus Mapper
├─ dto           请求 / 响应对象
├─ service/impl  业务逻辑
└─ controller    接口层
```

# 鉴权说明

- 登录成功返回 JWT，前端存 localStorage，请求头自动带 `Authorization: Bearer <token>`。
- 白名单：登录、注册、Knife4j 文档；`/api/admin/**` 需要 `ADMIN` 角色。
- 登录态同时写入 Redis（`hda:login:token:{userId}`）。

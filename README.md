# 智慧医养大数据公共服务平台 · 个人健康档案系统

前后端分离项目。主要目录如下：

- `backend/` —— Java 后端（Spring Boot）
- `web/` —— Web 端（Vue3 + Element Plus，含管理端）
- `miniprogram/` —— 小程序端（uni-app，面向老人，登录后直达 AI 健康咨询）
- `doctor-web/` —— 医生 Web 端（Vue3 + Element Plus）
- `doctor-miniprogram/` —— 医生小程序端（uni-app）
- `apk-output/` —— 患者端和医生端 Android 测试 APK 输出目录

## 技术栈

| 端 | 技术 |
|----|------|
| 后端 | JDK 21、Spring Boot 3.5.8、Spring Security + JWT、MyBatis-Plus、MySQL 8、Redis、Knife4j、Spring AI 1.0.9、Spring AI Alibaba（百炼） |
| Web | Vue3 + Vite + Element Plus + Pinia + Vue Router + Axios |
| 小程序 | uni-app (Vue3)，可编译到微信小程序 / H5 |
| 医生 Web | Vue3 + Vite + Element Plus + Pinia + Vue Router + Axios |
| 医生小程序 | uni-app (Vue3)，可编译到微信小程序 / H5 |
| Android 测试端 | Capacitor 8 + Android SDK 36，将 `web/` 与 `doctor-web/` 分别封装为 APK |

后端包名：`com.medcare.hda`。演示账号密码统一为 `123456`：管理员 `admin`、普通用户 `user001`、医生 `doctor1` 至 `doctor20`。

---

# 快速启动（照着做即可）

启动顺序很重要：**先开 MySQL + Redis + Chroma → 建数据库 → 启动后端 → 启动 Web 前端**。健康咨询 Agent 与现有 Spring Boot 工程是一体化模块，不使用 Docker，也不需要单独部署 Agent 服务。

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
- **Chroma**（默认端口 8000，健康知识库向量检索使用）

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

### 配置阿里云百炼

健康助手使用 Spring AI 1.0.9 与 Spring AI Alibaba，对接阿里云百炼聊天、向量化与重排模型。未配置密钥时后端仍可启动；使用聊天、向量化或重排功能前需要设置 `APIKEY` 环境变量，密钥不会写入项目配置文件：

```powershell
$env:APIKEY = "你的百炼 API Key"                     # 仅当前 PowerShell 会话
# 使用 qwen3-rerank 时按百炼控制台填写 Workspace ID
$env:AI_DASHSCOPE_WORKSPACE_ID = "你的 Workspace ID"
```

聊天模型默认 `qwen3.7-plus`，Embedding 默认 `text-embedding-v4`（1024 维），重排默认 `qwen3-rerank`。不配置 Workspace ID 时，RAG 会保留向量召回顺序，不会影响系统启动。

RAG 查询会先由聊天模型改写成独立的语义检索句和关键词；没有配置真实 API Key 时自动使用规则改写。召回阶段并行使用 Chroma 向量检索和 MySQL 已发布知识切片的关键词检索，再以加权 RRF 融合后送入重排模型。任一召回通道不可用时会自动使用另一个通道。可通过 `RAG_QUERY_REWRITE_ENABLED=false` 或 `RAG_HYBRID_ENABLED=false` 分别关闭模型改写或关键词召回。

个体症状、病因询问和个体化建议会先经过逐步问诊门控：每轮只询问 1 个关键问题，并提供快捷选项和自由回答；信息足够后才汇总全部回答并启动咨询与 RAG，纯健康科普则直接检索回答。默认最多追问 6 轮以避免循环，用户也可以回复“直接回答”或“不知道”跳过剩余追问，系统会明确说明信息局限。可通过 `AGENT_INTAKE_MAX_ROUNDS` 调整上限，通过 `AGENT_INTAKE_ENABLED=false` 关闭模型门控并使用规则降级。

知识文档默认启用 Embedding 语义分块：标题为强边界，以句子上下文向量的相邻余弦距离识别主题突变点；默认使用第 80 百分位、绝对距离 0.12、最小 280 字和最大 1100 字。可通过 `RAG_SEMANTIC_CHUNKING_ENABLED=false` 临时降级到章节/标点规则分块。

已有知识文档需要在管理端点击“重建索引”才会重新读取原文件并应用语义分块；仅重启后端不会修改历史 chunk。没有保留原始文件的旧文档请重新上传。

### 启动 Chroma（原生安装，无 Docker）
注意：如果你本机有chroma，执行chroma run --host 0.0.0.0 --port 8000即可。下面的脚本有问题，可以不管
首次运行先安装项目锁定的 Chroma 版本。脚本使用独立 Python 虚拟环境，不会污染全局 Python：

```powershell
.\scripts\install-chroma.ps1
```

新开一个 PowerShell，在项目根目录以前台方式运行：

```powershell
eb
```

也可以后台启动、检查和停止：

```powershell
.\scripts\start-chroma.ps1 -Background
.\scripts\test-chroma.ps1
.\scripts\stop-chroma.ps1
```

看到 Chroma 监听 `http://127.0.0.1:8000` 后即表示启动成功。数据持久化到项目的 `data/rag/chroma/`；后台日志也位于该目录。

如果临时不使用 AI/RAG，只想启动其余后端功能，可在启动后端前设置：

```powershell
$env:RAG_ENABLED = "false"
$env:AI_VECTOR_STORE_TYPE = "none"
```

Docker 部署无需单独安装 Chroma，`deploy/docker-compose.yml` 已包含固定版本的 Chroma 服务、持久化卷和健康检查。

健康助手接口：

- `POST /api/consult/chat`：同步聊天，兼容现有前端。
- `POST /api/consult/chat/stream`：SSE 流式聊天，包含 `meta`、`stage`、`risk`、`citation`、`delta`、`done` 事件；异常时返回 `error`。
- `GET /api/consult/history`：从 MySQL 持久化记忆中分页查询历史；可传 `sessionId` 过滤单个会话。
- `/api/admin/knowledge/**`：管理员上传、预览、发布、重建和停用知识文档。

应用使用助手接口：

- `POST /api/app-assistant/chat/stream`：独立的 SSE 流式聊天，仅使用应用助手提示词，不读取健康 RAG、健康档案或健康助手会话记忆。

首次使用知识库：管理员登录 Web 端，进入“智能体知识库”，切换到“健康助手”或“应用使用助手”，点击“导入首批语料”，预览分块后发布。发布时会调用 Embedding 模型并写入 Chroma；两类资料只会被对应助手检索。

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

## 已有数据库升级：药品库与用药建议

如果不是全新执行 `schema.sql`，需要在已有 `hda_db` 上补充药品库和用药建议单表：

```powershell
mysql -u root -p --default-character-set=utf8mb4 -e "source backend/src/main/resources/medicine_upgrade.sql"
```

执行后管理员端会出现“药品库”入口。管理员维护启用药品后，医生在咨询报告完成后可基于会话添加“用药建议单”；患者端会在咨询结果页查看药品、用法用量和注意事项，并点击“我已知晓”确认。

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
| Chroma 向量库 | http://localhost:8000 |
| 后端 API | http://localhost:8080 |
| 接口文档 | http://localhost:8080/doc.html |
| Web 端 | http://localhost:5173 |
| 小程序端(H5) | http://localhost:5175 |
| 医生 Web 端 | http://localhost:5176 |
| 医生小程序端(H5) | http://localhost:5177 |

## Android 测试 APK

项目使用 Capacitor 将现有 Vue Web 页面封装为两个独立 Android 应用。Android 构建只读取 `.env.android`，不会改变 `npm run dev` 使用的本地 Vite 代理。

| 应用 | Android 包名 | Android 环境配置 | 当前测试服务器 |
|------|-------------|------------------|----------------|
| 患者端 | `com.secp.hda.patient` | `web/.env.android` | `http://8.137.115.167` |
| 医生端 | `com.secp.hda.doctor` | `doctor-web/.env.android` | `http://8.137.115.167:8081` |

快速重新构建患者端：

```powershell
Set-Location D:\SECP\SECP\web
npm run android:sync
Set-Location android
$env:GRADLE_USER_HOME = "D:\GradleCache"
.\gradlew.bat assembleDebug
```

快速重新构建医生端：

```powershell
Set-Location D:\SECP\SECP\doctor-web
npm run android:sync
Set-Location android
$env:GRADLE_USER_HOME = "D:\GradleCache"
.\gradlew.bat assembleDebug
```

原始 APK 分别生成在 `web/android/app/build/outputs/apk/debug/app-debug.apk` 和 `doctor-web/android/app/build/outputs/apk/debug/app-debug.apk`。环境安装、服务器切换、真机安装、正式签名和故障排查请阅读 [APK 打包教程](APK_BUILD.md)。

> 当前测试服务器使用 HTTP，因此 Capacitor 配置已允许明文流量，仅适用于课程项目和内部测试。正式发布时应改为 HTTPS。服务器还需要正确代理 `/ws/doctor-consult`，否则普通 API 可用但实时咨询无法建立 WebSocket 连接。

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
| 健康助手 | 百炼 `qwen3.7-plus` 对话、SSE 流式输出、MySQL 持久化会话记忆与历史查询 | ✅ | ✅ |
| 应用使用助手 | 顶部控制栏对话浮窗、百炼 `qwen3.7-plus`、SSE 流式输出，与健康 RAG 隔离 | ✅ | — |
| 医生咨询 | 患者选择医生、WebSocket 实时对话、附件、咨询报告、评价闭环、用药建议确认 | ✅ | — |
| 健康预警 | AI 分析预警（骨架） | ✅ | ✅ |
| 系统管理 | 用户 / 商品 / 药品库 / 专家管理 / 医生审核 | ✅ | — |

医生端提供工作台、患者列表、患者详情、咨询会话和个人资料。医生可查看所有状态正常的患者及其健康资料；咨询会话和消息仍严格限定为向当前医生发起的会话。患者详情包括昵称、性别、年龄、健康档案、体征、就诊记录、健康报告和预警记录。医生 Web 与医生小程序均支持实时消息和附件。

医生咨询闭环：患者结束咨询后，医生端保留“待报告”会话，医生提交结构化咨询报告后患者端开放评价。若需要给出用药建议，医生只能从管理员维护的启用药品中选择药品，填写用法、剂量、频次、天数、数量和注意事项；系统会拦截停用药品、需线下确认药品、超出最大建议天数以及缺少剂量/频次的明细。患者端收到用药建议后需要点击“我已知晓”确认，确认后的建议单医生端不可再编辑。

> **健康助手 Agent**：所有聊天能力位于 `agent/` 包，使用 `ChatClient`、`MessageChatMemoryAdvisor` 与 MySQL `SPRING_AI_CHAT_MEMORY` 表实现会话记忆。每个会话只将最近 20 条消息传给模型，但完整历史会持久化保存，接口可继续查询。

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
├─ agent         健康助手 Agent / ChatMemory / SSE 响应 / 会话隔离
└─ controller    接口层
```

# 鉴权说明

- 登录成功返回 JWT，前端存 localStorage，请求头自动带 `Authorization: Bearer <token>`。
- 白名单：登录、注册、Knife4j 文档；`/api/admin/**` 需要 `ADMIN` 角色。
- 登录态同时写入 Redis（`hda:login:token:{userId}`）。

## Chroma 首次初始化与验收

Chroma 服务启动成功不代表知识库已经有数据。后端启动时只自动创建或复用
`medical_knowledge` 和 `long_term_memory` 两个集合，不会自动导入种子语料，也不会补齐缺失向量。

本地首次启动：

```powershell
.\scripts\install-chroma.ps1
.\scripts\start-chroma.ps1 -Background
Set-Location backend
mvn spring-boot:run
```

后端启动完成后，由管理员登录 Web 端进入“智能体知识库”，选择助手并点击“导入首批语料”，
预览分块后逐份发布。发布时才会调用 Embedding 模型并写入 Chroma。随后可在项目根目录验证集合和记录数：

```powershell
.\scripts\test-chroma.ps1
```

也可以运行 `.\scripts\bootstrap-chroma.ps1` 启动 Chroma；知识语料仍需在管理界面手动导入和发布。
尚未手动发布语料时，`medical_knowledge` 为空是正常现象。

全新 MySQL 使用完整的 `backend/src/main/resources/schema.sql`。已有数据库不要删除数据卷，执行：

```powershell
mysql -u root -p --default-character-set=utf8mb4 -e "source backend/src/main/resources/long_term_memory_upgrade.sql"
```

Docker 首次启动会自动执行完整 SQL，并由后端自动创建 Chroma collection，但不会自动填充知识向量：

```powershell
Set-Location deploy
docker compose up -d --build
docker compose logs -f backend
```

容器启动后仍需在管理员知识库界面手动导入并发布首批语料。

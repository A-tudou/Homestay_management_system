# 山居小筑 · 民宿预订系统

一个前后端分离的民宿预订与运营管理系统。房客可以按日期、人数和间数搜索房源，查看每晚价格并在线下单；房东可以维护房型与房间、配置每日价格和库存，并处理订单的确认、入住、完成与取消。

后端基于 Spring Boot 3 + MyBatis-Plus + MySQL，前端基于 Vue 3 + Vite + Element Plus，鉴权使用 JWT，权限分为房客（`CUSTOMER`）与房东（`HOST`）。

> 适合作为 Java Web 课程设计、毕业设计原型，或学习「日历库存 + 订单状态机 + 并发防超卖」的参考项目。

## 功能特性

### 房客端

- 房型列表、关键词筛选、按日期 / 人数 / 间数搜索
- 房型详情、未来 30 天价格日历、整段入住报价
- 注册、登录、JWT 会话保持
- 在线下单，系统自动分配具体房间
- 我的订单、订单详情、取消订单

### 管理端

- 房型增删改查、上下架
- 房间增删改查、可用 / 维修状态切换
- 日历生成、单日改价改库存、区间批量设置、关房
- 订单分页查询、确认、拒绝、办理入住、完成
- 订单状态日志与分配房号展示

### 工程能力

- 统一响应体 `{ code, message, data }` 与全局异常处理
- 无状态 JWT 鉴权，管理端接口按角色隔离
- 每晚库存的条件更新，防止并发超卖
- 下单时对候选房间加行锁，避免重复分配
- 取消订单按实际占用夜晚精确归还库存
- Windows 一键启动脚本，自动检测端口与依赖

## 技术栈

| 层次 | 技术 |
| --- | --- |
| 后端 | Java 17、Spring Boot 3.5.16、Spring MVC、Spring Security、Spring Validation |
| 持久层 | MyBatis-Plus 3.5.18、MySQL 8 |
| 鉴权 | JWT（jjwt 0.12.6）、BCrypt |
| 接口文档 | springdoc-openapi 2.8.17（Swagger UI） |
| 前端 | Vue 3.5、Vite 6、Vue Router 4、Element Plus、Axios |
| 构建 | Maven、npm |

## 系统架构

```text
┌─────────────────────────────┐
│  Vue 3 + Element Plus       │
│  房客端 / 管理端             │
└──────────────┬──────────────┘
               │ /api  (Vite 代理 / Nginx 反向代理)
┌──────────────▼──────────────┐
│  Spring Boot                │
│  Controller → Service → Mapper
│  Spring Security + JWT      │
└──────────────┬──────────────┘
               │ MyBatis-Plus
┌──────────────▼──────────────┐
│  MySQL 8                    │
│  users / room_type / room   │
│  room_calendar / orders     │
│  order_room / order_stay    │
│  order_log                  │
└─────────────────────────────┘
```

## 目录结构

```text
.
├─ homestay-booking/              后端 Spring Boot 工程
│  ├─ src/main/java/com/homestay/
│  │  ├─ common/                  统一响应、错误码、全局异常
│  │  ├─ config/                  安全配置、演示数据、房东初始化
│  │  ├─ security/                JWT 生成、解析与请求过滤器
│  │  ├─ user/                    注册、登录、用户
│  │  ├─ roomtype/                房型管理与房客端房源查询
│  │  ├─ room/                    物理房间管理
│  │  ├─ calendar/                日历价格、库存、报价
│  │  └─ order/                   订单、房间分配、状态流转
│  ├─ src/main/resources/
│  │  ├─ application.yml          开发配置
│  │  ├─ application-prod.yml     生产配置（环境变量注入）
│  │  └─ db/schema.sql            建表脚本，启动时自动执行
│  └─ README.md                   后端接口清单与运行说明
├─ homestay-web/                  前端 Vue 3 工程
│  ├─ public/images/              本地图片资源
│  └─ src/
│     ├─ api/                     接口封装与 Axios 拦截器
│     ├─ composables/             登录状态
│     ├─ layouts/                 管理端布局
│     ├─ router/                  路由与权限守卫
│     ├─ views/                   房客端页面
│     ├─ views/admin/             管理端页面
│     └─ components/              房型卡片等复用组件
├─ docs/使用说明.md               启动、操作、配置与排障
├─ start-local.bat                Windows 一键启动入口
└─ start-local.ps1                启动脚本
```

## 快速开始

### 环境要求

| 依赖 | 版本 |
| --- | --- |
| JDK | 17 及以上 |
| Maven | 3.8 及以上 |
| MySQL | 8.0 及以上 |
| Node.js | 18 及以上 |

### 1. 创建数据库与账号

用 MySQL 管理员账号执行：

```sql
CREATE DATABASE IF NOT EXISTS homestay
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

CREATE USER IF NOT EXISTS 'homestay_app'@'localhost' IDENTIFIED BY 'Homestay@2026';
GRANT ALL PRIVILEGES ON homestay.* TO 'homestay_app'@'localhost';
FLUSH PRIVILEGES;
```

> 以上账号密码是本地开发默认值，与 `homestay-booking/src/main/resources/application.yml` 保持一致。**开源部署或上线前请务必修改**，并同步修改配置文件。

表结构由应用启动时自动执行 `db/schema.sql` 创建，脚本全部使用 `CREATE TABLE IF NOT EXISTS`，可以重复执行。

### 2. 启动后端

```bash
cd homestay-booking
mvn spring-boot:run
```

默认地址 `http://localhost:8080`。首次启动会自动创建表结构；如果库中没有任何房东账号，还会创建一个调试管理员：

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `admin123` | `HOST`（房东） |

### 3. 启动前端

```bash
cd homestay-web
npm install
npm run dev
```

默认地址 `http://localhost:5173`。Vite 会将 `/api` 代理到 `http://localhost:8080`，前端代码里不需要写后端地址。

### 4. 访问系统

| 入口 | 地址 |
| --- | --- |
| 房客端 | <http://localhost:5173/> |
| 管理端 | <http://localhost:5173/admin> |
| 接口文档 | <http://localhost:8080/swagger-ui.html> |
| OpenAPI JSON | <http://localhost:8080/v3/api-docs> |

### 一键启动（Windows）

双击根目录的 `start-local.bat`，脚本会依次：

1. 检查 MySQL 的 3306 端口；
2. 在 8080 未运行时启动后端，等待接口就绪；
3. 在 5173 未运行时启动前端，缺少依赖会先执行 `npm install`；
4. 打开浏览器。

已经在运行的服务会显示 `[skip]`，不会重复启动。关闭弹出的后端 / 前端窗口即可停止对应服务。

## 演示账号

| 角色 | 账号 | 密码 | 说明 |
| --- | --- | --- | --- |
| 房东 | `admin` | `admin123` | 首次启动自动创建，仅用于本地调试 |
| 房客 | 自行注册 | 自定 | 注册接口固定创建 `CUSTOMER` 角色 |

当前版本没有提供修改密码的页面或接口。正式部署前请通过数据库替换 `users.password` 为新的 BCrypt 密文，或先补充修改密码接口。详见 [使用说明](docs/使用说明.md)。

## 写入演示数据

演示数据默认关闭。开启后，只有当 `room_type` 表为空时才会写入 2 个房型、4 个房间和未来 90 天日历，不会覆盖已有数据。

```bash
cd homestay-booking
mvn spring-boot:run -Dspring-boot.run.arguments=--app.seed.enabled=true
```

也可以把 `application.yml` 中的 `app.seed.enabled` 改为 `true` 后重启，写入完成后再改回 `false`。

## 核心业务规则

理解以下六条，就能理解整个系统的数据设计。

1. **日期采用半开区间** `[入住日, 离店日)`。入住当天占用，离店当天不占用。11-20 入住、11-22 离店记为 2 晚，占用 11-20 和 11-21。
2. **库存按「房型 × 每晚」存储**。`room_calendar` 中每一天一行，包含该晚价格、可售间数和是否开放。未生成日历的日期视为不可订。
3. **可订量取两个值的较小者**：区间内每晚的最小库存，以及整段日期都空闲的物理房间数。只有每晚都有余量、又能凑出同一批房间连续入住，才算真正可订。
4. **扣库存使用条件更新**。`UPDATE ... SET stock = stock - n WHERE stock >= n`，影响行数为 0 即失败，避免并发超卖。
5. **不静默调整已有订单的房间**。新订单凑不出连续房间时直接拒绝并提示房客，不会为了接单去改动其他订单的房号。
6. **订单状态单向流转**：`PENDING → CONFIRMED → CHECKED_IN → COMPLETED`，`PENDING` / `CONFIRMED` 可转为 `CANCELLED`。取消或拒绝时按 `order_stay` 逐晚归还库存。

## 数据库表

| 表 | 用途 |
| --- | --- |
| `users` | 房客与房东账号，`role` 区分角色 |
| `room_type` | 房型，对外售卖单位 |
| `room` | 物理房间，决定库存上限 |
| `room_calendar` | 房型每日价格、库存、开放状态 |
| `orders` | 订单主表 |
| `order_room` | 订单分配的物理房间 |
| `order_stay` | 订单占用的每一个夜晚，用于精确还库存 |
| `order_log` | 订单状态流转日志 |

完整字段、索引与注释见 [`schema.sql`](homestay-booking/src/main/resources/db/schema.sql)。

## 接口概览

统一响应体：

```json
{ "code": 0, "message": "ok", "data": {} }
```

`code = 0` 表示成功，失败时沿用 HTTP 语义（400 / 401 / 403 / 404 / 409 / 500）。

| 分组 | 路径前缀 | 权限 |
| --- | --- | --- |
| 认证 | `/api/auth/**` | 注册、登录公开，`/me` 需登录 |
| 房客浏览 | `/api/public/**` | 公开 |
| 房客订单 | `/api/orders/**` | 需登录 |
| 房东管理 | `/api/admin/**` | 需 `HOST` 角色 |

完整接口清单见 [后端 README](homestay-booking/README.md)，或启动后打开 Swagger UI 直接调试。

## 配置说明

开发配置位于 `homestay-booking/src/main/resources/application.yml`：

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `server.port` | `8080` | 后端端口 |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/homestay...` | 数据库连接 |
| `spring.datasource.username` / `password` | `homestay_app` / `Homestay@2026` | 数据库账号 |
| `app.jwt.secret` | 开发用字符串 | HS256 密钥，至少 32 字节 |
| `app.jwt.expire-minutes` | `1440` | Token 有效期（分钟） |
| `app.seed.enabled` | `false` | 是否写入演示数据 |
| `mybatis-plus.configuration.log-impl` | 控制台输出 | 开发期打印 SQL |

生产环境使用 `application-prod.yml`，敏感项全部通过环境变量注入：

| 环境变量 | 说明 |
| --- | --- |
| `DB_URL` | JDBC 连接串 |
| `DB_USERNAME` / `DB_PASSWORD` | 数据库账号 |
| `JWT_SECRET` | JWT 密钥，至少 32 字节 |
| `JWT_EXPIRE_MINUTES` | Token 有效期，默认 1440 |
| `SERVER_PORT` | 后端端口，默认 8080 |
| `LOG_FILE` | 日志文件路径 |

```bash
export DB_PASSWORD='你的数据库密码'
export JWT_SECRET='至少32字节的随机字符串'
java -jar homestay-booking-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

> **安全提示**
>
> - 开源或部署前必须替换数据库密码与 JWT 密钥，不要沿用仓库中的开发默认值。
> - `admin/admin123` 只适合本地体验，生产环境请立即更换。
> - 生产环境建议关闭 MyBatis-Plus 的 SQL 控制台输出（`application-prod.yml` 已关闭）。
> - 当前版本没有修改密码功能，上线前请补充或手工更新数据库密文。

## 构建与验证

```bash
# 后端编译
cd homestay-booking
mvn -q -DskipTests compile

# 后端打包
mvn clean package -DskipTests

# 前端生产构建
cd ../homestay-web
npm run build
```

仓库目前没有自动化测试用例。提交前建议至少完成以下人工检查：

- 未登录访问管理端会跳转登录；房客账号进入 `/admin` 会被拒绝。
- 未生成日历、关房、库存不足的日期均不可下单。
- 取消订单后，对应日期的库存恢复。
- 已入住订单不能直接取消。
- 两个请求同时抢最后一间房，只有一个成功。

更完整的操作步骤见 [使用说明](docs/使用说明.md)。

## 常见问题

| 现象 | 可能原因 | 处理 |
| --- | --- | --- |
| 启动提示未检测到 MySQL | MySQL 服务未启动 | 启动服务，确认 3306 可连接 |
| 后端启动报 Access denied | 数据库账号或密码不一致 | 重新授权，核对 `application.yml` |
| 登录后管理端仍 403 | 当前账号不是 `HOST` | 使用 `admin`，或把账号角色改为 `HOST` |
| 搜索有房但下单 409 | 库存被抢，或无连续空房 | 刷新报价后重试，换日期或减少间数 |
| 日历页没有数据 | 该房型尚未生成日历 | 在管理端先执行「生成日历」 |
| 图片不显示 | `cover_image` 指向失效外链 | 改用 `/images/xxx.jpg` 本地路径 |
| 页面接口 404 | 后端未启动或端口不一致 | 确认 8080 已就绪，检查 Vite 代理 |

更多排障说明见 [使用说明](docs/使用说明.md#7-常见问题)。

## 路线图

- [ ] 在线支付与退款
- [ ] 短信 / 邮件订单通知
- [ ] 房客评价与房东回复
- [ ] 经营数据看板（入住率、间夜、收入）
- [ ] 修改密码与账号管理页面
- [ ] 自动化测试与 CI
- [ ] Docker Compose 一键部署

## 贡献

欢迎提交 Issue 和 Pull Request。

1. Fork 仓库并从 `main` 创建功能分支。
2. 保持现有分层：Controller 只做参数与响应，业务规则放在 Service，SQL 放在 Mapper。
3. 修改库存或订单状态逻辑时，请同时验证并发与取消场景。
4. 不要提交数据库密码、JWT 密钥、`node_modules`、`target`、`dist` 等文件。
5. 提交前执行后端编译和前端构建。

## 许可证

当前仓库尚未附带许可证文件。计划采用 MIT 许可证，正式开源前请补充 `LICENSE`，或按团队要求选择其他许可证。

## 相关文档

- [使用说明](docs/使用说明.md)：启动、账号、房客端与管理端操作、配置、部署与排障
- [后端接口说明](homestay-booking/README.md)：接口清单、运行方式与业务约定

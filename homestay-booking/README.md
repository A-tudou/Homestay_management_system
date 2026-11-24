# 民宿预定管理系统（后端 API）

Spring Boot 单体后端，覆盖房客下单与管理端运营的核心闭环：登录鉴权、房型/房间管理、日历价与库存、按房型下单并自动分配房间、订单状态流转。

## 技术栈

- Java 17（编译目标）、Spring Boot 3.5.16
- Spring Security + JWT（jjwt 0.12.6）
- MyBatis-Plus 3.5.18 + MySQL 8
- springdoc-openapi（Swagger UI）

## 环境要求

- JDK 17 及以上
- Maven 3.8+
- MySQL 8.0 及以上，且已启动

## 1. 准备数据库

```sql
CREATE DATABASE IF NOT EXISTS homestay DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE USER IF NOT EXISTS 'homestay_app'@'localhost' IDENTIFIED BY 'Homestay@2026';
GRANT ALL PRIVILEGES ON homestay.* TO 'homestay_app'@'localhost';
FLUSH PRIVILEGES;
```

表结构由应用启动时执行 `src/main/resources/db/schema.sql` 自动创建（脚本内全部是 `CREATE TABLE IF NOT EXISTS`，可反复执行）。

如需修改连接信息，编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/homestay?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: homestay_app
    password: "Homestay@2026"
```

## 2. 本地启动

```bash
mvn clean package -DskipTests
java -jar target/homestay-booking-0.0.1-SNAPSHOT.jar
```

- 服务地址：http://localhost:8080
- 接口文档：http://localhost:8080/swagger-ui.html
- OpenAPI JSON：http://localhost:8080/v3/api-docs

首次启动如果库里没有任何房东账号，会自动创建一个：

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| admin | admin123 | HOST（房东） |

上线前请立即修改该密码。

## 3. 写入演示数据（可选）

演示数据默认关闭；打开后仅当 `room_type` 表为空时写入 2 个房型、4 个房间和未来 90 天日历：

```bash
java -jar target/homestay-booking-0.0.1-SNAPSHOT.jar --app.seed.enabled=true
```

## 4. 生产环境运行

生产配置在 `application-prod.yml`，敏感项全部走环境变量：

```bash
export DB_URL="jdbc:mysql://127.0.0.1:3306/homestay?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
export DB_USERNAME=homestay_app
export DB_PASSWORD='你的数据库密码'
export JWT_SECRET='至少 32 字节的随机字符串'
export SERVER_PORT=8080
export LOG_FILE=/var/log/homestay-booking.log

java -jar homestay-booking-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## 接口清单

统一响应体：`{"code":0,"message":"ok","data":...}`，`code=0` 表示成功；失败时 `code` 沿用 HTTP 语义（400/401/403/404/409/500）。

### 认证

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/register` | 房客注册，固定 CUSTOMER 角色 |
| POST | `/api/auth/login` | 登录，返回 JWT |
| GET | `/api/auth/me` | 当前登录用户 |

### 房东端（需 HOST 角色，请求头 `Authorization: Bearer <token>`）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST/PUT | `/api/admin/room-types`、`/api/admin/room-types/{id}` | 新增/修改房型 |
| PATCH | `/api/admin/room-types/{id}/status` | 上架/下架 |
| GET | `/api/admin/room-types`、`/api/admin/room-types/{id}` | 房型分页与详情 |
| DELETE | `/api/admin/room-types/{id}` | 删除房型（有房间时拒绝） |
| POST | `/api/admin/rooms` | 新增房间 |
| PUT | `/api/admin/rooms/{id}` | 修改房间 |
| PATCH | `/api/admin/rooms/{id}/status` | 可用/维修 |
| GET | `/api/admin/rooms`、`/api/admin/rooms/{id}` | 房间分页与详情 |
| DELETE | `/api/admin/rooms/{id}` | 删除房间 |
| POST | `/api/admin/room-types/{id}/calendar/generate` | 按可用房间数生成日历 |
| GET | `/api/admin/room-types/{id}/calendar` | 查询日历（最多 366 天） |
| PUT | `/api/admin/calendar/{id}` | 修改某天价格/库存/开关房 |
| PUT | `/api/admin/room-types/{id}/calendar/batch` | 区间批量改价/改库存/开关房 |
| GET | `/api/admin/orders` | 订单分页（按状态、关键字） |
| GET | `/api/admin/orders/{id}` | 订单详情（含状态日志） |
| POST | `/api/admin/orders/{id}/confirm` | 确认订单 |
| POST | `/api/admin/orders/{id}/reject` | 拒单（归还库存） |
| POST | `/api/admin/orders/{id}/check-in` | 办理入住 |
| POST | `/api/admin/orders/{id}/complete` | 完成订单 |

### 房客端（无需登录）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/public/room-types` | 上架房型分页，含可住人数与价格区间（未来 30 天日历价） |
| GET | `/api/public/room-types/search` | 按 `checkIn`/`checkOut`/`guests`/`roomCount` 搜索可订房型 |
| GET | `/api/public/room-types/{id}` | 房型详情 |
| GET | `/api/public/room-types/{id}/calendar` | 房型日历与可订状态（最多 92 天） |

### 房客端（需登录）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/orders` | 下单，按房型下单并自动分配房间号 |
| GET | `/api/orders/my` | 我的订单分页 |
| GET | `/api/orders/{id}` | 我的订单详情 |
| POST | `/api/orders/{id}/cancel` | 取消订单（归还库存） |

## 业务约定

- 日期区间采用 `[入住日, 离店日)`，只占用入住日到离店日前一晚。
- 房型必须在日历中逐日存在、开放且库存足够才能下单；缺失的日期视为不可订。
- 下单时按房间号升序加行锁挑选可用房间，同一房型同一晚不会重复分配。
- 订单状态流转：`PENDING → CONFIRMED → CHECKED_IN → COMPLETED`，`PENDING`/`CONFIRMED` 可取消（`CANCELLED`）。
- 单次预订最多 30 晚、最多 10 间。

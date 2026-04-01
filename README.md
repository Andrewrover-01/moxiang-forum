# 墨香论坛 (Moxiang Forum)

基于 Java 17 + Spring Boot 3 + Vue 3 + MySQL + Redis 构建的高并发小说论坛。

---

## 项目简介

墨香论坛是一个以小说为主题的综合性论坛平台，支持用户注册登录、帖子发布与互动、小说发布与章节管理、评论点赞等功能，参考百度贴吧的风格设计。

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端语言 | Java 17 |
| 后端框架 | Spring Boot 3.2.x |
| ORM | MyBatis-Plus 3.5.x |
| 数据库 | MySQL 8.x |
| 缓存 | Redis 6+（Lettuce 客户端） |
| 认证 | JWT（jjwt 0.12.x） |
| 安全 | Spring Security 6 |
| 构建工具 | Maven（多模块） |
| 前端框架 | Vue 3 + TypeScript |
| UI 组件库 | Element Plus |
| 前端构建 | Vite 5 |
| 状态管理 | Pinia |
| 路由 | Vue Router 4 |

---

## 项目结构

```
moxiang-forum/
├── backend/                        # 后端（Spring Boot 多模块）
│   ├── pom.xml                     # 父 POM
│   ├── moxiang-common/             # 公共工具、统一响应、异常、常量
│   ├── moxiang-mbg/                # MyBatis-Plus 实体类与 Mapper
│   ├── moxiang-service/            # 业务服务层
│   └── moxiang-web/                # Spring Boot 入口、Controller、配置
│       └── src/main/resources/
│           ├── application.yml
│           ├── application-dev.yml
│           └── db/schema.sql       # 数据库建表与初始化数据
└── frontend/                       # 前端（Vue 3 + Vite）
    ├── src/
    ├── package.json
    └── vite.config.ts
```

---

## 环境要求

| 软件 | 版本要求 |
|------|---------|
| JDK | 17+ |
| Maven | 3.8+ |
| MySQL | 8.x |
| Redis | 6+ |
| Node.js | 18+ |
| npm | 9+ |

---

## 本地部署

### 一、准备数据库

1. 启动 MySQL，创建数据库：

```sql
CREATE DATABASE moxiang_forum
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

2. 执行初始化 SQL（建表 + 种子数据）：

```bash
mysql -u root -p moxiang_forum < backend/moxiang-web/src/main/resources/db/schema.sql
```

### 二、准备 Redis

确保本地已启动 Redis 服务（默认端口 6379）：

```bash
# Linux / macOS
redis-server

# 或者使用 Docker 快速启动
docker run -d --name redis -p 6379:6379 redis:7
```

### 三、配置后端

编辑 `backend/moxiang-web/src/main/resources/application-dev.yml`，填入实际的 MySQL 密码：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/moxiang_forum?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: 你的MySQL密码

  data:
    redis:
      host: localhost
      port: 6379
      password:           # 如果 Redis 无密码则留空
```

> **提示**：也可以通过环境变量传入密码，无需修改配置文件：
> ```bash
> export MYSQL_USERNAME=root
> export MYSQL_PASSWORD=你的MySQL密码
> ```

### 四、启动后端

```bash
cd backend
mvn clean package -DskipTests
java -jar moxiang-web/target/moxiang-web-1.0.0.jar
```

启动成功后，后端接口地址为 `http://localhost:8080`。

### 五、启动前端

```bash
cd frontend
npm install
npm run dev
```

启动成功后，前端页面地址为 `http://localhost:3000`。

前端开发服务器已配置代理，所有 `/api/*` 请求会自动转发到 `http://localhost:8080`。

### 六、默认管理员账号

数据库初始化后，默认管理员账号为：

| 字段 | 值 |
|------|----|
| 用户名 | `admin` |
| 密码 | `Admin@123` |
| 角色 | `ADMIN` |

> ⚠️ **生产环境请立即修改默认密码！**

---

## 服务器部署（生产环境）

### 一、服务器环境准备

在服务器上安装以下软件：

```bash
# 安装 JDK 17（以 Ubuntu 为例）
sudo apt update
sudo apt install -y openjdk-17-jdk

# 安装 MySQL 8
sudo apt install -y mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql

# 安装 Redis
sudo apt install -y redis-server
sudo systemctl start redis
sudo systemctl enable redis

# 安装 Node.js 18（用于构建前端）
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install -y nodejs

# 安装 Nginx（用于前端静态文件托管和反向代理）
sudo apt install -y nginx
```

### 二、初始化生产数据库

```bash
# 登录 MySQL
sudo mysql -u root

# 创建数据库和专用用户
CREATE DATABASE moxiang_forum CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'moxiang'@'localhost' IDENTIFIED BY '强密码';
GRANT ALL PRIVILEGES ON moxiang_forum.* TO 'moxiang'@'localhost';
FLUSH PRIVILEGES;
EXIT;

# 导入初始化 SQL
mysql -u moxiang -p moxiang_forum < backend/moxiang-web/src/main/resources/db/schema.sql
```

### 三、配置生产环境参数

**方式一：使用环境变量（推荐）**

```bash
export MYSQL_USERNAME=moxiang
export MYSQL_PASSWORD=强密码
```

**方式二：创建 `application-prod.yml`**

在 `backend/moxiang-web/src/main/resources/` 目录下新建 `application-prod.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/moxiang_forum?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: moxiang
    password: 强密码

  data:
    redis:
      host: localhost
      port: 6379
      password: Redis密码（若无则留空）

server:
  port: 8080
```

修改 `application.yml` 激活生产 profile：

```yaml
spring:
  profiles:
    active: prod
```

**修改 JWT 密钥**（必须，生产环境需使用足够长的随机密钥）：

```yaml
jwt:
  secret: 替换为至少256位的随机字符串
  expiration: 604800
```

### 四、构建后端 JAR 包

```bash
cd backend
mvn clean package -DskipTests
```

构建产物位于 `backend/moxiang-web/target/moxiang-web-1.0.0.jar`。

### 五、构建前端静态文件

```bash
cd frontend
npm install
npm run build
```

构建产物位于 `frontend/dist/` 目录。

### 六、部署后端（Systemd 服务）

将 JAR 包上传到服务器（例如 `/opt/moxiang/`），然后创建 Systemd 服务文件：

```bash
sudo nano /etc/systemd/system/moxiang.service
```

写入以下内容：

```ini
[Unit]
Description=Moxiang Forum Backend
After=network.target mysql.service redis.service

[Service]
User=www-data
WorkingDirectory=/opt/moxiang
ExecStart=/usr/bin/java -jar /opt/moxiang/moxiang-web-1.0.0.jar
Environment="MYSQL_USERNAME=moxiang"
Environment="MYSQL_PASSWORD=强密码"
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启动并设置开机自启：

```bash
sudo systemctl daemon-reload
sudo systemctl start moxiang
sudo systemctl enable moxiang

# 查看运行日志
sudo journalctl -u moxiang -f
```

### 七、配置 Nginx

将前端 `dist/` 目录上传到服务器（例如 `/var/www/moxiang/`），然后配置 Nginx：

```bash
sudo nano /etc/nginx/sites-available/moxiang
```

写入以下内容（将 `your-domain.com` 替换为实际域名或服务器 IP）：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    root /var/www/moxiang;
    index index.html;

    # Vue Router history 模式支持
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 反向代理后端 API
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

启用站点并重载 Nginx：

```bash
sudo ln -s /etc/nginx/sites-available/moxiang /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### 八、（可选）配置 HTTPS

使用 Certbot 申请免费 TLS 证书：

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d your-domain.com
sudo systemctl reload nginx
```

---

## 使用 Docker 快速部署（可选）

如果服务器已安装 Docker 和 Docker Compose，可使用以下方式一键启动：

**1. 构建后端镜像**

在 `backend/` 目录创建 `Dockerfile`：

```dockerfile
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY moxiang-web/target/moxiang-web-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**2. 构建前端镜像**

在 `frontend/` 目录创建 `Dockerfile`：

```dockerfile
FROM node:18-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

**3. 创建 `docker-compose.yml`（在项目根目录）**

```yaml
version: "3.9"

services:
  mysql:
    image: mysql:8
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword
      MYSQL_DATABASE: moxiang_forum
      MYSQL_USER: moxiang
      MYSQL_PASSWORD: moxiangpassword
    volumes:
      - mysql_data:/var/lib/mysql
      - ./backend/moxiang-web/src/main/resources/db/schema.sql:/docker-entrypoint-initdb.d/schema.sql
    ports:
      - "3306:3306"

  redis:
    image: redis:7
    ports:
      - "6379:6379"

  backend:
    build: ./backend
    depends_on:
      - mysql
      - redis
    environment:
      MYSQL_USERNAME: moxiang
      MYSQL_PASSWORD: moxiangpassword
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/moxiang_forum?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
      SPRING_DATA_REDIS_HOST: redis
    ports:
      - "8080:8080"

  frontend:
    build: ./frontend
    depends_on:
      - backend
    ports:
      - "80:80"

volumes:
  mysql_data:
```

**4. 一键启动**

```bash
# 先构建后端 JAR
cd backend && mvn clean package -DskipTests && cd ..

# 启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f
```

---

## 常见问题

**Q: 后端启动报错 `Communications link failure`？**  
A: 检查 MySQL 是否已启动，以及 `application-dev.yml` 中的数据库连接信息是否正确。

**Q: 后端启动报错 `Unable to connect to Redis`？**  
A: 检查 Redis 是否已启动，端口是否为 6379。

**Q: 前端页面空白或路由跳转 404？**  
A: 生产环境需在 Nginx 中配置 `try_files $uri $uri/ /index.html`，以支持 Vue Router 的 history 模式。

**Q: 登录后提示 token 无效？**  
A: 检查 `application.yml` 中 `jwt.secret` 是否配置正确，且长度至少 256 位。

---

## 更多文档

- 后端详细文档：[backend/README.md](backend/README.md)

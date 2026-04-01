# 墨香论坛 (Moxiang Forum)

基于 Java 17 + Spring Boot 3 + Vue 3 + MySQL + Redis 构建的小说主题综合论坛，参考百度贴吧风格设计。

---

## 项目简介

墨香论坛是一个以小说为主题的综合性论坛平台，支持：

- 用户注册 / 登录（JWT 鉴权）
- 帖子发布、点赞、收藏、评论（带楼中楼）
- 小说发布、章节管理、收藏
- 用户关注 / 消息通知
- 管理员后台（版块管理、用户封禁、帖子置顶/精华）

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端语言 | Java 17 |
| 后端框架 | Spring Boot 3.2.x |
| ORM | MyBatis-Plus 3.5.x |
| 数据库 | MySQL 8.x |
| 缓存 | Redis 7（Lettuce 客户端） |
| 认证 | JWT（jjwt 0.12.x） |
| 安全 | Spring Security 6 |
| 构建工具 | Maven（多模块） |
| 前端框架 | Vue 3 + TypeScript |
| UI 组件库 | Element Plus |
| 前端构建 | Vite 5 |
| 状态管理 | Pinia |
| 路由 | Vue Router 4 |
| 容器化 | Docker + Docker Compose |
| 反向代理 | Nginx |

---

## 项目结构

```
moxiang-forum/
├── .env.example                    # 环境变量模板，复制为 .env 后填入真实值
├── docker-compose.yml              # 一键启动全部服务
├── nginx.conf                      # Nginx 配置（SPA 路由 + API 反向代理）
├── backend/                        # 后端（Spring Boot 多模块）
│   ├── Dockerfile                  # 后端镜像构建文件
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
    ├── Dockerfile                  # 前端镜像构建文件
    ├── src/
    ├── package.json
    └── vite.config.ts
```

---

## 🐳 Docker 一键部署（推荐）

> **只需要安装 [Docker](https://docs.docker.com/get-docker/) 和 [Docker Compose](https://docs.docker.com/compose/install/)，无需在本机安装 Java、Node.js、MySQL 或 Redis。**

### 步骤

**1. 克隆仓库**

```bash
git clone https://github.com/Andrewrover-01/moxiang-forum.git
cd moxiang-forum
```

**2. 配置环境变量**

```bash
cp .env.example .env
```

用编辑器打开 `.env`，至少修改以下几项：

```dotenv
MYSQL_ROOT_PASSWORD=你的MySQL_root密码
MYSQL_USERNAME=moxiang
MYSQL_PASSWORD=你的应用数据库密码
JWT_SECRET=用 openssl rand -hex 32 生成的随机字符串
```

> 💡 生成安全 JWT 密钥：`openssl rand -hex 32`

**3. 构建镜像并启动**

```bash
docker-compose up -d --build
```

**4. 查看启动日志**

```bash
docker-compose logs -f
```

所有服务健康后，访问 `http://localhost`（或 `.env` 中配置的 `APP_PORT`）即可使用。

### 服务说明

| 服务 | 镜像 | 说明 |
|------|------|------|
| `mysql` | `mysql:8.0` | 自动执行 `schema.sql` 初始化表结构与种子数据 |
| `redis` | `redis:7-alpine` | 缓存与 Token 黑名单 |
| `backend` | 本地构建 | Spring Boot API，监听 8080（仅内部可达） |
| `frontend` | 本地构建 | Nginx 静态服务 + `/api/` 反向代理到 backend |

### 默认管理员账号

| 字段 | 值 |
|------|----|
| 用户名 | `admin` |
| 密码 | `Admin@123` |
| 角色 | `ADMIN` |

> ⚠️ **生产环境请立即登录并修改默认密码！**

### 常用 Docker 命令

```bash
# 停止所有服务
docker-compose down

# 停止并清除数据卷（数据库数据将被删除）
docker-compose down -v

# 重新构建某个服务
docker-compose up -d --build backend

# 查看某个服务的日志
docker-compose logs -f backend
```

---

## 💻 本地开发（不使用 Docker）

如需在本机直接运行，需要预装：

| 软件 | 版本要求 |
|------|---------|
| JDK | 17+ |
| Maven | 3.8+ |
| MySQL | 8.x |
| Redis | 6+ |
| Node.js | 18+ |
| npm | 9+ |

### 一、准备数据库

```bash
# 登录 MySQL 并创建数据库
mysql -u root -p <<'EOF'
CREATE DATABASE IF NOT EXISTS moxiang_forum
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
EOF

# 导入表结构与种子数据
mysql -u root -p moxiang_forum < backend/moxiang-web/src/main/resources/db/schema.sql
```

### 二、准备 Redis

```bash
# Linux / macOS
redis-server

# 或使用 Docker 快速启动
docker run -d --name redis -p 6379:6379 redis:7-alpine
```

### 三、配置后端

编辑 `backend/moxiang-web/src/main/resources/application-dev.yml`：

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

> **提示**：也可以通过环境变量传入，无需修改配置文件：
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

后端启动成功后地址为 `http://localhost:8080`。

### 五、启动前端

```bash
cd frontend
npm install
npm run dev
```

前端页面地址为 `http://localhost:3000`。开发服务器已配置代理，所有 `/api/*` 请求自动转发到 `http://localhost:8080`。

---

## 🖥️ 服务器手动部署（生产环境）

如需在服务器上不依赖 Docker 进行部署，请参考以下步骤。

### 一、服务器环境准备（Ubuntu）

```bash
# JDK 17
sudo apt update && sudo apt install -y openjdk-17-jdk

# MySQL 8
sudo apt install -y mysql-server
sudo systemctl enable --now mysql

# Redis
sudo apt install -y redis-server
sudo systemctl enable --now redis

# Node.js 18（仅用于构建前端，构建后可卸载）
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install -y nodejs

# Nginx
sudo apt install -y nginx
```

### 二、初始化生产数据库

```bash
sudo mysql -u root <<'EOF'
CREATE DATABASE IF NOT EXISTS moxiang_forum CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'moxiang'@'localhost' IDENTIFIED BY 'YOUR_DB_PASSWORD';
GRANT ALL PRIVILEGES ON moxiang_forum.* TO 'moxiang'@'localhost';
FLUSH PRIVILEGES;
EOF

mysql -u moxiang -p moxiang_forum < backend/moxiang-web/src/main/resources/db/schema.sql
```

### 三、配置生产环境参数

**修改 JWT 密钥**（必须）：

```bash
openssl rand -hex 32   # 生成随机密钥，填入 application.yml
```

```yaml
# backend/moxiang-web/src/main/resources/application.yml
jwt:
  secret: 上面生成的随机字符串
  expiration: 604800
```

### 四、构建并部署后端（Systemd）

```bash
cd backend && mvn clean package -DskipTests
sudo mkdir -p /opt/moxiang
sudo cp moxiang-web/target/moxiang-web-1.0.0.jar /opt/moxiang/
```

创建 `/etc/systemd/system/moxiang.service`：

```ini
[Unit]
Description=Moxiang Forum Backend
After=network.target mysql.service redis.service

[Service]
User=www-data
WorkingDirectory=/opt/moxiang
ExecStart=/usr/bin/java -jar /opt/moxiang/moxiang-web-1.0.0.jar
Environment="MYSQL_USERNAME=moxiang"
Environment="MYSQL_PASSWORD=YOUR_DB_PASSWORD"
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now moxiang
sudo journalctl -u moxiang -f
```

### 五、构建前端并配置 Nginx

```bash
cd frontend && npm install && npm run build
sudo mkdir -p /var/www/moxiang
sudo cp -r dist/* /var/www/moxiang/
```

创建 `/etc/nginx/sites-available/moxiang`（将 `your-domain.com` 替换为实际域名或 IP）：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    root /var/www/moxiang;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/moxiang /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

### 六、（可选）配置 HTTPS

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d your-domain.com
sudo systemctl reload nginx
```

---

## ❓ 常见问题

**Q: 后端启动报错 `Communications link failure`？**  
A: 检查 MySQL 是否已启动，以及数据库连接信息（主机、端口、用户名、密码）是否正确。Docker 部署时请等待 `mysql` 服务通过健康检查后 backend 才会启动。

**Q: 后端启动报错 `Unable to connect to Redis`？**  
A: 检查 Redis 是否已启动，端口是否为 6379。

**Q: 前端页面空白或路由跳转 404？**  
A: 生产/Nginx 环境需配置 `try_files $uri $uri/ /index.html`，以支持 Vue Router history 模式。

**Q: 登录后提示 token 无效？**  
A: 检查 `application.yml` 中 `jwt.secret` 是否已配置，且长度至少 32 字节（256 位）。

**Q: Docker 部署时数据库没有自动建表？**  
A: MySQL 容器只在**数据卷为空**（首次创建）时才执行 `docker-entrypoint-initdb.d/` 下的脚本。如果数据卷已存在，需先执行 `docker-compose down -v` 清除后再重新启动。

---

## 更多文档

- 后端详细文档（API 列表、数据库设计、缓存策略）：[backend/README.md](backend/README.md)

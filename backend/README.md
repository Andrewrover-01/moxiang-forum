# 墨香论坛 Backend

A novel-forum backend inspired by Baidu Tieba, built with **Spring Boot 3**, **MyBatis-Plus**, **MySQL 8**, **Redis**, and **JWT**.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.x |
| ORM | MyBatis-Plus 3.5.x |
| Database | MySQL 8.x |
| Cache | Redis (Lettuce) |
| Auth | JWT (jjwt 0.12.x) |
| Build | Maven (multi-module) |
| Security | Spring Security 6 |

---

## Project Structure

```
backend/
├── pom.xml                      # Parent POM
├── moxiang-common/              # Shared utilities, API result, exceptions, constants
├── moxiang-mbg/                 # MyBatis-Plus entities + mappers
├── moxiang-service/             # Business service layer
└── moxiang-web/                 # Spring Boot application, controllers, config
    └── src/main/resources/
        ├── application.yml
        ├── application-dev.yml
        └── db/schema.sql
```

---

## Prerequisites

- JDK 17+
- Maven 3.8+
- MySQL 8.x
- Redis 6+

---

## Quick Start

> **Recommended:** Use the Docker one-liner from the repo root instead of running the backend manually — see the [root README](../README.md) for details.

### 1. Create the Database

```sql
CREATE DATABASE moxiang_forum
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

Then run the DDL + seed data:

```bash
mysql -u root -p moxiang_forum < moxiang-web/src/main/resources/db/schema.sql
```

### 2. Configure the Application

Edit `moxiang-web/src/main/resources/application-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/moxiang_forum?...
    username: root
    password: YOUR_MYSQL_PASSWORD

  data:
    redis:
      host: localhost
      port: 6379
      password:        # blank if no Redis auth
```

Change the JWT secret in `application.yml` for production:

```yaml
jwt:
  secret: YOUR_SECRET_KEY_MUST_BE_AT_LEAST_256_BITS
  expiration: 604800   # 7 days
```

### 3. Build

```bash
cd backend
mvn clean package -DskipTests
```

### 4. Run

```bash
java -jar moxiang-web/target/moxiang-web-1.0.0.jar
```

The API will be available at `http://localhost:8080`.

---

## API Overview

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/user/register` | Register new user |
| POST | `/api/user/login` | Login → returns JWT token |
| POST | `/api/user/logout` | Logout (blacklists token) |

> Add the token to requests: `Authorization: Bearer <token>`

### Users

| Method | Endpoint | Auth |
|--------|----------|------|
| GET | `/api/user/info` | ✅ |
| GET | `/api/user/{id}` | Public |
| PUT | `/api/user/profile` | ✅ |
| PUT | `/api/user/password` | ✅ |

### Forums

| Method | Endpoint | Auth |
|--------|----------|------|
| GET | `/api/forum/list` | Public |
| GET | `/api/forum/{id}` | Public |
| GET | `/api/forum/page` | Public |

### Posts

| Method | Endpoint | Auth |
|--------|----------|------|
| GET | `/api/post/list?forumId=1` | Public |
| GET | `/api/post/{id}` | Public |
| GET | `/api/post/search?keyword=xxx` | Public |
| GET | `/api/post/hot?limit=10` | Public |
| POST | `/api/post` | ✅ |
| PUT | `/api/post/{id}` | ✅ Owner |
| DELETE | `/api/post/{id}` | ✅ Owner |
| POST | `/api/post/{id}/like` | ✅ |
| GET | `/api/post/{id}/like/status` | ✅ |

### Comments

| Method | Endpoint | Auth |
|--------|----------|------|
| GET | `/api/comment/post/{postId}` | Public |
| GET | `/api/comment/replies/{parentId}` | Public |
| POST | `/api/comment` | ✅ |
| DELETE | `/api/comment/{id}` | ✅ Owner |
| POST | `/api/comment/{id}/like` | ✅ |

### Novels

| Method | Endpoint | Auth |
|--------|----------|------|
| GET | `/api/novel/list?category=玄幻` | Public |
| GET | `/api/novel/{id}` | Public |
| GET | `/api/novel/search?keyword=xxx` | Public |
| POST | `/api/novel` | ✅ |
| PUT | `/api/novel/{id}` | ✅ Owner |
| DELETE | `/api/novel/{id}` | ✅ Owner |
| POST | `/api/novel/{id}/collect` | ✅ |
| GET | `/api/novel/{id}/collect/status` | ✅ |
| POST | `/api/novel/{novelId}/chapter` | ✅ Owner |
| GET | `/api/novel/{novelId}/chapters` | Public |
| GET | `/api/novel/chapter/{chapterId}` | Public |
| PUT | `/api/novel/chapter/{chapterId}` | ✅ Owner |
| DELETE | `/api/novel/chapter/{chapterId}` | ✅ Owner |

### Admin (ADMIN role required)

| Method | Endpoint |
|--------|----------|
| POST | `/api/admin/forum` |
| PUT | `/api/admin/forum/{id}` |
| DELETE | `/api/admin/forum/{id}` |
| PUT | `/api/admin/forum/{id}/status` |
| PUT | `/api/admin/post/{id}/top` |
| PUT | `/api/admin/post/{id}/featured` |
| GET | `/api/admin/user/list` |
| PUT | `/api/admin/user/{id}/status` |
| DELETE | `/api/admin/user/{id}` |

---

## Database Schema

Key tables:

- **t_user** — user accounts with BCrypt passwords
- **t_forum** — forum sections with post counts
- **t_post** — posts with view/like/comment counters, pin & feature flags
- **t_comment** — threaded comments (self-referential via `parent_id`)
- **t_novel** — novel metadata with category, word count, chapters
- **t_novel_chapter** — individual chapters with ordered chapter numbers
- **t_tag** / **t_post_tag** — tagging system for posts
- **t_user_like_post** — persistent record of post likes
- **t_user_collect_novel** — persistent record of novel collections

All tables use:
- `BIGINT UNSIGNED AUTO_INCREMENT` primary keys
- `is_deleted` soft-delete flag (MyBatis-Plus `@TableLogic`)
- `created_at` / `updated_at` auto-managed timestamps

---

## Caching Strategy

| Data | Redis Key | TTL |
|------|-----------|-----|
| User info | `user:info:{id}` | 7 days |
| Hot posts list | `post:hot:list` | 10 minutes |
| Post view buffer | `post:view:{id}` | permanent (flushed to DB) |
| Post likes set | `post:like:{id}` | 30 days |
| Novel collects set | `novel:collect:{id}` | 30 days |
| Token blacklist | `token:blacklist:{token}` | token TTL |

---

## Security Notes

- Passwords are hashed with BCrypt (cost factor 10)
- JWT tokens are signed with HMAC-SHA256
- Logged-out tokens are blacklisted in Redis until expiry
- Role-based access enforced via Spring Security `@PreAuthorize`
- CSRF disabled (stateless API)
- CORS configured to allow all origins in dev — **restrict in production**

---

## Default Credentials

After running `schema.sql`, the default admin account is:

| Field | Value |
|-------|-------|
| Username | `admin` |
| Password | `Admin@123` |
| Role | `ADMIN` |

> **Change the admin password immediately in production.**

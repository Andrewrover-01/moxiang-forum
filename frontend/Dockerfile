# 构建阶段
FROM node:18-alpine AS build

WORKDIR /app

COPY package.json ./
RUN npm install

COPY . .
RUN npm run build

# 生产阶段：Nginx 静态服务
FROM nginx:alpine

COPY --from=build /app/dist /usr/share/nginx/html

# Nginx 配置：支持 Vue Router history 模式 + /api 反代
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]

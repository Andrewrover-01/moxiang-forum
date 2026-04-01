# ──────────────────────────────────────────────
# Stage 1 – Build the Vue / Vite app
# Build context must be the repository root so both
# the frontend/ sources and the root-level nginx.conf
# are available during the build.
# ──────────────────────────────────────────────
FROM node:20-alpine AS builder
WORKDIR /app

# Install dependencies (package-lock.json is optional)
COPY frontend/package*.json ./
RUN npm install

COPY frontend/ .
RUN npm run build

# ──────────────────────────────────────────────
# Stage 2 – Serve with Nginx
# ──────────────────────────────────────────────
FROM nginx:alpine

COPY --from=builder /app/dist /usr/share/nginx/html
# Inject the project-level nginx config (SPA routing + API proxy)
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]

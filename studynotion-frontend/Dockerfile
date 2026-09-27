# Stage 1 — Build React app
FROM node:20-alpine AS build

# Install git (required if any npm dependency is pulled from Git)
RUN apk add --no-cache git

WORKDIR /app

# Copy package files first — Docker caches node_modules layer
COPY package*.json ./
RUN npm install

# Copy all source files
COPY . .

# Set production API URL at build time
ARG REACT_APP_BASE_URL=/api/v1
ENV REACT_APP_BASE_URL=$REACT_APP_BASE_URL

# CRA treats ESLint warnings as errors when CI=1 (some build platforms set this).
ENV CI=false

# Build for production
RUN npm run build

# Stage 2 — Serve with Nginx
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Remove default nginx content
RUN rm -rf ./*

# Copy built React app
COPY --from=build /app/build .

# Copy custom nginx config (handles routing + API proxy)
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]

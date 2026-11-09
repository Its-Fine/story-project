# Build stage
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm install
COPY . .
ARG REACT_APP_API_URL=/api/v1
ARG REACT_APP_AUTH_URL=/api/v1
ENV REACT_APP_API_URL=$REACT_APP_API_URL
ENV REACT_APP_AUTH_URL=$REACT_APP_AUTH_URL
RUN npm run build

# Runtime stage
FROM nginx:alpine
COPY --from=build /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80

# Build the user-gui module
FROM node:lts-alpine as user-gui
WORKDIR /app
COPY ./user-gui/package*.json ./
COPY ./user-gui/yarn.lock ./
RUN yarn install
COPY ./user-gui/. .
RUN yarn build

# Build the admin-gui module
FROM node:lts-alpine as admin-gui
WORKDIR /app
COPY ./admin-gui/package*.json ./
COPY ./admin-gui/yarn.lock ./
RUN yarn install
COPY ./admin-gui/. .
RUN yarn build

# Production stage
FROM nginx:stable-alpine
RUN rm /etc/nginx/conf.d/default.conf
COPY ./nginx/nginx.conf /etc/nginx
COPY ./nginx/conf /etc/nginx/conf.d

# For user-gui
COPY --from=user-gui /app/dist /usr/share/nginx/html/home

# For admin-gui
COPY --from=admin-gui /app/dist /usr/share/nginx/html/admin

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
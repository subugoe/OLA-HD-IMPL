# Build the user-gui module
FROM node:lts-alpine as user-gui
WORKDIR /app
COPY ./user-gui/package*.json ./
COPY ./user-gui/yarn.lock ./
RUN yarn install
COPY ./user-gui/. .
RUN yarn build

# Build the admin-gui modul

# Production stage
FROM nginx:stable-alpine
RUN rm /etc/nginx/conf.d/default.conf
RUN mkdir -p /usr/share/nginx/user-gui
COPY ./nginx/nginx.conf /etc/nginx
COPY ./nginx/conf /etc/nginx/conf.d
COPY --from=user-gui /app/dist /usr/share/nginx/user-gui/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
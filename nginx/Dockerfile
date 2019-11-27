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
COPY --from=user-gui /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
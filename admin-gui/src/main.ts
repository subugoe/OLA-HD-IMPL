import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store/store'

import 'bootstrap';
//import 'bootstrap/scss/bootstrap.scss';
import "@/assets/scss/sb-admin-2.scss";
import '@fortawesome/fontawesome-free/css/all.css';

Vue.config.productionTip = false;

new Vue({
    router,
    store,
    render: h => h(App)
}).$mount('#app');

import Vue from 'vue'
import VueRouter from 'vue-router'
import Dashboard from '@/views/Dashboard.vue'
import Import from '@/views/Import.vue'
import Login from '@/views/Login.vue'
import DashView from '@/components/DashView.vue'

import store from '@/store/store'

Vue.use(VueRouter);

const router = new VueRouter({
    mode: 'history',
    base: process.env.BASE_URL,
    routes: [
        {
            path: '/login',
            name: 'login',
            component: Login,
            props: route => ({ redirect: route.query.redirect })
        },
        {
            path: '/',
            component: DashView,
            children: [
                {
                    path: '',
                    component: Dashboard,
                    name: 'home'
                },
                {
                    path: 'dashboard',
                    component: Dashboard,
                    name: 'dashboard'
                },
                {
                    path: 'import',
                    component: Import,
                    name: 'import'
                }
            ]
        }
    ]
});

router.beforeEach((to, from, next) => {

    // Check authentication for every route, except login
    if (to.name != 'login' && !store.getters.isAuthenticated) {

        // If not authenticated, try auto login using data in localStorage
        store.dispatch('tryAutoLogin').then(isSuccess => {
            // Auto login failed
            if (!isSuccess) {
                next({
                    path: '/login',
                    query: { redirect: to.fullPath }
                });
            } else {
                next();
            }
        });
    } else {
        next();
    }
});

export default router

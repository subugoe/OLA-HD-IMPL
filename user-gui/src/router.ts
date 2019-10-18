import Vue from 'vue';
import Router from 'vue-router';
import Home from './views/Home.vue';
import Search from './components/search/Search.vue';
import SearchDetail from './components/search/SearchDetail.vue';

Vue.use(Router);

export default new Router({
    mode: 'history',
    base: process.env.BASE_URL,
    routes: [
        {
            path: '/',
            name: 'home',
            component: Home,
        },
        {
            path: '/search',
            name: 'search',
            component: Search,
            props: route => ({ query: route.query.q })
        },
        {
            path: '/search-detail/:id',
            name: 'search-detail',
            component: SearchDetail,
            props: true
        },
        {
            path: '/about',
            name: 'about',
            // route level code-splitting
            // this generates a separate chunk (about.[hash].js) for this route
            // which is lazy-loaded when the route is visited.
            component: () => import(/* webpackChunkName: "about" */ './views/About.vue')
        },
    ],
});

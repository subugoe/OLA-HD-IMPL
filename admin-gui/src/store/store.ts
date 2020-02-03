import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex);

const store = new Vuex.Store( {
    state: {
        token: null,
        user: null
    },
    mutations: {},
    actions: {},
    getters: {
        user (state) {
            return state.user
        },
        isAuthenticated (state) {
            return state.token !== null
        }
    }
});

export default store
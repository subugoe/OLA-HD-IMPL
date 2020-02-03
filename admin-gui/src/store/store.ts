import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex);

const store = new Vuex.Store( {
    state: {
        token: null,
        username: null
    },
    mutations: {
        authUser (state, userData) {
            state.token = userData.token;
            state.username = userData.username
        },
    },
    actions: {
        login ({commit, dispatch}, authData) {
            // TODO: Login here
            commit('authUser', {
                token: 'fake token',
                username: authData.username
            })
        }
    },
    getters: {
        username (state) {
            return state.username
        },
        isAuthenticated (state) {
            return state.token !== null
        }
    }
});

export default store
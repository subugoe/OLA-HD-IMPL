import Vue from 'vue'
import Vuex from 'vuex'
import axios from '../axios-config'
import router from '../router'

Vue.use(Vuex);

const store = new Vuex.Store({
    state: {
        token: null,
        username: null
    },
    mutations: {
        authUser(state, userData) {
            state.token = userData.token;
            state.username = userData.username
        },
        clearAuthData(state) {
            state.token = null;
            state.username = null
        }
    },
    actions: {
        login({ commit, dispatch }, authData) {
            let data = new FormData();
            data.append('username', authData.username);
            data.append('password', authData.password);

            // Try to login
            axios.post('/login', data).then(response => {
                // Save the token to local storage
                localStorage.setItem('token', response.data.accessToken);
                localStorage.setItem('username', authData.username);

                // TODO: Implement expiration time

                // Save the information to Vuex
                commit('authUser', {
                    token: response.data.accessToken,
                    username: authData.username
                })
            }).catch(error => console.log(error));
        },
        tryAutoLogin({ commit }) {
            // Check if token is already here
            const token = localStorage.getItem('token');
            if (!token) {
                return
            }

            // TODO: check for token expiration
            // const expirationDate = localStorage.getItem('expirationDate')
            // const now = new Date()
            // if (now >= expirationDate) {
            //     return
            // }

            // Get username to display
            const username = localStorage.getItem('username');

            // Log user in
            commit('authUser', {
                token: token,
                username: username
            })
        },
        logout({ commit }) {
            // Clear all authentication information
            commit('clearAuthData');
            //localStorage.removeItem('expirationDate')
            localStorage.removeItem('token');
            //localStorage.removeItem('username')

            // Go to root (login page)
            router.replace('/');
        },
    },
    getters: {
        username(state) {
            return state.username
        },
        isAuthenticated(state) {
            return state.token !== null
        }
    }
});

export default store
import Vue from 'vue'
import Vuex from 'vuex'
import axios from '../axios-config'

Vue.use(Vuex);

let authInterceptor: any;

const store = new Vuex.Store({
    state: {
        token: null,
        username: null
    },
    mutations: {
        authUser(state, userData) {

            // Save the state
            state.token = userData.token;
            state.username = userData.username;

            // Save to local storage
            localStorage.setItem('token', userData.token);
            localStorage.setItem('username', userData.username);

            // TODO: Implement expiration time

            // Add authentication interceptor
            authInterceptor = axios.interceptors.request.use(config => {
                config.headers.Authorization = `Bearer ${userData.token}`;
                return config;
            });
        },
        clearAuthData(state) {

            // Remove state
            state.token = null;
            state.username = null;

            // Clear local storage
            localStorage.removeItem('token');
            localStorage.removeItem('username');
            //localStorage.removeItem('expirationDate')

            // Remove the interceptor
            axios.interceptors.request.eject(authInterceptor);
        }
    },
    actions: {
        login({ commit, dispatch }, authData) {
            let data = new FormData();
            data.append('username', authData.username);
            data.append('password', authData.password);

            // Try to login
            return axios.post('/login', data).then(response => {
                // Save the information to Vuex
                commit('authUser', {
                    token: response.data.accessToken,
                    username: authData.username
                })
            }).catch(error => {
                console.log(error);
                throw error;
            });
        },
        tryAutoLogin({ commit }) {
            // Check if token is already here
            const token = localStorage.getItem('token');
            if (!token) {
                return false;
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
            });
            return true;
        },
        logout({ commit }) {
            // Clear all authentication information
            commit('clearAuthData');
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
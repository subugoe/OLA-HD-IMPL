import Vue from 'vue'
import Vuex from 'vuex'
import axios from '../axios-config'

Vue.use(Vuex);

let authInterceptor: any;

const store = new Vuex.Store({
    state: {
        token: null,
        username: null,
        expiredTime: 0
    },
    mutations: {
        authUser(state, userData) {

            // Save the state
            state.token = userData.token;
            state.username = userData.username;
            state.expiredTime = userData.expiredTime;

            // Save to local storage
            localStorage.setItem('token', userData.token);
            localStorage.setItem('username', userData.username);
            localStorage.setItem('expiredTime', userData.expiredTime);

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
            state.expiredTime = 0;

            // Clear local storage
            localStorage.removeItem('token');
            localStorage.removeItem('username');
            localStorage.removeItem('expiredTime');

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
                    expiredTime: response.data.expiredTime,
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

            // Check for token expiration
            const expiredTime = localStorage.getItem('expiredTime');
            if (!expiredTime) {
                return false;
            }
            const expirationDate = parseInt(expiredTime);
            const now = Date.now();
            if (now >= expirationDate) {
                return false;
            }

            // Get username to display
            const username = localStorage.getItem('username');

            // Log user in
            commit('authUser', {
                token: token,
                username: username,
                expiredTime: expiredTime
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
            let isLoggedIn = true;

            // Check for token
            if (!state.token) {
                isLoggedIn = false;
            }

            // Check for expiration time
            const now = Date.now();
            if (now >= state.expiredTime) {
                isLoggedIn = false;
            }

            return isLoggedIn;
        }
    }
});

export default store
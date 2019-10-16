import axios from 'axios';

const instance = axios.create({
    baseURL: 'https://myapi.com',
    headers: {
        'Content-Type': 'application/json'
    }
});

export default {
    search() {
        console.log('Search from services');
    }
};
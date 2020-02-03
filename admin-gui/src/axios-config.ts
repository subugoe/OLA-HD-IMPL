import axios from 'axios';

const instance = axios.create({
    baseURL: 'http://141.5.98.232:8080',
    headers: {
        'Content-Type': 'application/json'
    }
});

export default instance
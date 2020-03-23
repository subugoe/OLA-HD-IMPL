import axios from 'axios';

const instance = axios.create({
    // TODO: Change the base URL to /api/ only
    baseURL: 'http://localhost/api',
    headers: {
        'Content-Type': 'application/json'
    }
});

export default instance
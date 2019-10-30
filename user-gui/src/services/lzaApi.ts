import axios from 'axios';

const instance = axios.create({
    baseURL: 'http://141.5.98.232:8080',
    headers: {
        'Content-Type': 'application/json'
    }
});

export default {
    search(query: string) {
        return instance.get('/search', {
            params: {
                q: query
            }
        });
    },

    getArchiveInfo(id: string) {
        return instance.get(`/search-archive/${id}`, {
            params: {
                withFile: true
            }
        });
    },

    downloadFiles(archiveId: string, files: []) {
        let url = 'http://141.5.98.232:8080/download';
        let data = {
            archiveId,
            files
        };

        return fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        });
    }
};
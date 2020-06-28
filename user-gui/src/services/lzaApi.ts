import axios from 'axios';

const instance = axios.create({
    baseURL: '/api/',
    headers: {
        'Content-Type': 'application/json'
    }
});

export default {
    search(query: string, limit: number, scroll: string) {
        return instance.get('/search', {
            params: {
                q: query,
                limit,
                scroll
            }
        });
    },

    getArchiveInfo(id: string, limit: number, offset: number) {
        return instance.get(`/search-archive/${id}`, {
            params: {
                withFile: true,
                limit,
                offset
            }
        });
    },

    getVersionInfo(id: string) {
        return instance.get(`/search-archive-info/${id}`);
    },

    downloadFiles(archiveId: string, files: []) {
        let url = '/api/download';
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
    },

    exportArchive(archiveId: string) {
        let url = `/api/export?id=${archiveId}&isInternal=true`;
        return fetch(url, {
            method: 'GET'
        });
    }
};
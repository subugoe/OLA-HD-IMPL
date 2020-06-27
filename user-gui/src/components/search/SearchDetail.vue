<template>
    <div class="container">

        <!-- Error message -->
        <div class="row my-3" v-if="error">
            <div class="col">
                <div class="alert alert-danger alert-dismissible fade show" role="alert">
                    <strong>Error!</strong> An error has occurred. Please try again.
                    <button type="button" class="close" data-dismiss="alert" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
            </div>
        </div>

        <div class="row" v-if="loading">
            <div class="col text-center">
                <img src="../../assets/images/spin-1s-100px.gif" alt="Searching">
            </div>
        </div>

        <template v-if="!loading">
            <div class="row">
                <div class="col">
                    <button type="button" class="btn btn-link" @click="$router.go(-1)">&laquo; Back</button>
                </div>
            </div>

            <!-- Full details -->
            <div class="row">
                <div class="col">
                    <div class="card">
                        <div class="card-header">
                            <div class="row align-items-center">
                                <div class="col-8">
                                    <h5 class="m-0">Archive ID: {{ archiveInfo.id }}</h5>
                                </div>
                                <div class="col-4 text-right">
                                    <button type="button" class="btn btn-primary" :disabled="!isOpen"
                                            @click="exportArchive">
                                        <i class="fas fa-download"/>
                                        Export
                                    </button>
                                </div>
                            </div>
                        </div>
                        <div class="card-body">
                            <table class="table table-borderless table-sm">
                                <tbody>
                                <tr>
                                    <td class="w-25">State:</td>
                                    <td class="w-75">{{ archiveInfo.state }}</td>
                                </tr>
                                <tr>
                                    <td class="w-25">Total file:</td>
                                    <td class="w-75">{{ archiveInfo.file_count }}</td>
                                </tr>
                                <tr>
                                    <td class="w-25">Created time:</td>
                                    <td class="w-75">{{ archiveInfo.created | formatDate }}</td>
                                </tr>
                                <tr>
                                    <td class="w-25">Last modified time:</td>
                                    <td class="w-75">{{ archiveInfo.modified | formatDate }}</td>
                                </tr>
                                <tr v-if="archiveInfo.meta" v-for="(value, name) in archiveInfo.meta">
                                    <td class="w-25">{{ name }}:</td>
                                    <td class="w-75">
                                        <div v-for="data in value">
                                            <span>{{ data }}</span>
                                            <br/>
                                        </div>
                                    </td>
                                </tr>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>

            <!-- File structure -->
            <div class="row mt-4">
                <div class="col">
                    <div class="card">
                        <div class="card-header">
                            <div class="row align-items-center">
                                <div class="col-8">
                                    <h5 class="m-0">File structure</h5>
                                </div>
                                <div class="col-4 text-right">
                                    <button type="button" class="btn btn-primary" :disabled="isDisabled"
                                            @click="download">
                                        <i class="fas fa-download"/>
                                        Download
                                    </button>
                                </div>
                            </div>
                        </div>
                        <div class="card-body">
                            <app-tree-select v-model="value"
                                             :multiple="true"
                                             :show-count="true"
                                             :options="options"
                                             placeholder="Click to view file structure. Type to search. Select to download.">

                                <label slot="option-label"
                                       slot-scope="{ node, shouldShowCount, count, labelClassName, countClassName }"
                                       :class="labelClassName">
                                    {{ node.label }}
                                    <template v-if="!node.isBranch && isOpen">
                                        <span> - </span>
                                        <a :href="buildUrl(id, node.id)" target="_blank">View</a>
                                    </template>
                                    <span v-if="shouldShowCount" :class="countClassName">({{ count }})</span>
                                </label>
                            </app-tree-select>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Version -->
            <div class="row my-4">
                <div class="col">
                    <div class="card">
                        <div class="card-header">
                            <h5>Other versions</h5>
                        </div>
                        <div class="card-body">
                            <span v-if="!hasOtherVersion">This archive does not have any other version.</span>
                            <ul>
                                <template v-if="versionInfo.previousVersion">
                                    <li>
                                        Previous version:
                                        <router-link
                                                :to="{name: 'search-detail', params: {id: versionInfo.previousVersion.offlineId}}">
                                            {{ versionInfo.previousVersion.pid }}
                                        </router-link>
                                    </li>
                                </template>
                                <template v-if="versionInfo.nextVersions">
                                    <li>
                                        Next version:
                                        <ul>
                                            <li v-for="value in versionInfo.nextVersions">
                                                <router-link
                                                        :to="{name: 'search-detail', params: {id: value.onlineId ? value.onlineId : value.offlineId}}">
                                                    {{ value.pid }}
                                                </router-link>
                                            </li>
                                        </ul>
                                    </li>
                                </template>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>
        </template>
    </div>
</template>

<script>
    import Treeselect from '@riophae/vue-treeselect';
    import '@riophae/vue-treeselect/dist/vue-treeselect.css';

    import moment from 'moment';
    import {WritableStream} from 'web-streams-polyfill/ponyfill';
    import streamSaver from 'streamsaver';

    import lzaApi from '@/services/lzaApi';
    import treeService from '@/services/treeService';
    import emojiService from '@/services/emojiService';
    import SearchResult from './SearchResult';

    export default {
        props: {
            id: String
        },
        data() {
            return {
                archiveInfo: {},
                versionInfo: {},
                error: null,
                loading: true,
                value: [],
                options: []
            }
        },
        computed: {
            isOpen() {
                // Check if the archive is on disk
                return this.archiveInfo.state !== 'archived';
            },
            isDisabled() {
                // Check if the download button should be disabled or not
                return !this.isOpen || this.value.length < 1;
            },
            hasOtherVersion() {
                let hasVersion = false;
                if (this.versionInfo.previousVersion || this.versionInfo.nextVersions) {
                    hasVersion = true;
                }

                return hasVersion;
            }
        },
        components: {
            appSearchResult: SearchResult,
            appTreeSelect: Treeselect
        },
        filters: {
            formatDate(value) {
                if (value) {
                    return moment(String(value)).format('DD/MM/YYYY HH:mm');
                }
            }
        },
        methods: {
            async loadData() {
                const limit = 1000;
                let offset = 0, firstCall = true;

                try {
                    while (true) {
                        let response = await lzaApi.getArchiveInfo(this.id, limit, offset);

                        if (firstCall) {
                            // Store all data for the first call
                            this.archiveInfo = response.data;
                            firstCall = false;
                        } else {
                            // Otherwise, just add more files to the file array
                            this.archiveInfo.files = this.archiveInfo.files.concat(response.data.files);
                        }

                        // There is no more file to get? Stop
                        if (response.data.files.length < limit) {
                            break;
                        } else {
                            // Get new files by increasing the offset
                            offset += limit;
                        }
                    }
                } catch (error) {
                    this.error = true;
                    console.log(error);
                } finally {
                    this.loading = false;
                }

                // Get version information
                lzaApi.getVersionInfo(this.id)
                    .then(response => {
                        this.versionInfo = response.data;
                    })
                    .catch(error => {
                        this.error = true;
                        console.log(error);
                    });

                this.options = this.buildTree();
            },

            buildTree() {
                let tree = [];

                for (let i = 0; i < this.archiveInfo.files.length; i++) {
                    let fullPath = this.archiveInfo.files[i].name;

                    // Split the full path into many parts
                    let parts = fullPath.split('/');

                    // Start looking at the root
                    let currentLevel = tree;

                    for (let j = 0; j < parts.length; j++) {
                        let part = parts[j];

                        // Build the ID of each part. ID is the full path starting from root to that part
                        let index = fullPath.indexOf(part);
                        let partId = fullPath.substring(0, index + part.length);

                        // Check if this part is already exist in the tree
                        let existingPath = findWhere(currentLevel, 'id', partId);

                        // If yes, looking deeper
                        if (existingPath) {
                            currentLevel = existingPath.children;
                        } else {

                            let newPart = {
                                // Full path to this node
                                id: partId,

                                // How it is displayed on the UI
                                label: part,

                                // The name of this folder/file only
                                name: part
                            };

                            // Disable selection if the archive is not in open state
                            if (!this.isOpen) {
                                newPart.isDisabled = true;
                            }

                            // For non-leaf nodes
                            if (j < parts.length - 1) {

                                // Add children
                                newPart.children = [];

                                // Add folder emoji
                                newPart.label = '📁 ' + newPart.label;
                            }

                            // Add emoji to leaf-node
                            if (j === parts.length - 1) {
                                newPart.label = emojiService.getEmoji(this.archiveInfo.files[i].type) + ' ' + newPart.label;
                            }

                            currentLevel.push(newPart);

                            // Only go deeper if this is not the leaf node
                            if (j < parts.length - 1) {
                                currentLevel = newPart.children;
                            }
                        }
                    }
                }

                function findWhere(array, key, value) {
                    let t = 0;
                    while (t < array.length && array[t][key] !== value) {
                        t++;
                    }
                    if (t < array.length) {
                        return array[t]
                    } else {
                        return false;
                    }
                }

                return tree;
            },

            download() {
                if (this.value.length < 1) {
                    return;
                }

                let downloadItems = [];

                // Evaluate each chosen option
                for (let path of this.value) {

                    // Select the node corresponding to the path
                    let node = treeService.getNode(this.options, path);

                    // If it's not a leaf node
                    if (node['children']) {

                        // Get all files under it
                        let leafNodes = treeService.getLeafNodes(node);
                        leafNodes.forEach(item => downloadItems.push(item.id));
                    } else {

                        // This is a leaf node, simply add it to the set
                        downloadItems.push(node.id);
                    }
                }

                // Send the download set to server
                lzaApi.downloadFiles(this.archiveInfo.id, downloadItems)
                    .then(response => {
                        this.consumeDownloadStream(response);
                    })
                    .catch(error => {
                        this.error = true;
                        console.log(error);
                    });
            },

            exportArchive() {
                lzaApi.exportArchive(this.id)
                    .then(response => {
                        this.consumeDownloadStream(response);
                    })
                    .catch(error => {
                        this.error = true;
                        console.log(error);
                    });
            },

            consumeDownloadStream(response) {
                let contentDisposition = response.headers.get('Content-Disposition');
                let fileName = contentDisposition.substring(contentDisposition.lastIndexOf('=') + 1);

                // These code section is adapted from an example of the StreamSaver.js
                // https://jimmywarting.github.io/StreamSaver.js/examples/fetch.html

                // If the WritableStream is not available (Firefox, Safari), take it from the ponyfill
                if (!window.WritableStream) {
                    streamSaver.WritableStream = WritableStream;
                    window.WritableStream = WritableStream;
                }

                const fileStream = streamSaver.createWriteStream(fileName);
                // const readableStream = response.body;

                // More optimized (but doesn't work on Safari!)
                // if (readableStream.pipeTo) {
                //     return readableStream.pipeTo(fileStream);
                // }

                window.writer = fileStream.getWriter();

                const reader = response.body.getReader();
                const pump = () => reader.read()
                    .then(res => res.done
                        ? writer.close()
                        : writer.write(res.value).then(pump));

                pump();
            },

            buildUrl(id, path) {
                // Used to escape special characters
                let esc = encodeURIComponent;

                // Base URL for to view file
                let base = "/api/download-file";

                return `${base}/${id}?path=${esc(path)}`;
            },
        },
        async created() {
            await this.loadData();
        },
        watch: {
            '$route.params.id': 'loadData'
        }
    }
</script>

<style lang="scss" scoped>
    .card .card-header {
        h5 {
            color: $primary;
        }
    }
</style>
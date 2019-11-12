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

        <!-- Full details -->
        <div class="row mt-4">
            <div class="col">
                <div class="card">
                    <div class="card-header">
                        <div class="row align-items-center">
                            <div class="col-8">
                                <h5 class="m-0">Archive ID: {{ archiveInfo.id }}</h5>
                            </div>
                            <div class="col-4 text-right">
                                <button type="button" class="btn btn-primary" :disabled="!isOpen" @click="exportArchive">
                                    <i class="fas fa-download"></i>
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
                                <button type="button" class="btn btn-primary" :disabled="isDisabled" @click="download">
                                    <i class="fas fa-download"></i>
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
                                         :disable-branch-nodes="!isOpen"
                                         placeholder="Click to view file structure. Type to search. Select to download."/>
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
                        <!-- TODO: Show previous and next versions here. -->
                        This archive does not have any other version.
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import Treeselect from '@riophae/vue-treeselect';
    import '@riophae/vue-treeselect/dist/vue-treeselect.css';

    import moment from 'moment';
    import { WritableStream } from 'web-streams-polyfill/ponyfill';
    import streamSaver from 'streamsaver';

    import lzaApi from '@/services/lzaApi';
    import treeService from '@/services/treeService';
    import emojiService from '@/services/emojiService';
    import SearchResult from './SearchResult';

    export default {
        props: {
            id: String
        },
        computed: {
            isOpen() {
                // Check if the archive is on disk
                return this.archiveInfo.state !== 'archived';
            },
            isDisabled() {

                // Check if the download button should be disabled or not
                return !this.isOpen || this.value.length < 1;
            }
        },
        data() {
            return {
                archiveInfo: {},
                error: null,
                loading: true,
                value: [],
                options: []
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

                            // For non-leaf nodes
                            if (j < parts.length - 1) {

                                // Add children
                                newPart.children = [];

                                // Add folder emoji
                                newPart.label = '📁 ' + newPart.label;
                            }

                            // Add emoji to leaf-node
                            if (j === parts.length - 1) {
                                //newPart.label = emojiService.getEmoji(part) + ' ' + newPart.label;
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
                const readableStream = response.body;

                // More optimized
                if (readableStream.pipeTo) {
                    return readableStream.pipeTo(fileStream);
                }

                window.writer = fileStream.getWriter();

                const reader = response.body.getReader();
                const pump = () => reader.read()
                    .then(res => res.done
                        ? writer.close()
                        : writer.write(res.value).then(pump));

                pump();
            }
        },
        created() {
            lzaApi.getArchiveInfo(this.id)
                .then(response => {
                    this.archiveInfo = response.data;
                    this.options = this.buildTree();
                })
                .catch(error => {
                    this.error = true;
                    console.log(error);
                })
                .finally(() => {
                    this.loading = false;
                });
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
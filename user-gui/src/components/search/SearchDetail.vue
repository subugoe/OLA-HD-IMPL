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
                        <i class="fas fa-download float-right"></i>
                        <h5>Archive ID: {{ archiveInfo.id }}</h5>
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
                        <i class="fas fa-download float-right"></i>
                        <h5>File structure</h5>
                    </div>
                    <div class="card-body">
                        <app-tree-select v-model="value"
                                         :multiple="true"
                                         :options="options"
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
                        Show previous and next versions here.
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

    import lzaApi from '@/services/lzaApi';
    import SearchResult from './SearchResult';

    export default {
        props: {
            id: String
        },
        data() {
            return {
                archiveInfo: {},
                error: null,
                loading: true,
                value: [],
                options: [
                    {
                        id: 'a',
                        label: 'a',
                        children: [
                            {
                                id: 'aa',
                                label: 'aa',
                            },
                            {
                                id: 'ab',
                                label: 'ab',
                            }
                        ],
                    },
                    {
                        id: 'b',
                        label: 'b',
                    },
                    {
                        id: 'c',
                        label: 'c',
                    }
                ]
            }
        },
        components: {
            appSearchResult: SearchResult,
            appTreeSelect: Treeselect
        },
        filters: {
            formatDate(value) {
                if (value) {
                    return moment(String(value)).format('DD/MM/YYYY hh:mm');
                }
            }
        },
        created() {
            lzaApi.getArchiveInfo(this.id)
                .then(response => {
                    this.archiveInfo = response.data;
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
        i {
            color: $primary;
            cursor: pointer;
        }

        h5 {
            color: $primary;
        }
    }
</style>
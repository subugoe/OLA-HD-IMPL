<template>
    <div class="container">

        <!-- Full details -->
        <div class="row mt-4">
            <div class="col">
                <app-search-result></app-search-result>
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
        <div class="row mt-4">
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

    import SearchResult from './SearchResult';

    export default {
        data() {
            return {
                value: null,
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
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

        <!-- Search status -->
        <div class="row my-3" v-if="!loading && !error">
            <div class="col-6">{{ this.totalMessage }} ({{ time }} ms)</div>
            <div class="col-6 text-right">Showing 1 - 12</div>
        </div>

        <div class="row" v-if="loading">
            <div class="col text-center">
                <img src="../../assets/images/spin-1s-100px.gif" alt="Searching">
            </div>
        </div>

        <!-- Search result -->
        <div class="row mb-3" v-if="!loading"
             v-for="(result, index) in results.hits" :key="index">
            <div class="col">
                <app-search-result :item="result"></app-search-result>
            </div>
        </div>

        <!-- Navigation -->
        <div class="row mb-3" v-if="!loading">
            <div class="col-12 text-center">
                <button class="btn btn-primary mr-5">Previous</button>
                <button class="btn btn-primary">Next</button>
            </div>
        </div>
    </div>
</template>

<script>
    import lzaApi from '@/services/lzaApi';
    import SearchResult from './SearchResult';

    export default {
        props: {
            query: String
        },
        data() {
            return {
                loading: true,
                error: null,
                time: 0,
                totalMessage: '',
                results: {}
            }
        },
        components: {
            appSearchResult: SearchResult
        },
        methods: {
            search() {
                // Reset the state
                this.loading = true;
                this.error = null;
                this.results = {};

                let start = performance.now();

                // TODO: set a limit and implement pagination
                lzaApi.search(this.query)
                    .then(response => {

                        // Calculate the elapsed time
                        let end = performance.now();
                        this.time = Math.round(end - start);

                        // Display proper total message
                        this.totalMessage = response.data.total;
                        if (response.data.total > 1) {
                            this.totalMessage += ' results';
                        } else {
                            this.totalMessage += ' result';
                        }

                        // Render the results
                        this.results = response.data;
                    })
                    .catch(error => {
                        this.error = true;
                        console.log(error);
                    })
                    .finally(() => {
                        this.loading = false;
                    });
            }
        },
        created() {
            this.search();
        },
        watch: {
            '$route': 'search'
        }
    }
</script>

<style scoped>

</style>
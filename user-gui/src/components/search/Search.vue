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

        <template v-if="!loading && !error">

            <!-- Search status -->
            <div class="row my-3">
                <div class="col-6">{{ totalMessage }} ({{ time }} ms)</div>
                <div class="col-6 text-right">{{ paginationMessage }}</div>
            </div>

            <!-- Search result -->
            <div class="row mb-3"
                 v-for="(result, index) in results.hits" :key="index">
                <div class="col">
                    <app-search-result :item="result"></app-search-result>
                </div>
            </div>

            <!-- Navigation -->
            <div class="row mb-3">
                <div class="col-12 text-center">
                    <button class="btn btn-primary mr-5" v-if="hasPreviousPage" @click="search(-1)">Previous</button>
                    <button class="btn btn-primary" v-if="hasNextPage" @click="search(1)">Next</button>
                </div>
            </div>
        </template>
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
                maxResultsSize: 10,
                paginationLowerBound: 1,
                scrolls: [''],
                time: 0,
                results: {}
            }
        },
        computed: {
            hasPreviousPage() {
                return this.scrolls.length > 2;
            },
            hasNextPage() {
                return this.results &&
                    this.results.count === this.maxResultsSize &&
                    this.results.total > this.maxResultsSize;
            },
            totalMessage() {
                // Display proper total message
                let totalMessage = this.results.total;
                if (this.results.total > 1) {
                    totalMessage += ' results';
                } else {
                    totalMessage += ' result';
                }
                return totalMessage;
            },
            paginationMessage() {
                let paginationUpperBound = this.paginationLowerBound + this.maxResultsSize - 1;

                if (paginationUpperBound > this.results.total) {
                    paginationUpperBound = this.results.total;
                }
                return `Showing ${this.paginationLowerBound} - ${paginationUpperBound}`;
            }
        },
        components: {
            appSearchResult: SearchResult
        },
        methods: {
            search(pageChange) {

                // Reset the state
                this.loading = true;
                this.error = null;
                this.results = {};

                let scroll = '';

                // What kind of search is it?
                switch (pageChange) {

                    // Go to next page?
                    case 1:
                        // Update the scroll state
                        scroll = this.scrolls[this.scrolls.length - 1];

                        // Update the pagination message
                        this.paginationLowerBound += this.maxResultsSize;

                        break;

                    // Go to previous page?
                    case -1:
                        // Remove 2 top scroll to get the scroll of the previous page
                        this.scrolls.pop();
                        this.scrolls.pop();

                        // Update the scroll state
                        scroll = this.scrolls[this.scrolls.length - 1];

                        // Update the pagination message
                        this.paginationLowerBound -= this.maxResultsSize;
                        break;

                    // New search?
                    default:
                        this.scrolls = [''];
                        this.paginationLowerBound = 1;
                }

                // Start the timer
                let start = performance.now();

                // Execute the search
                lzaApi.search(this.query, this.maxResultsSize, scroll)
                    .then(response => {

                        // Calculate the elapsed time
                        let end = performance.now();
                        this.time = Math.round(end - start);

                        // Render the results
                        this.results = response.data;

                        // Add the new scroll to the list
                        this.scrolls.push(response.data.scroll);
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
            this.search(0);
        },
        watch: {
            '$route': 'search'
        }
    }
</script>

<style scoped>

</style>
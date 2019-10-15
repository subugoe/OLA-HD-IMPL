<template>
    <div class="container">

        <!-- Search status -->
        <div class="row my-3">
            <div class="col-6">15 results (20 ms)</div>
            <div class="col-6 text-right">Showing 1 - 12</div>
        </div>

        <!-- Search result -->
        <div class="row">
            <div class="col-md-6 col-lg-4 mb-3"
                 v-for="result in [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]" :key="result">
                <app-search-result></app-search-result>
            </div>
        </div>

        <!-- Navigation -->
        <div class="row mb-3">
            <div class="col-12 text-center">
                <button class="btn btn-primary mr-5">Previous</button>
                <button class="btn btn-primary">Next</button>
            </div>
        </div>
    </div>
</template>

<script>
    import SearchResult from './SearchResult'

    export default {
        components: {
            appSearchResult: SearchResult
        }
    }
</script>

<style scoped>

</style>
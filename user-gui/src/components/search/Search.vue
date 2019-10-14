<template>
    <div class="container">

        <!-- Search status -->
        <div class="row pt-3">
            <p class="mr-auto">15 results (20 ms)</p>
            <p class="text-right">Showing 1 - 12</p>
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
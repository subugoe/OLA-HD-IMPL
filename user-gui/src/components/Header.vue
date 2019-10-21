<template>
    <header id="search-header">
        <div class="overlay"></div>
        <div class="container">
            <div class="row">
                <div class="col-sm-12 col-lg-8 mx-lg-auto">
                    <h1 class="header-text mb-5">OLA-HD - A Long Term Archive System</h1>
                </div>
            </div>

            <div class="row">
                <div class="col-md-6 col-sm-8 mx-auto">
                    <form @submit.prevent="submit">
                        <div class="input-group position-relative">
                            <label for="search-box" class="sr-only">Search box</label>
                            <input type="text" id="search-box" class="form-control" placeholder="Enter your search here" v-model="query">
                            <div class="input-group-append">
                                <button class="btn btn-sm btn-link" type="submit">
                                    <i class="fas fa-search fa-lg"></i>
                                </button>
                            </div>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </header>
</template>

<script>
    export default {
        data() {
            return {
                query: this.$route.query['q']
            }
        },
        methods: {
            submit() {
                // Trim the query
                if (this.query) {
                    this.query = this.query.trim();

                    // Only search if the query is not empty
                    if (this.query.length > 0) {
                        this.$router.push({
                            name: 'search',
                            query: {
                                q: this.query
                            }
                        }).catch(() => {}); // To ignore the Navigation Duplicated error
                    }
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    #search-header {
        position: relative;
        background: url("../assets/images/archive.jpg") no-repeat center center;
        background-size: cover;
        padding-top: 8rem;
        padding-bottom: 8rem;
        text-align: center;

        .overlay {
            position: absolute;
            background-color: #212529;
            height: 100%;
            width: 100%;
            top: 0;
            left: 0;
            opacity: 0.5;
        }

        .header-text {
            color: white;
            font-weight: bold;
            font-size: 1.5rem;

            @include media-breakpoint-up(sm) {
                font-size: 2rem;
            }

            @include media-breakpoint-up(xl) {
                font-size: 2.3rem;
            }
        }

        #search-box {
            border-radius: 1rem;
            border: 3px solid $primary;
            display: inline;
            padding-right: 40px;

            // Remove glow
            outline: none;
            -webkit-box-shadow: none !important;
            -moz-box-shadow: none !important;
            box-shadow: none !important;
        }

        form {
            .input-group-append {
                position: absolute;
                right: 3px;
                top: 3px;
                z-index: 99;
            }

            i {
                color: $secondary;
            }
        }
    }
</style>
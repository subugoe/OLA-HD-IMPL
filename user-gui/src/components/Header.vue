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
                    <form>
                        <div class="form-group position-relative">
                            <input type="text" id="search-box" class="form-control" placeholder="Enter your search here">
                            <i class="fa fa-search fa-lg"></i>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </header>
</template>

<script>
    export default {
        name: "Header"
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
            font-size: 2rem;

            @include media-breakpoint-down(sm) {
                font-size: 1.5rem;
            }

            @include media-breakpoint-up(xl) {
                font-size: 2.5rem;
            }
        }

        #search-box {
            border-radius: 1rem;
            border: 3px solid $primary;
            display: inline;
            padding-right: 40px;
        }

        .form-control + i {
            position: absolute;
            right: 0;
            padding: 11px 10px;
        }
    }
</style>
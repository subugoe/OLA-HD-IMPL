<template>
    <footer>
        <div class="container-fluid">
            <div class="row py-3">
                <div class="col-md-7 align-self-center text-center text-md-left">
                    <ul class="list-inline mb-0">
                        <li class="list-inline-item mx-0">
                            <router-link to="/about">About</router-link>
                            <span class="mx-2">&#183;</span>
                        </li>
                        <li class="list-inline-item mx-0">
                            <router-link to="/contact">Contact</router-link>
                            <span class="mx-2">&#183;</span>
                        </li>
                        <li class="list-inline-item mx-0">
                            <router-link to="/terms-of-use">Terms of Use</router-link>
                            <span class="mx-2">&#183;</span>
                        </li>
                        <li class="list-inline-item mx-0">
                            <router-link to="/privacy">Privacy Policy</router-link>
                        </li>
                    </ul>
                </div>
                <div class="col-md-5 align-self-center text-center text-md-right">
                    <div>&copy; GWDG 2019. All Rights Reserved.</div>
                </div>
            </div>
        </div>
    </footer>
</template>

<script>
    export default {
        name: "Footer"
    }
</script>

<!-- To keep the footer at the bottom of the page -->
<style lang="scss">
    html, body {
        height: 100%;
        position: relative;
    }

    #app {
        min-height: 100vh; /* will cover the 100% of viewport */
        overflow: hidden;
        display: block;
        position: relative;
        padding-bottom: 80px; /* height of your footer */

        @include media-breakpoint-down(xs) {
            padding-bottom: 104px;
        }

        @include media-breakpoint-up(sm) {
            padding-bottom: 56px;
        }
    }

    footer {
        position: absolute;
        bottom: 0;
        width: 100%;
    }
</style>

<style lang="scss" scoped>
    footer {
        background-color: $secondary;
        color: white;

        ul li.list-inline-item a {
            color: white;
            text-decoration: none;
        }
    }
</style>
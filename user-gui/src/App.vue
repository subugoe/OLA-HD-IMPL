<template>
    <div id="app">

        <!-- Navigation -->
        <nav class="navbar navbar-expand-md bg-light p-0">
            <router-link tag="a" to="/" class="navbar-brand p-2">
                <img src="./assets/logo-gwdg-full.png" alt="Brand logo" class="mr-2 mr-md-4">
                <img src="./assets/logo-sub.png" alt="Brand logo" class="mr-2 mr-md-4">
                <img src="./assets/logo-dfg.png" alt="Brand logo">
            </router-link>

            <!-- Hamburger button -->
            <button class="navbar-toggler" type="button" data-toggle="collapse"
                    data-target="#navbarNavDropdown"
                    aria-controls="navbarNavDropdown"
                    aria-expanded="false"
                    aria-label="Toggle navigation">
                <span class="navbar-toggler-icon">
                    <i class="fa fa-bars fa-lg"></i>
                </span>
            </button>

            <!-- Navigation menu -->
            <div class="collapse navbar-collapse" id="navbarNavDropdown">
                <ul class="navbar-nav ml-auto">
                    <router-link tag="li" class="nav-item px-2" active-class="active" to="/" exact>
                        <a class="nav-link">Home</a>
                    </router-link>
                    <router-link tag="li" class="nav-item px-2" active-class="active" to="/about">
                        <a class="nav-link">Über das Projekt</a>
                    </router-link>
                    <router-link tag="li" class="nav-item px-2" active-class="active" to="/contact">
                        <a class="nav-link">Kontakt</a>
                    </router-link>
                    <router-link tag="li" class="nav-item px-2" active-class="active" to="/faq">
                        <a class="nav-link">FAQ</a>
                    </router-link>
                    <li class="nav-item px-2">
                        <a class="nav-link" href="/admin/login">Login</a>
                    </li>
                </ul>
            </div>
        </nav>

        <app-header></app-header>
        <router-view/>
        <app-footer></app-footer>
    </div>
</template>

<script>
    import Header from './components/Header';
    import Footer from './components/Footer';

    export default {
        components: {
            appHeader: Header,
            appFooter: Footer
        }
    }
</script>

<style lang="scss" scoped>
    .navbar {

        .navbar-brand {
            img {
                height: 30px;

                @include media-breakpoint-up(sm) {
                    height: 50px;
                }
            }
        }

        .fa.fa-bars {
            color: $secondary;
        }

        .navbar-nav {

            .nav-item {
                padding-top: 1rem;
                padding-bottom: 1rem;

                a {
                    text-transform: uppercase;
                    color: $primary;
                }
            }

            .nav-item:hover, .nav-item.active {
                background-color: $primary;

                a {
                    color: white;
                }
            }
        }
    }
</style>
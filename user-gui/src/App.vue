<template>
    <div id="app">

        <!-- Navigation -->
        <nav class="navbar navbar-expand-sm bg-light p-0">
            <router-link tag="a" to="/" class="navbar-brand p-2">
                <img src="./assets/logo-gwdg-full.png" alt="Brand logo">
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
                        <a class="nav-link">About</a>
                    </router-link>
                    <router-link tag="li" class="nav-item px-2" active-class="active" to="/contact">
                        <a class="nav-link">Contact</a>
                    </router-link>
                    <router-link tag="li" class="nav-item px-2" active-class="active" to="/login">
                        <a class="nav-link">Login</a>
                    </router-link>
                </ul>
            </div>
        </nav>
        <router-view/>
    </div>
</template>

<style lang="scss" scoped>
    .navbar {

        .navbar-brand {
            img {
                height: 50px;
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
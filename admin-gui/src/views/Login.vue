<template>
    <div class="container">

        <!-- Outer Row -->
        <div class="row justify-content-center">

            <div class="col-xl-10 col-lg-12 col-md-9">

                <div class="card o-hidden border-0 shadow-lg my-5">
                    <div class="card-body p-0">
                        <!-- Nested Row within Card Body -->
                        <div class="row">
                            <div class="col-lg-6 d-none d-lg-block bg-login-image"></div>
                            <div class="col-lg-6">
                                <div class="login-logo pt-3">
                                    <a href="/home">
                                        <img src="../assets/images/logo-gwdg.png" class="mr-4" alt="GWDG Logo"/>
                                        <img src="../assets/images/logo-sub.png" alt="SUB Logo"/>
                                    </a>
                                </div>
                                <div class="p-5">
                                    <div class="text-center">
                                        <h1 class="h2 text-gray-900 mb-4">OLA-HD Admin Page</h1>
                                    </div>
                                    <form class="user" @submit.prevent="onSubmit">
                                        <div class="form-group">
                                            <input type="text"
                                                   class="form-control form-control-user"
                                                   placeholder="Username"
                                                   v-model="username">
                                        </div>
                                        <div class="form-group">
                                            <input type="password"
                                                   class="form-control form-control-user"
                                                   placeholder="Password"
                                                   v-model="password">
                                        </div>
                                        <!--<div class="form-group">
                                            <div class="custom-control custom-checkbox small">
                                                <input type="checkbox" class="custom-control-input" id="customCheck">
                                                <label class="custom-control-label" for="customCheck">Remember Me</label>
                                            </div>
                                        </div>-->
                                        <div class="invalid-feedback text-center d-block mb-2" v-if="isFailed">
                                            Login failed. Please check your credentials.
                                        </div>
                                        <button class="btn btn-primary btn-user btn-block" type="submit">Login</button>
                                    </form>
                                    <hr>
                                    <div class="text-center small">
                                        If you forget your password or want to create a test account, please contact
                                        <a href="mailto:triet.doan@gwdg.de">triet.doan@gwdg.de</a>
                                    </div>
                                    <hr>
                                    <div class="login-logo">
                                        <img src="../assets/images/logo-dfg.png"
                                             class="w-100"
                                             alt="DFG Logo"/>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

            </div>

        </div>

    </div>
</template>

<script>
    export default {
        props: {
            redirect: String
        },
        data() {
            return {
                username: '',
                password: '',
                isFailed: false
            }
        },
        methods: {
            onSubmit() {
                // Hide the error message
                this.isFailed = false;

                const formData = {
                    username: this.username,
                    password: this.password,
                };
                this.$store.dispatch('login', {
                    username: formData.username,
                    password: formData.password
                }).then(result => {
                    this.$router.replace(this.redirect ? this.redirect : '/');
                }).catch(() => {
                    this.isFailed = true;
                });
            },
            toggleBodyClass(addRemoveClass, className) {
                const el = document.body;

                if (addRemoveClass === 'addClass') {
                    el.classList.add(className);
                } else {
                    el.classList.remove(className);
                }
            },
        },
        mounted() {
            this.toggleBodyClass('addClass', 'bg-gradient-primary');
        },
        destroyed() {
            this.toggleBodyClass('removeClass', 'bg-gradient-primary');
        }
    }
</script>

<style scoped>

</style>
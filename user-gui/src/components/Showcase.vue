<template>
    <section id="feature" class="bg-light">
        <div class="container">
            <div class="row py-3">

                <!-- Feature -->
                <div class="col-md-4">
                    <div class="card text-center">
                        <div class="card-body">
                            <i class="fas fa-cogs fa-3x mb-3"></i>
                            <h3 class="card-title">PID Support</h3>
                            <p class="card-text lead">
                                Lorem ipsum dolor sit amet, consectetur adipiscing elit.
                            </p>
                        </div>
                    </div>
                </div>

                <!-- Feature -->
                <div class="col-md-4">
                    <div class="card text-center">
                        <div class="card-body">
                            <i class="fas fa-archive fa-3x mb-3"></i>
                            <h3 class="card-title">Long Term Archive</h3>
                            <p class="card-text lead">
                                Lorem ipsum dolor sit amet, consectetur adipiscing elit.
                            </p>
                        </div>
                    </div>
                </div>

                <!-- Feature -->
                <div class="col-md-4">
                    <div class="card text-center">
                        <div class="card-body">
                            <i class="far fa-check-circle fa-3x mb-3"></i>
                            <h3 class="card-title">Easy to Use</h3>
                            <p class="card-text lead">
                                Lorem ipsum dolor sit amet, consectetur adipiscing elit.
                            </p>
                        </div>
                    </div>
                </div>

            </div>
        </div>
    </section>
</template>

<script>
    export default {
        name: "Showcase"
    }
</script>

<style lang="scss" scoped>
    .card {
        background-color: transparent;
        border: none;

        .card-body i {
            color: $primary;
        }
    }
</style>
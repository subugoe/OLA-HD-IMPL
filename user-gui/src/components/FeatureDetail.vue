<template>
    <section id="feature-detail">
        <div class="container-fluid">

            <!-- Feature Detail -->
            <div class="row">
                <div class="col-md-6 align-self-center">
                    <div class="card">
                        <div class="card-body">
                            <h3 class="card-title">PID Support</h3>
                            <p class="card-text lead">
                                Lorem ipsum dolor sit amet, consectetur adipiscing elit.
                                Integer laoreet tellus a felis bibendum ullamcorper.
                                Vestibulum gravida vitae nunc nec venenatis.
                                Nullam sodales urna diam, id blandit lectus commodo vel.
                            </p>
                        </div>
                    </div>
                </div>
                <div class="col-md-6 p-0">
                    <img src="../assets/images/cog-wheels.jpg" alt="Cog wheel" class="img-fit">
                </div>
            </div>

            <!-- Feature Detail -->
            <div class="row">
                <div class="col-md-6 align-self-center order-md-2">
                    <div class="card">
                        <div class="card-body">
                            <h3 class="card-title">Long Term Archive</h3>
                            <p class="card-text lead">
                                Lorem ipsum dolor sit amet, consectetur adipiscing elit.
                                Integer laoreet tellus a felis bibendum ullamcorper.
                                Vestibulum gravida vitae nunc nec venenatis.
                                Nullam sodales urna diam, id blandit lectus commodo vel.
                            </p>
                        </div>
                    </div>
                </div>
                <div class="col-md-6 p-0 order-md-1">
                    <img src="../assets/images/book.jpg" alt="Archive" class="img-fit">
                </div>
            </div>

            <!-- Feature Detail -->
            <div class="row">
                <div class="col-md-6 align-self-center">
                    <div class="card">
                        <div class="card-body">
                            <h3 class="card-title">Easy to Use</h3>
                            <p class="card-text lead">
                                Lorem ipsum dolor sit amet, consectetur adipiscing elit.
                                Integer laoreet tellus a felis bibendum ullamcorper.
                                Vestibulum gravida vitae nunc nec venenatis.
                                Nullam sodales urna diam, id blandit lectus commodo vel.
                            </p>
                        </div>
                    </div>
                </div>
                <div class="col-md-6 p-0">
                    <img src="../assets/images/hot-air-balloon.jpg" alt="Hot air ballon" class="img-fit">
                </div>
            </div>

        </div>
    </section>
</template>

<script>
    export default {
        name: "FeatureDetail"
    }
</script>

<style lang="scss" scoped>
    #feature-detail .row {
        .card {
            background-color: transparent;
            border: none;
        }

        img.img-fit {
            width: 100%;
            height: 300px;
            object-fit: cover
        }
    }
</style>
<template>
    <section id="feature-detail">
        <div class="container-fluid">

            <!-- Feature Detail -->
            <div class="row">
                <div class="col-sm-12 col-md-6 align-self-center">
                    <div class="card">
                        <div class="card-body">
                            <h5 class="card-title">PID Support</h5>
                            <p class="card-text">
                                Lorem ipsum dolor sit amet, consectetur adipiscing elit.
                                Integer laoreet tellus a felis bibendum ullamcorper.
                                Vestibulum gravida vitae nunc nec venenatis.
                                Nullam sodales urna diam, id blandit lectus commodo vel.
                            </p>
                        </div>
                    </div>
                </div>
                <div class="col-sm-12 col-md-6 p-0">
                    <img src="../assets/images/cog-wheels.jpg" alt="Cog wheel" class="img-fit">
                </div>
            </div>

            <!-- Feature Detail -->
            <div class="row">
                <div class="col-sm-12 col-md-6 p-0">
                    <img src="../assets/images/book.jpg" alt="Archive" class="img-fit">
                </div>
                <div class="col-sm-12 col-md-6 align-self-center">
                    <div class="card">
                        <div class="card-body">
                            <h5 class="card-title">Long Term Archive</h5>
                            <p class="card-text">
                                Lorem ipsum dolor sit amet, consectetur adipiscing elit.
                                Integer laoreet tellus a felis bibendum ullamcorper.
                                Vestibulum gravida vitae nunc nec venenatis.
                                Nullam sodales urna diam, id blandit lectus commodo vel.
                            </p>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Feature Detail -->
            <div class="row">
                <div class="col-sm-12 col-md-6 align-self-center">
                    <div class="card">
                        <div class="card-body">
                            <h5 class="card-title">Easy to Use</h5>
                            <p class="card-text">
                                Lorem ipsum dolor sit amet, consectetur adipiscing elit.
                                Integer laoreet tellus a felis bibendum ullamcorper.
                                Vestibulum gravida vitae nunc nec venenatis.
                                Nullam sodales urna diam, id blandit lectus commodo vel.
                            </p>
                        </div>
                    </div>
                </div>
                <div class="col-sm-12 col-md-6 p-0">
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
        height: 300px;

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
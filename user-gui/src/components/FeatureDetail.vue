<template>
    <section id="feature-detail">
        <div class="container-fluid">

            <!-- Feature Detail -->
            <div class="row">
                <div class="col-md-6 align-self-center">
                    <div class="card">
                        <div class="card-body">
                            <h3 class="card-title">Über das Projekt</h3>
                            <p class="card-text lead">
                                Von der <a href="https://www.dfg.de/">Deutschen Forschungsgemeinschaft</a> gefördertes
                                Projekt zur Entwicklung eines integrierten Konzeptes für die Langzeitarchivierung und
                                persistente Identifizierung von OCR-Objekten sowie die prototypische Implementierung
                                dieses Konzepts. (
                                <router-link to="/about">Mehr...</router-link>
                                )
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
                            <h3 class="card-title">Konzept</h3>
                            <p class="card-text lead">
                                Das schriftliche Konzept "Langzeitarchivierung und persistente Identifizierung von
                                OCR-Objekten" enthält die Anforderungen, die Spezifikationen, die Dokumentationen zu den
                                technischen, organisatorischen und wirtschaftlichen Aspekten der Umsetzung, sowohl bei
                                der Langzeitarchivierung als auch bei der persistenten Identifikation. (<a
                                    href="https://github.com/subugoe/OLA-HD-IMPL/blob/master/docs/OLA-HD_Konzept.pdf"
                                    target="_blank">Konzept</a>)
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
                            <h3 class="card-title">Testarchiv</h3>
                            <p class="card-text lead">
                                Auf dieser Website kann die prototypische Implementierung des Konzeptes
                                ausprobiert werden.
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
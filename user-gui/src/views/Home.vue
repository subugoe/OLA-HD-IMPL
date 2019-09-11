<template>
    <div>
        <app-header></app-header>
        <app-showcase></app-showcase>
        <app-feature-detail></app-feature-detail>
        <app-footer></app-footer>
    </div>
</template>

<script>
    import Header from '../components/Header';
    import Showcase from '../components/Showcase';
    import FeatureDetail from '../components/FeatureDetail';
    import Footer from '../components/Footer';

    export default {
        components: {
            appHeader: Header,
            appShowcase: Showcase,
            appFeatureDetail: FeatureDetail,
            appFooter: Footer
        }
    }
</script>
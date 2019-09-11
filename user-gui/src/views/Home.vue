<template>
    <div>
        <app-header></app-header>
        <app-showcase></app-showcase>
        <app-feature-detail></app-feature-detail>
    </div>
</template>

<script>
    import Header from '../components/Header';
    import Showcase from '../components/Showcase';
    import FeatureDetail from '../components/FeatureDetail';

    export default {
        components: {
            appHeader: Header,
            appShowcase: Showcase,
            appFeatureDetail: FeatureDetail
        }
    }
</script>
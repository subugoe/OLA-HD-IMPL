<template>
    <div>
        <app-showcase></app-showcase>
        <app-feature-detail></app-feature-detail>
    </div>
</template>

<script>
    import Showcase from '../components/Showcase';
    import FeatureDetail from '../components/FeatureDetail';

    export default {
        components: {
            appShowcase: Showcase,
            appFeatureDetail: FeatureDetail
        }
    }
</script>
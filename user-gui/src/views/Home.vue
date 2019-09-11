<template>
    <div>
        <app-header></app-header>
        <app-showcase></app-showcase>
    </div>
</template>

<script>
    import Header from '../components/Header';
    import Showcase from '../components/Showcase';

    export default {
        components: {
            appHeader: Header,
            appShowcase: Showcase
        }
    }
</script>
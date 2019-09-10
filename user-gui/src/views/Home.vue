<template>
    <div class="home">
        <img alt="Vue logo" src="../assets/logo.png">
        <HelloWorld msg="Welcome to Your Vue.js + TypeScript App"/>
        <button class="btn btn-primary">Click me!</button>
    </div>
</template>

<script lang="ts">
    // import { Component, Vue } from 'vue-property-decorator';
    import HelloWorld from '@/components/HelloWorld.vue'; // @ is an alias to /src

    // @Component({
    //     components: {
    //         HelloWorld,
    //     },
    // })
    // export default class Home extends Vue {}
    export default {
        components: {
            HelloWorld: HelloWorld
        },
        created() {
            console.log("On Home Create!");
            console.log(process.env.BASE_URL);
            console.log(process.env.NODE_ENV);
            console.log(process.env);
        }
    }
</script>

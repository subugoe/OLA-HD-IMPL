<template>
    <div class="card">
        <div class="card-header">
            <i class="fas fa-download float-right"></i>
            <router-link :to="{ name: 'search-detail', params: { id: item.id }}">
                <h5>{{ title }}</h5>
            </router-link>
        </div>
        <div class="card-body">
            <table class="table table-borderless table-sm">
                <tbody>
                    <tr>
                        <td class="w-25">Archive ID:</td>
                        <td class="w-75">{{ item.id }}</td>
                    </tr>
                    <tr>
                        <td class="w-25">Type:</td>
                        <td class="w-75">{{ item.type }}</td>
                    </tr>
                    <tr v-if="item.name">
                        <td class="w-25">Path to file:</td>
                        <td class="w-75">{{ item.name }}</td>
                    </tr>
                    <!-- TODO: Display created / modified date and size -->
<!--                    <tr>-->
<!--                        <td>Date:</td>-->
<!--                        <td>14.10.2019</td>-->
<!--                    </tr>-->
                </tbody>
            </table>
        </div>
    </div>
</template>

<script>
    export default {
        props: {
            item: Object
        },
        computed: {
            title() {
                if (this.item.name) {

                    // Show the file name only
                    let slash = this.item.name.lastIndexOf('/');
                    if (slash > -1) {
                        return this.item.name.substring(slash + 1);
                    }
                    return this.item.name;
                }
                return 'This is an archive.';
            }
        }
    }
</script>

<style lang="scss" scoped>
    .card .card-header {
        i {
            color: $primary;
            cursor: pointer;
        }

        h5 {
            color: $primary;
        }
    }
</style>
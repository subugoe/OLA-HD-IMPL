<template>
    <div class="card">
        <div class="card-header">
            <i class="fas fa-download float-right"></i>
            <h5>{{ title }}</h5>
        </div>
        <div class="card-body">
            <table class="table table-borderless table-sm">
                <tbody>
                    <tr>
                        <td>Archive ID:</td>
                        <td>{{ item.id }}</td>
                    </tr>
                    <tr>
                        <td>Type:</td>
                        <td>{{ item.type }}</td>
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
<template>
    <div class="card">
        <div class="card-header">
            <i class="fas fa-download float-right"></i>
            <h5>Title</h5>
        </div>
        <div class="card-body">
            <table class="table table-borderless table-sm">
                <tbody>
                    <tr>
                        <td>PID:</td>
                        <td>1234</td>
                    </tr>
                    <tr>
                        <td>Identifier:</td>
                        <td>1234</td>
                    </tr>
                    <tr>
                        <td>Date:</td>
                        <td>14.10.2019</td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>
</template>

<script>
    export default {
        name: "SearchResult"
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
<template>
    <div class="card">
        <div class="card-header">
            <router-link :to="{ name: 'search-detail', params: { id: item.id }}">
                <h5>{{ title }}</h5>
            </router-link>
        </div>
        <div class="card-body">
            <table class="table table-borderless table-sm">
                <tbody>
                    <template v-if="item.type === 'archive'">
                        <tr>
                            <td class="w-25">Archive ID:</td>
                            <td class="w-75">{{ item.id }}</td>
                        </tr>
                        <tr>
                            <td class="w-25">State:</td>
                            <td class="w-75">{{ item.detail.state }}</td>
                        </tr>
                        <tr>
                            <td class="w-25">Total file:</td>
                            <td class="w-75">{{ item.detail.file_count }}</td>
                        </tr>
                        <tr>
                            <td class="w-25">Imported date:</td>
                            <td class="w-75">{{ item.detail.created | formatDate }}</td>
                        </tr>
                        <tr>
                            <td class="w-25">Last modified date:</td>
                            <td class="w-75">{{ item.detail.modified | formatDate }}</td>
                        </tr>

                        <tr v-if="item.detail.meta" v-for="(value, name) in item.detail.meta">
                            <td class="w-25">{{ name }}:</td>
                            <td class="w-75">
                                <div v-for="data in value">
                                    <span>{{ data }}</span>
                                    <br/>
                                </div>
                            </td>
                        </tr>
                    </template>

                    <template v-if="item.type === 'file'">
                        <tr>
                            <td class="w-25">File name:</td>
                            <td class="w-75">{{ title }}</td>
                        </tr>
                        <tr>
                            <td class="w-25">File type:</td>
                            <td class="w-75">{{ item.detail.type }}</td>
                        </tr>
                        <tr>
                            <td class="w-25">File size:</td>
                            <td class="w-75">{{ item.detail.size }} KB</td>
                        </tr>
                        <tr>
                            <td class="w-25">Path to file:</td>
                            <td class="w-75">{{ item.name }}</td>
                        </tr>
                        <tr>
                            <td class="w-25">Imported date:</td>
                            <td class="w-75">{{ item.detail.created | formatDate }}</td>
                        </tr>
                        <tr>
                            <td class="w-25">Last modified date:</td>
                            <td class="w-75">{{ item.detail.modified | formatDate }}</td>
                        </tr>
                    </template>
                </tbody>
            </table>
        </div>
    </div>
</template>

<script>
    import moment from 'moment';

    export default {
        props: {
            item: Object
        },
        filters: {
            formatDate(value) {
                if (value) {
                    return moment(String(value)).format('DD/MM/YYYY HH:mm');
                }
            }
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
                return `Archive ID: ${this.item.detail.meta['dc:identifier'][0]}`;
            }
        }
    }
</script>

<style lang="scss" scoped>
    .card .card-header {
        h5 {
            color: $primary;
        }
    }
</style>
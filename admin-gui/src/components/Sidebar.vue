<template>
    <!-- Sidebar -->
    <ul class="navbar-nav bg-gradient-primary sidebar sidebar-dark accordion" id="accordionSidebar">

        <!-- Sidebar - Brand -->
        <router-link to="/" class="sidebar-brand d-flex align-items-center justify-content-center" exact>
            <div class="sidebar-brand-icon rotate-n-15">
                <i class="fas fa-cogs"/>
            </div>
            <div class="sidebar-brand-text mx-3">OLA-HD</div>
        </router-link>

        <!-- Divider -->
        <hr class="sidebar-divider my-0">

        <!-- Nav Item - Dashboard -->
        <router-link to="/" tag="li" class="nav-item" active-class="active" exact>
            <a class="nav-link">
                <i class="fas fa-fw fa-tachometer-alt"/>
                <span>Dashboard</span>
            </a>
        </router-link>

        <!-- Divider -->
        <hr class="sidebar-divider">

        <!-- Heading -->
        <div class="sidebar-heading">
            Tools
        </div>

        <!-- Nav Item - Pages Collapse Menu -->
        <router-link to="/import" tag="li" class="nav-item" active-class="active">
            <a class="nav-link">
                <i class="fas fa-fw fa-upload"/>
                <span>Import</span>
            </a>
        </router-link>

        <!-- Divider -->
        <hr class="sidebar-divider d-none d-md-block">

        <!-- Sidebar Toggler (Sidebar) -->
        <div class="text-center d-none d-md-inline">
            <button class="rounded-circle border-0" id="sidebarToggle"></button>
        </div>

    </ul>
    <!-- End of Sidebar -->
</template>

<script>
    import $ from 'jquery';

    export default {
        mounted() {
            // Toggle the side navigation
            $("#sidebarToggle, #sidebarToggleTop").on('click', function(e) {
                $("body").toggleClass("sidebar-toggled");
                $(".sidebar").toggleClass("toggled");
                if ($(".sidebar").hasClass("toggled")) {
                    $('.sidebar .collapse').collapse('hide');
                }
            });
        }
    }
</script>

<style scoped>

</style>
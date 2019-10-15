module.exports = {
    lintOnSave: false,
    css: {
        loaderOptions: {
            sass: {
                data: `
                @import "@/assets/scss/_variable";
                @import "~bootstrap/scss/bootstrap";
                `
            }
        }
    }
};

module.exports = {
    lintOnSave: false,
    css: {
        loaderOptions: {
            sass: {
                data: `
                @import "~bootstrap/scss/bootstrap";
                @import "@/assets/scss/_variable";
                `
            }
        }
    }
};

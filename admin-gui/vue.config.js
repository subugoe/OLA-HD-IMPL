module.exports = {
  lintOnSave: false,
  css: {
    loaderOptions: {
      sass: {
        prependData: `
                @import "@/assets/scss/sb-admin-2";
                `
      }
    }
  }
};

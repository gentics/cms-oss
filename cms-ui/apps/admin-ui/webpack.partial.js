const TerserPlugin = require('terser-webpack-plugin');

const terserOptions = new TerserPlugin();
terserOptions.keep_classnames = true;
terserOptions.keep_fnames = true;

module.exports = {
  optimization: {
    minimize: false,
    minimizer: [ terserOptions ],
  },
};

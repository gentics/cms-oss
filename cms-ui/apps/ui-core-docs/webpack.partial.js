const webpack = require('webpack');

let latestBranch = true;
if (process.env.npm_config_docsVersion !== undefined) {
    latestBranch = false;
}

module.exports = {
    plugins: [
        new webpack.DefinePlugin({
            VERSION: JSON.stringify(require('../../libs/ui-core/package.json').version),
            LATESTBRANCH: latestBranch
        })
    ],
}

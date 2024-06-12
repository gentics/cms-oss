const path = require('path');
const webpack = require('webpack');
const { GitRevisionPlugin } = require('git-revision-webpack-plugin');

function getNextVersion() {
    const gitRevisionPlugin = new GitRevisionPlugin();
    const commitHash = gitRevisionPlugin.commithash();
    const dateString = new Date().toISOString().slice(0, 10);
    return `${dateString}+${commitHash}`;
}

module.exports = (config, options, targetOptions) => {
    if (targetOptions.target !== 'test') {
        config.plugins.push(new webpack.DefinePlugin({
            'GCMS_VERSION': `'${getNextVersion()}'`
        }));
    }

    config.module.rules.push({
        test: /\.yml/,
        loader: 'yaml-loader',
    });

    return config;
}

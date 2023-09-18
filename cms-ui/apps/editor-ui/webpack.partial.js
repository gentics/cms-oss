const path = require('path');
const webpack = require('webpack');
const GitRevisionPlugin = require('git-revision-webpack-plugin');

function getNextVersion() {
    const gitRevisionPlugin = new GitRevisionPlugin();
    commitHash = gitRevisionPlugin.commithash();
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

    // `precompile-scss` files, are for inline use, as these are styles which are
    // getting injected into the aloha iframe when editing a page.
    config.module.rules.push({
        test: /\.precompile-scss$/,
        use: [
            {
                loader: 'css-loader',
                options: {
                    sourceMap: false,
                },
            },
            {
                loader: 'sass-loader',
                options: {
                    sourceMap: false,
                    sassOptions: {
                        includePaths: [
                            path.normalize(path.resolve(__dirname, './src/styles')),
                            path.normalize(path.resolve(__dirname, '../../libs')),
                            path.normalize(path.resolve(__dirname, '../../node_modules')),
                        ],
                    }
                }
            },
        ]
    });

    return config;
}

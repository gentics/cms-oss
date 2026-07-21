/* eslint-disable @typescript-eslint/restrict-template-expressions */
/* eslint-disable @typescript-eslint/unbound-method */
/* eslint-disable import/no-nodejs-modules */
const { resolve } = require('path');
const { TsconfigPathsPlugin } = require('tsconfig-paths-webpack-plugin');

const CYPRESS_TYPE_COMPONENT = 'component';

/**
 * Mapping for each package to have it's own dedicated port.
 * This is used for component-tests, so that they can run in paralell,
 * as cypress always attempts to start the tests on 8081, which is in most
 * cases already used, or at the very least, once from in the first test.
 * All subsequent tests would result in an error where the port is already
 * in use, and removes the usefullness of paralellism.
 */
const PORT_MAPPING = {
    // Applications
    'admin-ui': 8381,
    'editor-ui': 8382,
    'ct-link-checker': 8383,
    // Libraries
    'cms-components': 8481,
    'form-grid': 8482,
    'image-editor': 8483,
    'ui-core': 8484,
};

function createReporterOptions(cypressType, type, name, isCI) {
    isCI = typeof isCI === 'boolean' ? isCI : false;

    if (!isCI) {
        return {
            reporter: 'min',
        }
    }

    return {
        // Screenshots are irrelevant on CI
        screenshotOnRunFailure: false,
        // Setup reporters
        reporter: resolve(__dirname, 'node_modules/cypress-multi-reporters'),
        reporterOptions: {
            reporterEnabled: 'min, mocha-junit-reporter',
            mochaJunitReporterReporterOptions: {
                mochaFile: resolve(__dirname, `.reports/${type}/${name}/CYPRESS-${cypressType}-report.xml`),
                testCaseSwitchClassnameAndName: true,
                jenkinsMode: true,
                rootSuiteTitle: name,
                testsuitesTitle: `UI Cypress ${cypressType} Tests: ${name}`,
                jenkinsClassnamePrefix: `ui.${cypressType}.${name}`,
            },
        },
    };
}

module.exports = {
    createComponentTestConfiguration(type, name, isCI, mainConfig) {
        const config = createReporterOptions(CYPRESS_TYPE_COMPONENT, type, name, isCI);
        config.port = PORT_MAPPING[name] || 8580;

        const mergedConfig = {
            ...mainConfig,
            ...config,
            // Cypress 14+ defaults justInTimeCompile to true (webpack only), which can
            // intermittently run 0 tests in CI. Remove this line to opt back in.
            justInTimeCompile: false,
        };

        /*
         * We have to add the tsconfig-paths plugin here, otherwise our paths for
         * packages from this mono-repo won't resolve.
         * No idea why we have to to this manually, and why this isn't in NX on default.
         */
        mergedConfig.devServer ??= {};
        mergedConfig.devServer.webpackConfig ??= {};
        mergedConfig.devServer.webpackConfig.resolve ??= {};
        mergedConfig.devServer.webpackConfig.resolve.plugins ??= [];

        mergedConfig.devServer.webpackConfig.resolve.plugins.push(new TsconfigPathsPlugin({
            configFile: resolve(__dirname, 'tsconfig.base.json'),
        }));

        /*
         * Styles have to be defined in here manually, as they are stripped by NX.
         */
        mergedConfig.devServer.options ??= {};
        mergedConfig.devServer.options.projectConfig ??= {};
        mergedConfig.devServer.options.projectConfig.buildOptions ??= {};
        mergedConfig.devServer.options.projectConfig.buildOptions.stylePreprocessorOptions ??= {};
        mergedConfig.devServer.options.projectConfig.buildOptions.stylePreprocessorOptions.includePaths ??= [];

        // Don't override the styles in case they *are* provided
        if (!mergedConfig.devServer.options.projectConfig.buildOptions.styles) {
            mergedConfig.devServer.options.projectConfig.buildOptions.styles = ['apps/component-tests-harness/src/styles.scss'];
        }

        // Always setup basic includePaths
        mergedConfig.devServer.options.projectConfig.buildOptions.stylePreprocessorOptions.includePaths.push(
            resolve(__dirname, "libs",),
            resolve(__dirname, "node_modules"),
            resolve(__dirname, type, name, 'src/styles'),
        );

        return mergedConfig;
    },
};

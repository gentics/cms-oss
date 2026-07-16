/* eslint-disable @nx/enforce-module-boundaries */
/* eslint-disable import-x/no-nodejs-modules */
import { nxComponentTestingPreset } from '@nx/angular/plugins/component-testing';
import { defineConfig } from 'cypress';
import { resolve } from 'node:path';
import { TsconfigPathsPlugin } from 'tsconfig-paths-webpack-plugin';
import { createComponentReporterOptions } from '../../cypress.preset';

const mainConfig = {
    ...nxComponentTestingPreset(__filename),
    ...createComponentReporterOptions('libs', 'ui-core', false),
    // Cypress 14+ defaults justInTimeCompile to true (webpack only), which can
    // intermittently run 0 tests in CI. Remove this line to opt back in.
    justInTimeCompile: false,
};

mainConfig.devServer ??= {};
mainConfig.devServer.webpackConfig ??= {};
mainConfig.devServer.webpackConfig.resolve ??= {};
mainConfig.devServer.webpackConfig.resolve.plugins ??= [];

(mainConfig.devServer.webpackConfig.resolve.plugins as Array<any>).push(new TsconfigPathsPlugin({
    configFile: resolve(__dirname, '../../tsconfig.base.json'),
}));

export default defineConfig({
    component: {
        ...mainConfig,
    },
});

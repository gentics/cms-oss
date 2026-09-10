/// <reference types='vitest' />
import { ConfigEnv, defineConfig } from 'vite';
import { createProjectConfiguration, getPlugins } from '../../vitest.project';

export default defineConfig((env: ConfigEnv) => ({
    root: __dirname,
    ...createProjectConfiguration('libs', 'cms-rest-client', env.mode),
    plugins: getPlugins('library'),
}));

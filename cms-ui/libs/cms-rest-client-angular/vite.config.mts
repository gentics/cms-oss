/// <reference types='vitest' />
import { defineConfig } from 'vite';
import { createProjectConfiguration, getPlugins } from '../../vitest.project';

export default defineConfig(() => ({
    root: __dirname,
    ...createProjectConfiguration('libs', 'cms-rest-client-angular'),
    plugins: getPlugins('angular'),
}));

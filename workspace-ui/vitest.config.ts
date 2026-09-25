import path from 'node:path';

import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

export default defineConfig({
    plugins: [react()],

    resolve: {
        alias: {
            '@': path.resolve(import.meta.dirname, './src'),
        },
    },

    test: {
        globals: true,

        // Only Vitest tests belong under src.
        include: ['src/**/*.test.{ts,tsx}'],

        // React components need a browser-like environment.
        environment: 'jsdom',

        // Runs before every test file.
        setupFiles: ['./src/test/setup.ts'],

        // Let Vitest understand imported CSS.
        css: true,

        coverage: {
            provider: 'v8',

            reporter: ['text', 'html', 'lcov'],

            exclude: [
                'src/test/**',
                '**/*.d.ts',
                '**/main.tsx',
            ],
        },
    },
});

import path from 'node:path';

import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

// Component catalogue (`npm run catalogue`): a dev server of its own, separate from the app.
// `npm run dev` and `npm run build` use `vite.config.ts` and do not include it.
export default defineConfig({
    root: path.resolve(import.meta.dirname, './src/catalogue'),

    // Own dependency cache, so the catalogue and the app dev server can run at the same time.
    cacheDir: path.resolve(import.meta.dirname, './node_modules/.vite-catalogue'),

    plugins: [
        react(),

        // Same as vite.config.ts, for the component layer in src/components/ui.
        tailwindcss(),
    ],

    resolve: {
        alias: {
            '@': path.resolve(import.meta.dirname, './src'),
        },
    },

    // Next to the app's 5173. Fails instead of moving to another port, so the URL stays fixed.
    server: {
        port: 5174,
        strictPort: true,
    },
});

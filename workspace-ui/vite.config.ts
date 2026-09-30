import path from 'node:path';

import react from '@vitejs/plugin-react';
import { defineConfig, loadEnv } from 'vite';

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
    // Only GENAIX_* and CMS_* variables, read here on the dev server. They have no VITE_ prefix, so
    // they never reach the browser bundle. The GenAIx defaults are the local GenAIx mock
    // (`npm run mock:genaix`) and its fixture credentials; see `.env.example`.
    const env = loadEnv(mode, import.meta.dirname, ['GENAIX_', 'CMS_']);
    const genaixApiUrl = env.GENAIX_API_URL || 'http://localhost:8080/api/v1';
    const genaixApiToken = env.GENAIX_API_TOKEN || 'sk_gnx_mock';
    const genaixSubject = env.GENAIX_SUBJECT || 'sub_workspace_dev';
    const cmsProxyTarget = env.CMS_PROXY_TARGET;

    return {
        plugins: [react()],

        // Same `@/` alias as `vitest.config.ts` and the `paths` in `tsconfig.app.json`.
        resolve: {
            alias: {
                '@': path.resolve(import.meta.dirname, './src'),
            },
        },

        server: {
            proxy: {
                // Stands in for the CMS proxy (09-integration-guide.md, section 3): forward all
                // of /genaix/api/v1, drop the browser's credentials and identity, and set the
                // installation token and X-GCMS-Subject server-side.
                '/genaix/api/v1/': {
                    target: genaixApiUrl,
                    changeOrigin: true,
                    rewrite: (path) => path.replace(/^\/genaix\/api\/v1/, ''),
                    configure: (proxy) => {
                        proxy.on('proxyReq', (proxyReq) => {
                            proxyReq.removeHeader('Cookie');
                            proxyReq.setHeader('Authorization', `Bearer ${genaixApiToken}`);
                            proxyReq.setHeader('X-GCMS-Subject', genaixSubject);
                        });
                    },
                },

                // In production the UI is served from the CMS host, so /rest is same-origin. In
                // development the dev server forwards it to a CMS and keeps the CMS cookies on localhost.
                ...(cmsProxyTarget
                    ? {
                        '/rest': {
                            target: cmsProxyTarget,
                            changeOrigin: true,
                            cookieDomainRewrite: '',
                        },
                    }
                    : {}),
            },
        },
    };
});

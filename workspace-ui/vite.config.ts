import path from 'node:path';

import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import { defineConfig, loadEnv, type Plugin } from 'vite';

/**
 * Development only, while no CMS is configured (`CMS_PROXY_TARGET` empty): the dev server answers
 * `POST /rest/admin/token` itself with a fixed test token, standing in for the CMS, so a session
 * can be started against the GenAIx mock, which accepts any token (except the prefixes `invalid_`,
 * `mismatch_` and `unreachable_`, which test its error cases). Every other `/rest` call still has
 * no CMS behind it.
 */
function devCmsTokenStub(): Plugin {
    return {
        name: 'dev-cms-token-stub',
        apply: 'serve',
        configureServer(server) {
            server.middlewares.use('/rest/admin/token', (req, res, next) => {
                // Mounted on the path, so `req.url` is what follows it: only the exact path is answered.
                if (req.method !== 'POST' || (req.url ?? '/').split('?')[0] !== '/') {
                    next();

                    return;
                }

                let body = '';

                req.on('data', (chunk: Buffer) => {
                    body += chunk.toString();
                });
                req.on('end', () => {
                    let requested: { name?: unknown; expires?: unknown } = {};

                    try {
                        requested = JSON.parse(body) as typeof requested;
                    } catch {
                        // A body that is not JSON gets the defaults.
                    }

                    const now = Math.floor(Date.now() / 1000);

                    res.setHeader('Content-Type', 'application/json');
                    res.end(JSON.stringify({
                        token: 'devtok_workspace',
                        id: 0,
                        userId: 0,
                        name: typeof requested.name === 'string' ? requested.name : 'genaix-workspace-dev',
                        cdate: now,
                        expires: typeof requested.expires === 'number' ? requested.expires : 0,
                        lastUsed: 0,
                        valid: true,
                    }));
                });
            });
        },
    };
}

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
    // Only GENAIX_* and CMS_* variables, read here on the dev server. They have no VITE_ prefix, so
    // they never reach the browser bundle. The GenAIx defaults are the local GenAIx mock
    // (`npm run mock:genaix`) and its fixture credentials; see `.env.example`.
    const env = loadEnv(mode, import.meta.dirname, ['GENAIX_', 'CMS_']);
    const genaixApiUrl = env.GENAIX_API_URL || 'http://localhost:8123/api/v1';
    const genaixApiToken = env.GENAIX_API_TOKEN || 'sk_gnx_mock';
    const genaixSubject = env.GENAIX_SUBJECT || 'sub_workspace_dev';
    const cmsProxyTarget = env.CMS_PROXY_TARGET;

    return {
        // Relative asset URLs in the build, so the CMS can serve `dist/` under any path (for example
        // `/tools/workspace/`), like its own UIs. The dev server keeps `/`.
        base: './',
        plugins: [
            react(),

            // Tailwind, for the component layer in src/components/ui only (see ui.css).
            tailwindcss(),

            // Without a CMS, the CMS API token of a new session comes from the dev server.
            ...(cmsProxyTarget ? [] : [devCmsTokenStub()]),
        ],

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

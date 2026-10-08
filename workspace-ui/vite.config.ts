import path from 'node:path';

import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import { defineConfig, loadEnv, type Plugin } from 'vite';

/**
 * Development only, while no CMS is configured (`CMS_PROXY_TARGET` empty): the dev server stands in
 * for the CMS. It answers `GET /rest/user/me` with a fixed dev user, so the app is logged in without
 * a login, and `POST /rest/auth/logout` with an empty success. It answers `POST /rest/admin/token`
 * with a fixed test token, so a session can be started against the GenAIx mock, which accepts any
 * token (except the prefixes `invalid_`, `mismatch_` and `unreachable_`, which test its error cases).
 * Every other `/rest` call still has no CMS behind it.
 */
function devCmsStub(): Plugin {
    return {
        name: 'dev-cms-stub',
        apply: 'serve',
        configureServer(server) {
            // Mounted on the path, so `req.url` is what follows it: only the exact path is answered.
            const answer = (path: string, method: string, body: unknown) => {
                server.middlewares.use(path, (req, res, next) => {
                    if (req.method !== method || (req.url ?? '/').split('?')[0] !== '/') {
                        next();

                        return;
                    }

                    res.setHeader('Content-Type', 'application/json');
                    res.end(JSON.stringify(body));
                });
            };

            answer('/rest/user/me', 'GET', { user: { id: 0, login: 'dev', firstName: 'Dev', lastName: 'User' } });
            answer('/rest/auth/logout', 'POST', { responseInfo: { responseCode: 'OK' } });

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

            // Without a CMS, the dev server answers the logged-in user and the CMS API token of a new session.
            ...(cmsProxyTarget ? [] : [devCmsStub()]),
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
                // of /rest/proxy/genaix, drop the browser's credentials and identity, and set the
                // installation token and X-GCMS-Subject server-side. Listed before /rest, so it
                // wins over the CMS forward below.
                '/rest/proxy/genaix/': {
                    target: genaixApiUrl,
                    changeOrigin: true,
                    rewrite: (path) => path.replace(/^\/rest\/proxy\/genaix/, ''),
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

import react from '@vitejs/plugin-react';
import { defineConfig, loadEnv } from 'vite';

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
    // Only GENAIX_* variables, read here on the dev server. They have no VITE_ prefix, so
    // they never reach the browser bundle. The defaults are the local GenAIx mock
    // (`npm run mock:genaix`) and its fixture credentials; see `.env.example`.
    const env = loadEnv(mode, import.meta.dirname, 'GENAIX_');
    const genaixApiUrl = env.GENAIX_API_URL || 'http://localhost:8080/api/v1';
    const genaixApiToken = env.GENAIX_API_TOKEN || 'sk_gnx_mock';
    const genaixSubject = env.GENAIX_SUBJECT || 'sub_workspace_dev';

    return {
        plugins: [react()],

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
            },
        },
    };
});

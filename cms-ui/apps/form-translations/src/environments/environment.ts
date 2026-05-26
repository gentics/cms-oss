/**
 * Development configuration. Run against a real CMS via `npm run start:proxy`
 * (proxy.conf.json points at the dev CMS). The mock-mode that previously
 * lived here was removed in f-gpu-2441 — see refactor note in the PR.
 */
export const environment = {
    production: false,
    toolKey: 'form-translations',
};

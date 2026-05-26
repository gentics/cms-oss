/**
 * Development configuration.
 *
 * `useDevMock: true` activates an HTTP interceptor that stubs the
 * `/rest/form/translations*` and `/rest/form/types*` endpoints so the tool
 * can be used before the backend lands them. Set to `false` once the real
 * endpoints are available, or run `start:proxy` against a CMS that already
 * implements them.
 */
export const environment = {
    production: false,
    toolKey: 'form-translations',
    useDevMock: true,
    devMockLatencyMs: 200,
};

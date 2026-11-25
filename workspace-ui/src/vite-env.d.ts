/// <reference types="vite/client" />

interface ImportMetaEnv {
    /** Same-origin path of the GenAIx proxy; defaults to `/rest/proxy/genaix` (`.env.example`). */
    readonly VITE_GENAIX_API_BASE?: string;
    /** CMS group id that "Request construct" creates its task for (`.env.example`); without it, nothing is requested. */
    readonly VITE_CONSTRUCT_REQUEST_GROUP_ID?: string;
}

interface ImportMeta {
    readonly env: ImportMetaEnv;
}

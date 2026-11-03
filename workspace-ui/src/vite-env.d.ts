/// <reference types="vite/client" />

interface ImportMetaEnv {
    /** Same-origin path of the GenAIx proxy; defaults to `/rest/proxy/genaix` (`.env.example`). */
    readonly VITE_GENAIX_API_BASE?: string;
}

interface ImportMeta {
    readonly env: ImportMetaEnv;
}

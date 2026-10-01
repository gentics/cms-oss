/// <reference types="vite/client" />

interface ImportMetaEnv {
    /** Same-origin path of the GenAIx proxy; defaults to `/genaix/api/v1` (`.env.example`). */
    readonly VITE_GENAIX_API_BASE?: string;
}

interface ImportMeta {
    readonly env: ImportMetaEnv;
}

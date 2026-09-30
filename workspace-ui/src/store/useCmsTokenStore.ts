import { create } from 'zustand';

import type { CmsTokenInfo } from '@/services/cmsApiService/cmsApiService';

/** The CMS API token of this page session (`POST /rest/admin/token`), needed by later API calls. */
export type CmsToken = Pick<CmsTokenInfo, 'token' | 'id' | 'name' | 'expires'>;

interface CmsTokenState {
    cmsToken: CmsToken | null;
    setCmsToken: (cmsToken: CmsToken) => void;
    clearCmsToken: () => void;
}

// Memory only, on purpose: no `persist` middleware (never written to storage or cookies; the CMS
// keeps its own HttpOnly cookie) and no `devtools` middleware (never exposed outside the page).
export const useCmsTokenStore = create<CmsTokenState>((set) => ({
    cmsToken: null,

    setCmsToken: (cmsToken) => set({ cmsToken }),

    clearCmsToken: () => set({ cmsToken: null }),
}));

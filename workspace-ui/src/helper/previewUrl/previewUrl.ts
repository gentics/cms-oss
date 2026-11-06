// The query parameters of a read-only CMS preview, each with the value it must have.
const READ_ONLY_PARAMS = { mode: 'view', real: 'view', tagmarkers: 'true' } as const;

// `url` (`preview_url` of `preview.updated`) as a read-only preview: `mode=view` as in the contract
// example, and `real=view`, which is what the CMS's `/alohapage` reads (`AlohaPageServlet`; without
// it, it renders for editing and locks the page). `tagmarkers=true` renders it like for editing, but
// without Aloha Editor and without locking the page, with the page's tags marked (`data-gcn-*`), which
// the part selection needs (`usePreviewElements`); a CMS that does not know it ignores it, and the page
// has no markers then.
// A parameter that is missing or has another value is set; everything else stays. A URL that already
// has all of them is returned unchanged.
// On the dev server (`npm run dev`, `npm run dev:debug`; `DEV`, but not Vitest's mode `test`) the URL is
// loaded from the dev server's own origin, which forwards
// `/alohapage` to the CMS (`vite.config.ts`): the CMS authenticates it only by its session cookie
// (`SessionToken`), which the /rest login left on the dev server's host, so `preview_url` pointing at
// the CMS host would answer "Invalid session ID".
export function readOnlyPreviewUrl(url: string): string {
    const parsed = new URL(url, window.location.href);
    const entries = Object.entries(READ_ONLY_PARAMS);
    const viaDevServer = import.meta.env.DEV && import.meta.env.MODE !== 'test' && parsed.origin !== window.location.origin;

    if (!viaDevServer && entries.every(([key, value]) => parsed.searchParams.get(key) === value)) {
        return url;
    }

    entries.forEach(([key, value]) => parsed.searchParams.set(key, value));

    return viaDevServer
        ? new URL(`${parsed.pathname}${parsed.search}${parsed.hash}`, window.location.origin).toString()
        : parsed.toString();
}

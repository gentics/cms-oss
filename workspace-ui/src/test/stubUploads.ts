import { vi } from 'vitest';

/** An upload a test made through the stubbed `XMLHttpRequest`. */
export interface StubbedUpload {
    method: string;
    url: string;
    headers: Record<string, string>;
    body: FormData;
}

/** The answer to one upload; without a `status` the request fails like a network error. */
export interface UploadReply {
    status?: number;
    body?: unknown;
    headers?: Record<string, string>;
    /** Upload progress reported before the answer, as shares 0 to 1. Default: `[1]`. */
    progress?: number[];
    /** Holds the answer, after the progress, until it settles: to see an upload while it runs. */
    hold?: Promise<void>;
}

/**
 * Stubs `XMLHttpRequest`, which `uploadSessionFile` sends files with (it reports upload progress,
 * `fetch` does not). Each upload takes the next of `replies`; the uploads made are returned, in
 * order, and each is also written to `log` as "METHOD url", so a test can see it among the requests
 * it makes through `fetch`. Undo with `vi.unstubAllGlobals()`.
 */
export function stubUploads(replies: UploadReply[], log?: string[]): StubbedUpload[] {
    const uploads: StubbedUpload[] = [];

    class XMLHttpRequestStub {
        upload: { onprogress: ((event: { lengthComputable: boolean; loaded: number; total: number }) => void) | null } = { onprogress: null };
        onload: (() => void) | null = null;
        onerror: (() => void) | null = null;
        onabort: (() => void) | null = null;
        status = 0;
        responseText = '';
        private method = '';
        private url = '';
        private requestHeaders: Record<string, string> = {};
        private responseHeaders: Record<string, string> = {};

        open(method: string, url: string) {
            this.method = method;
            this.url = url;
        }

        setRequestHeader(name: string, value: string) {
            this.requestHeaders[name] = value;
        }

        getAllResponseHeaders() {
            return Object.entries(this.responseHeaders).map(([name, value]) => `${name}: ${value}`).join('\r\n');
        }

        send(body: FormData) {
            const reply = replies.shift() ?? {};

            uploads.push({ method: this.method, url: this.url, headers: this.requestHeaders, body });
            log?.push(`${this.method} ${this.url}`);

            // Answered after `send` returns, as a real request is.
            setTimeout(async () => {
                if (reply.status === undefined) {
                    this.onerror?.();

                    return;
                }

                for (const share of reply.progress ?? [1]) {
                    this.upload.onprogress?.({ lengthComputable: true, loaded: share * 100, total: 100 });
                }

                await reply.hold;
                this.status = reply.status;
                this.responseText = reply.body === undefined ? '' : JSON.stringify(reply.body);
                this.responseHeaders = reply.headers ?? { 'Content-Type': 'application/json' };
                this.onload?.();
            });
        }
    }

    vi.stubGlobal('XMLHttpRequest', XMLHttpRequestStub);

    return uploads;
}

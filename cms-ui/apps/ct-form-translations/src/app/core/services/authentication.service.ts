import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

const SID_STORAGE_KEY = 'GCMSUI_sid';

/**
 * Reads the Gentics CMS Session-ID from the URL or localStorage.
 *
 * Mirrors the `GcmsAuthenticationService` implementation in `ct-link-checker`.
 * The reviewer asked to consolidate this into `@gentics/cms-components`
 * (along with the tool-api service) — tracked as a follow-up.
 */
@Injectable({ providedIn: 'root' })
export class AuthenticationService {

    private initialized = false;
    private currentSid: string | null = null;
    private readonly sid$ = new BehaviorSubject<string | null>(null);

    /** Must be called once during app bootstrap. */
    init(): void {
        if (this.initialized) {
            throw new Error('AuthenticationService.init() must be called only once.');
        }
        this.updateSid();
        this.initialized = true;
    }

    get sid(): string | null {
        return this.currentSid;
    }

    getSid(): Observable<string | null> {
        return this.sid$.asObservable();
    }

    /** Re-reads the SID from URL / storage. */
    updateSid(): void {
        this.currentSid = this.extractSidFromLocationOrStorage();
        this.sid$.next(this.currentSid);
    }

    /**
     * Priority is URL parameter first (the CMS shell hands the SID to the
     * embedded tool that way), then `localStorage['GCMSUI_sid']` as fallback
     * for refresh and standalone use.
     */
    private extractSidFromLocationOrStorage(): string | null {
        const fromUrl = (window.location.href.match(/[?&]sid=(\d+)/i) ?? [])[1];
        if (fromUrl) {
            return fromUrl;
        }

        let stored: string | null = null;
        try {
            stored = localStorage.getItem(SID_STORAGE_KEY);
        } catch {
            return null;
        }
        if (!stored) {
            return null;
        }
        /* Some places persist the SID JSON-stringified — strip surrounding quotes. */
        if (stored.startsWith('"') || stored.startsWith('\'')) {
            stored = stored.slice(1, -1);
        }
        return stored;
    }
}

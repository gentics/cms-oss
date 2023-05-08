import { Injectable } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { BehaviorSubject, Observable } from 'rxjs';

/**
 * Provides access to the GCMS session ID.
 * In the main app service this service's init() method must be called.
 */
@Injectable()
export class GcmsAuthenticationService {

    private initialized = false;
    private _sid: string;
    private sid$ = new BehaviorSubject<string>(null);

    constructor(private router: Router) {
    }

    /** Initialization method, which needs to be called once before accessing the SID or userId for the first time. */
    init(): void {
        if (this.initialized) {
            throw new Error('The GcmsAuthenticationService.init() method must be called only once.');
        }

        this.router.events.subscribe((val) => {
            this.updateSid();
        });

        this.updateSid();
        this.initialized = true;
    }

    updateSid(): void {
        this._sid = this.extractSidFromLocationOrStorage();
        this.sid$.next(this._sid);
    }

    /** Returns the current GCMS SID. */
    get sid(): string {
        return this._sid;
    }

    public getSid(): Observable<string> {
        return this.sid$.asObservable();
    }

    /** Extracts the GCMS session ID from window.location. */
    private extractSidFromLocationOrStorage(): string {
        const location = window.location.href;
        const match = location && location.match(/[?&]sid=(\d+)/i);
        let sid = match ? match[1] : '';

        if (sid) {
            return sid;
        }

        // Attempt to load the SID from the local-storage
        sid = localStorage.getItem('GCMSUI_sid');
        if (sid) {
            if (sid.startsWith('\'') || sid.startsWith('"')) {
                sid = sid.slice(1, -1);
            }
            return sid;
        }

        return '';
    }

}

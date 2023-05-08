import { Inject, Injectable, InjectionToken, Optional } from '@angular/core';
import { isEqual } from 'lodash';
import { Observable, Subscriber } from 'rxjs';
import { distinctUntilChanged } from 'rxjs/operators';

export const PAGE_VISIBILITY_API = new InjectionToken<PageVisibilityAPI>('PageVisibilityAPI');

export interface PageVisibilityAPI {
    readonly hidden?: boolean;
    readonly webkitHidden?: boolean;
    readonly mozHidden?: boolean;
    readonly msHidden?: boolean;
    readonly oHidden?: boolean;
    addEventListener(eventName: string, handler: () => any): void;
    removeEventListener(eventName: string, handler: () => any): void;
}


/**
 * A service that provides the Page Visibility API
 * in a cross-browser, testable, observable abstraction.
 * If the platform does not provide the Page Visibility API,
 * the Service will pretend the page is always visible.
 */
@Injectable()
export class PageVisibility {

    /**
     * Returns an observable that emits the current page visibility on changes.
     */
    readonly visible$: Observable<boolean>;

    /**
     * Returns the current page visibility state.
     */
    get visible(): boolean {
        return this.getPageVisibility();
    }

    private getPageVisibility: () => boolean;

    constructor(@Optional() @Inject(PAGE_VISIBILITY_API) api?: PageVisibilityAPI) {

        api = api || window.document;
        const { propName, eventName } = getVendorPrefixedNames(api);

        if (propName) {
            this.getPageVisibility = () => api[propName] != true;
        } else {
            this.getPageVisibility = () => true;
        }

        this.visible$ = new Observable<boolean>((subscriber: Subscriber<boolean>) => {
            subscriber.next(this.getPageVisibility());
            if (eventName) {
                const listener = () => {
                    subscriber.next(this.getPageVisibility());
                };

                api.addEventListener(eventName, listener);
                return () => api.removeEventListener(eventName, listener);
            }
        }).pipe(distinctUntilChanged(isEqual));
    }
}

function getVendorPrefixedNames(api: PageVisibilityAPI): { propName: keyof PageVisibilityAPI, eventName: string } {
    if (typeof api === 'object') {
        if (typeof api.hidden !== 'undefined') {
            return { propName: 'hidden', eventName: 'visibilitychange' };
        }
        for (let prefix of ['webkit', 'moz', 'ms', 'o']) {
            if (typeof (<any> api)[prefix + 'Hidden'] !== 'undefined') {
                return {
                    propName: prefix + 'Hidden' as keyof PageVisibilityAPI,
                    eventName: prefix + 'visibilitychange',
                };
            }
        }
    }
    return {
        propName: '' as keyof PageVisibilityAPI,
        eventName: '',
    };
}

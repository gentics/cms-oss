import * as punycodeModule from 'punycode';
import { Injectable, NgZone, OnDestroy } from '@angular/core';
import { ModalService } from '@gentics/ui-core';
import { Hotkey, HotkeysService } from 'angular2-hotkeys';
import * as domSerialize from 'dom-serialize';
import { Observable, Subscription, of, throwError, zip } from 'rxjs';
import { timeout, take, map, catchError } from 'rxjs/operators';
import * as serialize from 'serialize-to-js';
import * as StackTrace from 'stacktrace-js';
import { ApiBase } from '../../core/providers/api';
import { Api } from '../../core/providers/api/api.service';
import { ErrorHandler as CMSErrorHandler } from '../../core/providers/error-handler/error-handler.service';
import { I18nService } from '../../core/providers/i18n/i18n.service';
import { ApplicationStateService } from '../../state';
import { DebugTool } from '../components/debug-tool/debug-tool.component';
import { TraceErrorHandler } from './trace-error-handler';

declare const window: Window & typeof globalThis & {
    gcmsui_debugTool(): void;
};

/**
 * dom-serialize imports punycode using require, but it expects version 1.x and it can
 * apparently not handle the default export of punycode 2.x properly, so we patch the
 * punycode module temporarily.
 *
 * Currently the Editor UI uses punycode 1.x, because 1.x does not ship with an ES5 build.
 * But we include the workaround here nevertheless to avoid issues when upgrading later.
 */
const patchablePunycode: {
    ucs2: punycodeModule.ucs2;
    default: {
        ucs2: punycodeModule.ucs2;
    }
} = punycodeModule as any;

/**
 * This service is responsible for listening to the Debug Tool hotkey, which is CTRL+ALT+G then CTRL+ALT+D.
 * This keystroke is after [G]entics [D]ebug. The exact way to enter the hotkey sequence is:
 * press and keep holding down CTRL+ALT, then press first [G], and then after releasing it, press [D].
 *
 * The Debug Tool is intended to generate as many useful details on the actual state as possible.
 * Another goal of Debug Tool is to make it possible clear every locally stored data
 * (eg.: Local Storage, Cookies, etc) and force a reload without the browser cache.
 *
 * The generated report file is a plain JSON file contains serialized and captured local data, and also,
 * if possible, some API Endpoint call results.
 *
 * The Debug Tool is exposed as a global function, so calling it from Developer Console is possible with:
 * gcmsui_debugTools()
 *
 * If the hotkey needs to be used in other classes (such as iframes to let users to open Debug Tool from there),
 * the DebugToolService.hotkey static property can be used.
 */
@Injectable()
export class DebugToolService implements OnDestroy {

    // An end point request can wait until this time, then timeouts
    private static apiRequestTimeout = 20000;

    // Store hotkey as static property to be accessible by other classes
    static hotkey = ['ctrl+alt+g ctrl+alt+d'];

    private debugDataSnapshot: any = {};
    private collectedErrors: any = [];
    private collectedCmsErrors: any = [];

    private subscriptions = new Subscription();

    /**
     * Only one Debug Tool modal can be opened at a time, so this object property will return
     * a boolean if Debug Tool modal is active or not.
     *
     * @returns
     */
    get isDebugToolActive(): boolean {
        return !!this.modalService.openModals.filter( modal => modal.instance.hasOwnProperty('debugToolService') ).length;
    }

    constructor(
        private hotkeysService: HotkeysService,
        private modalService: ModalService,
        private appState: ApplicationStateService,
        private cmsErrorHandler: CMSErrorHandler,
        private apiService: Api,
        private apiBase: ApiBase,
        private zone: NgZone,
        private i18n: I18nService,
    ) {
        this.initialize();
    }

    /**
     * Initializes the DebugToolService it should called only once, and from the constructor
     */
    initialize(): void {
        this.subscriptions.add(
            this.cmsErrorHandler.caughtErrors$
                .subscribe(errorList => {
                    this.collectedCmsErrors = errorList;
                }),
        );

        this.subscriptions.add(
            TraceErrorHandler.collectErrors$.subscribe(error => {
                this.collectedErrors.push(error);
            }),
        );

        window.gcmsui_debugTool = () => this.zone.run(() => {
            if (!this.isDebugToolActive) {
                this.runDebugTool();
            }
        });

        this.hotkeysService.add(new Hotkey(DebugToolService.hotkey, (event: KeyboardEvent): boolean => {
            if (event.preventDefault) {
                event.preventDefault();
            } else {
                event.returnValue = false;
            }

            if (!this.isDebugToolActive) {
                this.runDebugTool();
            }

            return false;
        }, ['INPUT', 'TEXTAREA', 'SELECT']));
    }

    ngOnDestroy(): void {
        this.subscriptions.unsubscribe();
    }

    /**
     * Prepare a current snapshot of state then open the DebugTool modal
     * to let user choose from generating a report or clear site data.
     */
    runDebugTool(): void {
        this.debugDataSnapshot = {
            app: 'editor-ui',
            location: this.debug_location,
            date: this.debug_currentDate,
            platform: this.debug_platform,
            connection: this.debug_connection,
            cookies: this.debug_cookies,
            localStorage: this.debug_localStorage,
            sessionStorage: this.debug_sessionStorage,
            appState: this.debug_appState,
            dom: this.debug_dom,
            scripts: this.debug_loadedScripts,
            styles: this.debug_loadedStyles,
        };

        this.modalService.fromComponent(DebugTool,
            {
                closeOnOverlayClick: false,
                closeOnEscape: false,
                onClose: () => {
                    this.debugDataSnapshot = {}; // Clear Debug Data Snapshot
                },
                width: '700px',
            },
            { debugToolService: this })
            .then(modal => modal.open())
            .then(result => {
                if ( typeof result === 'object') {
                    this.downloadReport(result);
                }
            });
    }

    /**
     * Query some useful API Endpoints to extend the report with more data
     * queries are running parallel, but with a global timeout. If error or timeout
     * occuring then null data will be returned, so this method is failsafe.
     *
     * @returns - Responses from API Endpoints
     */
    requestApiData(): Promise<any> {
        let apiData: any = {};

        const requestList: { [key: string]: Observable<any> } = {
            user: this.apiBase.get('user/me'),
            userData: this.apiService.userData.getAllKeys(),
            nodes: this.apiService.folders.getFolders(0),
            currentNode: this.apiService.folders.getFolders(this.appState.now.folder.activeNode),
            currentFolder: this.apiService.folders.getFolders(this.appState.now.folder.activeFolder),
            currentBreadcrumbs: this.apiService.folders.getBreadcrumbs(this.appState.now.folder.activeFolder),
            tools: this.apiBase.get('admin/tools'),
        };

        const requestListArr = Object.entries(requestList).map(([key, request]) => {
            return request.pipe(
                timeout(DebugToolService.apiRequestTimeout),
                take(1),
                map(result => ({ [key]: result })),
                catchError((error: any) => {
                    // Collect Request Time Errors
                    // throwError(new Error(error));

                    // Return empty response if error occured
                    return of({ [key]: null });
                }),
            );
        });

        return zip(...requestListArr).pipe(
            timeout(DebugToolService.apiRequestTimeout + 50),
            take(1),
            map(result => { Object.assign(apiData, ...result); return apiData; }),
            catchError((error: any) => {
                // Collect Request Time Errors
                // throwError(new Error(error));

                // Return empty response if error occured
                return of(null);
            }),
        ).toPromise();
    }

    /**
     * Prepares the report object for further processing
     *
     * @returns - Prepared report data
     */
    generateReport(): Promise<any> {
        let apiData: any;
        let apiDataPromise = this.requestApiData();
        let traceCmsPromises: Promise<any>[] = [];
        let tracedCmsErrors: any = [];

        // Build stack from CMS Errors
        this.collectedCmsErrors.forEach((error: any) => {
            traceCmsPromises.push(StackTrace.fromError(error, { offline: true }).then((trace) => {
                tracedCmsErrors.push({error, trace});
            }));
        });

        // Load API Data
        apiDataPromise.then( result => apiData = result );

        // Wait until all data becomes ready, then resolve
        return Promise.all([...traceCmsPromises, apiDataPromise]).then(() =>
            Promise.resolve({
                ...this.debugDataSnapshot,
                apiData: apiData,
                errors: serialize([...this.collectedErrors]),
                cmsErrors: serialize([...tracedCmsErrors]),
            }),
        );

    }

    /**
     * Clears local data from browser after user consent
     *
     * @returns
     */
    clearSiteData(): Promise<any> {
        return this.modalService.dialog({
            title: this.i18n.translate('modal.debug_tool_clear_modal_title'),
            body: this.i18n.translate('modal.debug_tool_clear_modal_body'),
            buttons: [
                { label: this.i18n.translate('common.cancel_button'), type: 'secondary', flat: true, returnValue: false },
                { label: this.i18n.translate('modal.debug_tool_clear_modal_clear_button'), type: 'alert', returnValue: true },
            ],
        })
            .then(modal => modal.open())
            .then(shouldContinue => {
                if (!shouldContinue) {
                    return Promise.reject(false);
                }
                // TODO: Implement full cache clear / cache busting

                // Clear Local Storage
                if (typeof localStorage.clear === 'function') {
                    localStorage.clear();
                }

                // Clear Session Storage
                if (typeof sessionStorage.clear === 'function') {
                    sessionStorage.clear();
                }

                // Clear All Cookies on all paths and domain variants which accessible
                this.clearAllCookies();

                // Reload Page without caches
                window.location.reload();

                return Promise.resolve('clear');
            });
    }

    clearAllCookies(): void {
        let cookies = document.cookie.split('; ');
        for (let c = 0; c < cookies.length; c++) {
            let d = window.location.hostname.split('.');
            while (d.length > 0) {
                let cookieBase = encodeURIComponent(cookies[c].split(';')[0].split('=')[0]) +
                '=; expires=Thu, 01-Jan-1970 00:00:01 GMT; domain=' +
                d.join('.') + ' ;path=';
                let p = location.pathname.split('/');
                document.cookie = cookieBase + '/';
                while (p.length > 0) {
                    document.cookie = cookieBase + p.join('/');
                    p.pop();
                }
                d.shift();
            }
        }
    }

    downloadReport(data: any): void {
        let fileName = 'GCMS_Debug_Report_' + this.debug_currentDate.replace(/[\.\:\-]+/g, '') + '.grd'; // GRD: Gentics Report Data
        let json = JSON.stringify(data);
        let blob = new Blob([json], { type: 'application/octet-stream' });

        if ((window.navigator as any).msSaveOrOpenBlob) {
            (window.navigator as any).msSaveOrOpenBlob(blob, fileName);
        } else {
            let url = (window.URL || (window as any).webkitURL).createObjectURL(blob);
            let downloadLink = document.createElement('a');
            downloadLink.download = fileName,
            downloadLink.href = url;
            let event = document.createEvent('MouseEvents');
            event.initMouseEvent('click', true, true, window, 1, 0, 0, 0, 0, false, false, false, false, 0, null);
            downloadLink.dispatchEvent(event);
        }
    }

    get debug_currentDate(): any {
        return new Date().toJSON();
    }

    get debug_cookies(): any {
        let cookies: any = {};
        if (document.cookie) {
            let split = document.cookie.split(';');
            for (let i = 0; i < split.length; i++) {
                let nameValue = split[i].split('=');
                nameValue[0] = nameValue[0].replace(/^ /, '');
                cookies[decodeURIComponent(nameValue[0])] = decodeURIComponent(nameValue[1]);
            }
        }
        return cookies;
    }

    get debug_localStorage(): any {
        if (typeof localStorage == 'object') {
            return localStorage;
        } else {
            return {};
        }
    }

    get debug_sessionStorage(): any {
        if (typeof sessionStorage == 'object') {
            return sessionStorage;
        } else {
            return {};
        }
    }

    get debug_appState(): any {
        return this.appState.now;
    }

    get debug_dom(): any {
        return this.runWithPatchedPunycode(() => domSerialize(window.document));
    }

    get debug_platform(): any {
        if (typeof window.navigator == 'object') {
            let navigator: any = window.navigator;
            let _navigator: any = {};
            for (let i in navigator) {
                _navigator[i] = navigator[i];
            }

            delete _navigator.plugins;
            delete _navigator.mimeTypes;
            return _navigator;
        } else {
            return {};
        }
    }

    get debug_connection(): any {
        let navigator: any = (window.navigator as any);
        if (navigator.connection && typeof navigator.connection == 'object') {
            return {
                type: navigator.connection.effectiveType,
                downlink: navigator.connection.downlink,
                rtt: navigator.connection.rtt,
            };
        } else {
            return {};
        }
    }

    get debug_location(): any {
        if (typeof window.location == 'object') {
            return window.location;
        } else {
            return {};
        }
    }

    get debug_loadedScripts(): any {
        let scripts: any = [];

        [].forEach.call(document.querySelectorAll('script[src]'), (element: any) => {
            scripts.push(element.getAttribute('src'));
        });

        return scripts;
    }

    get debug_loadedStyles(): any {
        let stylesheets: any = [];
        let styles: any = [];

        [].forEach.call(document.querySelectorAll('link[rel="stylesheet"][href]'), (element: any) => {
            stylesheets.push(element.getAttribute('href'));
        });

        [].forEach.call(document.querySelectorAll('style'), (element: any) => {
            styles.push(element.innerText);
        });

        return { stylesheets, styles };
    }

    private runWithPatchedPunycode<R>(action: () => R): R {
        const origValue = patchablePunycode.ucs2;
        const needsPatch = typeof origValue !== 'object';

        if (needsPatch) {
            patchablePunycode.ucs2 = patchablePunycode.default.ucs2;
        }
        const ret = action();
        if (needsPatch) {
            patchablePunycode.ucs2 = origValue;
        }

        return ret;
    }
}

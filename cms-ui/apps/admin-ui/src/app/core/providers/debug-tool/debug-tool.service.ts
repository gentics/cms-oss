import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { IndexByKey } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { ModalService } from '@gentics/ui-core';
import { Hotkey, HotkeysService } from 'angular2-hotkeys';
import domSerialize from 'dom-serialize';
import { Observable, of as observableOf, throwError, zip } from 'rxjs';
import { catchError, map, take, takeUntil, timeout } from 'rxjs/operators';
import serialize from 'serialize-to-js';
import * as StackTrace from 'stacktrace-js';
import { InitializableServiceBase } from '../../../shared/providers/initializable-service-base';
import { DebugToolModalComponent } from '../../components/debug-tool-modal/debug-tool-modal.component';
import { ErrorHandler as CMSErrorHandler } from '../error-handler';
import { I18nService } from '../i18n';
import { TraceErrorHandler } from '../trace-error-handler/trace-error-handler';

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
export class DebugToolService extends InitializableServiceBase {

    // An end point request can wait until this time, then timeouts
    private static apiRequestTimeout = 20000;

    // Store hotkey as static property to be accessible by other classes
    static hotkey = ['ctrl+alt+g'];

    private debugDataSnapshot: any = {};
    private collectedErrors: any = [];
    private collectedCmsErrors: any = [];

    /**
     * Only one Debug Tool modal can be opened at a time, so this object property will return
     * a boolean if Debug Tool modal is active or not.
     */
    get isDebugToolActive(): boolean {
        return !!this.modalService.openModals.filter( modal => modal.instance.hasOwnProperty('debugToolService') ).length;
    }

    constructor(
        private hotkeysService: HotkeysService,
        private modalService: ModalService,
        private appState: AppStateService,
        private cmsErrorHandler: CMSErrorHandler,
        private apiService: GcmsApi,
        private i18n: I18nService,
    ) {
        super();
    }

    /**
     * Initializes the DebugToolService.
     */
    protected onServiceInit(): void {
        this.cmsErrorHandler.caughtErrors$.pipe(
            takeUntil(this.stopper.stopper$),
        ).subscribe(errorList => {
            this.collectedCmsErrors = errorList;
        });

        TraceErrorHandler.collectErrors$.pipe(
            takeUntil(this.stopper.stopper$),
        ).subscribe(error => {
            this.collectedErrors.push(error);
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

    /**
     * Prepare a current snapshot of state then open the DebugTool modal
     * to let user choose from generating a report or clear site data.
     */
    runDebugTool(): void {
        this.debugDataSnapshot = {
            app: 'admin-ui',
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

        this.modalService.fromComponent(DebugToolModalComponent,
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
     * @returns Response from API Endpoints
     */
    requestApiData(): Promise<any> {
        const apiData: any = {};

        const requestList: IndexByKey<Observable<any>> = {
            user: this.apiService.auth.validate(this.appState.now.auth.sid),
            userData: this.apiService.userData.getAllKeys(),
            nodes: this.apiService.folders.getFolders(0),
            tools: this.apiService.admin.getAvailableEmbeddedTools(),
        };

        const requestListArr = [];
        for (const key of Object.keys(requestList)) {
            requestListArr.push(
                requestList[key].pipe(
                    timeout(DebugToolService.apiRequestTimeout),
                    take(1),
                    map((result: any) => ({ [key]: result }) ),
                    catchError((error: any) => {
                        // Collect Request Time Errors
                        throwError(new Error(error));

                        // Return empty response if error occurred
                        return observableOf({ [key]: null });
                    }),
                ),
            );
        }

        const requests$ = zip(...requestListArr);

        return requests$.pipe(
            timeout(DebugToolService.apiRequestTimeout + 50),
            take(1),
            map(result => {
                Object.assign(apiData, ...result);
                return apiData;
            }),
            catchError( (error: any) => {
                // Collect Request Time Errors
                throwError(new Error(error));

                // Return empty response if error occured
                return observableOf(null);
            }),
        ).toPromise();
    }

    /**
     * Prepares the report object for further processing
     * @returns - Prepared report data
     */
    generateReport(): Promise<any> {
        let apiData: any;
        const apiDataPromise = this.requestApiData();
        const traceCmsPromises: Promise<any>[] = [];
        const tracedCmsErrors: any = [];

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
                apiData,
                errors: serialize([...this.collectedErrors]),
                cmsErrors: serialize([...tracedCmsErrors]),
            }),
        );

    }

    /**
     * Clears local data from browser after user consent
     */
    clearSiteData(): Promise<any> {
        return this.modalService.dialog({
            title: this.i18n.instant('modal.debug_tool_clear_modal_title'),
            body: this.i18n.instant('modal.debug_tool_clear_modal_body'),
            buttons: [
                { label: this.i18n.instant('common.cancel_button'), type: 'secondary', flat: true, returnValue: false },
                { label: this.i18n.instant('modal.debug_tool_clear_modal_clear_button'), type: 'alert', returnValue: true },
            ],
        })
            .then(modal => modal.open())
            .then(shouldContinue => {
                if (shouldContinue) {
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
                } else {
                    return Promise.reject(false);
                }
            });
    }

    clearAllCookies(): void {
        const cookies = document.cookie.split('; ');
        for (const cookie of cookies) {
            const d = window.location.hostname.split('.');
            while (d.length > 0) {
                const cookieBase = encodeURIComponent(cookie.split(';')[0].split('=')[0]) +
                '=; expires=Thu, 01-Jan-1970 00:00:01 GMT; domain=' +
                d.join('.') + ' ;path=';
                const p = location.pathname.split('/');
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
        const fileName = 'GCMSA_Debug_Report_' + this.debug_currentDate.replace(/[\.\:\-]+/g, '') + '.grd'; // GRD: Gentics Report Data
        const json = JSON.stringify(data);
        const blob = new Blob([json], { type: 'application/octet-stream' });

        if ((window.navigator as any).msSaveOrOpenBlob) {
            (window.navigator as any).msSaveOrOpenBlob(blob, fileName);
        } else {
            const url = (window.URL || (window as any).webkitURL).createObjectURL(blob);
            const downloadLink = document.createElement('a');
            downloadLink.download = fileName,
            downloadLink.href = url;
            const event = document.createEvent('MouseEvents');
            event.initMouseEvent('click', true, true, window, 1, 0, 0, 0, 0, false, false, false, false, 0, null);
            downloadLink.dispatchEvent(event);
        }
    }

    get debug_currentDate(): any {
        return new Date().toJSON();
    }

    get debug_cookies(): any {
        const cookies: any = {};
        if (document.cookie) {
            const split = document.cookie.split(';');
            for (const rawCookie of split) {
                const nameValue = rawCookie.split('=');
                nameValue[0] = nameValue[0].replace(/^ /, '');
                cookies[decodeURIComponent(nameValue[0])] = decodeURIComponent(nameValue[1]);
            }
        }
        return cookies;
    }

    get debug_localStorage(): any {
        if (typeof localStorage === 'object') {
            return localStorage;
        } else {
            return {};
        }
    }

    get debug_sessionStorage(): any {
        if (typeof sessionStorage === 'object') {
            return sessionStorage;
        } else {
            return {};
        }
    }

    get debug_appState(): any {
        return this.appState.now;
    }

    get debug_dom(): any {
        return domSerialize(window.document);
    }

    get debug_platform(): any {
        if (typeof window.navigator === 'object') {
            const navigator: any = window.navigator;
            const _navigator: any = {};
            for (const key of Object.keys(navigator)) {
                _navigator[key] = navigator[key];
            }

            delete _navigator.plugins;
            delete _navigator.mimeTypes;
            return _navigator;
        } else {
            return {};
        }
    }

    get debug_connection(): any {
        const navigator: any = (window.navigator as any);
        if (navigator.connection && typeof navigator.connection === 'object') {
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
        if (typeof window.location === 'object') {
            return window.location;
        } else {
            return {};
        }
    }

    get debug_loadedScripts(): any {
        const scripts: any = [];

        [].forEach.call(document.querySelectorAll('script[src]'), (element: any) => {
            scripts.push(element.getAttribute('src'));
        });

        return scripts;
    }

    get debug_loadedStyles(): any {
        const stylesheets: any = [];
        const styles: any = [];

        [].forEach.call(document.querySelectorAll('link[rel="stylesheet"][href]'), (element: any) => {
            stylesheets.push(element.getAttribute('href'));
        });

        [].forEach.call(document.querySelectorAll('style'), (element: any) => {
            styles.push(element.innerText);
        });

        return { stylesheets, styles };
    }
}

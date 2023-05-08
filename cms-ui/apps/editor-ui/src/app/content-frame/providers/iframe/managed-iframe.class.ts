import { Observable, Subject } from 'rxjs';
import { filter, map, merge, publish, refCount, take, takeUntil, tap, withLatestFrom } from 'rxjs/operators';
import { BLANK_PAGE, DYNAMIC_FRAME, logIFrameLifecycle } from '../../components/content-frame/common';

export type ManagedIFrameEvent = {
    id: string;
    iframe: HTMLIFrameElement;
    currentPageId: string;
    currentUrl: string;
};

/**
 * Polling is used to simulate the DOMContentsLoaded event in the iframes. This limit will prevent infinite
 * polling when unforeseen conditions arise. The limit is very high because some (rare) CMS implementations
 * have pages which can take tens of seconds or even minutes to load.
 */
const MAX_POLL_ATTEMPTS = 30000; // 5 minutes
const POLL_INTERVAL = 10;

/**
 * Sometimes page resources take a long time to download, which delays the firing of the "load" event. This is a
 * timeout in ms, which will force the `IFrameManager` to fire its internal load event.
 */
const MAX_PAGE_LOAD_DELAY = 5000;

/**
 * Name of the meta tag added to frames to store the frame's id.
 */
const ID_META_TAG_NAME = 'managed-iframe-id';

/**
 * A wrapper around an iframe which exposes the important iframe events as observable streams, and encapsulates
 * the dirty work of managing the iframe's lifecycle.
 */
export class ManagedIFrame {

    /** Emits when the (simulated) DOMContentLoaded event is fired. */
    public domContentLoaded$ = new Subject<ManagedIFrameEvent>();
    /** Emits when the iframe load event is fired.  */
    public load$ = new Subject<ManagedIFrameEvent>();
    /** Emits when the window beforeunload event is fired.  */
    public beforeUnload$: Observable<ManagedIFrameEvent>;
    /** Emits when the window unload event is fired.  */
    public unload$: Observable<ManagedIFrameEvent>;
    /** Emits when the navigation is cancelled by the user via the beforeunload prompt.  */
    public unloadCancelled$: Observable<ManagedIFrameEvent>;
    public currentPageId: string;
    public get currentUrl(): string {
        return this.iframe.contentWindow &&
            this.iframe.contentWindow.location &&
            this.iframe.contentWindow.location.href;
    }
    private nativeLoad$: Observable<any>;
    private didNotUnload$ = new Subject<boolean>();
    private closing$ = new Subject<boolean>();
    private timeoutLoad$ = new Subject<any>();
    private fakeDOMContentLoaded$ = new Subject<any>();
    private blankPageLoaded$ = new Subject<any>();
    private destroy$ = new Subject<any>();

    private onDOMContentLoadedWasInvoked = false;
    private onLoadWasInvoked = false;
    private beforeUnloadCallback: (e: BeforeUnloadEvent) => any;

    private pollCount = 0;
    // used to check when a new document has started loading into the iframe.
    private lastPageId: string;
    private classHasBeenDestroyed = false;
    // Due to the async nature of setting the URL of the window.location value, several
    // setTimeout() calls are needed in this class. We keep all the references together so we can ensure
    // they are all cleaned up when the class is destroyed.
    private timers: { [key: string]: ReturnType<typeof setTimeout> } = {
        pollTimer: null,
        blankPageLoaded: null,
        beforeUnloadOuter: null,
        beforeUnloadInner: null,
        setBlank: null,
        startLoadEvent: null,
    };

    // Re _hackyBeforeUnload$, _hackyUnload$
    // -------------------------------------
    // Originally (commit 0984efeab2a18c5e1442f855fabf4c04764aece3) we used `Observable.fromEvent('beforeunload', ...)`
    // to produce a stream of "beforeunload" and "unload" events. However, with that implementation there were edge-case
    // bugs (https://jira.gentics.com/browse/GCU-261) which could not be resolved. In certain circumstances, after
    // opening a  tagfill iframe and then attempting to close the ContentFrame, the event streams would not fire.
    //
    // Therefore we are using the native addEventListener API, which always works, and simulating the `fromEvent`
    // functionality by simply emitting a new value on the Subjects _hackyBeforeUnload$ & _hackyUnload$.
    private _hackyBeforeUnload$ = new Subject<Event>();
    private _hackyUnload$ = new Subject<Event>();

    private onBeforeUnload = (e: BeforeUnloadEvent): BeforeUnloadEvent => {
        this._hackyBeforeUnload$.next(e);
        if (typeof this.beforeUnloadCallback === 'function') {
            return this.beforeUnloadCallback(e);
        }
    }

    private onUnload = (e: Event): void => {
        this._hackyUnload$.next(e);
        this.safelyRemoveEventListener('beforeunload', this.onBeforeUnload);
        this.safelyRemoveEventListener('unload', this.onUnload);
    }

    constructor(public iframe: HTMLIFrameElement, public id?: string) {
        if (id === undefined) {
            this.id = Math.random().toString(36).substring(4);
        }
        logIFrameLifecycle(this, 'creating ManagedFrame');

        const toManagedIFrameEvent = () => ({
            id: this.id,
            iframe: iframe,
            currentPageId: this.currentPageId,
            currentUrl: this.currentUrl,
        });

        this.nativeLoad$ = Observable.fromEvent(iframe, 'load').pipe(
            filter(() => this.onLoadWasInvoked === false),
            filter(() => iframe.contentWindow && iframe.contentWindow.location.toString() !== BLANK_PAGE),
            takeUntil(this.destroy$),
        );

        this.fakeDOMContentLoaded$.pipe(
            map(toManagedIFrameEvent),
            takeUntil(this.destroy$),
        ).subscribe(e => {
            this.safelyAddEventListener('beforeunload', this.onBeforeUnload);
            this.safelyAddEventListener('unload', this.onUnload);
            const htmlElement = this.iframe.contentWindow.document.querySelector('html') ;
            // add a class to allow better targeting via CSS.
            if (this.iframe.dataset[DYNAMIC_FRAME]) {
                htmlElement.dataset[DYNAMIC_FRAME] = 'true';
            }
            this.domContentLoaded$.next(e);
        });

        this.nativeLoad$.pipe(
            merge(this.timeoutLoad$),
            tap(() => this.onLoadWasInvoked = true),
            map(toManagedIFrameEvent),
            takeUntil(this.destroy$),
        ).subscribe(e => {
            this.load$.next(e);
        });

        this.beforeUnload$ = this._hackyBeforeUnload$.pipe(
            map(toManagedIFrameEvent),
            takeUntil(this.destroy$),
            publish(),
            refCount(),
        );

        this.unload$ =  Observable.merge(this.blankPageLoaded$, this._hackyUnload$).pipe(
            tap(() => logIFrameLifecycle(this, 'unload event fired')),
            filter(() => {
                const contentWindow = this.safelyGetContentWindow();
                return contentWindow && contentWindow.location.toString() !== BLANK_PAGE;
            }),
            withLatestFrom(this.closing$.startWith(false)),
            tap(([e, isClosing]) => {
                this.onLoadWasInvoked = false;
                if (!isClosing) {
                    this.setPageIds();
                    this.fakeDOMContentLoaded();
                } else {
                    this.timers.blankPageLoaded = setTimeout(() => this.blankPageLoaded$.next(true), 10);
                }
            }),
            map(toManagedIFrameEvent),
            takeUntil(this.destroy$),
            publish(),
            refCount(),
        );

        this.unloadCancelled$ = this.didNotUnload$.map(toManagedIFrameEvent);

        // For iframes which already have a document loaded, start the polling process.
        if (iframe.src && iframe.src !== BLANK_PAGE) {
            this.setPageIds();
            this.fakeDOMContentLoaded();
        }
    }

    /**
     * The next url change will not trigger the lifecycle methods.
     */
    public prepareToClose(): void {
        this.closing$.next(true);
    }

    /**
     * Load a new URL into the iframe. Returns a promise which resolves to true if the navigation completed,
     * or false if not (i.e. the user elected to cancel navigation via the beforeunload prompt).
     */
    public setUrl(url: string): Promise<boolean> {
        if (this.classHasBeenDestroyed) {
            return Promise.resolve(false);
        }

        this.safeReplaceUrl(url);
        this.closing$.next(false);

        if (this.currentUrl === undefined || this.currentUrl === BLANK_PAGE) {
            // this is the initial load
            this.onDOMContentLoadedWasInvoked = false;
            this.onLoadWasInvoked = false;
            this.setPageIds();
            this.fakeDOMContentLoaded();
        }

        return new Promise(resolve => {
            this.fakeDOMContentLoaded$.pipe(take(1)).subscribe(
                () => {
                    this.onLoadWasInvoked = false;
                    resolve(true);
                },
                err => { throw err; },
                () => resolve(true),
            );

            this.didNotUnload$.pipe(
                take(1),
                takeUntil(this.unload$),
            ).subscribe(() => {
                resolve(false);
            });
        });
    }

    /**
     * Reload the current document in the iframe.
     */
    public reload(): void {
        this.iframe.contentWindow.location.reload();
    }

    /**
     * Clean up event listeners and timers.
     */
    public destroy(): void {
        this.classHasBeenDestroyed = true;
        this.destroy$.next(true);
        this.safelyRemoveEventListener('beforeunload', this.onBeforeUnload);
        this.safelyRemoveEventListener('unload', this.onUnload);
        if (this.iframe.parentElement && this.iframe.parentElement.removeChild) {
            this.iframe.parentElement.removeChild(this.iframe);
        }
        for (let timerId in this.timers) {
            clearTimeout((this.timers as any)[timerId]);
        }
        logIFrameLifecycle(this, 'destroyed');
    }

    /**
     * Adds an event listener from the iframe.contentWindow without causing an exception in IE11.
     */
    private safelyAddEventListener(eventName: string, listener: EventListener): void {
        const contentWindow = this.safelyGetContentWindow();
        if (!contentWindow) {
            // console.warn(`Cannot bind Event ${eventName} to managed iframe, because the content-windows does not exist!`);
            return;
        }
        contentWindow.addEventListener(eventName, listener);
    }

    /**
     * Removes an event listener from the iframe.contentWindow without causing an exception in IE11.
     */
    private safelyRemoveEventListener(eventName: string, listener: EventListener): void {
        const contentWindow = this.safelyGetContentWindow();
        if (!contentWindow) {
            // console.warn(`Cannot unbind Event ${eventName} from managed iframe, because the content-windows does not exist!`);
            return;
        }
        contentWindow.removeEventListener(eventName, listener);
    }

    /**
     * IE11 sometimes runs into an error where the `iframe` is no longer accessible, which causes the
     * cryptic "SCRIPT16389: Unspecified error". This can be caused by the Aloha modal window being
     * closed and the iframe being already detatched from the DOM when this function is run.
     *
     * Even just checking for the existence of `iframe.contentWindow` will throw an exception in IE11.
     * See https://msdn.microsoft.com/en-us/library/gg622929(v=vs.85).aspx
     *
     * This try-catch prevents app-breaking uncaught exceptions in this case.
     */
    private safelyGetContentWindow(): Window {
        let contentWindow: Window;
        try {
            contentWindow = this.iframe && this.iframe.contentWindow;
        } catch (e) {
            // iframe no longer accessible - swallow the exception because there is nothing else
            // profitable to be done. This is simply IE11.
        }
        return contentWindow;
    }

    /**
     * Replace the window.location in a safe manner.
     */
    private safeReplaceUrl(url: string): void {
        const contentWindow = this.safelyGetContentWindow();
        if (!contentWindow || !contentWindow.location) {
            console.warn(`Cannot navigate to URL ${url} in managed iframe, because the content-window does not exist!`);
            return;
        }
        contentWindow.location.replace(url);
    }

    /**
     * Sets a new random id for the currently-loaded document.
     */
    private setPageIds(): void {
        this.lastPageId = this.currentPageId;
        this.currentPageId = Math.random().toString(36).substring(4);
    }

    /**
     * Since it is not possible to listen for DOMContentLoaded on an iframe, we need to use another
     * means to detect when the document (excluding external assets) has loaded. This method
     * uses polling to try to guess when a new document has loaded, and when it seems so, it
     * will invoke the callbacks in the onLoadHandlers array.
     *
     * We are trying to solve the problem of how to know when a new iframe page has been loaded, so we can,
     * for instance, inject our own custom styles to override the document styles, or attach events to the
     * new document's window object.
     *
     * The obvious thing to do is to invoke a callback on the iframe window's "onload" event.
     * This is no good, however, since "onload" only fires after *all* resources (images, scripts etc.)
     * have loaded, which is way too late, leading to (in the case of styles) a flash of old style
     * before the new overrides are applied.
     *
     * This function uses polling to check whether:
     *  a) the CMS document has started loading at all, indicated by the presence of
     *      child nodes in the <head>
     *  b) If the styles from the last page are still to be found in the DOM, then we assume
     *      the new CMS document has not yet loaded, and keep polling.
     *  c) If neither of the above are the case, we can assume the new CMS document has loaded into the iframe,
     *      and therefore it is safe to inject the custom styles.
     */
    private fakeDOMContentLoaded(): void {
        logIFrameLifecycle(this, 'polling started');
        clearTimeout(this.timers.pollTimer);
        const poll = () => {
            // prevent memory leaks.
            if (this.classHasBeenDestroyed || !this.isAttachedToDOM(this.iframe)) {
                clearTimeout(this.timers.pollTimer);
                return;
            }
            const pollCount = this.pollCount++;

            if (!this.documentHasMinimumContents(this.iframe) || this.lastPageIdStillPresent(this.iframe)) {
                if (MAX_POLL_ATTEMPTS <= pollCount) {
                    logIFrameLifecycle(this, 'MAX_POLL_ATTEMPTS reached', 'red');
                    this.emitDOMContentLoaded();
                    return;
                }

                logIFrameLifecycle(this, `polling, ${this.iframe.src}`, '#aaa');
                this.timers.pollTimer = setTimeout(poll, POLL_INTERVAL);
                return;
            }

            clearTimeout(this.timers.pollTimer);
            logIFrameLifecycle(this, 'polling complete');

            // do not invoke the rest of the set-up for blank pages.
            if (this.currentUrl !== BLANK_PAGE) {
                this.emitDOMContentLoaded();
            }

        };

        poll();
    }

    /**
     * Checks to see if the given element is attached to the DOM by walking up the parent chain checking
     * that there is a non-null value for parentElement.
     */
    private isAttachedToDOM(el: HTMLElement): boolean {
        const levelsToWalk = 5;
        let testEl = el.parentElement;

        for (let i = 0; i < levelsToWalk; i++) {
            if (!testEl) {
                return false;
            }
            if (testEl.tagName === 'BODY') {
                return true;
            }
            testEl = testEl.parentElement;
        }
        return true;
    }

    /**
     * Returns true if the iframe has both a head and a body element, and at least one of
     * those has children. Not certain that this is a solid test of page-readiness, but so far
     * from testing it seems to work.
     */
    private documentHasMinimumContents(iframe: HTMLIFrameElement): boolean {
        if (!iframe || !iframe.contentWindow) {
            return false;
        }
        const head =  iframe.contentWindow.document.querySelector('head');
        const body =  iframe.contentWindow.document.querySelector('body');

        return (head && body) && (0 < head.children.length || 0 < body.children.length);
    }

    /**
     * Adds a meta tag to the document header containing the current page id. Used to simulate the
     * DOMContentLoaded event.
     */
    private injectPageId(): void {
        let doc = this.iframe.contentWindow.document;
        let metaTag = doc.createElement('meta');
        metaTag.setAttribute('name', ID_META_TAG_NAME);
        metaTag.setAttribute('id', `${this.currentPageId}`);
        doc.head.appendChild(metaTag);
    }

    /**
     * Returns true if the meta tag with the lastPageId are found in the iframe's <head>.
     * If the lastPageId is falsy, we can assume this is the first time a page is being loaded,
     * and return false.
     */
    private lastPageIdStillPresent(iframe: HTMLIFrameElement): boolean {
        if (!this.lastPageId) {
            return false;
        }
        let lastIdMetaTag = iframe.contentWindow.document
            .querySelector(`meta[name="${ID_META_TAG_NAME}"][id="${this.lastPageId}"]`);
        return lastIdMetaTag !== null;
    }

    private emitDOMContentLoaded(): void {
        if (this.iframe.contentWindow) {
            this.injectPageId();
            this.pollCount = 0;
            this.onDOMContentLoadedWasInvoked = true;
            this.fakeDOMContentLoaded$.next(true);
            this.startLoadEventTimeout();
        }
    }

    /**
     * If the iframe's "load" event has not fired after a certain timeout, force it to fire.
     */
    private startLoadEventTimeout(): void {
        clearTimeout(this.timers.startLoadEvent);
        let timer = 0;
        const interval = 50;
        logIFrameLifecycle(this, 'loading started');
        const checkLoaded = () => {
            if (this.classHasBeenDestroyed) {
                // prevent memory leaks
                return;
            }
            timer += interval;
            logIFrameLifecycle(this, 'check load event', '#aaa');
            if (this.onLoadWasInvoked || !this.onDOMContentLoadedWasInvoked) {
                return;
            }
            if (MAX_PAGE_LOAD_DELAY < timer) {
                logIFrameLifecycle(this, 'MAX_PAGE_LOAD_DELAY reached', 'red');
                this.timeoutLoad$.next(true);
                return;
            }
            this.timers.startLoadEvent = setTimeout(checkLoaded, interval);
        };
        this.timers.startLoadEvent = setTimeout(checkLoaded, interval);
    }
}

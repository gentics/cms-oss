import {Observable, Subject} from 'rxjs';

export function getTestPageUrl(pageNumber: number): string {
    let { protocol, host } = window.location;
    return `${protocol}//${host}${getTestPagePath(pageNumber)}`;
}

export function getTestPagePath(pageNumber: number): string {
    return `/base/testing/test-page-${pageNumber}.html`;
}

export class MockManagedIFrame {
    id: string;
    domContentLoaded$ = new Subject<any>();
    load$ = new Subject<any>();
    beforeUnload$ = new Subject<any>();
    unload$ = new Subject<any>();
    unloadCancelled$ = new Subject<any>();
    beforeUnload(): void {}
    setBlank(): Observable<boolean> { return Observable.of(true); }
    prepareToClose(): void {}
    setUrl(): void {}
    destroy(): void {}

    constructor(public iframe?: MockIFrame) {
        if (iframe === undefined) {
            this.iframe = new MockIFrame();
        }
    }
}

export class MockIFrame {
    contentWindow = {
        contentDocument: {},
        document: {
            head: {
                appendChild(): void {}
            },
            createElement(): any {
                return {
                    setAttribute(): void {},
                    appendChild(): void {}
                };
            },
            createTextNode(): void {},
            documentElement: document.createElement('div'),
            querySelectorAll(): any { return []; }
        }
    };
    [key: string]: any;
}

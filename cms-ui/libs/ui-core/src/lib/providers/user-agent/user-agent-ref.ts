import { Injectable } from '@angular/core';

@Injectable()
export class UserAgentProvider {
    static windowRef: Window = window;

    /** @deprecated Support for IE11 has been removed */
    readonly isIE11: boolean;

    /** If the browser is MS Edge, which still has some quirks */
    readonly isEdge: boolean;

    /** If the browser is Firefox/Gecko based */
    readonly isGecko: boolean;

    constructor() {
        const window = UserAgentProvider.windowRef;
        this.isIE11 = !!((window as any).MSInputMethodContext && (window.document as any).documentMode);
        this.isEdge = window.navigator.userAgent.includes('Edge');
        this.isGecko = window.navigator.userAgent.includes('Gecko/');
    }
}

import { Injectable } from '@angular/core';

@Injectable()
export class UserAgentProvider {
    static windowRef: Window = window;

    // eslint-disable-next-line @typescript-eslint/naming-convention
    readonly isIE11: boolean;
    // eslint-disable-next-line @typescript-eslint/naming-convention
    readonly isEdge: boolean;

    constructor() {
        const window = UserAgentProvider.windowRef;
        this.isIE11 = !!((window as any).MSInputMethodContext && (window.document as any).documentMode);
        this.isEdge = !!(window.navigator.userAgent.indexOf('Edge') > -1);
    }
}

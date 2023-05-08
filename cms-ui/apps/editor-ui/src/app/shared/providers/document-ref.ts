import { Injectable } from '@angular/core';

@Injectable()
export class DocumentRef {
    get nativeDocument(): any {
        return window.document;
    }
}

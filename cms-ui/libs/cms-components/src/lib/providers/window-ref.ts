import { Injectable } from '@angular/core';

const _window = window;

@Injectable()
export class WindowRef {
    get nativeWindow(): Window & typeof globalThis {
        return _window;
    }
}

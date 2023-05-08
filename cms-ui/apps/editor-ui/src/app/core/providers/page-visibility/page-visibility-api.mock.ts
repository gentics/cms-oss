/**
 * Mock to test code which relies on the Page Visibility API
 * independently of the "real" page visibility state.
 * The mock should be destroyed via `destroy()` after each test.
 * 
 * Usage:
 * ```typescript
 * let pageVisibility: MockPageVisibility;
 * beforeEach(() => pageVisibility = new MockPageVisibility());
 * afterEach(() => pageVisibility.destroy());
 *
 * it('Pauses the video when the page is hidden', () => {
 *     pageVisibility.hidePage();
 *     expect(fakeVideo.playing).toBe(false);
 * });
 */
export class MockPageVisibilityAPI {

    private _visible = true;
    private _triggeringFakeEvent = false;

    constructor(private document: any = window.document) {
        this.fakeGetHidden = this.fakeGetHidden.bind(this);
        this.fakeGetVisibilityState = this.fakeGetVisibilityState.bind(this);
        this.preventRealPageEvents = this.preventRealPageEvents.bind(this);

        Object.defineProperties(this.document, {
            hidden: {
                configurable: true,
                enumerable: true,
                get: this.fakeGetHidden
            },
            visibilityState: {
                configurable: true,
                enumerable: true,
                get: this.fakeGetVisibilityState
            }
        });

        for (let prefix of ['', 'webkit', 'moz', 'ms', 'o']) {
            this.document.addEventListener(prefix + 'visibilitychange', this.preventRealPageEvents);
        }
    }

    public destroy(): void {
        const hiddenProperty = Object.getOwnPropertyDescriptor(this.document, 'hidden');
        const visibilityStateProperty = Object.getOwnPropertyDescriptor(this.document, 'visibilityState');

        if (hiddenProperty && hiddenProperty.get === this.fakeGetHidden) {
            delete this.document.hidden;
        }
        if (visibilityStateProperty && visibilityStateProperty.get === this.fakeGetVisibilityState) {
            delete this.document.visibilityState;
        }

        for (let prefix of ['', 'webkit', 'moz', 'ms', 'o']) {
            this.document.removeEventListener(prefix + 'visibilitychange', this.preventRealPageEvents);
        }
    }

    public get visible(): boolean {
        return this._visible;
    }

    public showPage(): void {
        this.setVisibility(true);
    }

    public hidePage(): void {
        this.setVisibility(false);
    }

    private setVisibility(visible: boolean): void {
        if (visible !== this._visible) {
            this._visible = visible;
            this._triggeringFakeEvent = true;
            let ev: Event;
            try {
                ev = new Event('visibilitychange');
            } catch (ie11) {
                ev = document.createEvent('Event');
                ev.initEvent('visibilitychange', false, false);
            }
            this.document.dispatchEvent(ev);
            this._triggeringFakeEvent = false;
        }
    }

    private fakeGetHidden(): boolean {
        return !this._visible;
    }

    private fakeGetVisibilityState(): string {
        return this._visible ? 'visible' : 'hidden';
    }

    private preventRealPageEvents(event: Event): void {
        if (!this._triggeringFakeEvent) {
            event.stopImmediatePropagation();
        }
    }
}

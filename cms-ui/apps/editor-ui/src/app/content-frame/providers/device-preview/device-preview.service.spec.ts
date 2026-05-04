import { TestBed } from '@angular/core/testing';
import { NavigationEnd, Router } from '@angular/router';
import { Subject } from 'rxjs';
import {
    DEFAULT_DEVICE_PRESETS,
    DEVICE_PREVIEW_QUERY_PARAM,
    DevicePreviewService,
} from './device-preview.service';

class MockRouter {
    public events = new Subject<any>();
    public url = '/editor/1/page/2/preview';
    public navigateByUrlCalls: { tree: any; extras?: any }[] = [];

    parseUrl(url: string): any {
        const [path, query = ''] = url.split('?');
        const queryParams: Record<string, string> = {};
        if (query) {
            for (const part of query.split('&')) {
                const [k, v] = part.split('=');
                if (k) {
                    queryParams[decodeURIComponent(k)] = decodeURIComponent(v ?? '');
                }
            }
        }
        return {
            path,
            queryParams,
            toString(): string {
                const qp = Object.entries(this.queryParams)
                    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v as string)}`)
                    .join('&');
                return qp ? `${this.path}?${qp}` : this.path;
            },
        };
    }

    navigateByUrl(tree: any, extras?: any): Promise<boolean> {
        this.navigateByUrlCalls.push({ tree, extras });
        const newUrl = tree.toString();
        this.url = newUrl;
        // Emit a NavigationEnd to mimic real router behaviour
        this.events.next(new NavigationEnd(1, newUrl, newUrl));
        return Promise.resolve(true);
    }
}

describe('DevicePreviewService', () => {

    let service: DevicePreviewService;
    let router: MockRouter;

    function createService(initialUrl: string = '/editor/1/page/2/preview'): void {
        router = new MockRouter();
        router.url = initialUrl;
        TestBed.configureTestingModule({
            providers: [
                DevicePreviewService,
                { provide: Router, useValue: router },
            ],
        });
        service = TestBed.inject(DevicePreviewService);
    }

    afterEach(() => {
        service?.ngOnDestroy?.();
    });

    it('starts deactivated when no device query param is present in the URL', () => {
        createService('/editor/1/page/2/preview');
        expect(service.currentState).toEqual({ active: false, presetId: null });
        expect(service.presets.length).toBe(DEFAULT_DEVICE_PRESETS.length);
    });

    it('seeds initial state from a `?device=` query param on the current URL', () => {
        createService('/editor/1/page/2/preview?device=tablet');
        expect(service.currentState).toEqual({ active: true, presetId: 'tablet' });
    });

    it('ignores an unknown preset id in the URL', () => {
        createService('/editor/1/page/2/preview?device=xxx');
        expect(service.currentState).toEqual({ active: false, presetId: null });
    });

    it('activate() writes the preset id into the URL via Router.navigateByUrl', () => {
        createService();
        service.activate('mobile');
        const last = router.navigateByUrlCalls.at(-1);
        expect(last?.tree.queryParams[DEVICE_PREVIEW_QUERY_PARAM]).toBe('mobile');
        expect(service.currentState).toEqual({ active: true, presetId: 'mobile' });
    });

    it('activate() ignores an unknown preset id', () => {
        createService();
        service.activate('does-not-exist');
        expect(router.navigateByUrlCalls.length).toBe(0);
        expect(service.currentState).toEqual({ active: false, presetId: null });
    });

    it('deactivate() removes the device query param from the URL', () => {
        createService('/editor/1/page/2/preview?device=tablet');
        expect(service.currentState.active).toBe(true);
        service.deactivate();
        const last = router.navigateByUrlCalls.at(-1);
        expect(last?.tree.queryParams[DEVICE_PREVIEW_QUERY_PARAM]).toBeUndefined();
        expect(service.currentState).toEqual({ active: false, presetId: null });
    });

    it('deactivate() is a no-op when device-preview is already off', () => {
        createService('/editor/1/page/2/preview');
        service.deactivate();
        expect(router.navigateByUrlCalls.length).toBe(0);
    });

    it('toggle() switches the same preset off and a different preset on', () => {
        createService();

        service.toggle('desktop');
        expect(service.currentState.active).toBe(true);
        expect(service.currentState.presetId).toBe('desktop');

        service.toggle('desktop');
        expect(service.currentState.active).toBe(false);

        service.toggle('mobile');
        expect(service.currentState.presetId).toBe('mobile');
    });

    it('reflects URL changes coming from the router (browser back/forward)', () => {
        createService('/editor/1/page/2/preview?device=tablet');
        expect(service.currentState.presetId).toBe('tablet');

        // Simulate the user navigating back to a URL without the device param
        router.url = '/editor/1/page/2/preview';
        router.events.next(new NavigationEnd(2, router.url, router.url));
        expect(service.currentState).toEqual({ active: false, presetId: null });

        // Simulate forward to a URL with a different preset
        router.url = '/editor/1/page/2/preview?device=mobile';
        router.events.next(new NavigationEnd(3, router.url, router.url));
        expect(service.currentState).toEqual({ active: true, presetId: 'mobile' });
    });

    it('activePreset$ emits null while inactive and the resolved preset while active', (done) => {
        createService();
        const emissions: (string | null)[] = [];
        service.activePreset$.subscribe(p => emissions.push(p?.id ?? null));

        service.activate('mobile');
        service.deactivate();

        // BehaviorSubject seed (null) + activate (mobile) + deactivate (null)
        setTimeout(() => {
            expect(emissions).toEqual([null, 'mobile', null]);
            done();
        }, 0);
    });
});

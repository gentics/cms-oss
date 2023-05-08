import { SpyEventTarget, SpyObserver } from '../../../../testing';
import { MockPageVisibilityAPI } from './page-visibility-api.mock';
import { PageVisibility } from './page-visibility.service';

describe('PageVisibilityService', () => {

    let fakeDocument: SpyEventTarget & { hidden: boolean };
    let fakeDocumentWithoutAPISupport: SpyEventTarget & { hidden: boolean };
    let mockPageVisibility: MockPageVisibilityAPI;

    beforeEach(() => {
        fakeDocument = <any> new SpyEventTarget('document');
        fakeDocumentWithoutAPISupport = Object.apply(
            new SpyEventTarget('document (no API support)'),
            {
                hidden: <any> undefined,
                webkitHidden: <any> undefined,
                mozHidden: <any> undefined,
                msHidden: <any> undefined,
                oHidden: <any> undefined,
            },
        );
    });

    afterEach(() => {
        if (mockPageVisibility) {
            mockPageVisibility.destroy();
            mockPageVisibility = undefined;
        }
    });

    describe('visible', () => {

        it('returns the current page visibility', () => {
            mockPageVisibility = new MockPageVisibilityAPI(fakeDocument);
            const service = new PageVisibility(fakeDocument);

            mockPageVisibility.showPage();
            expect(service.visible).toBe(true);
            mockPageVisibility.hidePage();
            expect(service.visible).toBe(false);
        });

        it('always returns true if the Page Visibility API is not supported', () => {
            const service = new PageVisibility(fakeDocumentWithoutAPISupport);
            expect(service.visible).toBe(true);
        });

    });

    describe('visible$', () => {

        it('emits the current visibility when subscribed to', () => {
            mockPageVisibility = new MockPageVisibilityAPI(fakeDocument);
            const service = new PageVisibility(fakeDocument);
            mockPageVisibility.showPage();

            const visible$ = new SpyObserver('visible$');
            const sub = service.visible$.subscribe(visible$);
            expect(visible$.next).toHaveBeenCalledTimes(1);
            expect(visible$.next).toHaveBeenCalledWith(true);

            mockPageVisibility.hidePage();
            expect(visible$.next).toHaveBeenCalledTimes(2);
            expect(visible$.next).toHaveBeenCalledWith(false);

            sub.unsubscribe();
        });

        it('emits true (exactly once) if the Page Visibility API is not supported', () => {
            let service = new PageVisibility(fakeDocumentWithoutAPISupport);
            let visible$ = new SpyObserver('visible$');
            const sub = service.visible$.subscribe(visible$);
            expect(visible$.next).toHaveBeenCalledTimes(1);
            expect(visible$.next).toHaveBeenCalledWith(true);
            sub.unsubscribe();
        });

    });

});

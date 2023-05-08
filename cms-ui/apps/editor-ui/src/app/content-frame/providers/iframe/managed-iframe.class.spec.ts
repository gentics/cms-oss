import { BLANK_PAGE } from '../../components/content-frame/common';
import { ManagedIFrame } from './managed-iframe.class';
import { getTestPageUrl } from './testing-helpers';

const testPage1 = getTestPageUrl(1);
const testPage2 = getTestPageUrl(2);

describe('ManagedIFrame', () => {

    function createIframe(): HTMLIFrameElement & { srcdoc: string; } {
        let iframe = document.createElement('iframe') as any;
        document.body.appendChild(iframe);
        return iframe;
    }

    /**
     * Weird hack that prevents tests failing. Seems like it has to do with the async nature of iframes - without this
     * code, tests which should pass will fail seemingly due to some type of contamination from other tests.
     * This timeout probably just gives the DOM a change to update and clear out the old iframe from the
     * previous test.
     */
    beforeEach((done: DoneFn) => {
        setTimeout(() => done());
    });

    it('should not emit DOMContentLoaded if the document is empty', (done: DoneFn) => {
        let iframe = createIframe();
        let mif = new ManagedIFrame(iframe);
        let emitted = false;
        mif.domContentLoaded$.subscribe(() => emitted = true);

        setTimeout(() => {
            expect(emitted).toBe(false);
            mif.destroy();
            done();
        }, 50);
    });

    it('should emit DOMContentLoaded when document has content', (done: DoneFn) => {
        let iframe = createIframe();
        iframe.srcdoc = '<body><h1>iframe content</h1></body>';
        let mif = new ManagedIFrame(iframe);

        mif.domContentLoaded$.subscribe(event => {
            expect(event).toBeDefined();
            expect(event.iframe).toBe(iframe);
            mif.destroy();
            done();
        });
        mif.setUrl('');
    });

    it('should emit load event when document loaded', (done: DoneFn) => {
        let iframe = createIframe();
        iframe.srcdoc = '<body><h1>iframe content</h1></body>';
        let mif = new ManagedIFrame(iframe);

        mif.load$.subscribe(event => {
            expect(event).toBeDefined();
            expect(event.iframe).toBe(iframe);
            mif.destroy();
            done();
        });
    });

    it('should emit beforeunload', (done: DoneFn) => {
        let iframe = createIframe();
        let mif = new ManagedIFrame(iframe);
        mif.beforeUnload$.subscribe(event => {
            expect(event).toBeDefined();
            expect(event.iframe).toBe(iframe);
            mif.destroy();
            done();
        });
        triggerPageLoadCycle(mif);
    });

    it('should emit unload', (done: DoneFn) => {
        let iframe = createIframe();
        let mif = new ManagedIFrame(iframe);
        mif.unload$.subscribe(event => {
            expect(event).toBeDefined();
            expect(event.iframe).toBe(iframe);
            mif.destroy();
            done();
        });
        triggerPageLoadCycle(mif);
    });

    it('should not allow concurrent polling (domContentLoaded$ cannot emit twice consecutively)', (done: DoneFn) => {
        let iframe = createIframe();
        let mif = new ManagedIFrame(iframe);
        let counter = 0;
        mif.domContentLoaded$.subscribe(() => {
            counter++;
            expect(counter).toBe(1);
        });
        Promise.all([
            mif.setUrl(testPage2),
            mif.setUrl(testPage1),
            mif.setUrl(testPage2),
            mif.setUrl(testPage1),
        ])
            .then(() => {
                setTimeout(() => {
                    mif.destroy();
                    done();
                }, 100);
            });
    });


    it('should not allow concurrent polling (domContentLoaded$ cannot emit twice consecutively)', (done: DoneFn) => {
        let iframe = createIframe();
        let mif = new ManagedIFrame(iframe);
        let counter = 0;
        mif.domContentLoaded$.subscribe(() => {
            counter++;
            expect(counter).toBe(1);
        });
        Promise.all([
            mif.setUrl(testPage2),
            mif.setUrl(testPage1),
            mif.setUrl(testPage2),
            mif.setUrl(testPage1),
        ])
            .then(() => {
                setTimeout(() => {
                    mif.destroy();
                    done();
                }, 100);
            });
    });

    it('setUrl() should update the iframe.contentWindow location', (done: DoneFn) => {
        let iframe = createIframe();
        let mif = new ManagedIFrame(iframe);
        expect(mif.currentUrl).toBe(BLANK_PAGE);
        expect(iframe.contentWindow.location.href).toBe(BLANK_PAGE);

        let nextUrl = testPage1;
        mif.setUrl(nextUrl)
            .then(() => {
                expect(mif.currentUrl).toBe(nextUrl);
                expect(iframe.contentWindow.location.href).toBe(nextUrl);
                mif.destroy();
                done();
            })
            .catch(() => {
                expect(false).toBe(true, 'setUrl threw');
                mif.destroy();
                done();
            });
    });

    it('should inject a meta tag with the currentPageId', (done: DoneFn) => {
        let iframe = createIframe();
        let mif = new ManagedIFrame(iframe);
        let id1: string;
        let id2: string;

        mif.setUrl(testPage1)
            .then(() => {
                id1 = mif.currentPageId;
                let metaTag = iframe.contentDocument.documentElement.querySelector(`meta[id="${id1}"]`);
                expect(metaTag).not.toBeNull();
                return mif.setUrl(testPage2);
            })
            .then(() => {
                id2 = mif.currentPageId;
                let metaTag1 = iframe.contentDocument.documentElement.querySelector(`meta[id="${id1}"]`);
                let metaTag2 = iframe.contentDocument.documentElement.querySelector(`meta[id="${id2}"]`);
                expect(metaTag1).toBeNull();
                expect(metaTag2).not.toBeNull();
                mif.destroy();
                done();
            });
    });

    describe('navigating', () => {

        it('should emit DOMContentLoaded$ on next page', (done: DoneFn) => {
            let iframe = createIframe();
            let mif = new ManagedIFrame(iframe);
            mif.setUrl(testPage1)
                .then(() => {
                    mif.domContentLoaded$.subscribe((e) => {
                        expect(e.currentUrl).toBe(testPage2);
                        mif.destroy();
                        done();
                    });
                    // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
                    (iframe.contentDocument.querySelector('a.link') as HTMLElement).click();
                });
        });

        it('should emit load$ on next page', (done: DoneFn) => {
            let iframe = createIframe();
            let mif = new ManagedIFrame(iframe);
            mif.setUrl(testPage1)
                .then(() => {
                    mif.load$.subscribe((e) => {
                        expect(e.currentUrl).toBe(testPage2);
                        mif.destroy();
                        done();
                    });
                    // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
                    (iframe.contentDocument.querySelector('a.link') as HTMLElement).click();
                });
        });

    });

    describe('destroy()', () => {

        it('should remove iframe from parent element', () => {
            let iframe = document.createElement('iframe');
            let container = document.createElement('div');
            container.appendChild(iframe);

            let mif = new ManagedIFrame(iframe);
            expect(container.querySelector('iframe')).not.toBeNull();

            mif.destroy();

            expect(container.querySelector('iframe')).toBeNull();
        });

        it('should not throw if iframe already detached from DOM', () => {
            let iframe = document.createElement('iframe');
            let mif = new ManagedIFrame(iframe);

            expect(() => mif.destroy()).not.toThrow();
        });

        it('should no longer emit events', async () => {
            let iframe = createIframe();
            iframe.srcdoc = '<body><h1>iframe content</h1></body>';
            let mif = new ManagedIFrame(iframe);
            mif.destroy();

            mif.domContentLoaded$.subscribe(event => {
                expect(false).toBe(true, 'should never reach here');
            });
            mif.load$.subscribe(event => {
                expect(false).toBe(true, 'should never reach here');
            });
            mif.beforeUnload$.subscribe(event => {
                expect(false).toBe(true, 'should never reach here');
            });
            mif.unload$.subscribe(event => {
                expect(false).toBe(true, 'should never reach here');
            });

            await mif.setUrl(testPage1);
        });
    });
});

/**
 * Loads test page 1, and then test page 2, so that the unload events get triggered for test page 1.
 */
// eslint-disable-next-line prefer-arrow/prefer-arrow-functions
function triggerPageLoadCycle(mif: ManagedIFrame): Promise<any> {
    return mif.setUrl(testPage1)
        .then(() => mif.setUrl(testPage2));
}

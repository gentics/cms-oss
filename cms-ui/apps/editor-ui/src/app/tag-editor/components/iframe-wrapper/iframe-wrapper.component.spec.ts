import { Component, ElementRef, QueryList, ViewChild } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ApplicationStateService } from '@editor-ui/app/state';
import { TestApplicationState } from '@editor-ui/app/state/test-application-state.mock';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { Subject } from 'rxjs';
import { componentTest, configureComponentTest } from '../../../../testing';
import { getTestPagePath, getTestPageUrl } from '../../../../testing/iframe-helpers';
import { IFrameStylesService } from '../../providers/iframe-styles/iframe-styles.service';
import { IFrameWrapperComponent } from './iframe-wrapper.component';

// A longer timeout for Jasmine async tests to allow the IFrame contents to load.
const JASMINE_TIMEOUT = 15000;

// The timeout for waiting for an IFrame to load.
const WAIT_FOR_LOAD_TIMEOUT = 5000;

const SRC_URL1 = getTestPagePath(1);
const SRC_URL2 = getTestPagePath(2);

const INITIAL_WIDTH = '100px';
const CHANGED_WIDTH = '200px';
const INITIAL_HEIGHT = '100px';
const CHANGED_HEIGHT = '200px';

describe('IFrameWrapperComponent', () => {

    let origDefaultTimeout: number;

    beforeEach(() => {
        configureComponentTest({
            imports: [
                GenticsUICoreModule.forRoot(),
            ],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: IFrameStylesService, useClass: MockIFrameStylesService },
            ],
            declarations: [
                IFrameWrapperComponent,
                TestComponent,
            ],
        });

        origDefaultTimeout = jasmine.DEFAULT_TIMEOUT_INTERVAL;
        jasmine.DEFAULT_TIMEOUT_INTERVAL = JASMINE_TIMEOUT;
    });

    afterEach(() => {
        jasmine.DEFAULT_TIMEOUT_INTERVAL = origDefaultTimeout;
    });

    it('setting srcUrl loads an IFrame and fires the iFrameLoad event when the IFrame has completed loading', (done: DoneFn) => {
        const fixture = TestBed.createComponent(TestComponent);
        const instance = fixture.componentInstance;
        const iFrameLoadedSpy = spyOn(instance, 'onIFrameLoad').and.stub();

        instance.srcUrl = SRC_URL1;
        fixture.detectChanges();

        const iFrame = fixture.debugElement.query(By.css('iframe'));
        expect(iFrame).toBeTruthy();
        const iFrameElem: HTMLIFrameElement = iFrame.nativeElement;

        setTimeout(() => {
            expect(iFrameLoadedSpy).toHaveBeenCalledTimes(1);
            expect(iFrameLoadedSpy).toHaveBeenCalledWith(iFrameElem);
            expect(iFrameElem.contentWindow.location.href).toContain(SRC_URL1);
            done();
        }, WAIT_FOR_LOAD_TIMEOUT);
    });

    it('changing srcUrl loads the new page in the IFrame and fires the iFrameLoad event again when the IFrame has completed loading', (done: DoneFn) => {
        const fixture = TestBed.createComponent(TestComponent);
        const instance = fixture.componentInstance;
        const iFrameLoadedSpy = spyOn(instance, 'onIFrameLoad').and.stub();

        instance.srcUrl = SRC_URL1;
        fixture.detectChanges();

        const iFrameElem: HTMLIFrameElement = fixture.debugElement.query(By.css('iframe')).nativeElement;

        const checkIFrameLoaded = (srcUrl: string) => {
            expect(iFrameLoadedSpy).toHaveBeenCalledTimes(1);
            expect(iFrameLoadedSpy).toHaveBeenCalledWith(iFrameElem);
            expect(iFrameElem.contentWindow.location.href).toContain(srcUrl);
        };

        setTimeout(() => {
            checkIFrameLoaded(SRC_URL1);

            iFrameLoadedSpy.calls.reset();
            instance.srcUrl = SRC_URL2;
            fixture.detectChanges();

            setTimeout(() => {
                checkIFrameLoaded(SRC_URL2);
                done();
            }, WAIT_FOR_LOAD_TIMEOUT);

        }, WAIT_FOR_LOAD_TIMEOUT);
    });

    it('an initial IFrame load of about:blank does not trigger the iFrameLoad',
        componentTest(() => TestComponent, (fixture, instance) => {
            const iFrameLoadedSpy = spyOn(instance, 'onIFrameLoad').and.stub();

            // Defer the subscription to the QueryList.
            const origNgAfterViewInit = instance.iFrameWrapper.ngAfterViewInit.bind(instance.iFrameWrapper);
            spyOn(instance.iFrameWrapper, 'ngAfterViewInit').and.stub();

            instance.srcUrl = getTestPagePath(1);
            fixture.detectChanges();

            const mockedQueryList = mockQueryList(instance.iFrameWrapper);
            origNgAfterViewInit();

            // Supply an IFrame in readyState = 'complete'
            const mockedIFrame = new MockIFrame();
            mockedIFrame.contentDocument.readyState = 'complete';
            mockedIFrame.contentWindow.location.href = 'about:blank';
            const addEventListenerSpy = spyOn(mockedIFrame, 'addEventListener');
            mockedQueryList.nextChange([ createElmentRef(mockedIFrame as any) ]);

            expect(iFrameLoadedSpy).not.toHaveBeenCalled();
            expect(addEventListenerSpy).toHaveBeenCalled();

            // Trigger the load event with the correct page loaded.
            // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
            const triggerLoadEvent = (addEventListenerSpy.calls.argsFor(0) as any[])[1] as () => void;
            mockedIFrame.contentWindow.location.href = getTestPageUrl(1);
            triggerLoadEvent();

            expect(iFrameLoadedSpy).toHaveBeenCalledTimes(1);
            expect(iFrameLoadedSpy).toHaveBeenCalledWith(mockedIFrame);
        }),
    );

    it('sets the data-gcms-ui-styles attribute correctly on the IFrame',
        componentTest(() => TestComponent, (fixture, instance) => {
            const stylesService = TestBed.get(IFrameStylesService) as IFrameStylesService;
            instance.srcUrl = SRC_URL1;
            fixture.detectChanges();

            const iFrameElem: HTMLIFrameElement = fixture.debugElement.query(By.css('iframe')).nativeElement;
            expect(iFrameElem.dataset.gcmsUiStyles).toEqual(stylesService.stylesUrl);
        }),
    );

    it('changing width works',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.srcUrl = SRC_URL1;
            instance.iFrameWidth = INITIAL_WIDTH;
            fixture.detectChanges();

            const iFrameElem: HTMLIFrameElement = fixture.debugElement.query(By.css('iframe')).nativeElement;
            expect(iFrameElem.style.width).toEqual(INITIAL_WIDTH);

            instance.iFrameWidth = CHANGED_WIDTH;
            fixture.detectChanges();
            expect(iFrameElem.style.width).toBe(CHANGED_WIDTH);
        }),
    );

    it('changing height works',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.srcUrl = SRC_URL1;
            fixture.detectChanges();

            const iFrameElem: HTMLIFrameElement = fixture.debugElement.query(By.css('iframe')).nativeElement;
            expect(iFrameElem.style.height).toEqual(INITIAL_HEIGHT);

            instance.iFrameHeight = CHANGED_HEIGHT;
            fixture.detectChanges();
            expect(iFrameElem.style.height).toBe(CHANGED_HEIGHT);
        }),
    );

    it('changing width and height before setting srcUrl works',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.iFrameWidth = CHANGED_WIDTH;
            instance.iFrameHeight = CHANGED_HEIGHT;
            fixture.detectChanges();

            instance.srcUrl = SRC_URL1;
            fixture.detectChanges();

            const iFrameElem: HTMLIFrameElement = fixture.debugElement.query(By.css('iframe')).nativeElement;
            expect(iFrameElem.style.width).toEqual(CHANGED_WIDTH);
            expect(iFrameElem.style.height).toBe(CHANGED_HEIGHT);
        }),
    );

});

function mockQueryList(iFrameWrapper: IFrameWrapperComponent): MockQueryList<ElementRef> {
    const mockQueryList = new MockQueryList<ElementRef>();
    iFrameWrapper.iFrameList = mockQueryList as any;
    return mockQueryList;
}

const createElmentRef = (element: HTMLElement): ElementRef => ({
    nativeElement: element,
});

@Component({
    template: `
        <iframe-wrapper
            #iFrameWrapper
            [srcUrl]="srcUrl"
            [width]="iFrameWidth"
            [height]="iFrameHeight"
            (iFrameLoad)="onIFrameLoad($event)">
        </iframe-wrapper>`,
    standalone: false,
})
class TestComponent {
    @ViewChild('iFrameWrapper', { static: true })
    iFrameWrapper: IFrameWrapperComponent;

    srcUrl: string;
    iFrameHeight = INITIAL_HEIGHT;
    iFrameWidth: string;

    onIFrameLoad(): void { }
}

class MockIFrameStylesService {
    stylesUrl = 'StylesUrlForIFrame';
}

class MockQueryList<T> {
    changes = new Subject<QueryList<T>>();

    nextChange(newList: T[]): void {
        const newQueryList: Partial<QueryList<T>> = {
            length: newList.length,
            first: newList[0],
            last: newList[newList.length - 1],
        };
        this.changes.next(newQueryList as any);
    }

    notifyOnChanges(): void { }
}

class MockIFrame {
    contentDocument = {
        readyState: 'loading',
    };

    contentWindow = {
        location: {
            href: 'about:blank',
        },
    };

    style = {
        width: 0,
        height: 0,
    };

    addEventListener(): void { }
    removeEventListener(): void { }
}

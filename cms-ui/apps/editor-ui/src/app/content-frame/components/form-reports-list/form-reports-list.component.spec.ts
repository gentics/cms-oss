import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { API_BASE_URL, I18nNotificationService, I18nNumberPipe, ValuesPipe } from '@gentics/cms-components';
import { Form, FormDataListResponse, FormDownloadInfo, FormTypeConfiguration } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { GenticsUICoreModule, ModalService } from '@gentics/ui-core';
import { Observable, of } from 'rxjs';
import { configureComponentTest } from '../../../../testing/configure-component-test';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { ApplicationStateService } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { FormReportsListComponent } from './form-reports-list.component';

const FORM_ID = 42;
const ENTRY_UUID = 'entry-uuid-1';
const FIELD_SINGLE = 'upload_single';
const FIELD_MULTI = 'upload_multi';
const FILE_NAME_SINGLE = 'single.pdf';
const FILE_NAME_MULTI = 'multi.png';

const DOWNLOAD_INFO: FormDownloadInfo = {
    downloadReady: false,
    requestPending: false,
};

const LIST_RESPONSE: FormDataListResponse = {
    totalCount: 1,
    currentPage: 1,
    pageCount: 1,
    perPage: 15,
    elements: {
        [FIELD_SINGLE]: {
            active: true, elements: [], globalId: FIELD_SINGLE, mandatory: false, multivalue: false, type: 'file',
        },
        [FIELD_MULTI]: {
            active: true, elements: [], globalId: FIELD_MULTI, mandatory: false, multivalue: true, type: 'file',
        },
    },
    entries: [{
        uuid: ENTRY_UUID,
        fields: {
            [FIELD_SINGLE]: { fileName: FILE_NAME_SINGLE },
            [FIELD_MULTI]: [{ fileName: FILE_NAME_MULTI }],
        },
    }],
};

class MockClient {
    form = {
        binariesStatus: (): Observable<FormDownloadInfo> => of(DOWNLOAD_INFO),
        exportStatus: (): Observable<FormDownloadInfo> => of(DOWNLOAD_INFO),
        listData: (): Observable<FormDataListResponse> => of(LIST_RESPONSE),
    };
}

class MockModalService {}
class MockNotificationService {
    show(): void {}
}
class MockErrorHandler {
    catch(): void {}
}

describe('FormReportsListComponent', () => {
    let fixture: ComponentFixture<FormReportsListComponent>;

    beforeEach(async () => {
        configureComponentTest({
            imports: [GenticsUICoreModule.forRoot()],
            declarations: [FormReportsListComponent, ValuesPipe, I18nNumberPipe],
            providers: [
                { provide: GCMSRestClientService, useClass: MockClient },
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: ModalService, useClass: MockModalService },
                { provide: I18nNotificationService, useClass: MockNotificationService },
                { provide: ErrorHandler, useClass: MockErrorHandler },
            ],
        });
        await TestBed.compileComponents();

        fixture = TestBed.createComponent(FormReportsListComponent);
        fixture.componentRef.setInput('form', { id: FORM_ID, data: {} } as Partial<Form> as Form);
        fixture.componentRef.setInput('formConfig', { controls: {} } as Partial<FormTypeConfiguration> as FormTypeConfiguration);
        // All mocked client calls emit synchronously. `whenStable()` must not be awaited here,
        // as the 30s status polling interval of the component keeps the zone unstable.
        fixture.detectChanges();
        fixture.detectChanges();
    });

    afterEach(() => {
        // Stops the 30s status polling interval of the component
        fixture.destroy();
    });

    function getBinaryLinks(): HTMLAnchorElement[] {
        return fixture.debugElement
            .queryAll(By.css('gtx-table .data-row a.form-value.type-binary'))
            .map((el) => el.nativeElement as HTMLAnchorElement);
    }

    /**
     * Dispatches a cancelable click on the link and returns whether the browser would still
     * perform the default action (i.e. follow the link).
     * The link target is redirected to an in-page fragment beforehand, so that following it
     * does not open a window or leave the test page.
     */
    function clickLinkAndCheckDefaultAction(link: HTMLAnchorElement): boolean {
        link.removeAttribute('target');
        link.setAttribute('href', '#form-reports-list-spec');

        const event = new MouseEvent('click', { bubbles: true, cancelable: true });
        const notCanceled = link.dispatchEvent(event);

        return notCanceled && !event.defaultPrevented;
    }

    it('renders the binary values as links to the binary download endpoint', () => {
        const links = getBinaryLinks();

        expect(links.map((link) => link.textContent.trim())).toEqual([FILE_NAME_SINGLE, FILE_NAME_MULTI]);
        expect(links.map((link) => link.getAttribute('href'))).toEqual([
            `${API_BASE_URL}/form/${FORM_ID}/data/${ENTRY_UUID}/binary/${FIELD_SINGLE}`,
            `${API_BASE_URL}/form/${FORM_ID}/data/${ENTRY_UUID}/binary/${FIELD_MULTI}/0`,
        ]);
        expect(links.map((link) => link.getAttribute('target'))).toEqual(['_blank', '_blank']);
    });

    it('does not cancel the default action of a click on a single-value binary link', () => {
        const [singleLink] = getBinaryLinks();

        expect(clickLinkAndCheckDefaultAction(singleLink)).toBe(true);
    });

    it('does not cancel the default action of a click on a multi-value binary link', () => {
        const [, multiLink] = getBinaryLinks();

        expect(clickLinkAndCheckDefaultAction(multiLink)).toBe(true);
    });
});

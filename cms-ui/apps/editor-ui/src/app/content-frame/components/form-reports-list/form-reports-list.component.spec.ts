import { Component, ErrorHandler, Pipe, ViewChild } from '@angular/core';
import { TestBed, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormReportsListComponent } from '@editor-ui/app/content-frame/components/form-reports-list/form-reports-list.component';
import { Api } from '@editor-ui/app/core/providers/api';
import { EntityResolver } from '@editor-ui/app/core/providers/entity-resolver/entity-resolver';
import { MockErrorHandler } from '@editor-ui/app/core/providers/error-handler/error-handler.mock';
import { I18nNotification } from '@editor-ui/app/core/providers/i18n-notification/i18n-notification.service';
import { SharedModule } from '@editor-ui/app/shared/shared.module';
import { ApplicationStateService } from '@editor-ui/app/state';
import { TestApplicationState } from '@editor-ui/app/state/test-application-state.mock';
import { FormDownloadInfo } from '@gentics/cms-models';
import { getExampleFormDataNormalized, getExampleReports } from '@gentics/cms-models/testing/test-data.mock';
import { FormEditorService, FormReportService } from '@gentics/form-generator';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { of } from 'rxjs';
import { componentTest, configureComponentTest } from '../../../../testing';

const MOCK_EXPORT_DOWNLOAD_INFO: FormDownloadInfo = {
    requestPending: false,
    downloadReady: true,
    downloadUuid: '291b80fc0f344b4abc15ff7fb0487da1',
    downloadTimestamp: '2023-05-13T18:43:22',
};

const MOCK_BINARY_DOWNLOAD_INFO: FormDownloadInfo = {
    requestPending: true,
    downloadReady: false,
    downloadUuid: null,
    downloadTimestamp: null,
};

describe('FormReportListComponent', () => {
    let state: TestApplicationState;

    beforeEach(() => {
        configureComponentTest({
            providers: [
                {provide: Api, useClass: MockApi},
                {provide: ApplicationStateService, useClass: TestApplicationState},
                {provide: I18nNotification, useClass: MockI18nNotification},
                {provide: FormEditorService, useClass: MockFormEditorService},
                {provide: FormReportService, useClass: MockFormReportService},
                {provide: ErrorHandler, useClass: MockErrorHandler},
                {provide: EntityResolver, useClass: MockEntityResolver},
            ],
            declarations: [
                MockI18nDatePipe,
                FormReportsListComponent,
                TestComponent,
            ],
            imports: [
                GenticsUICoreModule.forRoot(),
                FormsModule,
                SharedModule,
                BrowserAnimationsModule,
            ],
        });
        state = TestBed.get(ApplicationStateService);
        state.mockState({
            auth: {
                sid: 123,
            },
        })
    });

    it('loads the status from the api on init',
        componentTest(() => TestComponent, (fixture, testComponent) => {
            fixture.detectChanges();

            tick();

            const list = testComponent.formReportsList;
            expect(list.binaryStatus).toEqual(MOCK_BINARY_DOWNLOAD_INFO);
            expect(list.exportStatus).toEqual(MOCK_EXPORT_DOWNLOAD_INFO);
        }),
    );

})

@Component({
    selector: 'test-component',
    template: `
        <form-reports-list [form]="form"></form-reports-list>`,
    })
class TestComponent {
    @ViewChild(FormReportsListComponent, {static: true})
    formReportsList: FormReportsListComponent;

    form = getExampleFormDataNormalized();
}

class MockApi {
    forms = {
        getReports: jasmine.createSpy('getReports').and.returnValue(of(getExampleReports())),
        getBinaryStatus: jasmine.createSpy('getBinaryStatus').and.returnValue(of(MOCK_BINARY_DOWNLOAD_INFO)),
        getExportStatus: jasmine.createSpy('getExportStatus').and.returnValue(of(MOCK_EXPORT_DOWNLOAD_INFO)),
    };
}

@Pipe({ name: 'i18nDate' })
class MockI18nDatePipe {
    transform(val: any): any {
        return val;
    }
}

class MockI18nNotification {
    show = jasmine.createSpy('show').and.stub();
}

class MockFormEditorService {
    activeContentLanguageCode$ = of('en');
    activeUiLanguageCode$ = of('en');
}

class MockFormReportService {
    getFormElementLabelPropertyValues(): any {
        return of({type: 'input'});
    }
}

class MockEntityResolver {
    getLanguage(): any {
        return {language: 'en'}
    }
}

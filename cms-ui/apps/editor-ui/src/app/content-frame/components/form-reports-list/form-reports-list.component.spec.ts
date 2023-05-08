import {TestBed} from '@angular/core/testing';
import {FormReportsListComponent} from '@editor-ui/app/content-frame/components/form-reports-list/form-reports-list.component';
import {TestApplicationState} from '@editor-ui/app/state/test-application-state.mock';
import {Component, ErrorHandler, ViewChild} from '@angular/core';
import {MockErrorHandler} from '@editor-ui/app/core/providers/error-handler/error-handler.mock';
import {getExampleFormDataNormalized, getExampleReports} from '@gentics/cms-models/lib/testing/test-data.mock';
import {of} from 'rxjs';
import {Api} from '@editor-ui/app/core/providers/api';
import {ApplicationStateService} from '@editor-ui/app/state';
import {EntityResolver} from '@editor-ui/app/core/providers/entity-resolver/entity-resolver';
import {I18nNotification} from '@editor-ui/app/core/providers/i18n-notification/i18n-notification.service';
import {GenticsUICoreModule} from '@gentics/ui-core';
import {FormsModule} from '@angular/forms';
import {SharedModule} from '@editor-ui/app/shared/shared.module';
import {FormEditorService, FormReportService} from '@gentics/form-generator';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {By} from '@angular/platform-browser';
import {API_BASE_URL} from '@editor-ui/app/common/utils/base-urls';
import {componentTest, configureComponentTest} from '../../../../testing';

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

    it('adds href to file fields',
        componentTest(() => TestComponent, (fixture, testComponent) => {
            fixture.detectChanges();
            const formId = testComponent.form.id;
            const sid = state.now.auth.sid;

            const firstEntryFileField = fixture.debugElement.query(By.css('table tbody tr:nth-child(2) td:nth-child(3) a'));
            expect(firstEntryFileField.attributes.href).toBe(
                `${API_BASE_URL}/form/${formId}/data/291b80fc0f344b4abc15ff7fb0487da1/binary/file_127d4c49_9415_4ef8_aa1a_0aa96d1b94ce?sid=${sid}`);

            const secondEntryFileField = fixture.debugElement.query(By.css('table tbody tr:nth-child(3) td:nth-child(3) a'));
            expect(secondEntryFileField.attributes.href).toBe(
                `${API_BASE_URL}/form/${formId}/data/72a7334c56c641fe93a44b10bdffc98f/binary/file_127d4c49_9415_4ef8_aa1a_0aa96d1b94ce?sid=${sid}`);
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
    };
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

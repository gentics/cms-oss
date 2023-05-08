import { InterfaceOf, ObservableStopper } from '@admin-ui/common';
import { AppStateService } from '@admin-ui/state/providers/app-state/app-state.service';
import { TestAppState } from '@admin-ui/state/utils/test-app-state';
import { createDelayedError } from '@admin-ui/testing';
import { fakeAsync, TestBed } from '@angular/core/testing';
import { RecursivePartial } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Store } from '@ngxs/store';
import { EntityManagerService } from '../../entity-manager';
import { MockEntityManagerService } from '../../entity-manager/entity-manager.service.mock';
import { ErrorHandler } from '../../error-handler';
import { MockErrorHandler } from '../../error-handler/error-handler.mock';
import { I18nNotificationService } from '../../i18n-notification/i18n-notification.service';
import { MockI18nNotificationService } from '../../i18n-notification/i18n-notification.service.mock';
import { LanguageOperations } from './language.operations';

class MockApi implements RecursivePartial<InterfaceOf<GcmsApi>> {
    i18n = {
        setActiveUiLanguage: jasmine.createSpy('setActiveUiLanguage').and.stub(),
    };
}

class MockStore {}

describe('LanguageOperations', () => {
    let api: MockApi;
    let errorHandler: MockErrorHandler;
    let languageOperations: LanguageOperations;
    let stopper: ObservableStopper;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                LanguageOperations,
                { provide: AppStateService, useClass: TestAppState },
                { provide: EntityManagerService, useClass: MockEntityManagerService },
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: GcmsApi, useClass: MockApi },
                { provide: I18nNotificationService, useClass: MockI18nNotificationService },
                { provide: Store, useClass: MockStore },
            ],
        });

        api = TestBed.get(GcmsApi);
        errorHandler = TestBed.get(ErrorHandler);
        languageOperations = TestBed.get(LanguageOperations);
        stopper = new ObservableStopper();
    });

    afterEach(() => {
        stopper.stop();
    });

    describe('setBackendLanguage()', () => {

        it('notifies the user about errors and rethrows them', fakeAsync(() => {
            const error = new Error('Test Error');
            api.i18n.setActiveUiLanguage.and.returnValue(createDelayedError(error));
            errorHandler.assertNotifyAndRethrowIsCalled(languageOperations.setActiveUiLanguage('en'), error);
        }));

    });


});

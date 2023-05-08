import { TestBed } from '@angular/core/testing';
import { TestApplicationState } from '@editor-ui/app/state/test-application-state.mock';
import { Page, Raw } from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';
import { configureComponentTest } from '../../../../testing';
import { getExampleFormData, getExamplePageData } from '../../../../testing/test-data.mock';
import { I18nDatePipe } from '../../../shared/pipes/i18n-date/i18n-date.pipe';
import { ApplicationStateService, FolderActionsService, STATE_MODULES } from '../../../state';
import { getFormattedTimeMgmtValue } from './i18n-utils';
import { I18nService } from './i18n.service';

class MockI18nService {
    translate = jasmine.createSpy('translate').and.returnValue('RETURN_VALUE')
}

class MockI18nDatePipe {
    transform(value: Date | number, format: any = 'date'): string {
        return '';
    }
}

class MockFolderActions {
    getPage = jasmine.createSpy('getPage').and.returnValue(Promise.resolve( getExamplePageData({ id: 1 }) ));
    getForm = jasmine.createSpy('getForm').and.returnValue(Promise.resolve( getExampleFormData({ id: 2 }) ));
}

describe('getFormattedTimeMgmtValue()', () => {

    let i18nService: I18nService;
    let i18nPipe: I18nDatePipe;
    let folderActions: FolderActionsService;

    beforeEach(() => {
        configureComponentTest({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: I18nService, useClass: MockI18nService },
                { provide: FolderActionsService, useClass: MockFolderActions },
            ],
        });

        i18nService = TestBed.inject(I18nService);
        folderActions = TestBed.inject(FolderActionsService);
        i18nPipe = new MockI18nDatePipe() as I18nDatePipe;
    });

    it('can handle migrated timemanagement data', () => {

        const testPage: Page<Raw> = getExamplePageData({ id: 1 })
        testPage.timeManagement.at = 1627720500;
        testPage.versions = [{
            editor: { id: 3, firstName: 'Node', lastName: 'Admin', email: 'nowhere@gentics.com' },
            number: '2.0',
            timestamp: 1626777808,
        }];
        delete testPage.timeManagement.version;

        getFormattedTimeMgmtValue(
            testPage,
            'at',
            1,
            i18nService,
            i18nPipe,
            folderActions,
        ).subscribe(result => {
            console.log(result);
            expect(result).toBe('RETURN_VALUE');
        });
    });

});

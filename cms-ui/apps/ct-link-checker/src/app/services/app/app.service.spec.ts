import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { GcmsUiLanguage } from '@gentics/cms-integration-api-models';
import { ApiBase } from '@gentics/cms-rest-clients-angular';
import { TranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { AppSettings } from '../../common/models/app-settings';
import { GcmsAuthenticationService } from '../../core/services/authentication/gcms-authentication.service';
import { FilterService } from '../filter/filter.service';
import { UserSettingsService } from '../user-settings/user-settings.service';
import { AppService } from './app.service';

class MockTranslateService {
    setDefaultLang = jasmine.createSpy('setDefaultLang');
}

class MockGcmsAuthenticationService {
    getSid = jasmine.createSpy('getSid').and.returnValue(
        of({})
    );
    init = jasmine.createSpy('init');
}

class MockUserSettings {
    getUserSettings = jasmine.createSpy('getUserSettings').and.returnValue(
        of({})
    );
    getUserLanguage = jasmine.createSpy('getUserLanguage').and.returnValue(
        of({})
    );
}

describe('AppService', () => {
    let appService: AppService;
    let userSettings: UserSettingsService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                { provide: GcmsAuthenticationService, useClass: MockGcmsAuthenticationService },
                { provide: ApiBase },
                AppService,
                FilterService,
                { provide: TranslateService, useClass: MockTranslateService },
                { provide: UserSettingsService, useClass: MockUserSettings }
            ]
        });

        appService = TestBed.inject(AppService);
        userSettings = TestBed.inject(UserSettingsService);
    });

    it('should be created', () => {
        expect(appService).toBeTruthy();
    });

    it('should build correctly from default settings', fakeAsync(() => {
        const defaultLanguage: GcmsUiLanguage = 'en';
        const defaultAppSettings: AppSettings = {
            sid: null,
            language: defaultLanguage as any,
            displayFields: []
        };

        appService.init();

        tick();

        expect(appService.settings).toEqual(jasmine.objectContaining(defaultAppSettings));
    }));

    it('should call UserSettings and save the right properties', fakeAsync(() => {
        const displayFields = ['someTest'];
        appService.init();

        tick();

        expect(userSettings.getUserSettings).toHaveBeenCalledTimes(1);
        expect(userSettings.getUserLanguage).toHaveBeenCalledTimes(1);

        appService.setDisplayFields(displayFields);

        expect(appService.settings.displayFields).toEqual(displayFields);
    }));
});

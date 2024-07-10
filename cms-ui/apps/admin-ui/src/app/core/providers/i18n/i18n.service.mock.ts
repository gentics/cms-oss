import { InterfaceOf } from '@admin-ui/common/utils/util-types/util-types';
import { GcmsUiLanguage } from '@gentics/cms-integration-api-models';
import { Observable, of as observableOf } from 'rxjs';
import { I18nService, JoinOptions } from './i18n.service';

export class MockI18nService implements InterfaceOf<Omit<I18nService, 'ngOnDestroy'>> {

    instant(key: string): string {
        return key;
    }

    get(key: string): Observable<string> {
        return observableOf(this.instant(key));
    }

    setLanguage(): void { }

    inferUserLanguage(): GcmsUiLanguage {
        return 'en';
    }

    join(parts: string[], options?: JoinOptions): string {
        return parts.join(', ');
    }

}

export class MockI18nServiceWithSpies implements InterfaceOf<Omit<I18nService, 'ngOnDestroy'>>  {

    instant = jasmine.createSpy('instant').and.callFake(key => key);

    get = jasmine.createSpy('get').and.callFake(key => observableOf(this.instant(key)));

    setLanguage = jasmine.createSpy('setLanguage').and.stub();

    inferUserLanguage = jasmine.createSpy('inferUserLanguage').and.returnValue('en');

    join = jasmine.createSpy('join').and.callFake(values => values.join(', '));

}

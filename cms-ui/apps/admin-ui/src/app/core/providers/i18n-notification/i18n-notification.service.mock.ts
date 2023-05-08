import { InterfaceOf } from '@admin-ui/common/utils/util-types/util-types';
import { Omit } from '@gentics/cms-models';
import { I18nNotificationService } from './i18n-notification.service';

export class MockI18nNotificationService implements InterfaceOf<Omit<I18nNotificationService, 'ngOnDestroy'>> {

    show = jasmine.createSpy('show').and.stub();

    destroyAllToasts(): void {}

}

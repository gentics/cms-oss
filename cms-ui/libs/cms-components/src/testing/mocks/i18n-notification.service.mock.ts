import { TranslatedNotificationOptions } from '../../lib/common/models';
import { I18nNotificationService } from '../../lib/core/providers/i18n-notification/i18n-notification.service';

export class MockI18nNotificationService implements Required<I18nNotificationService> {
    show(_options: TranslatedNotificationOptions): { dismiss: () => void } {
        return {
            dismiss: () => {},
        };
    }

    destroyAllToasts(): void {}
}

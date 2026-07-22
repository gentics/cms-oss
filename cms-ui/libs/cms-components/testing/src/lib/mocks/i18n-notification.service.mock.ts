import { I18nNotificationService, TranslatedNotificationOptions } from '@gentics/cms-components';
import { Response } from '@gentics/cms-models';

export class MockI18nNotificationService implements Required<I18nNotificationService> {
    showFromResponse(_response: Response): void {
    }

    show(_options: TranslatedNotificationOptions): { dismiss: () => void } {
        return {
            dismiss: () => {},
        };
    }

    destroyAllToasts(): void {}
}

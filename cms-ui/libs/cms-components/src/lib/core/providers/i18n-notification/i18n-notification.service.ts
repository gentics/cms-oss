import { Injectable } from '@angular/core';
import { Response } from '@gentics/cms-models';
import { INotificationOptions, NotificationService } from '@gentics/ui-core';
import { responseMessageToNotification } from '../../../common/utils';
import { I18nService } from '../i18n/i18n.service';

export interface TranslatedNotificationOptions extends INotificationOptions {
    translationParams?: { [key: string]: any };
}

/**
 * A drop-in replacement for the GUIC Notification service, which is able to transparently
 * translate translation keys passed in the `message` property of the options object.
 */
@Injectable()
export class I18nNotificationService {

    constructor(
        private notification: NotificationService,
        private i18n: I18nService,
    ) { }

    /**
     * Display a toast with the `message` property being passed through the I18nService#translate()
     * method. Optional translation parameters can be provided.
     */
    show(options: TranslatedNotificationOptions): { dismiss: () => void } {
        options.message = this.i18n.instant(options.message, options.translationParams);
        if (options.action && options.action.label) {
            options.action.label = this.i18n.instant(options.action.label, options.translationParams);
        }
        return this.notification.show(options);
    }

    // Should probably be moved to a shared service instead
    showFromResponse(response: Response): void {
        if (response.messages?.length > 0) {
            for (const msg of response.messages) {
                this.notification.show(responseMessageToNotification(msg));
            }
        }
    }

    destroyAllToasts(): void {
        this.notification.destroyAllToasts();
    }
}

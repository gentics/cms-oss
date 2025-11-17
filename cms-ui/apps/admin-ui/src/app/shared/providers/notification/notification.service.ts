import { Injectable } from '@angular/core';
import { I18nNotificationService } from '@gentics/cms-components';

@Injectable()
export class NotificationService {
    constructor(protected notification: I18nNotificationService) {}

    notificationNoneSelected(): void {
        this.notification.show({
            type: 'warning',
            message: 'shared.no_row_selected_warning',
        });
    }
}

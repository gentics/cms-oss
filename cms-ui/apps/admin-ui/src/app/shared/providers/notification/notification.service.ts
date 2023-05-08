import { I18nNotificationService } from '@admin-ui/core';
import { Injectable } from '@angular/core';



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

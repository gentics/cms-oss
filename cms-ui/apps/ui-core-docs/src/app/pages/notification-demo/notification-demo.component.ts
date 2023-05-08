import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ColorThemes, NotificationService } from '@gentics/ui-core';
import { IDocumentation } from '../../common/docs';
import { InjectDocumentation } from '../../common/docs-loader';

@Component({
    templateUrl: './notification-demo.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotificationDemoPage {

    @InjectDocumentation('notification.service')
    documentation: IDocumentation;

    message = 'Hello, this is Toast.';
    multilineMessage = 'Notifications may have'
        + '\nmulti-line messages'
        + '\n    and white-space for indentation'
        + '\nas well, but auto-wrap when lines are too long';
    delay = 3000;
    type: ColorThemes | 'default' = 'default';

    constructor(private notification: NotificationService) {}

    showBasic(): void {
        this.notification.show({
            message: this.message,
            type: this.type,
            delay: this.delay,
        });
    }

    showWithAction(): void {
        this.notification.show({
            message: 'Email sent',
            action: {
                label: 'Undo',
                onClick: (): any =>  this.notification.show({
                    message: 'Cancelled sending',
                    type: 'success',
                }),
            },
        });
    }

    showMultiline(): void {
        const toast = this.notification.show({
            message: this.multilineMessage,
            type: this.type,
            delay: 10000,
            action: {
                label: 'Dismiss',
                onClick: () => toast.dismiss(),
            },
        });
    }

}

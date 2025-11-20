import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { I18nNotificationService, MessageLink } from '@gentics/cms-components';
import { Message, Node, Normalized } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { IModalDialog } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { EntityManagerService } from '../../providers';

@Component({
    selector: 'gtx-message-modal',
    templateUrl: './message-modal.tpl.html',
    styleUrls: ['./message-modal.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class MessageModalComponent implements IModalDialog {
    @Input() message: Message<Normalized>;

    nodes$: Observable<Node[]>;
    displayReplyInput = false;
    sendMessageText: string;

    constructor(
        private entityManager: EntityManagerService,
        private api: GcmsApi,
        private notification: I18nNotificationService,
    ) {}

    getFullName(userId: number): Observable<string> {
        const user$ = this.entityManager.getEntity('user', userId);
        return user$.pipe(
            map((user) => user ? user.firstName + ' ' + user.lastName : ''),
        );
    }

    sendMessage(): void {
        this.api.messages.sendMessage(this.transformValuesForApi(this.sendMessageText, this.message.sender)).subscribe(() => {
            this.notification.show({
                message: 'shared.message_sent',
                type: 'success',
            });
        }, (error) => {
            this.notification.show({
                message: 'shared.message_sent_error',
                type: 'alert',
                delay: 5000,
            });
            this.closeFn();
        }, () => this.closeFn(),
        );
    }

    transformValuesForApi(sendMessageText: string, recipient: number): any {
        return {
            message: sendMessageText,
            toUserId: [recipient],
        };
    }

    closeFn = (val?: MessageLink) => {};
    cancelFn = (val?: any) => {};

    registerCloseFn(close: (val?: MessageLink) => void): void {
        this.closeFn = close;
        this.cancelFn = () => this.closeFn();
    }

    registerCancelFn(cancel: (val?: any) => void): void { }
}

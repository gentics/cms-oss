import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { Message, Node, Normalized } from '@gentics/cms-models';
import { IModalDialog } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { ApplicationStateService } from '../../../state';
import { Api } from '../../providers/api/api.service';
import { EntityResolver } from '../../providers/entity-resolver/entity-resolver';
import { I18nNotification } from '../../providers/i18n-notification/i18n-notification.service';
import { MessageLink } from '../message-body';

@Component({
    selector: 'message-modal',
    templateUrl: './message-modal.tpl.html',
    styleUrls: ['./message-modal.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class MessageModal implements IModalDialog {
    @Input() message: Message<Normalized>;

    nodes$: Observable<Node[]>;
    displayReplyInput = false;
    sendMessageText: string;

    constructor(private appState: ApplicationStateService,
                private entityResolver: EntityResolver,
                private api: Api,
                private notification: I18nNotification) {
    }

    ngOnInit(): void {
        this.nodes$ = this.appState.select(state => state.folder.nodes.list)
            .map(nodeIds => nodeIds.map(id => this.entityResolver.getNode(id)));

    }

    getFullName(userId: number): string {
        let user = this.entityResolver.getUser(userId);
        return user.firstName + ' ' + user.lastName;
    }

    sendMessage(): void {
        this.api.messages.sendMessage(this.transformValuesForApi(this.sendMessageText, this.message.sender)).subscribe(() => {
            this.notification.show({
                message: 'message.message_sent',
                type: 'success'
            });
        }, error => {
            this.notification.show({
                message: 'message.message_sent_error',
                type: 'alert',
                delay: 5000
            });
            this.closeFn();
        }, () => this.closeFn()
        );
    }

    transformValuesForApi(sendMessageText: string, recipient: number): any {
        return {
            'message':  sendMessageText,
            'toUserId': [recipient]
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

import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';
import { Message, Node, Normalized } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApplicationStateService } from '../../../state';
import { Api } from '../../providers/api/api.service';
import { EntityResolver } from '../../providers/entity-resolver/entity-resolver';
import { I18nNotification } from '../../providers/i18n-notification/i18n-notification.service';
import { MessageLink } from '../message-body';

@Component({
    selector: 'message-modal',
    templateUrl: './message-modal.component.html',
    styleUrls: ['./message-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class MessageModal extends BaseModal<MessageLink | void> implements OnInit {

    @Input()
    message: Message<Normalized>;

    nodes$: Observable<Node[]>;
    displayReplyInput = false;
    sendMessageText = '';

    constructor(
        private appState: ApplicationStateService,
        private entityResolver: EntityResolver,
        private api: Api,
        private notification: I18nNotification,
    ) {
        super();
    }

    ngOnInit(): void {
        this.nodes$ = this.appState.select(state => state.folder.nodes.list).pipe(
            map(nodeIds => nodeIds.map(id => this.entityResolver.getNode(id))),
        );
    }

    getFullName(userId: number): string {
        const user = this.entityResolver.getUser(userId);
        return user.firstName + ' ' + user.lastName;
    }

    sendMessage(): void {
        this.api.messages.sendMessage(this.transformValuesForApi(this.sendMessageText, this.message.sender)).subscribe(() => {
            this.notification.show({
                message: 'message.message_sent',
                type: 'success',
            });
            this.closeFn();
        }, error => {
            this.notification.show({
                message: 'message.message_sent_error',
                type: 'alert',
                delay: 5000,
            });
            console.error('Error while sending message response', error);
        });
    }

    transformValuesForApi(sendMessageText: string, recipient: number): any {
        return {
            message:  sendMessageText,
            toUserId: [recipient],
        };
    }
}

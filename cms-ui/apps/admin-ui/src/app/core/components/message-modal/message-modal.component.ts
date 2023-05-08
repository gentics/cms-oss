import { ChangeDetectionStrategy, Component, ElementRef, Input, OnInit, ViewChild } from '@angular/core';
import { IModalDialog } from '@gentics/ui-core';
import { BehaviorSubject, Observable } from 'rxjs';

import { AppStateService } from '@admin-ui/state';
import { Message, Node, Normalized } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { map } from 'rxjs/operators';
import { EntityManagerService, I18nNotificationService } from '../../providers';
import { MessageLink } from '../message-body';

@Component({
    selector: 'gtx-message-modal',
    templateUrl: './message-modal.tpl.html',
    styleUrls: ['./message-modal.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MessageModalComponent implements IModalDialog, OnInit {
    @Input() message: Message<Normalized>;

    nodes$: Observable<Node[]>;
    displayReplyInput = false;
    sendMessageText: string;

    constructor(
        private appState: AppStateService,
        private entityManager: EntityManagerService,
        private api: GcmsApi,
        private notification: I18nNotificationService,
    ) {}

    ngOnInit(): void {
        /*this.nodes$ = this.appState.select(state => state.folder.nodes.list)
            .map(nodeIds => nodeIds.map(id => this.entityResolver.getNode(id)));*/

    }

    getFullName(userId: number): Observable<string> {
        const user$ = this.entityManager.getEntity('user', userId);
        return user$.pipe(
            map(user => user ? user.firstName + ' ' + user.lastName : ''),
        );
    }

    sendMessage(): void {
        this.api.messages.sendMessage(this.transformValuesForApi(this.sendMessageText, this.message.sender)).subscribe(() => {
            this.notification.show({
                message: 'message.message_sent',
                type: 'success',
            });
        }, error => {
            this.notification.show({
                message: 'message.message_sent_error',
                type: 'alert',
                delay: 5000,
            });
            this.closeFn();
        }, () => this.closeFn(),
        );
    }

    transformValuesForApi(sendMessageText: string, recipient: number): any {
        return {
            message:  sendMessageText,
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

import { Component, OnInit } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { SendMessageForm } from '@editor-ui/app/common/models';
import { Group, SendMessageRequest, User } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Api } from '../../../core/providers/api/api.service';
import { I18nNotification } from '../../../core/providers/i18n-notification/i18n-notification.service';


const DEFAULT_INSTANT_TIME_MINUTES = 2;

@Component({
    selector: 'send-message-modal',
    templateUrl: './send-message-modal.tpl.html',
    styleUrls: ['./send-message-modal.scss'],
    standalone: false
})
export class SendMessageModal extends BaseModal<any> implements OnInit {

    users$: Observable<User[]>;
    groups$: Observable<Group[]>;

    form: UntypedFormControl;

    constructor(
        private api: Api,
        private notification: I18nNotification,
    ) {
        super();
    }

    ngOnInit(): void {
        this.users$ = this.api.user.getUsers().pipe(
            map(response => response.items),
        );
        this.groups$ = this.api.group.getGroupsTree().pipe(
            map(response => response.groups),
        );
        this.form = new UntypedFormControl({});
    }

    okayClicked(): void {
        const messageRequest = this.transformValuesForApi(this.form.value);

        this.api.messages.sendMessage(messageRequest).subscribe(() => {
            this.notification.show({
                message: 'message.message_sent',
                type: 'success',
                delay: 5000,
            });
            this.closeFn(true);
        }, error => {
            this.notification.show({
                message: 'message.message_sent_error',
                type: 'alert',
                delay: 5000,
            });
        });
    }

    transformValuesForApi(formValues: SendMessageForm): SendMessageRequest {
        const INSTANT_TIME = formValues.isInstant ? DEFAULT_INSTANT_TIME_MINUTES : 0;

        return formValues.recipientIds.reduce((acc: any, selectedId: string) => {
            const [key, value] = selectedId.split('_');
            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
            acc[key].push(value);
            return acc;
        }, {
            message: formValues.message,
            toGroupId: [],
            toUserId: [],
            instantTimeMinutes: INSTANT_TIME,
        });
    }
}

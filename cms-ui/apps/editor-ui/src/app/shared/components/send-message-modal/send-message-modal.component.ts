import { Component, ViewChild } from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';
import { Group, SendMessageRequest, User } from '@gentics/cms-models';
import { IModalDialog } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { Api } from '../../../core/providers/api/api.service';
import { I18nNotification } from '../../../core/providers/i18n-notification/i18n-notification.service';
import { SendMessageForm, SendMessageFormValue } from '../send-message-form/send-message-form.component';

@Component({
    selector: 'send-message-modal',
    templateUrl: './send-message-modal.tpl.html',
    styleUrls: ['./send-message-modal.scss']
    })
export class SendMessageModal implements IModalDialog {
    users$: Observable<User[]>;
    groups$: Observable<Group[]>;

    form: UntypedFormGroup;
    @ViewChild(SendMessageForm, { static: true }) private sendMessageForm: SendMessageForm;

    constructor(
        private api: Api,
        private notification: I18nNotification,
    ) {
    }

    ngOnInit(): void {
        this.users$ = this.api.user.getUsers().map(response => response.items);
        this.groups$ = this.api.group.getGroupsTree().map(response => response.groups);
    }

    ngAfterViewInit(): void {
        this.form = this.sendMessageForm.form;
    }

    okayClicked(): void {
        this.api.messages.sendMessage(this.transformValuesForApi(this.form.value)).subscribe(() => {
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
            this.closeFn(true);
        }, () => this.closeFn(true),
        );
    }

    transformValuesForApi(formValues: SendMessageFormValue): SendMessageRequest {
        return formValues.recipientIds.reduce((acc: any, selectedId: string) => {
            const [key, value] = selectedId.split('_');
            acc[key].push(value);
            return acc;
        }, {
            message:  formValues.message,
            toGroupId: [],
            toUserId: [],
        });
    }

    closeFn = (val: any) => {};
    cancelFn = () => {};

    registerCloseFn(close: (val: any) => void): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: (val?: any) => void): void {
        this.cancelFn = cancel;
    }
}

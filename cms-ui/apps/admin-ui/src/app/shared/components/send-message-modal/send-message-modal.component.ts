import { AfterViewInit, Component, OnInit, ViewChild } from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';
import { IModalDialog } from '@gentics/ui-core';
import { Observable } from 'rxjs';

import { I18nNotificationService } from '@admin-ui/core';
import { Group, SendMessageRequest, User } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { map } from 'rxjs/operators';
import { SendMessageFormComponent, SendMessageFormValue } from '../send-message-form/send-message-form.component';


const DEFAULT_INSTANT_TIME_MINUTES = 2;

@Component({
    selector: 'gtx-send-message-modal',
    templateUrl: './send-message-modal.tpl.html',
    styleUrls: ['./send-message-modal.scss'],
    standalone: false
})
export class SendMessageModalComponent implements IModalDialog, OnInit, AfterViewInit {
    users$: Observable<User[]>;
    groups$: Observable<Group[]>;

    form: UntypedFormGroup;
    @ViewChild(SendMessageFormComponent) private sendMessageForm: SendMessageFormComponent;

    constructor(private api: GcmsApi,
        private notification: I18nNotificationService) {
    }

    ngOnInit(): void {
        this.users$ = this.api.user.getUsers().pipe(map(response => response.items));
        this.groups$ = this.api.group.getGroupsTree().pipe(map(response => response.groups));
    }

    ngAfterViewInit(): void {
        this.form = this.sendMessageForm.form;
    }

    okayClicked(): void {
        this.api.messages.sendMessage(this.transformValuesForApi(this.form.value)).subscribe(() => {
            this.notification.show({
                message: 'shared.message_send',
                type: 'success',
            });
        }, error => {
            this.notification.show({
                message: 'shared.message_sent_error',
                type: 'alert',
                delay: 5000,
            });
            this.closeFn(true);
        }, () => this.closeFn(true),
        );
    }

    transformValuesForApi(formValues: SendMessageFormValue): SendMessageRequest {
        const INSTANT_TIME = formValues.isInstant ? DEFAULT_INSTANT_TIME_MINUTES : 0;

        return formValues.recipientIds.reduce((acc: any, selectedId: string) => {
            const [key, value] = selectedId.split('_');
            acc[key].push(value);
            return acc;
        }, {
            message:  formValues.message,
            instantTimeMinutes: INSTANT_TIME,
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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { SendMessageForm } from '@editor-ui/app/common/models';
import { Group, SendMessageRequest, User } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { BaseModal } from '@gentics/ui-core';
import { Subscription } from 'rxjs';
import { I18nNotification } from '../../../core/providers/i18n-notification/i18n-notification.service';

const DEFAULT_INSTANT_TIME_MINUTES = 2;

@Component({
    selector: 'gtx-send-message-modal',
    templateUrl: './send-message-modal.tpl.html',
    styleUrls: ['./send-message-modal.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class SendMessageModal extends BaseModal<boolean> implements OnInit, OnDestroy {

    public users: User[] | null = null;
    public groups: Group[] | null = null;

    public form: FormControl<SendMessageForm>;

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private client: GCMSRestClientService,
        private notification: I18nNotification,
    ) {
        super();
    }

    ngOnInit(): void {
        this.subscriptions.push(this.client.user.list().subscribe({
            next: res => {
                this.users = res.items;
                this.changeDetector.markForCheck();
            },
            error: error => {
                this.notification.show({
                    type: 'alert',
                    message: 'Could not load Users',
                });
                // TODO: Close modal?
            },
        }));
        this.subscriptions.push(this.client.group.tree().subscribe({
            next: res => {
                this.groups = res.groups;
                this.changeDetector.markForCheck();
            },
            error: error => {
                this.notification.show({
                    type: 'alert',
                    message: 'Could not load Groups',
                });
            },
        }));

        this.form = new FormControl<SendMessageForm>({
            isInstant: false,
            recipientIds: [],
            message: [],
        });
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    okayClicked(): void {
        const messageRequest = this.transformValuesForApi(this.form.value);

        this.client.message.send(messageRequest).subscribe(() => {
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

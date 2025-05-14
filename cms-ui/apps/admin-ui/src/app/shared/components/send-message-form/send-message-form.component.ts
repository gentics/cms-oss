import {Component, Input} from '@angular/core';
import {UntypedFormBuilder, UntypedFormGroup} from '@angular/forms';
import {Group, User} from '@gentics/cms-models';

export interface SendMessageFormValue {
    recipientIds: string[];
    message: string[];
    isInstant: boolean;
}
@Component({
    selector: 'gtx-send-message-form',
    templateUrl: './send-message-form.tpl.html',
    styleUrls: ['./send-message-form.scss'],
    standalone: false
})
export class SendMessageFormComponent {
    @Input() users: User[] = [];
    @Input() groups: Group[] = [];

    form: UntypedFormGroup;

    constructor(private fb: UntypedFormBuilder) {
        this.createForm();
    }

    createForm(): void {
        this.form = this.fb.group({
            recipientIds: [],
            message: '',
            isInstant: false,
        });
    }
}

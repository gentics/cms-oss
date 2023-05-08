import {Component, Input, OnInit} from '@angular/core';
import {UntypedFormBuilder, UntypedFormGroup} from '@angular/forms';
import {Group, User} from '@gentics/cms-models';

export interface SendMessageFormValue {
    recipientIds: string[];
    message: string[];
}
@Component({
    selector: 'gtx-send-message-form',
    templateUrl: './send-message-form.tpl.html',
    styleUrls: ['./send-message-form.scss']
})
export class SendMessageFormComponent implements OnInit {
    @Input() users: User[] = [];
    @Input() groups: Group[] = [];

    form: UntypedFormGroup;

    constructor(private fb: UntypedFormBuilder) {
        this.createForm();
    }

    ngOnInit(): void {
    }

    createForm(): void {
        this.form = this.fb.group({
            recipientIds: [],
            message: ''
        });
    }
}

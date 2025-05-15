import { Component, Input } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { SendMessageForm } from '@editor-ui/app/common/models';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { Group, User } from '@gentics/cms-models';
import { generateFormProvider } from '@gentics/ui-core';

@Component({
    selector: 'gtx-message-properties',
    templateUrl: './message-properties.component.html',
    styleUrls: ['./message-properties.component.scss'],
    providers: [generateFormProvider(MessagePropertiesComponent)],
    standalone: false
})
export class MessagePropertiesComponent extends BasePropertiesComponent<SendMessageForm> {

    @Input()
    public users: User[] = [];

    @Input()
    public groups: Group[] = [];

    protected createForm(): UntypedFormGroup {
        return new UntypedFormGroup({
            recipientIds: new UntypedFormControl([], Validators.required),
            message: new UntypedFormControl(''),
            isInstant: new UntypedFormControl(false),
        });
    }

    protected configureForm(value: SendMessageForm, loud?: boolean): void {
        // Nothing to configure
        return;
    }

    protected assembleValue(value: SendMessageForm): SendMessageForm {
        // Nothing to manipulate
        return value;
    }
}

import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { DynamicFormModalConfiguration } from '@gentics/cms-integration-api-models';
import { BaseModal } from '@gentics/ui-core';

@Component({
    selector: 'gtx-dynamic-form-modal',
    templateUrl: './dynamic-form-modal.component.html',
    styleUrls: ['./dynamic-form-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DynamicFormModal extends BaseModal<void> {

    @Input()
    public configuiration: DynamicFormModalConfiguration<any>;
}

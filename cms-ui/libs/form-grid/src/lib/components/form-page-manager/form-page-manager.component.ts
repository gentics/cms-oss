import { ChangeDetectionStrategy, Component, input, model } from '@angular/core';
import { FormUISchema } from '@gentics/cms-models';
import { FormGridEditMode } from '../../models';

@Component({
    selector: 'gtx-form-page-manager',
    templateUrl: './form-page-manager.component.html',
    styleUrls: ['./form-page-manager.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class FormPageManagerComponent {

    public readonly uiSchema = model.required<FormUISchema>();
    public readonly pageIndex = model.required<number>();
    public readonly mode = input<FormGridEditMode>();

}

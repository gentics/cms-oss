/* eslint-disable @typescript-eslint/naming-convention */
import {
    ChangeDetectionStrategy,
    Component,
    Input,
    OnChanges,
    OnInit,
} from '@angular/core';
import { FormGroup } from '@angular/forms';
import {
    EditableFormProperties,
    Form,
    FormTypeConfiguration,
    Language,
} from '@gentics/cms-models';
import {
    BaseFormPropertiesComponent,
    FormProperties,
    generateFormProvider,
    generateValidatorProvider,
} from '@gentics/ui-core';

export enum FormPropertiesMode {
    CREATE = 'create',
    EDIT = 'edit',
}

@Component({
    selector: 'gtx-form-properties',
    templateUrl: './form-properties.component.html',
    styleUrls: ['./form-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(FormPropertiesComponent),
        generateValidatorProvider(FormPropertiesComponent),
    ],
    standalone: false,
})
export class FormPropertiesComponent
    extends BaseFormPropertiesComponent<EditableFormProperties>
    implements OnInit, OnChanges {

    @Input()
    public nodeId: number;

    @Input()
    public folderId: number;

    @Input()
    public item: Form;

    @Input()
    public languages: Language[];

    @Input()
    public disableLanguageSelect = false;

    @Input()
    public autoUpdateFileName = true;

    @Input()
    public mode: FormPropertiesMode = FormPropertiesMode.EDIT;

    @Input()
    public isMultiLang: boolean;

    @Input()
    public showDetailProperties = false;

    public nodeConfigurations: FormTypeConfiguration[] = [];

    protected createForm(): FormGroup<FormProperties<EditableFormProperties>> {
        return new FormGroup<FormProperties<EditableFormProperties>>({} as any);
    }

    protected configureForm(value: EditableFormProperties, loud?: boolean): void {
        // noop
    }

    protected assembleValue(value: EditableFormProperties): EditableFormProperties {
        return value;
    }
}

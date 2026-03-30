/* eslint-disable @typescript-eslint/naming-convention */
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnChanges,
    OnInit,
} from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import {
    EditableFormProperties,
    Form,
    FormTypeConfiguration,
    Language,
} from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
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
    extends BaseFormPropertiesComponent<Omit<EditableFormProperties, 'schema' | 'uiSchema'>>
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

    public formTypeConfigurations: Record<string, FormTypeConfiguration> | null = null;
    public activeConfiguration: FormTypeConfiguration | null = null;

    constructor(
        changeDetector: ChangeDetectorRef,
        public client: GCMSRestClientService,
    ) {
        super(changeDetector);
    }

    public ngOnInit(): void {
        super.ngOnInit();

        this.subscriptions.push(this.client.form.listConfigurations({
            nodeId: this.nodeId,
            external: false,
        }).subscribe((res) => {
            this.formTypeConfigurations = {};
            for (const config of res.items) {
                this.formTypeConfigurations[config.type] = config;
            }

            this.changeDetector.markForCheck();
        }));
    }

    protected createForm(): FormGroup<FormProperties<Omit<EditableFormProperties, 'schema' | 'uiSchema'>>> {
        return new FormGroup<FormProperties<Omit<EditableFormProperties, 'schema' | 'uiSchema'>>>({
            name: new FormControl(this.safeValue('name'), Validators.required),
            formType: new FormControl(this.safeValue('formType'), Validators.required),
            description: new FormControl(this.safeValue('description')),
            languages: new FormControl(this.safeValue('languages') || [], Validators.minLength(1)),
            fileName: new FormControl(this.safeValue('fileName')),
            successUrlI18n: new FormControl(this.safeValue('successUrlI18n')),
            successPageId: new FormControl(this.safeValue('successPageId')),
            successNodeId: new FormControl(this.safeValue('successNodeId')),
            adminEmailAddress: new FormControl(this.safeValue('adminEmailAddress')),
            adminEmailSubject: new FormControl(this.safeValue('adminEmailSubject')),
            adminEmailPageId: new FormControl(this.safeValue('adminEmailPageId')),
            adminEmailNodeId: new FormControl(this.safeValue('adminEmailNodeId')),
            adminEmailTemplate: new FormControl(this.safeValue('adminEmailTemplate')),
        });
    }

    protected configureForm(value: Omit<EditableFormProperties, 'schema' | 'uiSchema'>, loud?: boolean): void {
        // noop
    }

    protected assembleValue(value: Omit<EditableFormProperties, 'schema' | 'uiSchema'>): Omit<EditableFormProperties, 'schema' | 'uiSchema'> {
        return value;
    }
}

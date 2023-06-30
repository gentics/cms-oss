import { FormControlOnChangeFn, FormControlOnTouchedFn } from '@admin-ui/common';
import { AppStateService } from '@admin-ui/state';
import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output,
} from '@angular/core';
import {
    AbstractControl,
    ControlValueAccessor,
    UntypedFormControl,
    UntypedFormGroup,
    ValidationErrors,
    ValidatorFn,
    Validators,
} from '@angular/forms';
import { GtxJsonValidator } from '@gentics/cms-components';
import { ContentRepositoryBO, ContentRepositoryType, Feature, Normalized } from '@gentics/cms-models';
import { generateFormProvider } from '@gentics/ui-core';
import { Subscription } from 'rxjs';
import { map, tap } from 'rxjs/operators';

export interface ContentRepositoryPropertiesFormData {
    basepath: string;
    crType: ContentRepositoryType;
    dbType: string;
    defaultPermission: string;
    diffDelete: boolean;
    elasticsearch: string;
    instantPublishing: boolean;
    languageInformation: boolean;
    name: string;
    password: string;
    repeat_password: string;
    permissionInformation: boolean;
    permissionProperty: string;
    projectPerNode: boolean;
    version: string;
    url: string;
    usePassword: boolean;
    username: string;
}

export enum ContentRepositoryPropertiesComponentMode {
    CREATE = 'create',
    UPDATE = 'update',
}

type CRDisplayType = {
    id: ContentRepositoryType;
    label: string;
};

/**
 * Defines the data editable by the `ContentRepositoryPropertiesComponent`.
 *
 * To convey the validity state of the user's input, the onChange callback will
 * be called with `null` if the form data is currently invalid.
 */
@Component({
    selector: 'gtx-content-repository-properties',
    templateUrl: './content-repository-properties.component.html',
    styleUrls: ['./content-repository-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(ContentRepositoryPropertiesComponent)],
})
export class ContentRepositoryPropertiesComponent implements AfterViewInit, OnInit, OnDestroy, ControlValueAccessor {

    @Input()
    mode: ContentRepositoryPropertiesComponentMode;

    @Input()
    value: ContentRepositoryBO<Normalized>;

    @Output()
    valueChange = new EventEmitter<ContentRepositoryBO<Normalized>>();

    @Output()
    isValidChange = new EventEmitter<boolean>();

    fgProperties: UntypedFormGroup;

    /** selectable options for contentRepository input crtype */
    public crTypes: CRDisplayType[] = [];

    isModeUpdate: boolean;
    isCrTypeMesh: boolean;
    usePassword: boolean;
    meshCrEnabled = false;

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private appState: AppStateService,
    ) { }

    private validatorPasswordsDontMatch: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
        const error = control.value !== this.fgProperties?.get('repeat_password')?.value;
        if (error) {
            return { passwordsDontMatch: true };
        } else {
            return null;
        }
    }

    ngOnInit(): void {
        this.fgPropertiesInit();
        this.updateCRTypes();

        this.subscriptions.push(this.appState.select(state => state.features.global[Feature.MESH_CR]).subscribe(featureEnabled => {
            this.meshCrEnabled = featureEnabled;
            this.updateCRTypes();
            this.changeDetector.markForCheck();
        }));
    }

    ngAfterViewInit(): void {
        // Set FormGroup logic and rendering dependencies from external value
        this.isModeUpdate = this.mode === ContentRepositoryPropertiesComponentMode.UPDATE;
        this.isCrTypeMesh = this.fgProperties.get('crType').value === ContentRepositoryType.MESH;
        this.usePassword = this.value.usePassword;
        // refresh form with not null dependencies
        this.configureFormControls(this.value);
        this.fgProperties.updateValueAndValidity();
        this.changeDetector.markForCheck();
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    protected updateCRTypes(): void {
        const types: CRDisplayType[] = [
            {
                id: ContentRepositoryType.CR,
                label: `contentRepository.contentRepository_type_${ContentRepositoryType.CR}`,
            },
        ];

        if (this.meshCrEnabled) {
            types.push({
                id: ContentRepositoryType.MESH,
                label: `contentRepository.contentRepository_type_${ContentRepositoryType.MESH}`,
            });
        }

        this.crTypes = types;
    }

    writeValue(value: ContentRepositoryBO<Normalized>): void {
        if (!value || !this.fgProperties) {
            return;
        }
        this.value = value;
        this.fgPropertiesUpdate(value);
    }

    registerOnChange(fn: FormControlOnChangeFn<ContentRepositoryBO<Normalized>>): void {
        this.subscriptions.push(this.fgProperties.valueChanges.pipe(
            map((formData: ContentRepositoryPropertiesFormData) => {
                this.value = this.assembleValue(formData);

                // Set FormGroup logic and rendering dependencies from internal value
                this.isCrTypeMesh = this.fgProperties.get('crType').value === ContentRepositoryType.MESH;
                this.usePassword = this.fgProperties.value.usePassword;

                this.configureFormControls(formData);

                return this.value;
            }),
            tap(() => this.isValidChange.emit(this.fgProperties.valid)),
        ).subscribe(fn))
    }

    registerOnTouched(fn: FormControlOnTouchedFn): void { }

    /**
     * Alter FormGroup depending from values, which input fields appear and disappear or change validation logic.
     *
     * @param value values of active fields to be to (re-)initialized
     */
    private configureFormControls(value: ContentRepositoryBO<Normalized> | ContentRepositoryPropertiesFormData): void {
        const _options = { emitEvent: false };

        // handle JSON data
        let _elasticsearch: string;
        if (value.elasticsearch instanceof Object) {
            try {
                _elasticsearch = JSON.stringify(value.elasticsearch, null, 4);
            } catch (error) {
                _elasticsearch = String(value.elasticsearch);
            }
        } else if (typeof value.elasticsearch === 'string') {
            _elasticsearch = value.elasticsearch;
        } else {
            _elasticsearch = '';
        }

        if (this.isCrTypeMesh) {
            this.fgProperties.removeControl('basepath', _options);
            this.fgProperties.removeControl('dbType', _options);
            this.fgProperties.setControl('defaultPermission', new UntypedFormControl(value.defaultPermission), _options);
            this.fgProperties.removeControl('diffDelete', _options);
            this.fgProperties.setControl('elasticsearch', new UntypedFormControl(_elasticsearch, GtxJsonValidator), _options);
            this.fgProperties.removeControl('languageInformation', _options);
            this.fgProperties.setControl('permissionProperty', new UntypedFormControl(value.permissionProperty), _options);
            this.fgProperties.setControl('projectPerNode', new UntypedFormControl(value.projectPerNode), _options);
            this.fgProperties.setControl('version', new UntypedFormControl(value.version), _options);
        } else {
            this.fgProperties.setControl('basepath', new UntypedFormControl(value.basepath), _options);
            this.fgProperties.setControl('dbType', new UntypedFormControl(value.dbType, Validators.required), _options);
            this.fgProperties.removeControl('defaultPermission', _options);
            this.fgProperties.setControl('diffDelete', new UntypedFormControl(value.diffDelete), _options);
            this.fgProperties.removeControl('elasticsearch', _options);
            this.fgProperties.setControl('languageInformation', new UntypedFormControl(value.languageInformation), _options);
            this.fgProperties.removeControl('permissionProperty', _options);
            this.fgProperties.removeControl('projectPerNode', _options);
            this.fgProperties.removeControl('version', _options);
        }

        if (this.usePassword) {
            this.fgProperties.setControl('password', new UntypedFormControl(value.password, this.validatorPasswordsDontMatch), _options);
            this.fgProperties.setControl('repeat_password', new UntypedFormControl((value as any).repeat_password ?? ''), _options);
        } else {
            this.fgProperties.removeControl('password', _options);
            this.fgProperties.removeControl('repeat_password', _options);
        }
    }

    /**
     * Initialize form 'Properties'
     */
    private fgPropertiesInit(): void {
        // Set FormGroup logic and rendering dependencies initially.
        this.isModeUpdate = this.mode === ContentRepositoryPropertiesComponentMode.UPDATE;
        this.isCrTypeMesh = this.value ? this.value.crType === ContentRepositoryType.MESH : false;
        this.usePassword = this.value ? this.value.usePassword : false;

        this.fgProperties = new UntypedFormGroup({
            ...(!this.isCrTypeMesh && { basepath: new UntypedFormControl('') }),
            // once a contentRepository is created it cannot change its type
            crType: new UntypedFormControl({ value: null, disabled: this.isModeUpdate }, Validators.required),
            ...(!this.isCrTypeMesh && { dbType: new UntypedFormControl('', Validators.required) }),
            ...(this.isCrTypeMesh && { defaultPermission: new UntypedFormControl('') }),
            ...(!this.isCrTypeMesh && { diffDelete: new UntypedFormControl(false) }),
            ...(this.isCrTypeMesh && { elasticsearch: new UntypedFormControl('', GtxJsonValidator) }),
            instantPublishing: new UntypedFormControl(false),
            ...(!this.isCrTypeMesh && { languageInformation: new UntypedFormControl(false) }),
            name: new UntypedFormControl('', Validators.required),
            ...(this.usePassword && { password: new UntypedFormControl('', this.validatorPasswordsDontMatch) }),
            ...(this.usePassword && { repeat_password: new UntypedFormControl('') }),
            permissionInformation: new UntypedFormControl(''),
            ...(this.isCrTypeMesh && { permissionProperty: new UntypedFormControl(false) }),
            ...(this.isCrTypeMesh && { projectPerNode: new UntypedFormControl(false) }),
            ...(this.isCrTypeMesh && { version: new UntypedFormControl('') }),
            url: new UntypedFormControl('', Validators.required),
            usePassword: new UntypedFormControl(false),
            username: new UntypedFormControl('', Validators.required),
        });
    }

    private fgPropertiesUpdate(value: Partial<ContentRepositoryBO<Normalized>>): void {
        const _value: ContentRepositoryPropertiesFormData = {
            ...(!this.isCrTypeMesh && { basepath: value.basepath }),
            crType: value.crType,
            ...(!this.isCrTypeMesh && { dbType: value.dbType }),
            ...(this.isCrTypeMesh && { defaultPermission: value.defaultPermission }),
            ...(!this.isCrTypeMesh && { diffDelete: value.diffDelete }),
            ...(this.isCrTypeMesh && { elasticsearch: value.elasticsearch ? JSON.stringify(value.elasticsearch, null, 4) : '' }),
            instantPublishing: value.instantPublishing,
            ...(!this.isCrTypeMesh && { languageInformation: value.languageInformation }),
            name: value.name,
            ...(this.usePassword && { password: value.password ?? '' }),
            ...(this.usePassword && { repeat_password: value.password ?? '' }),
            permissionInformation: value.permissionInformation,
            ...(this.isCrTypeMesh && { permissionProperty: value.permissionProperty }),
            ...(this.isCrTypeMesh && { projectPerNode: value.projectPerNode }),
            ...(this.isCrTypeMesh && { version: value.version }),
            url: value.url,
            usePassword: value.usePassword,
            username: value.username,
        };
        this.fgProperties.setValue(_value);
        this.fgProperties.markAsPristine();
    }

    private assembleValue(formData: ContentRepositoryPropertiesFormData): ContentRepositoryBO<Normalized> {
        const _output: ContentRepositoryBO<Normalized> = {
            globalId: this.value.globalId,
            id: this.value.id,
            name: formData.name,
            usePassword: formData.usePassword,
            version: this.value.version,
            checkDate: this.value.checkDate,
            checkStatus: this.value.checkStatus,
            checkResult: this.value.checkResult,
            statusDate: this.value.statusDate,
            dataStatus: this.value.dataStatus,
            dataCheckResult: this.value.dataCheckResult,
            basepath: formData.basepath,
            crType: formData.crType,
            dbType: formData.dbType,
            defaultPermission: formData.defaultPermission,
            diffDelete: formData.diffDelete,
            elasticsearch: null,
            instantPublishing: formData.instantPublishing,
            languageInformation: formData.languageInformation,
            password: formData.password,
            permissionInformation: formData.permissionInformation,
            permissionProperty: formData.permissionProperty,
            projectPerNode: formData.projectPerNode,
            url: formData.url,
            username: formData.username,
        };
        try {
            _output.elasticsearch = JSON.parse(formData.elasticsearch);
        } catch (error) {
            _output.elasticsearch = null;
        }
        return _output;
    }

}

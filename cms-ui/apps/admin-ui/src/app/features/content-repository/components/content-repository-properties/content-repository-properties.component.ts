import { AppStateService } from '@admin-ui/state';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnChanges,
    OnInit,
    SimpleChanges,
} from '@angular/core';
import {
    AbstractControl,
    FormGroup,
    UntypedFormControl,
    UntypedFormGroup,
    ValidationErrors,
    ValidatorFn,
    Validators,
} from '@angular/forms';
import { BasePropertiesComponent, GtxJsonValidator } from '@gentics/cms-components';
import { AnyModelType, ContentRepository, ContentRepositoryPasswordType, ContentRepositoryType, Feature } from '@gentics/cms-models';
import { generateFormProvider, setControlsEnabled } from '@gentics/ui-core';

export enum ContentRepositoryPropertiesMode {
    CREATE = 'create',
    UPDATE = 'update',
}

type CRDisplayType = {
    id: ContentRepositoryType;
    label: string;
};

@Component({
    selector: 'gtx-content-repository-properties',
    templateUrl: './content-repository-properties.component.html',
    styleUrls: ['./content-repository-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(ContentRepositoryPropertiesComponent)],
})
export class ContentRepositoryPropertiesComponent extends BasePropertiesComponent<ContentRepository> implements OnInit, OnChanges {

    public readonly ContentRepositoryPropertiesMode = ContentRepositoryPropertiesMode;

    @Input()
    public mode: ContentRepositoryPropertiesMode;

    @Input()
    public crType?: ContentRepositoryType;

    /** selectable options for contentRepository input crtype */
    public crTypes: CRDisplayType[] = [];

    public passwordRepeat = '';

    /** selectable options for contentRepository input passwordType */
    readonly PASSWORD_TYPES: { id: ContentRepositoryPasswordType; label: string; }[] = [
        {
            id: ContentRepositoryPasswordType.NONE,
            label: 'contentRepository.passwordType_none',
        },
        {
            id: ContentRepositoryPasswordType.VALUE,
            label: 'contentRepository.passwordType_value',
        },
        {
            id: ContentRepositoryPasswordType.PROPERTY,
            label: 'contentRepository.passwordType_property',
        },
    ];

    meshCrEnabled = false;

    constructor(
        changeDetector: ChangeDetectorRef,
        private appState: AppStateService,
    ) {
        super(changeDetector);
    }

    private validatorPasswordsDontMatch: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
        if (control.value == null || control.value === '') {
            return null;
        }

        const error = control.value !== this.passwordRepeat;

        if (error) {
            return { passwordsDontMatch: true };
        } else {
            return null;
        }
    }

    public ngOnInit(): void {
        super.ngOnInit();

        this.updateCRTypes();

        this.subscriptions.push(this.appState.select(state => state.features.global[Feature.MESH_CR]).subscribe(featureEnabled => {
            this.meshCrEnabled = featureEnabled;
            this.updateCRTypes();
            this.changeDetector.markForCheck();
        }));
    }

    public ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.mode && this.form) {
            setControlsEnabled(this.form, ['crType'], this.mode !== ContentRepositoryPropertiesMode.UPDATE, { emitEvent: true });
        }

        // When the root element changes, we have to reset the repeat value
        if (changes.initialValue && this.initialValue) {
            this.passwordRepeat = '';
        }
    }

    public updatePasswordRepeat(value: string): void {
        this.passwordRepeat = value;
        if (this.form) {
            const ctl = this.form.get('password');
            if (ctl) {
                ctl.updateValueAndValidity();
            }
        }
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

    protected createForm(): FormGroup<any> {
        return new UntypedFormGroup({
            basepath: new UntypedFormControl(this.value?.basepath || ''),
            crType: new UntypedFormControl(this.value?.crType || null, Validators.required),
            dbType: new UntypedFormControl(this.value?.dbType || null, Validators.required),
            defaultPermission: new UntypedFormControl(this.value?.defaultPermission || ''),
            diffDelete: new UntypedFormControl(this.value?.diffDelete ?? false),
            elasticsearch: new UntypedFormControl(this.value?.elasticsearch || '', GtxJsonValidator),
            instantPublishing: new UntypedFormControl(this.value?.instantPublishing ?? false),
            languageInformation: new UntypedFormControl(this.value?.languageInformation ?? false),
            name: new UntypedFormControl(this.value?.name || '', Validators.required),
            passwordType: new UntypedFormControl(this.value?.passwordType || ContentRepositoryPasswordType.NONE),
            password: new UntypedFormControl('', this.validatorPasswordsDontMatch),
            passwordProperty: new UntypedFormControl(this.value?.passwordProperty || '', Validators.pattern(/^\$\{(env|sys):[^\}]+\}$/)),
            permissionProperty: new UntypedFormControl(this.value?.permissionProperty || ''),
            permissionInformation: new UntypedFormControl(this.value?.permissionInformation ?? false),
            projectPerNode: new UntypedFormControl(this.value?.projectPerNode ?? false),
            version: new UntypedFormControl(this.value?.version || ''),
            url: new UntypedFormControl(this.value?.url || '', Validators.required),
            username: new UntypedFormControl(this.value?.username || '', Validators.required),
        });
    }

    protected configureForm(value: ContentRepository<AnyModelType>, loud?: boolean): void {
        const options = { emitEvent: !!loud };

        setControlsEnabled(this.form, ['crType'], this.crType == null || this.mode !== ContentRepositoryPropertiesMode.UPDATE, options);
        setControlsEnabled(this.form, ['password', 'repeat_password'], value?.passwordType == ContentRepositoryPasswordType.VALUE ?? false, options);
        setControlsEnabled(this.form, ['passwordProperty'], value?.passwordType == ContentRepositoryPasswordType.PROPERTY ?? false, options);

        const dbControls =  [
            'basepath',
            'dbType',
            'diffDelete',
            'languageInformation',
        ];
        const meshControls = [
            'defaultPermission',
            'elasticsearch',
            'permissionProperty',
            'projectPerNode',
            'version',
        ];

        const crType = this.mode === ContentRepositoryPropertiesMode.UPDATE
            ? this.crType || value?.crType
            : value?.crType;

        // If no type is selected, disable all options
        if (crType == null) {
            setControlsEnabled(this.form, [...dbControls, ...meshControls], false, options);
        } else {
            setControlsEnabled(this.form, dbControls, crType !== ContentRepositoryType.MESH, options);
            setControlsEnabled(this.form, meshControls, crType === ContentRepositoryType.MESH, options);
        }
    }

    protected assembleValue(value: ContentRepository): ContentRepository {
        if (this.mode === ContentRepositoryPropertiesMode.UPDATE) {
            value.crType = this.crType;
        }

        return value;
    }
}

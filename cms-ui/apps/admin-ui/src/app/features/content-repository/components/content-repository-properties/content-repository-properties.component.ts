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
    FormControl,
    FormGroup,
    ValidationErrors,
    ValidatorFn,
    Validators,
} from '@angular/forms';
import { BasePropertiesComponent, GtxJsonValidator } from '@gentics/cms-components';
import {
    AnyModelType,
    BasepathType,
    CONTENT_REPOSIROTY_USERNAME_PROPERTY_PREFIX,
    CONTENT_REPOSITORY_BASE_PATH_PROPERTY_PREFIX,
    CONTENT_REPOSITORY_PASSWORD_PROPERTY_PREFIX,
    CONTENT_REPOSITORY_URL_PROPERTY_PREFIX,
    ContentRepository,
    ContentRepositoryPasswordType,
    ContentRepositoryType,
    EditableContentRepositoryProperties,
    Feature,
    UrlType,
    UsernameType,
} from '@gentics/cms-models';
import {
    FormProperties,
    createPropertyPatternValidator,
    generateFormProvider,
    generateValidatorProvider,
    setControlsEnabled,
    setControlsValidators,
} from '@gentics/ui-core';

export enum ContentRepositoryPropertiesMode {
    CREATE = 'create',
    UPDATE = 'update',
}

type CRDisplayType = {
    id: ContentRepositoryType;
    label: string;
};

const DB_CONTROLS: (keyof EditableContentRepositoryProperties)[] =  [
    'dbType',
    'diffDelete',
    'languageInformation',
];

const MESH_CONTROLS: (keyof EditableContentRepositoryProperties)[] = [
    'defaultPermission',
    'elasticsearch',
    'permissionProperty',
    'projectPerNode',
    'http2',
    'noFoldersIndex',
    'noFilesIndex',
    'noPagesIndex',
    'noFormsIndex',
    'version',
];

@Component({
    selector: 'gtx-content-repository-properties',
    templateUrl: './content-repository-properties.component.html',
    styleUrls: ['./content-repository-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(ContentRepositoryPropertiesComponent),
        generateValidatorProvider(ContentRepositoryPropertiesComponent),
    ],
})
export class ContentRepositoryPropertiesComponent extends BasePropertiesComponent<EditableContentRepositoryProperties> implements OnInit, OnChanges {

    public readonly ContentRepositoryPropertiesMode = ContentRepositoryPropertiesMode;

    @Input()
    public mode: ContentRepositoryPropertiesMode;

    @Input()
    public crType?: ContentRepositoryType;

    /** selectable options for contentRepository input crtype */
    public crTypes: CRDisplayType[] = [];

    public passwordRepeat = '';
    public meshCrEnabled = false;

    public showBasepath = false;

    public urlType: UrlType;

    public basepathType: BasepathType;

    public usernameType: UsernameType;

    /** selectable options for contentRepository input passwordType */
    public readonly PASSWORD_TYPES: { id: ContentRepositoryPasswordType; label: string; }[] = [
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

    /** selectable options for contentRepository input usernameType */
    public readonly USERNAME_TYPES: { id: UsernameType; label: string; }[] = [
        {
            id: UsernameType.VALUE,
            label: 'contentRepository.usernameType_value',
        },
        {
            id: UsernameType.PROPERTY,
            label: 'contentRepository.usernameType_property',
        },
    ];

    /** selectable options for contentRepository input urlType */
    public readonly URL_TYPES: { id: UrlType; label: string; }[] = [
        {
            id: UrlType.VALUE,
            label: 'contentRepository.urlType_value',
        },
        {
            id: UrlType.PROPERTY,
            label: 'contentRepository.urlType_property',
        },
    ];

    /** selectable options for contentRepository input basepathType */
    public readonly BASEPATH_TYPES: { id: BasepathType; label: string; }[] = [
        {
            id: BasepathType.VALUE,
            label: 'contentRepository.basepathType_value',
        },
        {
            id: BasepathType.PROPERTY,
            label: 'contentRepository.basepathType_property',
        },
    ];

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
    }

    protected override onValueReset(): void {
        super.onValueReset();
        if (this.form) {
            this.form.controls.password.setValue(null);
        }
        this.passwordRepeat = '';
    }

    public updatePasswordRepeat(value: string): void {
        this.passwordRepeat = value;
        if (this.form) {
            const ctl = this.form.controls.password;
            if (ctl) {
                ctl.updateValueAndValidity();
            }
        }
    }

    public updateUrlType(value: UrlType): void {
        this.urlType = value;
        if (this.form) {
            setControlsEnabled(this.form, ['url'], this.urlType === UrlType.VALUE, { emitEvent: false });
            setControlsEnabled(this.form, ['urlProperty'], this.urlType === UrlType.PROPERTY, { emitEvent: false });
        }
    }

    public updateBasepathType(value: BasepathType): void {
        this.basepathType = value;
        if (this.form) {
            setControlsEnabled(this.form, ['basepath'], this.basepathType === BasepathType.VALUE, { emitEvent: false });
            setControlsEnabled(this.form, ['basepathProperty'], this.basepathType === BasepathType.PROPERTY, { emitEvent: false });
        }
    }

    public updateUsernameType(value: UsernameType): void {
        this.usernameType = value;
        if (this.form) {
            setControlsEnabled(this.form, ['username'], this.usernameType === UsernameType.VALUE, { emitEvent: false });
            setControlsEnabled(this.form, ['usernameProperty'], this.usernameType === UsernameType.PROPERTY, { emitEvent: false });
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

    protected createForm(): FormGroup<FormProperties<EditableContentRepositoryProperties>> {
        return new FormGroup<FormProperties<EditableContentRepositoryProperties>>({
            basepath: new FormControl(this.value?.basepath || ''),
            basepathProperty: new FormControl(this.value?.basepathProperty || '', [
                createPropertyPatternValidator(CONTENT_REPOSITORY_BASE_PATH_PROPERTY_PREFIX),
            ]),
            crType: new FormControl(this.value?.crType || null, Validators.required),
            dbType: new FormControl(this.value?.dbType || null, Validators.required),
            defaultPermission: new FormControl(this.value?.defaultPermission || ''),
            diffDelete: new FormControl(this.value?.diffDelete ?? false),
            elasticsearch: new FormControl<any>(this.value?.elasticsearch || '', GtxJsonValidator),
            instantPublishing: new FormControl(this.value?.instantPublishing ?? false),
            languageInformation: new FormControl(this.value?.languageInformation ?? false),
            name: new FormControl(this.value?.name || '', Validators.required),
            passwordType: new FormControl(this.value?.passwordType || ContentRepositoryPasswordType.NONE),
            password: new FormControl('', this.validatorPasswordsDontMatch),
            passwordProperty: new FormControl(this.value?.passwordProperty || '', [
                createPropertyPatternValidator(CONTENT_REPOSITORY_PASSWORD_PROPERTY_PREFIX),
            ]),
            permissionProperty: new FormControl(this.value?.permissionProperty || ''),
            permissionInformation: new FormControl(this.value?.permissionInformation ?? false),
            projectPerNode: new FormControl(this.value?.projectPerNode ?? false),
            version: new FormControl(this.value?.version || ''),
            url: new FormControl(this.value?.url || '', Validators.required),
            urlProperty: new FormControl(this.value?.urlProperty || '', [
                Validators.required,
                createPropertyPatternValidator(CONTENT_REPOSITORY_URL_PROPERTY_PREFIX),
            ]),
            username: new FormControl(this.value?.username || '', Validators.required),
            usernameProperty: new FormControl(this.value?.usernameProperty || '', [
                Validators.required,
                createPropertyPatternValidator(CONTENT_REPOSIROTY_USERNAME_PROPERTY_PREFIX),
            ]),
            http2: new FormControl(this.value?.http2 ?? false),
            noFoldersIndex: new FormControl(this.value?.noFoldersIndex ?? false),
            noFilesIndex: new FormControl(this.value?.noFilesIndex ?? false),
            noPagesIndex: new FormControl(this.value?.noPagesIndex ?? false),
            noFormsIndex: new FormControl(this.value?.noFormsIndex ?? false),
        });
    }

    protected configureForm(value: ContentRepository<AnyModelType>, loud?: boolean): void {
        const options = { emitEvent: !!loud };

        if (!this.urlType && this.value) {
            this.urlType = this.value?.urlProperty ? UrlType.PROPERTY : UrlType.VALUE;
        }
        if (!this.usernameType && this.value) {
            this.usernameType = this.value?.usernameProperty ? UsernameType.PROPERTY : UsernameType.VALUE;
        }
        if (!this.basepathType && this.value) {
            this.basepathType = this.value?.basepathProperty ? BasepathType.PROPERTY : BasepathType.VALUE;
        }

        setControlsEnabled(this.form, ['crType'], this.crType == null || this.mode !== ContentRepositoryPropertiesMode.UPDATE, options);
        setControlsEnabled(
            this.form,
            ['password'],
            value?.passwordType === ContentRepositoryPasswordType.VALUE,
            options,
        );
        setControlsEnabled(this.form, ['passwordProperty'], value?.passwordType === ContentRepositoryPasswordType.PROPERTY, options);

        const crType = this.mode === ContentRepositoryPropertiesMode.UPDATE
            ? this.crType || value?.crType
            : value?.crType;

        // If no type is selected, disable all options
        if (crType == null) {
            setControlsEnabled(this.form, [...DB_CONTROLS, ...MESH_CONTROLS] as any, false, options);
        } else {
            setControlsEnabled(this.form, DB_CONTROLS, crType !== ContentRepositoryType.MESH, options);
            setControlsEnabled(this.form, MESH_CONTROLS, crType === ContentRepositoryType.MESH, options);
        }
        if (crType === ContentRepositoryType.MESH) {
            setControlsValidators(this.form, ['username'], null);
        } else {
            setControlsValidators(this.form, ['username'], Validators.required);
        }

        this.showBasepath = crType !== ContentRepositoryType.MESH;

        setControlsEnabled(this.form, ['url'], this.urlType === UrlType.VALUE, options);
        setControlsEnabled(this.form, ['urlProperty'], this.urlType === UrlType.PROPERTY, options);

        setControlsEnabled(this.form, ['username'], this.usernameType === UsernameType.VALUE, options);
        setControlsEnabled(this.form, ['usernameProperty'], this.usernameType === UsernameType.PROPERTY, options);

        setControlsEnabled(this.form, ['basepath'], this.basepathType === BasepathType.VALUE, options);
        setControlsEnabled(this.form, ['basepathProperty'], this.basepathType === BasepathType.PROPERTY, options);
    }

    protected assembleValue(value: ContentRepository): ContentRepository {
        if (this.mode === ContentRepositoryPropertiesMode.UPDATE) {
            value.crType = this.crType;
        }

        return value;
    }
}

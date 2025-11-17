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
    BasepathType,
    CONTENT_REPOSIROTY_USERNAME_PROPERTY_PREFIX,
    CONTENT_REPOSITORY_BASE_PATH_PROPERTY_PREFIX,
    CONTENT_REPOSITORY_PASSWORD_PROPERTY_PREFIX,
    CONTENT_REPOSITORY_URL_PROPERTY_PREFIX,
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
import { AppStateService } from '../../../../state';

export enum ContentRepositoryPropertiesMode {
    CREATE = 'create',
    UPDATE = 'update',
}

export interface ContentRepositoryPropertiesFormData extends EditableContentRepositoryProperties {
    urlType: UrlType;
    basepathType: BasepathType;
    usernameType: UsernameType;
}

type CRDisplayType = {
    id: ContentRepositoryType;
    label: string;
};

const DB_CONTROLS: (keyof EditableContentRepositoryProperties)[] = [
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
    standalone: false,
})
export class ContentRepositoryPropertiesComponent extends BasePropertiesComponent<ContentRepositoryPropertiesFormData> implements OnInit, OnChanges {

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

    /** selectable options for contentRepository input passwordType */
    public readonly PASSWORD_TYPES: { id: ContentRepositoryPasswordType; label: string }[] = [
        {
            id: ContentRepositoryPasswordType.NONE,
            label: 'content_repository.passwordType_none',
        },
        {
            id: ContentRepositoryPasswordType.VALUE,
            label: 'content_repository.passwordType_value',
        },
        {
            id: ContentRepositoryPasswordType.PROPERTY,
            label: 'content_repository.passwordType_property',
        },
    ];

    /** selectable options for contentRepository input usernameType */
    public readonly USERNAME_TYPES: { id: UsernameType; label: string }[] = [
        {
            id: UsernameType.VALUE,
            label: 'content_repository.usernameType_value',
        },
        {
            id: UsernameType.PROPERTY,
            label: 'content_repository.usernameType_property',
        },
    ];

    /** selectable options for contentRepository input urlType */
    public readonly URL_TYPES: { id: UrlType; label: string }[] = [
        {
            id: UrlType.VALUE,
            label: 'content_repository.urlType_value',
        },
        {
            id: UrlType.PROPERTY,
            label: 'content_repository.urlType_property',
        },
    ];

    /** selectable options for contentRepository input basepathType */
    public readonly BASEPATH_TYPES: { id: BasepathType; label: string }[] = [
        {
            id: BasepathType.VALUE,
            label: 'content_repository.basepathType_value',
        },
        {
            id: BasepathType.PROPERTY,
            label: 'content_repository.basepathType_property',
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
    };

    public ngOnInit(): void {
        super.ngOnInit();

        this.updateCRTypes();

        this.subscriptions.push(this.appState.select((state) => state.features.global[Feature.MESH_CR]).subscribe((featureEnabled) => {
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

    protected updateCRTypes(): void {
        const types: CRDisplayType[] = [
            {
                id: ContentRepositoryType.CR,
                label: `content_repository.contentRepository_type_${ContentRepositoryType.CR}`,
            },
        ];

        if (this.meshCrEnabled) {
            types.push({
                id: ContentRepositoryType.MESH,
                label: `content_repository.contentRepository_type_${ContentRepositoryType.MESH}`,
            });
        }

        this.crTypes = types;
    }

    protected createForm(): FormGroup<FormProperties<ContentRepositoryPropertiesFormData>> {
        return new FormGroup<FormProperties<ContentRepositoryPropertiesFormData>>({
            basepathType: new FormControl(this.safeValue('basepathType') ?? this.safeValue('basepathProperty')
                ? BasepathType.PROPERTY
                : BasepathType.VALUE,
            ),
            basepath: new FormControl(this.safeValue('basepath') || ''),
            basepathProperty: new FormControl(this.safeValue('basepathProperty') || '', [
                createPropertyPatternValidator(CONTENT_REPOSITORY_BASE_PATH_PROPERTY_PREFIX),
            ]),
            crType: new FormControl(this.safeValue('crType') || null, Validators.required),
            dbType: new FormControl(this.safeValue('dbType') || null, Validators.required),
            defaultPermission: new FormControl(this.safeValue('defaultPermission') || ''),
            diffDelete: new FormControl(this.safeValue('diffDelete') ?? false),
            elasticsearch: new FormControl<any>(this.safeValue('elasticsearch') || '', GtxJsonValidator),
            instantPublishing: new FormControl(this.safeValue('instantPublishing') ?? false),
            languageInformation: new FormControl(this.safeValue('languageInformation') ?? false),
            name: new FormControl(this.safeValue('name') || '', Validators.required),
            passwordType: new FormControl(this.safeValue('passwordType') || ContentRepositoryPasswordType.NONE),
            password: new FormControl('', this.validatorPasswordsDontMatch),
            passwordProperty: new FormControl(this.safeValue('passwordProperty') || '', [
                createPropertyPatternValidator(CONTENT_REPOSITORY_PASSWORD_PROPERTY_PREFIX),
            ]),
            permissionProperty: new FormControl(this.safeValue('permissionProperty') || ''),
            permissionInformation: new FormControl(this.safeValue('permissionInformation') ?? false),
            projectPerNode: new FormControl(this.safeValue('projectPerNode') ?? false),
            version: new FormControl(this.safeValue('version') || ''),
            url: new FormControl(this.safeValue('url') || '', Validators.required),
            urlType: new FormControl(this.safeValue('urlType') ?? this.value?.urlProperty
                ? UrlType.PROPERTY
                : UrlType.VALUE,
            ),
            urlProperty: new FormControl(this.safeValue('urlProperty') || '', [
                Validators.required,
                createPropertyPatternValidator(CONTENT_REPOSITORY_URL_PROPERTY_PREFIX),
            ]),
            usernameType: new FormControl(this.safeValue('usernameType') ?? this.value?.usernameProperty
                ? UsernameType.PROPERTY
                : UsernameType.VALUE,
            ),
            username: new FormControl(this.safeValue('username') || '', Validators.required),
            usernameProperty: new FormControl(this.safeValue('usernameProperty') || '', [
                Validators.required,
                createPropertyPatternValidator(CONTENT_REPOSIROTY_USERNAME_PROPERTY_PREFIX),
            ]),
            http2: new FormControl(this.safeValue('http2') ?? false),
            noFoldersIndex: new FormControl(this.safeValue('noFoldersIndex') ?? false),
            noFilesIndex: new FormControl(this.safeValue('noFilesIndex') ?? false),
            noPagesIndex: new FormControl(this.safeValue('noPagesIndex') ?? false),
            noFormsIndex: new FormControl(this.safeValue('noFormsIndex') ?? false),
        });
    }

    protected configureForm(value: ContentRepositoryPropertiesFormData, loud?: boolean): void {
        const options = { emitEvent: !!loud };

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

        setControlsEnabled(this.form, ['url'], value?.urlType === UrlType.VALUE, options);
        setControlsEnabled(this.form, ['urlProperty'], value?.urlType === UrlType.PROPERTY, options);

        setControlsEnabled(this.form, ['username'], value?.usernameType === UsernameType.VALUE, options);
        setControlsEnabled(this.form, ['usernameProperty'], value?.usernameType === UsernameType.PROPERTY, options);

        setControlsEnabled(this.form, ['basepath'], value?.basepathType === BasepathType.VALUE, options);
        setControlsEnabled(this.form, ['basepathProperty'], value?.basepathType === BasepathType.PROPERTY, options);
    }

    protected assembleValue(value: ContentRepositoryPropertiesFormData): ContentRepositoryPropertiesFormData {
        if (this.mode === ContentRepositoryPropertiesMode.UPDATE) {
            value.crType = this.crType;
        }

        return value;
    }
}

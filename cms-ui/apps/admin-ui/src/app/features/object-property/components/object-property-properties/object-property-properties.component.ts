import { createI18nRequiredValidator } from '@admin-ui/common';
import { ConstructHandlerService, LanguageHandlerService, ObjectPropertyCategoryHandlerService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnInit,
    Output,
} from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, ValidatorFn, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import {
    Feature,
    Language,
    ModelType,
    Normalized,
    ObjectPropertiesObjectType,
    ObjectPropertyBO,
    ObjectPropertyCategory,
    Raw,
    TagType,
    TagTypeBO,
} from '@gentics/cms-models';
import { generateFormProvider, generateValidatorProvider } from '@gentics/ui-core';
import { Observable, combineLatest } from 'rxjs';
import { map } from 'rxjs/operators';

export enum ObjectpropertyPropertiesMode {
    CREATE = 'create',
    UPDATE = 'update',
}

/**
 * Defines the data editable by the `ObjectpropertyPropertiesComponent`.
 *
 * To convey the validity state of the user's input, the onChange callback will
 * be called with `null` if the form data is currently invalid.
 */
@Component({
    selector: 'gtx-object-property-properties',
    templateUrl: './object-property-properties.component.html',
    styleUrls: ['./object-property-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(ObjectpropertyPropertiesComponent),
        generateValidatorProvider(ObjectpropertyPropertiesComponent),
    ],
})
export class ObjectpropertyPropertiesComponent
    extends BasePropertiesComponent<ObjectPropertyBO<Normalized>> implements OnInit {

    @Input()
    public mode: ObjectpropertyPropertiesMode;

    @Output()
    public isValidChange = new EventEmitter<boolean>();

    public constructs$: Observable<TagType<Raw>[]>;
    public languages$: Observable<Language[]>;
    public objectPropertyCategories$: Observable<ObjectPropertyCategory<Raw>[]>;

    public multiChannelingEnabled = false;
    public objTagSyncEnabled = false;

    public localConstructs: TagTypeBO[] = [];
    public languages: Language[];
    public activeTabI18nLanguage: Language;
    public invalidLanguages: string[] = [];

    /** selectable options for contentRepository input objecttype */
    readonly OBJECT_TYPES: { id: ObjectPropertiesObjectType; label: string; }[] = [
        {
            id: ObjectPropertiesObjectType.FOLDER,
            label: 'common.folder_singular',
        },
        {
            id: ObjectPropertiesObjectType.PAGE,
            label: 'common.page_singular',
        },
        {
            id: ObjectPropertiesObjectType.IMAGE,
            label: 'common.image_singular',
        },
        {
            id: ObjectPropertiesObjectType.FILE,
            label: 'common.file_singular',
        },
        {
            id: ObjectPropertiesObjectType.TEMPLATE,
            label: 'common.template_singular',
        },
    ];

    constructor(
        changeDetector: ChangeDetectorRef,
        private constructHandler: ConstructHandlerService,
        private categoryHandler: ObjectPropertyCategoryHandlerService,
        private languageHandler: LanguageHandlerService,
        private appState: AppStateService,
    ) {
        super(changeDetector);
    }

    ngOnInit(): void {
        super.ngOnInit();

        this.constructs$ = this.constructHandler.listMapped().pipe(
            map(res => res.items),
        );
        this.objectPropertyCategories$ = this.categoryHandler.listMapped().pipe(
            map(res => res.items),
        );
        this.languages$ = this.languageHandler.getSupportedLanguages();

        this.subscriptions.push(this.languages$.subscribe(languages => {
            this.languages = languages;
            if (this.form) {
                this.form.get('nameI18n').setValidators(this.createNameValidator());

                const defaultDesc = {};
                const descCtl = this.form.get('descriptionI18n');

                (languages || []).forEach(l => {
                    defaultDesc[l.code] = '';
                });

                descCtl.setValue({
                    ...defaultDesc,
                    ...descCtl.value || {},
                });
            }

            const defaultLanguage = languages[0];
            if (defaultLanguage) {
                this.activeTabI18nLanguage = defaultLanguage;
            }
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(combineLatest([
            this.appState.select(state => state.features.global[Feature.MULTICHANNELLING]),
            this.appState.select(state => state.features.global[Feature.OBJECT_TAG_SYNC]),
        ]).subscribe(([multiChannelingEnabled, objTagSyncEnabled]) => {
            this.multiChannelingEnabled = multiChannelingEnabled;
            this.objTagSyncEnabled = objTagSyncEnabled;

            if (this.form) {
                this.configureForm(this.form.value as any);
            }

            this.changeDetector.markForCheck();
        }));
    }

    protected createForm(): UntypedFormGroup {
        const defaultDesc = {};
        (this.languages || []).forEach(l => {
            defaultDesc[l.code] = '';
        });

        return new UntypedFormGroup({
            nameI18n: new UntypedFormControl(this.value?.nameI18n, this.createNameValidator()),
            descriptionI18n: new UntypedFormControl({
                ...defaultDesc,
                ...this.value?.descriptionI18n || {},
            }),
            /* eslint-disable @typescript-eslint/unbound-method */
            keyword: new UntypedFormControl(this.value?.keyword, Validators.required),
            type: new UntypedFormControl(this.value?.type, Validators.required),
            constructId: new UntypedFormControl(this.value?.constructId, Validators.required),
            categoryId: new UntypedFormControl(this.value?.categoryId),
            required: new UntypedFormControl(this.value?.required),
            inheritable: new UntypedFormControl(this.value?.inheritable),
            syncContentset: new UntypedFormControl(this.value?.syncContentset),
            syncChannelset: new UntypedFormControl(this.value?.syncChannelset),
            syncVariants: new UntypedFormControl(this.value?.syncVariants),
            /* eslint-disable @typescript-eslint/unbound-method */
        }, { updateOn: 'change' });
    }

    protected configureForm(value: ObjectPropertyBO<ModelType.Normalized>, loud: boolean = false): void {
        const options = { emitEvent: !!loud };
        if (this.mode === ObjectpropertyPropertiesMode.UPDATE) {
            this.form.get('keyword').disable(options);
        }

        const inheritCtl = this.form.get('inheritable');
        const syncContentCtl = this.form.get('syncContentset');
        const syncChannelCtl = this.form.get('syncChannelset');
        const syncVariantsCtl = this.form.get('syncVariants');

        switch (value?.type) {
            case ObjectPropertiesObjectType.FOLDER:
                inheritCtl.enable(options);
                syncContentCtl.disable(options);
                syncVariantsCtl.disable(options);
                break;

            case ObjectPropertiesObjectType.PAGE:
                inheritCtl.disable(options);
                if (this.objTagSyncEnabled) {
                    syncContentCtl.enable(options);
                    syncVariantsCtl.enable(options);
                } else {
                    syncContentCtl.disable(options);
                    syncVariantsCtl.disable(options);
                }
                break;

            case ObjectPropertiesObjectType.IMAGE:
            case ObjectPropertiesObjectType.FILE:
            case ObjectPropertiesObjectType.TEMPLATE:
            default:
                inheritCtl.disable(options);
                syncContentCtl.disable(options);
                syncVariantsCtl.disable(options);
                break;
        }

        if (this.objTagSyncEnabled && this.multiChannelingEnabled) {
            syncChannelCtl.enable(options);
        } else {
            syncChannelCtl.disable(options);
        }
    }

    protected assembleValue(formData: ObjectPropertyBO<Normalized>): ObjectPropertyBO<Normalized> {
        if (this.mode === ObjectpropertyPropertiesMode.UPDATE) {
            return {
                ...formData,
                constructId: formData.constructId,
                globalId: this.value?.globalId,
                id: this.value?.id,
                keyword: this.value?.keyword,
            };
        } else {
            return {
                ...formData,
                constructId: Number(formData.constructId),
            };
        }
    }

    protected createNameValidator(): ValidatorFn {
        const validator = createI18nRequiredValidator((this.languages || []).map(lang => lang.code), langs => {
            this.invalidLanguages = langs;
            this.changeDetector.markForCheck();
        });

        return validator;
    }

    setActiveI18nTab(languageId: number): void {
        this.activeTabI18nLanguage = this.languages.find(l => l.id === languageId);
    }
}

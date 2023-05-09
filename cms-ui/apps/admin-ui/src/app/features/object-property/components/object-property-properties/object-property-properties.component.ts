import { createI18nRequiredValidator } from '@admin-ui/common';
import { ConstructDataService, LanguageDataService, ObjectPropertyCategoryDataService } from '@admin-ui/shared';
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
import { BasePropertiesComponent, CONTROL_INVALID_VALUE } from '@gentics/cms-components';
import {
    Language,
    ModelType,
    Normalized,
    ObjectPropertiesObjectType,
    ObjectPropertyBO,
    ObjectPropertyCategoryBO,
    Raw,
    TagTypeBO,
} from '@gentics/cms-models';
import { generateFormProvider } from '@gentics/ui-core';
import { Observable } from 'rxjs';

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
    providers: [generateFormProvider(ObjectpropertyPropertiesComponent)],
})
export class ObjectpropertyPropertiesComponent
    extends BasePropertiesComponent<ObjectPropertyBO<Normalized>> implements OnInit {

    @Input()
    public mode: ObjectpropertyPropertiesMode;

    @Output()
    public isValidChange = new EventEmitter<boolean>();

    public constructs$: Observable<TagTypeBO<Raw>[]>;
    public languages$: Observable<Language[]>;
    public objectPropertyCategories$: Observable<ObjectPropertyCategoryBO<Raw>[]>;

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
        private entityData: ConstructDataService,
        private categoryData: ObjectPropertyCategoryDataService,
        private languageData: LanguageDataService,
    ) {
        super(changeDetector);
    }

    ngOnInit(): void {
        super.ngOnInit();

        this.constructs$ = this.entityData.watchAllEntities();
        this.objectPropertyCategories$ = this.categoryData.watchAllEntities();
        this.languages$ = this.languageData.watchSupportedLanguages();

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
            keyword: new UntypedFormControl(this.value?.keyword, Validators.required),
            type: new UntypedFormControl(this.value?.type, Validators.required),
            constructId: new UntypedFormControl(this.value?.constructId + '', Validators.required),
            categoryId: new UntypedFormControl(this.value?.categoryId),
            required: new UntypedFormControl(this.value?.required),
            inheritable: new UntypedFormControl(this.value?.inheritable),
            syncContentset: new UntypedFormControl(this.value?.syncContentset),
            syncChannelset: new UntypedFormControl(this.value?.syncChannelset),
            syncVariants: new UntypedFormControl(this.value?.syncVariants),
        }, { updateOn: 'change' });
    }

    protected configureForm(value: ObjectPropertyBO<ModelType.Normalized>, loud: boolean = false): void {
        if (this.mode === ObjectpropertyPropertiesMode.UPDATE) {
            this.form.get('keyword').disable({ onlySelf: !loud });
        }
    }

    protected assembleValue(formData: ObjectPropertyBO<Normalized>): ObjectPropertyBO<Normalized> {
        if (this.mode === ObjectpropertyPropertiesMode.UPDATE) {
            return {
                ...formData,
                constructId: Number(formData.constructId),
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

    protected onValueChange(): void {
        if (this.form && this.value && (this.value as any) !== CONTROL_INVALID_VALUE) {
            this.form.setValue({
                nameI18n: this.value?.nameI18n ?? null,
                descriptionI18n: this.value?.descriptionI18n ?? null,
                keyword: this.value?.keyword ?? null,
                type: this.value?.type ?? null,
                constructId: (this.value?.constructId ?? '') + '',
                categoryId: this.value?.categoryId ?? null,
                required: this.value?.required ?? false,
                inheritable: this.value?.inheritable ?? false,
                syncContentset: this.value?.syncContentset ?? false,
                syncChannelset: this.value?.syncChannelset ?? false,
                syncVariants: this.value?.syncVariants ?? false,
            });
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

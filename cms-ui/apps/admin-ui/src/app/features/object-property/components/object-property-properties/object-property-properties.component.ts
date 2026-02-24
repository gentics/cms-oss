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
import { FormControl, FormGroup, ValidatorFn, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import {
    EditableObjectProperty,
    Feature,
    Language,
    ObjectPropertiesObjectType,
    ObjectPropertyCategory,
    Raw,
    TagType,
} from '@gentics/cms-models';
import { FormProperties, generateFormProvider, generateValidatorProvider, setControlsEnabled } from '@gentics/ui-core';
import { combineLatest } from 'rxjs';

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
    standalone: false,
})
export class ObjectpropertyPropertiesComponent
    extends BasePropertiesComponent<EditableObjectProperty> implements OnInit {

    @Input()
    public mode: ObjectpropertyPropertiesMode;

    @Output()
    public isValidChange = new EventEmitter<boolean>();

    public multiChannelingEnabled = false;
    public objTagSyncEnabled = false;

    public objectPropertyCategories: ObjectPropertyCategory<Raw>[];
    public constructs: TagType<Raw>[];
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

        this.subscriptions.push(this.constructHandler.listMapped().subscribe(res => {
            this.constructs = res.items;
            this.changeDetector.markForCheck();
        }));
        this.subscriptions.push(this.categoryHandler.listMapped().subscribe(res => {
            this.objectPropertyCategories = res.items;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.languageHandler.getSupportedLanguages().subscribe(languages => {
            this.languages = languages;

            if (this.form) {
                this.form.controls.nameI18n.setValidators(this.createNameValidator());

                const defaultDesc = {};
                const descCtl = this.form.controls.descriptionI18n;

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

    protected createForm(): FormGroup<FormProperties<EditableObjectProperty>> {
        const defaultDesc = {};
        (this.languages || []).forEach(l => {
            defaultDesc[l.code] = '';
        });

        return new FormGroup<FormProperties<EditableObjectProperty>>({
            nameI18n: new FormControl(this.safeValue('nameI18n'), this.createNameValidator()),
            descriptionI18n: new FormControl({
                ...defaultDesc,
                ...this.safeValue('descriptionI18n') || {},
            }),
            /* eslint-disable @typescript-eslint/unbound-method */
            keyword: new FormControl(this.safeValue('keyword'), Validators.required),
            type: new FormControl(this.safeValue('type'), Validators.required),
            constructId: new FormControl(this.safeValue('constructId'), Validators.required),
            categoryId: new FormControl(this.safeValue('categoryId')),
            required: new FormControl(this.safeValue('required')),
            inheritable: new FormControl(this.safeValue('inheritable')),
            syncContentset: new FormControl(this.safeValue('syncContentset')),
            syncChannelset: new FormControl(this.safeValue('syncChannelset')),
            syncVariants: new FormControl(this.safeValue('syncVariants')),
            restricted: new FormControl(this.safeValue('restricted')),
            /* eslint-disable @typescript-eslint/unbound-method */
        }, { updateOn: 'change' });
    }

    protected configureForm(value: EditableObjectProperty, loud: boolean = false): void {
        const options = { emitEvent: !!loud };
        if (this.mode === ObjectpropertyPropertiesMode.UPDATE) {
            this.form.controls.keyword.disable(options);
        }

        setControlsEnabled(this.form, ['inheritable'], value?.type === ObjectPropertiesObjectType.FOLDER, options);
        setControlsEnabled(
            this.form,
            ['syncContentset', 'syncVariants'],
            value?.type === ObjectPropertiesObjectType.PAGE && this.objTagSyncEnabled,
            options,
        );
        setControlsEnabled(this.form, ['syncChannelset'], this.objTagSyncEnabled && this.multiChannelingEnabled, options);
    }

    protected assembleValue(formData: EditableObjectProperty): EditableObjectProperty {
        return formData;
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

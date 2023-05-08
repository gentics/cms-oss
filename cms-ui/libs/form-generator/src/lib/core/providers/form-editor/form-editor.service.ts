import { Injectable } from '@angular/core';
import { CmsFormElementBO, CmsFormElementProperty, CmsFormElementPropertyType, CmsFormType } from '@gentics/cms-models';
import { BehaviorSubject, Observable } from 'rxjs';
import { filter, take } from 'rxjs/operators';
import {
    FormEditorConfiguration,
    FormElementConfiguration,
    FormElementPropertyOptionConfiguration,
    FormElementPropertyTypeConfiguration,
} from '../../../common/models/form-editor-configuration';
import { FormEditorConfigurationService } from '../form-editor-configuration/form-editor-configuration.service';

@Injectable()
export class FormEditorService {

    /** Current UI language. */
    set activeUiLanguageCode(v: string) {
        this._activeUiLanguageCode$.next(v);
    }
    get activeUiLanguageCode(): string {
        return this._activeUiLanguageCode$.getValue();
    }
    get activeUiLanguageCode$(): Observable<string> {
        return this._activeUiLanguageCode$.asObservable().pipe(
            filter(v => !!v),
        );
    }
    private _activeUiLanguageCode$ = new BehaviorSubject<string>(null);

    /** Current content language. */
    set activeContentLanguageCode(v: string) {
        this._activeContentLanguageCode$.next(v);
    }
    get activeContentLanguageCode(): string {
        return this._activeContentLanguageCode$.getValue();
    }
    get activeContentLanguageCode$(): Observable<string> {
        return this._activeContentLanguageCode$.asObservable().pipe(
            filter(v => !!v),
        );
    }
    private _activeContentLanguageCode$ = new BehaviorSubject<string>(null);

    /** All languages the form shall be available in. */
    set formLanguages(v: string[]) {
        this._formLanguages$.next(v);
    }
    get formLanguages(): string[] {
        return this._formLanguages$.getValue();
    }
    get formLanguages$(): Observable<string[]> {
        return this._formLanguages$.asObservable().pipe(
            filter(v => !!v),
        );
    }
    private _formLanguages$ = new BehaviorSubject<string[]>([]);

    private formElementsSubjects: { [key in CmsFormType]?: BehaviorSubject<CmsFormElementBO[]> } = {}

    constructor(private formEditorConfigurationService: FormEditorConfigurationService) { }

    /** Form elements provided by configuration. */
    getFormElements$ = (type: CmsFormType): Observable<CmsFormElementBO[]> => {
        if (!type) {
            type = CmsFormType.GENERIC;
        }
        if (!this.formElementsSubjects[type]) {
            this.formElementsSubjects[type] = new BehaviorSubject(null);
            this.formEditorConfigurationService.getConfiguration$(type).subscribe((configuration: FormEditorConfiguration) => {
                // since form editor configurations are never updated / never fetched a second time, we can cache the mapped result
                this.formElementsSubjects[type].next(this.generateAvailableElementsAndPropertiesFromConfiguration(configuration));
            });
        }
        return this.formElementsSubjects[type].asObservable().pipe(
            filter(configuration => configuration !== null),
            take(1),
        );
    }

    private generateAvailableElementsAndPropertiesFromConfiguration = (configuration: FormEditorConfiguration): CmsFormElementBO[] => {
        const formElements: CmsFormElementBO[] = configuration.elements.map(elementConfiguration => {
            const formElement: CmsFormElementBO = {
                name: null,
                type: elementConfiguration.type,
                active: true,
                type_label_i18n_ui: elementConfiguration.type_label_i18n_ui,
                description_i18n_ui: elementConfiguration.description_i18n_ui,
                label_property_ui: elementConfiguration.label_property_ui,
                isContainer: elementConfiguration.is_container,
                cannotContain: elementConfiguration.cannot_contain,
                elements: [],
            };

            if (elementConfiguration.properties.length > 0) {
                formElement.properties = this.mapFormProperties(elementConfiguration);
            }

            return formElement;
        });

        return formElements;
    }

    private mapFormProperties(elementConfiguration: FormElementConfiguration): CmsFormElementProperty[] {
        return elementConfiguration.properties.map(propertyConfiguration => {
            switch (propertyConfiguration.type) {
                case FormElementPropertyTypeConfiguration.SELECT:
                    return {
                        name: propertyConfiguration.name,
                        type: CmsFormElementPropertyType[propertyConfiguration.type],
                        label_i18n_ui: propertyConfiguration.label_i18n_ui,
                        description_i18n_ui: propertyConfiguration.description_i18n_ui,
                        required: propertyConfiguration.required,
                        options: propertyConfiguration.options.map((optionConfiguration: FormElementPropertyOptionConfiguration) => {
                            return { key: optionConfiguration.key, value_i18n_ui: optionConfiguration.value_i18n_ui };
                        }),
                        value_i18n: propertyConfiguration.default_value_i18n ? propertyConfiguration.default_value_i18n : null,
                    };

                case FormElementPropertyTypeConfiguration.SELECTABLE_OPTIONS:
                    return {
                        name: propertyConfiguration.name,
                        type: CmsFormElementPropertyType[propertyConfiguration.type],
                        label_i18n_ui: propertyConfiguration.label_i18n_ui,
                        description_i18n_ui: propertyConfiguration.description_i18n_ui,
                        key_label_i18n_ui: propertyConfiguration.key_label_i18n_ui,
                        value_label_i18n_ui: propertyConfiguration.value_label_i18n_ui,
                        required: propertyConfiguration.required,
                        value_i18n: propertyConfiguration.default_value ? propertyConfiguration.default_value : null,
                    };
                case FormElementPropertyTypeConfiguration.REPOSITORY_BROWSER:
                    return {
                        name: propertyConfiguration.name,
                        type: CmsFormElementPropertyType[propertyConfiguration.type],
                        label_i18n_ui: propertyConfiguration.label_i18n_ui,
                        description_i18n_ui: propertyConfiguration.description_i18n_ui,
                        required: propertyConfiguration.required,
                        options: propertyConfiguration.options,
                        value: propertyConfiguration.default_value ? propertyConfiguration.default_value : null,
                    };
                case FormElementPropertyTypeConfiguration.BOOLEAN:
                case FormElementPropertyTypeConfiguration.NUMBER:
                case FormElementPropertyTypeConfiguration.STRING:
                default:
                    return {
                        name: propertyConfiguration.name,
                        type: CmsFormElementPropertyType[propertyConfiguration.type],
                        label_i18n_ui: propertyConfiguration.label_i18n_ui,
                        description_i18n_ui: propertyConfiguration.description_i18n_ui,
                        required: propertyConfiguration.required,
                        validation: propertyConfiguration.validator,
                        value_i18n: propertyConfiguration.default_value_i18n ? propertyConfiguration.default_value_i18n : null,
                    };
            }
        });
    }

}

/* eslint-disable @typescript-eslint/no-unsafe-call */
import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import {
    CmsFormElementProperty,
    CmsFormElementPropertyType,
} from '@gentics/cms-models';
import { isEqual } from 'lodash-es';
import { BehaviorSubject, Subject, Subscription } from 'rxjs';
import { distinctUntilChanged } from 'rxjs/operators';
import { FormEditorService } from '../../providers';

@Component({
    selector: 'gtx-form-element-properties-editor',
    templateUrl: './form-element-properties-editor.component.html',
    styleUrls: ['./form-element-properties-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FormElementPropertiesEditorComponent implements OnInit, OnChanges, OnDestroy {

    public readonly CmsFormElementPropertyType = CmsFormElementPropertyType;

    @Input()
    properties: CmsFormElementProperty[] = [];

    @Output()
    propertiesChange: EventEmitter<CmsFormElementProperty[]> = new EventEmitter<CmsFormElementProperty[]>();

    /**
     * Emits whether there is a translation missing in the active language
     */
    @Output()
    translationErrorStatus = new EventEmitter<boolean>();

    /**
     * Emits whether there is a required value missing or any other type of validation fails in the active language
     */
    @Output()
    requiredOrValidationErrorStatus = new EventEmitter<boolean>();

    private propertiesSubject: BehaviorSubject<CmsFormElementProperty[]> = new BehaviorSubject([]);

    formGroup: UntypedFormGroup;
    private formGroupSubscription: Subscription;
    private formGroupStatusSubscription: Subscription;

    private destroyed$ = new Subject<void>();

    constructor(
        private formBuilder: UntypedFormBuilder,
        public formEditorService: FormEditorService,
    ) {}

    ngOnInit(): void {
        this.propertiesSubject.asObservable().pipe(
            distinctUntilChanged((oldVal, newVal) => {
                const reducer = (mapping, property) => {
                    mapping[property.name] = property.type;
                    return mapping;
                };
                const oldMapping: Record<string, CmsFormElementPropertyType> = oldVal.reduce(reducer, {});
                const newMapping: Record<string, CmsFormElementPropertyType> = newVal.reduce(reducer, {});

                return isEqual(oldMapping, newMapping);
            }),
        ).subscribe((properties) => {
            this.createFormGroup(properties);
        });
    }

    ngOnChanges(): void {
        if (!Array.isArray(this.properties)) {
            this.properties = [];
        }
        this.propertiesSubject.next(this.properties);
    }

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
        this.propertiesSubject.complete();

        if (this.formGroupSubscription) {
            this.formGroupSubscription.unsubscribe();
        }
        if (this.formGroupStatusSubscription) {
            this.formGroupStatusSubscription.unsubscribe();
        }
    }

    private createFormGroup(
        properties: CmsFormElementProperty[],
    ): void {
        const controlsConfig = properties.reduce((
            currentControlsConfig: any,
            property: CmsFormElementProperty) =>
        {
            switch (property.type) {
                case CmsFormElementPropertyType.SELECTABLE_OPTIONS:
                    currentControlsConfig[property.name] = [property.value];
                    break;
                case CmsFormElementPropertyType.REPOSITORY_BROWSER:
                    currentControlsConfig[property.name] = { id: property.value, nodeId: property.nodeId };
                    break;
                case CmsFormElementPropertyType.BOOLEAN:
                case CmsFormElementPropertyType.SELECT:
                case CmsFormElementPropertyType.NUMBER:
                case CmsFormElementPropertyType.STRING:
                    currentControlsConfig[property.name] = [property.value_i18n];
                    break;
            }

            return currentControlsConfig;
        }, {});

        if (this.formGroupSubscription) {
            this.formGroupSubscription.unsubscribe();
        }
        if (this.formGroupStatusSubscription) {
            this.formGroupStatusSubscription.unsubscribe();
        }

        this.formGroup = this.formBuilder.group(controlsConfig);
        // improve by listening to changes per form control (not on the whole group)
        this.formGroupSubscription = this.formGroup.valueChanges.subscribe((formValues) => {
            (this.properties || []).forEach((property: CmsFormElementProperty) => {
                switch (property.type) {
                    case CmsFormElementPropertyType.SELECTABLE_OPTIONS:
                        property.value = formValues[property.name]?.trim();
                        break;
                    case CmsFormElementPropertyType.REPOSITORY_BROWSER:
                        property.value = formValues[property.name]?.id;
                        property.nodeId = formValues[property.name]?.nodeId;
                        break;
                    case CmsFormElementPropertyType.BOOLEAN:
                    case CmsFormElementPropertyType.SELECT:
                    case CmsFormElementPropertyType.NUMBER:
                    case CmsFormElementPropertyType.STRING:
                        property.value_i18n = formValues[property.name]?.trim();
                        break;
                }
            });
            this.propertiesChange.emit(this.properties);
        });

        this.formGroupStatusSubscription = this.formGroup.statusChanges.subscribe((status) => {
            if (status === 'VALID') {
                this.translationErrorStatus.emit(false);
                this.requiredOrValidationErrorStatus.emit(false);
            } else if (status === 'INVALID') {
                let untranslated = false;
                let requiredInCurrentLanguage = false;
                for (const property of this.properties) {
                    const control = this.formGroup.controls[property.name];
                    if (control && control.status === 'INVALID') {
                        if (control.hasError('untranslated')) {
                            untranslated = true;
                        }
                        if (control.hasError('requiredInCurrentLanguage')) {
                            requiredInCurrentLanguage = true;
                        }
                        if (control.hasError('invalidSelection')) {
                            requiredInCurrentLanguage = true;
                        }
                        if (control.hasError('duplicateKeys')) {
                            requiredInCurrentLanguage = true;
                        }
                        if (untranslated && requiredInCurrentLanguage) {
                            // if both flags are already true, we can stop searching
                            break;
                        }
                    }
                }
                this.translationErrorStatus.emit(untranslated);
                this.requiredOrValidationErrorStatus.emit(requiredInCurrentLanguage);
            }
        });

        this.formGroup.updateValueAndValidity();
        // needed due to a life cycle bug
        this.formGroup.updateValueAndValidity();
    }

    identify(_index: number, element: CmsFormElementProperty): string {
        return element.name;
    }
}

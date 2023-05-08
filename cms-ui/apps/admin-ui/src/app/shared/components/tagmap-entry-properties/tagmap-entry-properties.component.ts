import {
    FormControlOnChangeFn,
    FormControlOnTouchedFn,
    MESH_TAGMAP_ENTRY_ATTRIBUTES,
    ObservableStopper,
    SQL_TAGMAP_ENTRY_ATTRIBUTES,
    TAGMAP_ENTRY_ATTRIBUTES,
} from '@admin-ui/common';
import { TagmapEntryDisplayFields } from '@admin-ui/shared/components/create-update-tagmapentry-modal';
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
import { AbstractControl, ControlValueAccessor, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { GtxJsonValidator } from '@gentics/cms-components';
import {
    MeshTagmapEntryAttributeTypes,
    Normalized,
    SQLTagmapEntryAttributeTypes,
    TagmapEntryAttributeTypes,
    TagmapEntryBO,
    TagmapEntryPropertiesObjectType,
} from '@gentics/cms-models';
import { generateFormProvider } from '@gentics/ui-core';
import { map, takeUntil, tap } from 'rxjs/operators';

export interface TagmapEntryPropertiesFormData {
    mapname: string;
    tagname: string;
    objType: number;
    attributeType: number;
    multivalue: boolean;
    optimized?: boolean;
    reserved?: boolean;
    filesystem?: boolean;
    targetType?: number;
    foreignlinkAttribute?: string;
    foreignlinkAttributeRule?: string;
    urlfield?: boolean;
    elasticsearch?: string;
    micronodeFilter?: string;
    segmentfield?: boolean;
    displayfield?: boolean;
}

export enum TagmapEntryPropertiesMode {
    CREATE = 'create',
    UPDATE = 'update',
}

/**
 * Defines the data editable by the `TagmapEntryPropertiesComponent`.
 *
 * To convey the validity state of the user's input, the onChange callback will
 * be called with `null` if the form data is currently invalid.
 */
@Component({
    selector: 'gtx-tagmap-entry-properties',
    templateUrl: './tagmap-entry-properties.component.html',
    styleUrls: [ './tagmap-entry-properties.component.scss' ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(TagmapEntryPropertiesComponent)],
    })
export class TagmapEntryPropertiesComponent implements AfterViewInit, OnInit, OnDestroy, ControlValueAccessor {

    @Input()
    mode: TagmapEntryPropertiesMode;

    @Input()
    displayFields: TagmapEntryDisplayFields;

    @Input()
    value: TagmapEntryBO<Normalized>;

    @Output()
    valueChange = new EventEmitter<TagmapEntryBO<Normalized>>();

    @Output()
    isValidChange = new EventEmitter<boolean>();

    fgProperties: UntypedFormGroup;

    isModeUpdate: boolean;

    attributes: { id: TagmapEntryAttributeTypes; label: string; }[] = [];

    /** selectable options for contentRepository input objecttype */
    readonly objectTypes: readonly { id: TagmapEntryPropertiesObjectType; label: string; }[] = [
        {
            id: TagmapEntryPropertiesObjectType.FOLDER,
            label: 'tagmapEntry.objecttype_folder',
        },
        {
            id: TagmapEntryPropertiesObjectType.PAGE,
            label: 'tagmapEntry.objecttype_page',
        },
        {
            id: TagmapEntryPropertiesObjectType.FILE,
            label: 'tagmapEntry.objecttype_file',
        },
    ] as const;

    private stopper = new ObservableStopper();

    constructor(
        private changeDetectorRef: ChangeDetectorRef,
    ) { }

    ngOnInit(): void {
        this.fgPropertiesInit();

        switch (this.displayFields) {
            case TagmapEntryDisplayFields.ALL:
                this.attributes = TAGMAP_ENTRY_ATTRIBUTES;
                break;

            case TagmapEntryDisplayFields.MESH:
                this.attributes = MESH_TAGMAP_ENTRY_ATTRIBUTES;
                break;

            case TagmapEntryDisplayFields.SQL:
                this.attributes = SQL_TAGMAP_ENTRY_ATTRIBUTES;
                break;
        }
    }

    ngAfterViewInit(): void {
        // Set FormGroup logic and rendering dependencies from external value
        this.isModeUpdate = this.mode === TagmapEntryPropertiesMode.UPDATE;
        // refresh form with not null dependencies
        setTimeout(() => {
            this.configureFormControls(this.value);
            this.fgProperties.updateValueAndValidity();
            this.changeDetectorRef.markForCheck();
        });
    }

    ngOnDestroy(): void {
        this.stopper.stop();
    }

    writeValue(value: TagmapEntryBO<Normalized>): void {
        if (!value || !this.fgProperties) {
            return;
        }
        this.value = value;
        this.fgPropertiesUpdate(value);
    }

    registerOnChange(fn: FormControlOnChangeFn<TagmapEntryBO<Normalized>>): void {
        this.fgProperties.valueChanges.pipe(
            map((formData: TagmapEntryPropertiesFormData) => {
                this.value = this.assembleValue(formData);
                this.configureFormControls(formData);
                return this.value;
            }),
            tap(() => this.isValidChange.emit(this.fgProperties.valid)),
            takeUntil(this.stopper.stopper$),
        ).subscribe(fn);
    }

    registerOnTouched(fn: FormControlOnTouchedFn): void { }

    setDisabledState(isDisabled: boolean): void {
        if (isDisabled) {
            this.fgProperties.disable({ emitEvent: false });
        } else {
            this.fgProperties.enable({ emitEvent: false });
        }
    }

    /**
     * Alter FormGroup depending from values, which input fields appear and disappear or change validation logic.
     *
     * @param value values of active fields to be to (re-)initialized
     */
    private configureFormControls(value: TagmapEntryBO<Normalized> | TagmapEntryPropertiesFormData): void {
        if (!value?.attributeType) {
            return;
        }
        const _options = { emitEvent: false };

        // reset controls
        this.fgProperties.removeControl('urlfield', _options);
        this.fgProperties.removeControl('elasticsearch', _options);
        this.fgProperties.removeControl('micronodeFilter', _options);
        this.fgProperties.removeControl('filesystem', _options);
        this.fgProperties.removeControl('optimized', _options);
        this.fgProperties.removeControl('targetType', _options);
        this.fgProperties.removeControl('foreignlinkAttribute', _options);
        this.fgProperties.removeControl('foreignlinkAttributeRule', _options);

        if (this.displayFields === TagmapEntryDisplayFields.ALL || this.displayFields === TagmapEntryDisplayFields.MESH) {
            // handle JSON data
            let esSettings: string;
            if (value.elasticsearch instanceof Object) {
                try {
                    esSettings = JSON.stringify(value.elasticsearch, null, 4);
                } catch (error) {
                    esSettings = String(value.elasticsearch);
                }
            } else if (typeof value.elasticsearch === 'string') {
                esSettings = value.elasticsearch;
            } else {
                esSettings = '';
            }

            // fields of CrTypeMesh
            this.fgProperties.setControl('urlfield', new UntypedFormControl(value.urlfield), _options);
            this.fgProperties.setControl('elasticsearch', new UntypedFormControl(esSettings, GtxJsonValidator), _options);

            switch (value.attributeType) {
                case MeshTagmapEntryAttributeTypes.BINARY:
                case MeshTagmapEntryAttributeTypes.BOOLEAN:
                case MeshTagmapEntryAttributeTypes.DATE:
                case MeshTagmapEntryAttributeTypes.INTEGER:
                case MeshTagmapEntryAttributeTypes.TEXT:
                    this.fgProperties.removeControl('micronodeFilter', _options);
                    break;

                case MeshTagmapEntryAttributeTypes.MICRONODE:
                    this.fgProperties.setControl('micronodeFilter', new UntypedFormControl(value.micronodeFilter), _options);
                    break;

                case MeshTagmapEntryAttributeTypes.REFERENCE:
                    this.fgProperties.setControl('targetType', new UntypedFormControl(value.targetType), _options);
                    break;

                default:
                    if (this.displayFields !== TagmapEntryDisplayFields.ALL) {
                        throw new Error(`Selected value "${value.attributeType}" is invalid for crType Mesh.`);
                    }
            }
        }

        if (this.displayFields === TagmapEntryDisplayFields.ALL || this.displayFields === TagmapEntryDisplayFields.SQL) {
            // fields of old CRs
            this.fgProperties.setControl('filesystem', new UntypedFormControl(value.filesystem), _options);
            this.fgProperties.setControl('optimized', new UntypedFormControl(value.optimized), _options);

            switch (value.attributeType) {
                case SQLTagmapEntryAttributeTypes.TEXT:
                case SQLTagmapEntryAttributeTypes.INTEGER:
                case SQLTagmapEntryAttributeTypes.TEXT_LONG:
                case SQLTagmapEntryAttributeTypes.BINARY:
                    break;

                case SQLTagmapEntryAttributeTypes.REFERENCE:
                    this.fgProperties.setControl('targetType', new UntypedFormControl(value.targetType, Validators.required), _options);
                    break;

                case SQLTagmapEntryAttributeTypes.FOREIGN_LINK:
                    this.fgProperties.setControl('targetType', new UntypedFormControl(value.targetType, Validators.required), _options);
                    this.fgProperties.setControl('foreignlinkAttribute', new UntypedFormControl(value.foreignlinkAttribute, Validators.required), _options);
                    this.fgProperties.setControl('foreignlinkAttributeRule', new UntypedFormControl(value.foreignlinkAttributeRule), _options);
                    break;

                default:
                    if (this.displayFields !== TagmapEntryDisplayFields.ALL) {
                        throw new Error(`Selected value "${value.attributeType}" is invalid for crType SQL.`);
                    }
            }
        }
    }

    /**
     * Initialize form 'Properties'
     */
    private fgPropertiesInit(): void {
        // Set FormGroup logic and rendering dependencies initially.
        this.isModeUpdate = this.mode === TagmapEntryPropertiesMode.UPDATE;

        let controls: Record<string, AbstractControl> = {
            mapname: new UntypedFormControl('', Validators.required),
            tagname: new UntypedFormControl('', Validators.required),
            objType: new UntypedFormControl({
                value: '',
                disabled: this.isModeUpdate && this.displayFields !== TagmapEntryDisplayFields.ALL,
            }, Validators.required),
            attributeType: new UntypedFormControl({
                value: '',
                disabled: this.isModeUpdate && this.displayFields !== TagmapEntryDisplayFields.ALL,
            }, Validators.required),
            multivalue: new UntypedFormControl({
                value: false,
                disabled: this.isModeUpdate && this.displayFields !== TagmapEntryDisplayFields.ALL,
            }),
        };

        if (this.displayFields === TagmapEntryDisplayFields.ALL || this.displayFields === TagmapEntryDisplayFields.MESH) {
            controls = {
                ...controls,
                urlfield: new UntypedFormControl(false),
                elasticsearch: new UntypedFormControl('', GtxJsonValidator),
            };
        }

        if (this.displayFields === TagmapEntryDisplayFields.ALL || this.displayFields === TagmapEntryDisplayFields.SQL) {
            controls = {
                ...controls,
                filesystem: new UntypedFormControl(false),
                optimized: new UntypedFormControl(false),
            };
        }

        this.fgProperties = new UntypedFormGroup(controls);
    }

    private hasControl(formControlname: string): boolean {
        return !!this.fgProperties.get(formControlname);
    }

    private fgPropertiesUpdate(value: Partial<TagmapEntryBO<Normalized>>): void {
        const tmpValue: TagmapEntryPropertiesFormData = {
            mapname: value.mapname,
            tagname: value.tagname,
            objType: value.objType || value.object,
            attributeType: value.attributeType,
            multivalue: value.multivalue,
        };

        const optionalFields: (keyof TagmapEntryPropertiesFormData)[] = [
            'optimized',
            'reserved',
            'filesystem',
            'targetType',
            'foreignlinkAttribute',
            'foreignlinkAttributeRule',
            'urlfield',
            'micronodeFilter',
            'segmentfield',
            'displayfield',
        ];

        optionalFields.forEach(fieldName => {
            if (this.hasControl(fieldName)) {
                (tmpValue[fieldName] as any) = value[fieldName];
            }
        });

        // Elasticsearch field requires special handling
        if (this.hasControl('elasticsearch')) {
            tmpValue.elasticsearch = value.elasticsearch ? JSON.stringify(value.elasticsearch, null, 4) : '';
        }

        this.fgProperties.setValue(tmpValue);
        this.fgProperties.markAsPristine();
    }

    private assembleValue(formData: TagmapEntryPropertiesFormData): TagmapEntryBO<Normalized> {
        const output: TagmapEntryBO<Normalized> = {
            globalId: this.value.globalId,
            id: this.value.id,
            tagname: formData.tagname,
            mapname: formData.mapname,
            objType: formData.objType || formData.objType,
            attributeType: formData.attributeType,
            targetType: formData.targetType,
            multivalue: !!formData.multivalue,
            optimized: !!formData.optimized,
            reserved: !!formData.reserved,
            filesystem: !!formData.filesystem,
            foreignlinkAttribute: formData.foreignlinkAttribute,
            foreignlinkAttributeRule: formData.foreignlinkAttributeRule,
            category: this.value.category,
            segmentfield: formData.segmentfield,
            displayfield: formData.displayfield,
            urlfield: !!formData.urlfield,
            elasticsearch: null,
            micronodeFilter: formData.micronodeFilter,
            fragmentName: this.value.fragmentName,
        };

        if (formData.elasticsearch) {
            try {
                output.elasticsearch = JSON.parse(formData.elasticsearch);
            } catch (error) {
                output.elasticsearch = null;
            }
        }

        return output;
    }

}

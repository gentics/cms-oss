import {
    MESH_TAGMAP_ENTRY_ATTRIBUTES,
    SQL_TAGMAP_ENTRY_ATTRIBUTES,
    TAGMAP_ENTRY_ATTRIBUTES,
} from '@admin-ui/common';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnChanges,
    OnInit,
    SimpleChanges,
} from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent, GtxJsonValidator } from '@gentics/cms-components';
import {
    EditableTagmapEntry,
    MeshTagmapEntryAttributeTypes,
    SQLTagmapEntryAttributeTypes,
    TagmapEntryAttributeTypes,
    TagmapEntryPropertiesObjectType,
} from '@gentics/cms-models';
import { FormProperties, generateFormProvider, generateValidatorProvider, setControlsEnabled } from '@gentics/ui-core';
import { environment } from 'apps/admin-ui/src/environments/environment';
import { TagmapEntryDisplayFields } from '../create-update-tagmapentry-modal/create-update-tagmapentry-modal.component';

export enum TagmapEntryPropertiesMode {
    CREATE = 'create',
    UPDATE = 'update',
}

/** These properties are locked/readonly when the tagmap entry is marked as `reserved` */
const RESERVED_LOCKED_PROPERTIES: (keyof EditableTagmapEntry)[] = [
    'tagname',
    'mapname',
    'objType',
    'attributeType',
    'targetType',
    'multivalue',
    'filesystem',
    'foreignlinkAttribute',
    'foreignlinkAttributeRule',
    'segmentfield',
    'displayfield',
];

@Component({
    selector: 'gtx-tag-map-entry-properties',
    templateUrl: './tag-map-entry-properties.component.html',
    styleUrls: ['./tag-map-entry-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(TagMapEntryPropertiesComponent),
        generateValidatorProvider(TagMapEntryPropertiesComponent),
    ],
})
export class TagMapEntryPropertiesComponent extends BasePropertiesComponent<EditableTagmapEntry> implements OnInit, OnChanges {

    @Input()
    public mode: TagmapEntryPropertiesMode;

    @Input()
    public displayFields: TagmapEntryDisplayFields;

    @Input()
    public reserved = false;

    attributes: { id: TagmapEntryAttributeTypes; label: string; }[] = [];

    /** selectable options for contentRepository input objecttype */
    readonly OBJECT_TYPES: readonly { id: TagmapEntryPropertiesObjectType; label: string; }[] = [
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

    constructor(
        changeDetector: ChangeDetectorRef,
    ) {
        super(changeDetector);
    }

    ngOnInit(): void {
        super.ngOnInit();
        this.updateAttributes();

        setTimeout(() => {
            this.form.updateValueAndValidity();
            this.changeDetector.markForCheck();
        }, 10);
    }

    ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.displayFields) {
            this.updateAttributes();
        }
    }

    protected updateAttributes(): void {
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

    protected createForm(): FormGroup<FormProperties<EditableTagmapEntry>> {
        return new FormGroup<FormProperties<EditableTagmapEntry>>({
            mapname: new FormControl(this.value?.mapname || '', Validators.required),
            tagname: new FormControl(this.value?.tagname || '', Validators.required),
            objType: new FormControl(this.value?.objType ?? 0, Validators.required),
            attributeType: new FormControl(this.value?.attributeType ?? 0, Validators.required),
            multivalue: new FormControl(this.value?.multivalue ?? false),
            targetType: new FormControl(this.value?.targetType),
            segmentfield: new FormControl(this.value?.segmentfield ?? false),
            displayfield: new FormControl(this.value?.displayfield ?? false),
            category: new FormControl(this.value?.category ?? ''),
            // Mesh CR
            urlfield: new FormControl(this.value?.urlfield ?? false),
            noIndex: new FormControl(this.value?.noIndex ?? false),
            elasticsearch: new FormControl(this.value?.elasticsearch ?? null, GtxJsonValidator),
            micronodeFilter: new FormControl(this.value?.micronodeFilter),
            // SQL CR
            filesystem: new FormControl(this.value?.filesystem ?? false),
            optimized: new FormControl(this.value?.optimized ?? false),
            foreignlinkAttribute: new FormControl(this.value?.foreignlinkAttribute, Validators.required),
            foreignlinkAttributeRule: new FormControl(this.value?.foreignlinkAttributeRule),
        });
    }

    protected configureForm(value: EditableTagmapEntry, loud?: boolean): void {
        const options = { emitEvent: !!loud };
        const dynamicControls: (keyof EditableTagmapEntry)[] = [
            'urlfield',
            'noIndex',
            'elasticsearch',
            'micronodeFilter',
            'filesystem',
            'optimized',
            'targetType',
            'foreignlinkAttribute',
            'foreignlinkAttributeRule',
        ];

        // On default, disable all controls which may not be valid
        for (const ctlName of dynamicControls) {
            this.form.controls[ctlName].disable(options);
        }

        const enableControls: (keyof EditableTagmapEntry)[] = [];
        let targetTypeIsRequired = false;

        if (this.displayFields === TagmapEntryDisplayFields.ALL || this.displayFields === TagmapEntryDisplayFields.MESH) {

            // fields of CrTypeMesh
            enableControls.push('urlfield', 'noIndex', 'elasticsearch');

            switch (value?.attributeType) {
                case MeshTagmapEntryAttributeTypes.BINARY:
                case MeshTagmapEntryAttributeTypes.BOOLEAN:
                case MeshTagmapEntryAttributeTypes.DATE:
                case MeshTagmapEntryAttributeTypes.INTEGER:
                case MeshTagmapEntryAttributeTypes.TEXT:
                    break;

                case MeshTagmapEntryAttributeTypes.MICRONODE:
                    enableControls.push('micronodeFilter');
                    break;

                case MeshTagmapEntryAttributeTypes.REFERENCE:
                    enableControls.push('targetType');
                    break;

                default:
                    if (!value?.attributeType) {
                        break;
                    }

                    if (this.displayFields !== TagmapEntryDisplayFields.ALL) {
                        const msg = `Selected value "${value?.attributeType}" is invalid for crType Mesh.`;
                        if (environment.production) {
                            console.warn(msg);
                        } else {
                            throw new Error(msg);
                        }
                    }
            }
        }

        if (this.displayFields === TagmapEntryDisplayFields.ALL || this.displayFields === TagmapEntryDisplayFields.SQL) {
            // fields of old CRs
            enableControls.push('filesystem', 'optimized');

            switch (value?.attributeType) {
                case SQLTagmapEntryAttributeTypes.TEXT:
                case SQLTagmapEntryAttributeTypes.INTEGER:
                case SQLTagmapEntryAttributeTypes.TEXT_LONG:
                case SQLTagmapEntryAttributeTypes.BINARY:
                    break;

                case SQLTagmapEntryAttributeTypes.REFERENCE:
                    targetTypeIsRequired = true;
                    enableControls.push('targetType');
                    break;

                case SQLTagmapEntryAttributeTypes.FOREIGN_LINK:
                    targetTypeIsRequired = true;
                    enableControls.push('targetType', 'foreignlinkAttribute', 'foreignlinkAttributeRule');
                    break;

                default:
                    if (!value?.attributeType) {
                        break;
                    }

                    if (this.displayFields !== TagmapEntryDisplayFields.ALL) {
                        const msg = `Selected value "${value?.attributeType}" is invalid for crType SQL.`;
                        if (environment.production) {
                            console.warn(msg);
                        } else {
                            throw new Error(msg);
                        }
                    }
            }
        }

        this.form.controls.targetType.setValidators(targetTypeIsRequired ? Validators.required : null);

        setControlsEnabled(this.form, enableControls, true, options);

        if (this.reserved) {
            setControlsEnabled(this.form, RESERVED_LOCKED_PROPERTIES, false, options);
        }
    }

    protected assembleValue(value: EditableTagmapEntry): EditableTagmapEntry {
        return value;
    }

    protected override onValueChange(): void {
        if (this.form && this.value) {
            const tmpObj: Partial<EditableTagmapEntry> = {};
            Object.keys(this.form.controls).forEach(controlName => {
                if (this.value?.hasOwnProperty?.(controlName)) {
                    tmpObj[controlName] = this.value?.[controlName];
                }
            });

            if (tmpObj?.elasticsearch) {
                // handle JSON data
                let esSettings: string;
                if (tmpObj?.elasticsearch instanceof Object) {
                    try {
                        esSettings = JSON.stringify(tmpObj.elasticsearch, null, 4);
                    } catch (error) {
                        esSettings = String(tmpObj.elasticsearch);
                    }
                } else if (typeof tmpObj?.elasticsearch === 'string') {
                    esSettings = tmpObj.elasticsearch;
                } else {
                    esSettings = '';
                }

                (tmpObj as any).elasticsearch = esSettings;
            }

            this.form.patchValue(tmpObj);
        }
    }
}

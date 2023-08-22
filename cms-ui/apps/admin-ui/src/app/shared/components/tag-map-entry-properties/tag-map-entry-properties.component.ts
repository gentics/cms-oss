import {
    MESH_TAGMAP_ENTRY_ATTRIBUTES,
    SQL_TAGMAP_ENTRY_ATTRIBUTES,
    TAGMAP_ENTRY_ATTRIBUTES,
} from '@admin-ui/common';
import { TagmapEntryDisplayFields } from '@admin-ui/shared/components/create-update-tagmapentry-modal';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnChanges,
    OnInit,
    SimpleChanges,
} from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent, CONTROL_INVALID_VALUE, GtxJsonValidator } from '@gentics/cms-components';
import {
    AnyModelType,
    MeshTagmapEntryAttributeTypes,
    SQLTagmapEntryAttributeTypes,
    TagmapEntry,
    TagmapEntryAttributeTypes,
    TagmapEntryPropertiesObjectType,
} from '@gentics/cms-models';
import { generateFormProvider } from '@gentics/ui-core';
import { environment } from 'apps/admin-ui/src/environments/environment';

export enum TagmapEntryPropertiesMode {
    CREATE = 'create',
    UPDATE = 'update',
}

@Component({
    selector: 'gtx-tag-map-entry-properties',
    templateUrl: './tag-map-entry-properties.component.html',
    styleUrls: ['./tag-map-entry-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(TagMapEntryPropertiesComponent)],
})
export class TagMapEntryPropertiesComponent extends BasePropertiesComponent<TagmapEntry> implements OnInit, OnChanges {

    @Input()
    mode: TagmapEntryPropertiesMode;

    @Input()
    displayFields: TagmapEntryDisplayFields;

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

    protected createForm(): UntypedFormGroup {
        return new UntypedFormGroup({
            mapname: new UntypedFormControl('', Validators.required),
            tagname: new UntypedFormControl('', Validators.required),
            objType: new UntypedFormControl('', Validators.required),
            attributeType: new UntypedFormControl('', Validators.required),
            multivalue: new UntypedFormControl(false),
            targetType: new UntypedFormControl(null),
            segmentfield: new UntypedFormControl(null),
            displayfield: new UntypedFormControl(null),
            // Mesh CR
            urlfield: new UntypedFormControl(false),
            elasticsearch: new UntypedFormControl('', GtxJsonValidator),
            micronodeFilter: new UntypedFormControl(null),
            // SQL CR
            filesystem: new UntypedFormControl(false),
            optimized: new UntypedFormControl(false),
            foreignlinkAttribute: new UntypedFormControl(null, Validators.required),
            foreignlinkAttributeRule: new UntypedFormControl(null),
        });
    }

    protected configureForm(value: TagmapEntry<AnyModelType>, loud?: boolean): void {
        const options = { emitEvent: !!loud };
        const dynamicControls = [
            'urlfield',
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
            this.form.get(ctlName).disable({ emitEvent: false });
        }

        const enableControls: string[] = [];
        let targetTypeIsRequired = false;

        if (this.displayFields === TagmapEntryDisplayFields.ALL || this.displayFields === TagmapEntryDisplayFields.MESH) {


            // fields of CrTypeMesh
            enableControls.push('urlfield', 'elasticsearch');

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

        this.form.get('targetType').setValidators(targetTypeIsRequired ? Validators.required : null);
        for (const ctlName of enableControls) {
            this.form.get(ctlName).enable(options);
        }
    }

    protected assembleValue(value: TagmapEntry<AnyModelType>): TagmapEntry<AnyModelType> {
        return value;
    }

    protected override onValueChange(): void {
        if (this.form && this.value && (this.value as any) !== CONTROL_INVALID_VALUE) {
            const tmpObj: Partial<TagmapEntry> = {};
            Object.keys(this.form.controls).forEach(controlName => {
                tmpObj[controlName] = this.value?.[controlName] ?? null;
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

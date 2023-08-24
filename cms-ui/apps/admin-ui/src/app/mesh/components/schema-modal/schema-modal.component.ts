import { MicroschemaHandlerService, SchemaHandlerService } from '@admin-ui/mesh/providers';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { EditableSchemaProperties, ElasticSearchSettings, Schema, SchemaField } from '@gentics/mesh-models';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';
import { BaseModal, createJsonValidator } from '@gentics/ui-core';
import { SchemaPropertiesMode } from '../schema-properties/schema-properties.component';

@Component({
    selector: 'gtx-mesh-schema-modal',
    templateUrl: './schema-modal.component.html',
    styleUrls: ['./schema-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SchemaModal extends BaseModal<Schema> implements OnInit {

    public readonly SchemaPropertiesMode = SchemaPropertiesMode;

    @Input()
    public mode: SchemaPropertiesMode;

    @Input()
    public schema: Schema;

    public schemaNames: string[];
    public microschemaNames: string[];
    public fieldNames: string[];

    public fgProperties: FormControl<EditableSchemaProperties>;
    public fgElasticsearch: FormControl<ElasticSearchSettings>;
    public fgFields: FormControl<SchemaField[]>;

    public namesLoaded = false;

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected mesh: MeshRestClientService,
        protected handler: SchemaHandlerService,
        protected microHandler: MicroschemaHandlerService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.loadNames();
        this.fieldNames = (this.schema?.fields || []).map(field => field.name);
        this.fgProperties = new FormControl(this.schema || {} as any);
        this.fgElasticsearch = new FormControl(this.schema?.elasticsearch, createJsonValidator());
        this.fgFields = new FormControl(this.schema?.fields || []);
    }

    updateFieldNames(names: string[]): void {
        this.fieldNames = names;
    }

    async loadNames(): Promise<void> {
        const project = (await this.mesh.projects.list({ perPage: 1 }))?.data?.[0]?.name;
        const [schemas, microschemas] = await Promise.all([
            this.handler.getAllNames(project, false),
            this.microHandler.getAllNames(project, false),
        ]);

        this.schemaNames = schemas.map(s => s.name).filter(s => s != null && s !== this.schema?.name);
        this.microschemaNames = microschemas.map(s => s.name);
        this.namesLoaded = true;
        this.changeDetector.markForCheck();
    }

    buttonCreateEntityClicked(): void {
        const val: EditableSchemaProperties = {
            ...this.fgProperties.value,
            elasticsearch: this.fgElasticsearch.value,
            fields: this.fgFields.value,
        };

        this.fgProperties.disable();
        this.fgElasticsearch.disable();
        this.fgFields.disable();
        this.changeDetector.markForCheck();

        const op = this.mode === SchemaPropertiesMode.CREATE
            ? this.handler.create(val)
            : this.handler.update(this.schema.uuid, val);

        op.then(res => {
            this.closeFn(res);
        }).catch(() => {
            this.fgProperties.enable();
            this.fgElasticsearch.enable();
            this.fgFields.enable();
            this.changeDetector.markForCheck();
        });
    }
}

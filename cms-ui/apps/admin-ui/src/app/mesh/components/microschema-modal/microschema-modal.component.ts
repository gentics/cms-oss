import { MicroschemaHandlerService } from '@admin-ui/mesh/providers';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { Microschema, Schema } from '@gentics/mesh-models';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';
import { BaseModal } from '@gentics/ui-core';
import { MicroschemaPropertiesMode } from '../microschema-properties/microschema-properties.component';

@Component({
    selector: 'gtx-mesh-microschema-modal',
    templateUrl: './microschema-modal.component.html',
    styleUrls: ['./microschema-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MicroschemaModal extends BaseModal<Schema> implements OnInit {

    public readonly MicroschemaPropertiesMode = MicroschemaPropertiesMode;

    @Input()
    public mode: MicroschemaPropertiesMode;

    @Input()
    public microschema: Microschema;

    public schemaNames: string[];
    public microschemaNames: string[];

    public form: FormControl;

    public namesLoaded = false;

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected handler: MicroschemaHandlerService,
        protected mesh: MeshRestClientService,
        protected schemaHandler: MicroschemaHandlerService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.loadNames();
        this.form = new FormControl(this.microschema || {});
    }

    async loadNames(): Promise<void> {
        const project = (await this.mesh.projects.list({ perPage: 1 }))?.data?.[0]?.name;
        const [schemas, microschemas] = await Promise.all([
            this.handler.getAllNames(project, false),
            this.schemaHandler.getAllNames(project, false),
        ]);

        this.schemaNames = schemas.map(s => s.name);
        this.microschemaNames = microschemas.map(s => s.name).filter(s => s != null && s !== this.microschema?.name);
        this.namesLoaded = true;
        this.changeDetector.markForCheck();
    }

    buttonCreateEntityClicked(): void {
        const val = this.form.value;
        this.form.disable();
        this.changeDetector.markForCheck();

        const op = this.mode === MicroschemaPropertiesMode.CREATE
            ? this.handler.create(val)
            : this.handler.update(this.microschema.uuid, val);

        op.then(res => {
            this.closeFn(res);
        }).catch(() => {
            this.form.enable();
            this.changeDetector.markForCheck();
        });
    }
}

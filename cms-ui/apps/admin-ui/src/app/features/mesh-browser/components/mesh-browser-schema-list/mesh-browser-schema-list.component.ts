import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { MeshBrowserLoaderService, Schema } from '../../providers';



@Component({
    selector: 'gtx-mesh-browser-schema-list',
    templateUrl: './mesh-browser-schema-list.component.html',
    styleUrls: ['./mesh-browser-schema-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeshBrowserSchemaListComponent implements OnInit {

    @Input()
    public project: string;

    public schemas: Array<Schema> = [];

    public rootNodeUuid: string;

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected loader: MeshBrowserLoaderService,
    ) {

    }

    async ngOnInit(): Promise<void> {
        const response = await this.loader.listSchemasWithRootNode(this.project)
        this.schemas = response.schemas
        this.schemas.map(schema => schema.name.toUpperCase())
        this.rootNodeUuid = response.rootNodeUuid;

        this.loadNodesChildrenForSchemas();

    }


    protected async loadNodesChildrenForSchemas(): Promise<void> {
        for (const schema of this.schemas) {
            const schemaElements = await this.loader.listNodeChildrenForSchema(this.project, {
                schemaName: schema.name,
                nodeUuid: this.rootNodeUuid,
            })

            schema.elements = schemaElements;
        }

        this.changeDetector.markForCheck();
    }


}


import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { PaginationService } from 'ngx-pagination';
import { MeshBrowserLoaderService, Schema, SchemaElement } from '../../providers';


let uniqueComponentId = 0;

@Component({
    selector: 'gtx-mesh-browser-schema-list',
    templateUrl: './mesh-browser-schema-list.component.html',
    styleUrls: ['./mesh-browser-schema-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [PaginationService],
})
export class MeshBrowserSchemaListComponent implements OnInit {

    public readonly UNIQUE_ID = `gtx-paging-${uniqueComponentId++}`;

    @Input()
    public project: string;

    public schemas: Array<Schema> = [];

    public page = 1;

    public perPage = 10;

    private currentNodeUuid: string;


    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected loader: MeshBrowserLoaderService,
    ) {

    }

    async ngOnInit(): Promise<void> {
        const response = await this.loader.listSchemasWithRootNode(this.project)
        this.schemas = response.schemas
        this.schemas.map(schema => schema.name.toUpperCase())

        await this.loadNodesChildrenForSchemas(response.rootNodeUuid);
        this.changeDetector.markForCheck();
    }


    protected async loadNodesChildrenForSchemas(nodeUuid: string): Promise<void> {
        for (const schema of this.schemas) {
            const schemaElements = await this.loader.listNodeChildrenForSchema(this.project, {
                schemaName: schema.name,
                nodeUuid: nodeUuid,
            });

            schema.elements = schemaElements;
        }
    }


    public async loadNodeContent(element: SchemaElement): Promise<void> {
        await this.loadNodesChildrenForSchemas(element.uuid)
        this.currentNodeUuid = element.uuid;

        this.changeDetector.markForCheck();
    }


    public changePage($event: Event): void {
        this.page = parseInt($event as unknown as string, 10);

        this.loader.listNodeChildrenForSchema(this.project, {
            nodeUuid: this.currentNodeUuid,
            page: this.page,
        })

    }

}


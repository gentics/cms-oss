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

    @Input()
    public currentBranch: string;

    public currentNodeUuid: string;

    public languages: Array<string> = ['de', 'en']; // todo: fetch languages

    public schemas: Array<Schema> = [];


    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected loader: MeshBrowserLoaderService,
    ) {

    }

    async ngOnInit(): Promise<void> {
        const response = await this.loader.listSchemasWithRootNode(this.project)
        this.schemas = response.schemas
        this.currentNodeUuid= response.rootNodeUuid;

        await this.loadNodesChildrenForSchemas(response.rootNodeUuid);
        this.changeDetector.markForCheck();
    }


    protected async loadNodesChildrenForSchemas(nodeUuid: string): Promise<void> {
        for (const schema of this.schemas) {
            const schemaElements = await this.loader.listNodeChildrenForSchema(this.project, {
                schemaName: schema.name,
                nodeUuid: nodeUuid,
                lang: this.languages,
            }, this.currentBranch);

            schema.elements = schemaElements;
        }
    }

    public nodeChanged(nodeUuid: string): void {
        this.currentNodeUuid = nodeUuid;
    }

}


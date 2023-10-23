import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { MeshBrowserLoaderService } from '../../providers';
import { SchemaContainer } from '../../models/mesh-browser-models';


@Component({
    selector: 'gtx-mesh-browser-schema-list',
    templateUrl: './mesh-browser-schema-list.component.html',
    styleUrls: ['./mesh-browser-schema-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeshBrowserSchemaListComponent implements OnInit {

    @Input()
    public currentProject: string;

    @Input()
    public languages: Array<string> = [];

    @Input()
    public currentLanguage: string;

    @Input()
    public currentBranch: string;

    public currentNodeUuid: string;

    public schemas: Array<SchemaContainer> = [];


    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected loader: MeshBrowserLoaderService,
    ) {

    }

    async ngOnInit(): Promise<void> {
        const response = await this.loader.listSchemasWithRootNode(this.currentProject)
        this.schemas = response.schemas
        this.currentNodeUuid= response.rootNodeUuid;

        await this.loadNodesChildrenForSchemas(response.rootNodeUuid);
        this.changeDetector.markForCheck();
    }


    protected async loadNodesChildrenForSchemas(nodeUuid: string): Promise<void> {
        for (const schema of this.schemas) {
            const schemaElements = await this.loader.listNodeChildrenForSchema(this.currentProject, {
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


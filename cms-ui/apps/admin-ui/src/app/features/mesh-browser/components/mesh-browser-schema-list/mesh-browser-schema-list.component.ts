import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { SchemaContainer } from '../../models/mesh-browser-models';
import { MeshBrowserLoaderService } from '../../providers';


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

    @Output()
    public nodeChange = new EventEmitter<string>();

    @Input()
    public currentNodeUuid: string;

    public schemas: Array<SchemaContainer> = [];


    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected loader: MeshBrowserLoaderService,
        protected route: ActivatedRoute,
    ) {

    }

    ngOnInit(): void{
        this.route.params.subscribe((params) => {
            if (params.parent) {
                this.currentNodeUuid = params.parent
            }
        })
        this.loadSchemasAndElements()
    }


    protected async loadSchemasAndElements(): Promise<void> {
        this.schemas = await this.loader.listProjectSchemas(this.currentProject)

        if (this.currentNodeUuid === 'undefined' || !this.currentNodeUuid) {
            this.currentNodeUuid = await this.loader.getRootNodeUuid(this.currentProject);
        }

        await this.loadNodesChildrenForSchemas(this.currentNodeUuid);
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
        if (nodeUuid === 'undefined' || !nodeUuid) {
            this.loadSchemasAndElements();
        }
        if (nodeUuid === this.currentNodeUuid) {
            return;
        }
        this.currentNodeUuid = nodeUuid;
        this.nodeChange.emit(nodeUuid);
    }

}


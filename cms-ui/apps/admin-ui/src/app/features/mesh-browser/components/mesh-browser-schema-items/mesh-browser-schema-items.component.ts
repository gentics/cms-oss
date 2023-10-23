import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { BranchReference } from '@gentics/mesh-models';
import { SchemaContainer } from '../../models/mesh-browser-models';
import { MeshBrowserLoaderService } from '../../providers';


let uniqueComponentId = 0;


@Component({
    selector: 'gtx-mesh-browser-schema-items',
    templateUrl: './mesh-browser-schema-items.component.html',
    styleUrls: ['./mesh-browser-schema-items.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeshBrowserSchemaItemsComponent implements OnChanges {

    public readonly UNIQUE_ID = `gtx-mesh-browser-schema-items-${uniqueComponentId++}`;

    @Input()
    public currentProject: string;

    @Input()
    public schema: SchemaContainer;

    @Input()
    public currentNodeUuid: string;

    @Input()
    public currentBranch: BranchReference;

    @Input()
    public languages: Array<string>;

    @Input()
    public currentLanguage: string;

    @Output()
    public nodeChanged = new EventEmitter<string>();

    public page = 1;

    public perPage = 10;

    public collapsed = false;


    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected loader: MeshBrowserLoaderService,
    ) { }


    ngOnChanges(changes: SimpleChanges): void {
        if (changes?.currentNodeUuid || changes?.project  || changes?.currentBranch || changes?.currentLanguage) {
            // make sure current language is the first element
            this.languages = this.languages.sort((a,_b) => a === this.currentLanguage ? -1 : 1)
            this.loadNodeContent(this.currentNodeUuid)
            this.page = 1;
        }
    }


    public async loadNodeContent(nodeUuid: string): Promise<void> {
        const schemaElements = await this.loader.listNodeChildrenForSchema(this.currentProject, {
            schemaName: this.schema.name,
            nodeUuid: nodeUuid,
            lang: this.languages,
        },this.currentBranch.uuid);
        schemaElements?.forEach((schemaElement) =>
            schemaElement.languages = schemaElement?.languages.sort( (a,_b) => a.language === this.currentLanguage ? -1 :1),
        );
        this.schema.elements = schemaElements?.sort((a,b) => a.displayName.localeCompare(b.displayName));
        this.changeDetector.markForCheck();
        this.nodeChanged.emit(nodeUuid)
    }


    public changePage(page: number): void {
        this.page = page;

        this.loader.listNodeChildrenForSchema(this.currentProject, {
            schemaName: this.schema.name,
            nodeUuid: this.currentNodeUuid,
            page: this.page,
            lang: this.languages,
        }, this.currentBranch.uuid)
    }

    public toggleSection(): void {
        this.collapsed = !this.collapsed;
    }

}


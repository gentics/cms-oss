import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { PaginationService } from 'ngx-pagination';
import { MeshBrowserLoaderService, Schema } from '../../providers';


let uniqueComponentId = 0;


@Component({
    selector: 'gtx-mesh-browser-schema-items',
    templateUrl: './mesh-browser-schema-items.component.html',
    styleUrls: ['./mesh-browser-schema-items.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [PaginationService],
})
export class MeshBrowserSchemaItemsComponent implements OnChanges {

    public readonly UNIQUE_ID = `gtx-mesh-browser-schema-items-${uniqueComponentId++}`;

    @Input()
    public project: string;

    @Input()
    public schema: Schema;

    @Input()
    public currentNodeUuid: string;

    @Input()
    public languages: Array<string>;

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
        console.log('changes', changes);

        if (changes?.currentNodeUuid || changes?.project) {
            this.loadNodeContent(this.currentNodeUuid)
            this.page = 1;
        }
    }


    public async loadNodeContent(nodeUuid: string): Promise<void> {
        const schemaElements = await this.loader.listNodeChildrenForSchema(this.project, {
            schemaName: this.schema.name,
            nodeUuid: nodeUuid,
            lang: this.languages,
        });

        this.schema.elements = schemaElements?.sort((a,b) => a.displayName.localeCompare(b.displayName));
        this.changeDetector.markForCheck();
        this.nodeChanged.emit(nodeUuid)
    }


    public changePage(page: number): void {
        this.page = page;

        this.loader.listNodeChildrenForSchema(this.project, {
            schemaName: this.schema.name,
            nodeUuid: this.currentNodeUuid,
            page: this.page,
        })
    }

    public toggleSection(): void {
        this.collapsed = !this.collapsed;
    }

}


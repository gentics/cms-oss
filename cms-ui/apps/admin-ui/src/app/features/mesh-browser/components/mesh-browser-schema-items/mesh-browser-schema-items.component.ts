import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input } from '@angular/core';
import { PaginationService } from 'ngx-pagination';
import { MeshBrowserLoaderService, Schema, SchemaElement } from '../../providers';


let uniqueComponentId = 0;


@Component({
    selector: 'gtx-mesh-browser-schema-items',
    templateUrl: './mesh-browser-schema-items.component.html',
    styleUrls: ['./mesh-browser-schema-items.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [PaginationService],
})
export class MeshBrowserSchemaItemsComponent  {

    public readonly UNIQUE_ID = `gtx-item-paging-${uniqueComponentId++}`;

    @Input()
    public project: string;

    @Input()
    public schema: Schema;

    public page = 1;

    public perPage = 10;

    private currentNodeUuid: string;

    public collapsed = false;


    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected loader: MeshBrowserLoaderService,
    ) { }


    public async loadNodeContent(element: SchemaElement): Promise<void> {
        const schemaElements = await this.loader.listNodeChildrenForSchema(this.project, {
            nodeUuid: element.uuid,
        });

        this.schema.elements = schemaElements;
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

    public toggleSection(): void {
        this.collapsed = !this.collapsed;
    }

}


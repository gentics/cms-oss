import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { BranchReference } from '@gentics/mesh-models';
import { SchemaElement } from '../../models/mesh-browser-models';
import { MeshBrowserLoaderService, MeshBrowserNavigatorService } from '../../providers';


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
    public schemaName: string;

    @Input()
    public schemaElements: Array<SchemaElement>;

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

    @Output()
    public elementsLoaded = new EventEmitter<number>();

    public page = 1;

    public perPage = 10;

    public collapsed = false;


    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected loader: MeshBrowserLoaderService,
        protected router: Router,
        protected route: ActivatedRoute,
        protected navigator: MeshBrowserNavigatorService,
    ) { }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes?.currentNodeUuid || changes?.currentProject || changes?.currentBranch || changes?.currentLanguage) {
            this.page = 1;
            // make sure current language is the first element
            this.languages = this.languages.sort((a,_b) => a === this.currentLanguage ? -1 : 1);
            this.loadNodeChildren(this.currentNodeUuid);
        }
    }

    public loadContent(element: SchemaElement): void {
        if (element.isContainer) {
            this.loadNodeChildren(element.uuid)
            return;
        }

        this.showDetails(element);
    }

    public showDetails(element: SchemaElement): void {
        this.navigator.navigateToDetails(
            this.route, element.uuid,
            this.currentProject,
            this.currentBranch.uuid,
            element.language,
        )
    }

    private async loadNodeChildren(nodeUuid: string): Promise<void> {
        const schemaElements = await this.loader.listNodeChildrenForSchema(this.currentProject, {
            schemaName: this.schemaName,
            nodeUuid: nodeUuid,
            lang: this.languages,
            page: this.page,
            perPage: this.perPage,
        }, this.currentBranch?.uuid);

        schemaElements?.forEach((schemaElement) =>
            schemaElement.languages = schemaElement?.languages.sort((a,_b) => a.language === this.currentLanguage ? -1 :1),
        );

        this.schemaElements = schemaElements?.sort((a,b) => a.displayName?.localeCompare(b.displayName));
        this.changeDetector.markForCheck();
        this.nodeChanged.emit(nodeUuid)
        this.elementsLoaded.emit(this.schemaElements?.length ?? 0);
    }

    public changePage(page: number): void {
        this.page = page;

        this.loader.listNodeChildrenForSchema(this.currentProject, {
            schemaName: this.schemaName,
            nodeUuid: this.currentNodeUuid,
            page: this.page,
            lang: this.languages,
            perPage: this.perPage,
        }, this.currentBranch.uuid)
    }

    public toggleSection(): void {
        this.collapsed = !this.collapsed;
        this.changeDetector.markForCheck();
    }

}


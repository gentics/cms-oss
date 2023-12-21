import { AdminUIEntityDetailRoutes, ROUTE_DETAIL_OUTLET } from '@admin-ui/common';
import { AppStateService, FocusEditor } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { ActivatedRoute, NavigationExtras, Router } from '@angular/router';
import { BranchReference } from '@gentics/mesh-models';
import { getFullPrimaryPath } from '@gentics/ui-core';
import { SchemaElement } from '../../models/mesh-browser-models';
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
        protected appState: AppStateService,
    ) { }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes?.currentNodeUuid || changes?.currentProject || changes?.currentBranch || changes?.currentLanguage) {
            this.page = 1;
            // make sure current language is the first element
            this.languages = this.languages.sort((a,_b) => a === this.currentLanguage ? -1 : 1);
            this.loadNodeContent(this.currentNodeUuid);
        }
    }

    public loadContent(element: SchemaElement): void {
        if (element.isContainer) {
            this.loadNodeContent(element.uuid)
            return;
        }

        this.showDetails(element);
    }

    public showDetails(element: SchemaElement): void {
        this.navigateToDetails(element.uuid, element.language);
    }

    public async navigateToDetails(nodeUuid: string, elementLanguage: string): Promise<void> {
        const fullUrl = getFullPrimaryPath(this.route);

        const commands: any[] = [
            fullUrl,
            {
                outlets: {
                    [ROUTE_DETAIL_OUTLET]:  [
                        AdminUIEntityDetailRoutes.MESH_BROWSER,
                        this.currentProject,
                        this.currentBranch.uuid,
                        nodeUuid,
                        elementLanguage,
                    ],
                },
            },
        ] ;
        const extras: NavigationExtras = { relativeTo: this.route };

        await this.router.navigate(commands, extras);
        this.appState.dispatch(new FocusEditor());
    }

    public async loadNodeContent(nodeUuid: string): Promise<void> {
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


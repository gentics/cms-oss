import { ContentRepositoryBO } from '@admin-ui/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ChangesOf } from '@gentics/ui-core';
import { SchemaElement } from '../../models/mesh-browser-models';
import { MeshBrowserLoaderService, MeshBrowserNavigatorService } from '../../providers';

let uniqueComponentId = 0;

export interface LoadingState {
    loading: boolean;
    hasElements?: boolean;
}

@Component({
    selector: 'gtx-mesh-browser-schema-items',
    templateUrl: './mesh-browser-schema-items.component.html',
    styleUrls: ['./mesh-browser-schema-items.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class MeshBrowserSchemaItemsComponent implements OnChanges {

    public readonly UNIQUE_ID = `gtx-mesh-browser-schema-items-${uniqueComponentId++}`;

    @Input()
    public contentRepository: ContentRepositoryBO;

    @Input()
    public project: string;

    @Input()
    public schemaName: string;

    @Input()
    public node: string;

    @Input()
    public branch: string;

    @Input()
    public availableLanguages: Array<string>;

    @Input()
    public language: string;

    @Input()
    public loading = false;

    @Output()
    public nodeChange = new EventEmitter<string>();

    @Output()
    public loadingStateChange = new EventEmitter<LoadingState>();

    public page = 1;
    public perPage = 10;
    public totalCount = 0;
    public schemaElements: Array<SchemaElement> = [];

    public collapsed = false;
    public elementsLoading = false;

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected loader: MeshBrowserLoaderService,
        protected router: Router,
        protected route: ActivatedRoute,
        protected navigator: MeshBrowserNavigatorService,
    ) { }

    ngOnChanges(changes: ChangesOf<this>): void {
        if (changes?.node || changes?.project || changes?.branch || changes?.language) {
            // Reset the page, as we're in a new context
            this.page = 1;
            this.loadNodeChildren(this.node);
        }
    }

    public handleElementClick(element: SchemaElement): void {
        if (element.isContainer) {
            this.nodeChange.emit(element.uuid);
            return;
        }

        this.showElementDetails(element);
    }

    public showElementDetails(element: SchemaElement): void {
        this.navigator.navigateToDetails(
            this.route,
            {
                project: this.project,
                branch: this.branch,
                node: element.uuid,
                language: element.language,
            },
            this.contentRepository,
        );
    }

    private async loadNodeChildren(nodeUuid: string): Promise<void> {
        this.elementsLoading = true;

        this.loadingStateChange.emit({ loading: true });
        this.changeDetector.markForCheck();

        const schemaPage = await this.loader.listNodeChildrenForSchema(this.project, {
            schemaName: this.schemaName,
            nodeUuid: nodeUuid,
            lang: this.availableLanguages,
            page: this.page,
            perPage: this.perPage,
        }, this.branch);

        this.totalCount = schemaPage.totalCount;
        const schemaElements: SchemaElement[] = schemaPage.elements;

        // Make sure the languages are properly sorted
        schemaElements?.forEach((schemaElement) => {
            schemaElement.availableLanguages.sort(lang => lang === this.language ? -1 : 1);
        });

        this.schemaElements = schemaElements;
        this.elementsLoading = false;
        this.loadingStateChange.emit({ loading: false, hasElements: this.schemaElements?.length > 0 });

        this.changeDetector.markForCheck();
    }

    public changePage(page: number): void  {
        this.page = page;
        this.loadNodeChildren(this.node);
    }

    public toggleSection(): void {
        this.collapsed = !this.collapsed;
        this.changeDetector.markForCheck();
    }
}

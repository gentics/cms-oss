import { RouteEntityResolverService } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MeshBrowserNavigatorService } from '../../providers';
import { BreadcrumbNode } from '../../models/mesh-browser-models';


@Component({
    selector: 'gtx-mesh-browser-breadcrumbs',
    templateUrl: './mesh-browser-breadcrumbs.component.html',
    styleUrls: ['./mesh-browser-breadcrumbs.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeshBrowserBreadcrumbComponent implements OnChanges {

    @Input()
    public currentProject: string;

    @Input()
    public currentBranchUuid: string;

    @Input()
    public currentNodeUuid: string;

    @Input()
    public currentLanguage: string;


    public breadcrumbEntries: BreadcrumbNode[] = [];


    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected navigationService: MeshBrowserNavigatorService,
        protected route: ActivatedRoute,
        protected router: Router,
        protected resolver: RouteEntityResolverService,
    ) { }


    ngOnChanges(): void {
        this.updateComponent();
    }

    async updateComponent(): Promise<void> {
        const breadcrumbs = await this.navigationService.getBreadcrumbNavigationEntries(
            this.currentProject,
            { nodeUuid: this.currentNodeUuid },
            this.currentBranchUuid,
        );

        this.breadcrumbEntries = [];

        for(const entry of breadcrumbs) {
            this.breadcrumbEntries.push({
                uuid: entry.node.uuid,
                displayName: entry.node.displayName,
            })
        }

        this.changeDetector.markForCheck();
    }

}

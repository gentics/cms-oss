import { AdminUIModuleRoutes, ROUTE_MESH_BROWSER_OUTLET } from '@admin-ui/common';
import { RouteEntityResolverService } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { IBreadcrumbRouterLink } from '@gentics/ui-core';
import { MeshBrowserNavigatorService } from '../../providers';


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


    public breadcrumbEntries: IBreadcrumbRouterLink[] = [];


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
        breadcrumbs.splice(0,1);

        const selectedRepositoryId = this.resolver.extractRepositoryId(this.route.snapshot)
        this.breadcrumbEntries = [];

        for(const entry of breadcrumbs) {
            this.breadcrumbEntries.push({
                route: [
                    '/' + AdminUIModuleRoutes.MESH_BROWSER,
                    selectedRepositoryId,
                    {
                        outlets: {
                            [ROUTE_MESH_BROWSER_OUTLET]: [
                                'list',
                                this.currentProject,
                                this.currentBranchUuid,
                                entry.node.uuid,
                                this.currentLanguage,
                            ],
                        },
                    },
                ],
                text: entry.node?.displayName,
            })
        }

        this.changeDetector.markForCheck();
    }

}

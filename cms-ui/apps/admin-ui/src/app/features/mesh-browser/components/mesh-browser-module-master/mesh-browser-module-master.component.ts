import {
    AdminUIModuleRoutes,
    ContentRepositoryBO,
    ROUTE_DATA_MESH_REPO_ID,
    ROUTE_DATA_MESH_REPO_ITEM,
    ROUTE_MESH_BROWSER_OUTLET,
    ROUTE_MESH_REPOSITORY_ID,
} from '@admin-ui/common';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TableRow, toValidNumber } from '@gentics/ui-core';

@Component({
    selector: 'gtx-mesh-browser-module-master',
    templateUrl: './mesh-browser-module-master.component.html',
    styleUrls: ['./mesh-browser-module-master.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class MeshBrowserModuleMasterComponent {

    public readonly ROUTE_MESH_BROWSER_OUTLET = ROUTE_MESH_BROWSER_OUTLET;

    @Input({ alias: ROUTE_MESH_REPOSITORY_ID, transform: toValidNumber })
    public currentRepositoryId: number;

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected router: Router,
        protected route: ActivatedRoute,
    ) {}

    public rowClickHandler(row: TableRow<ContentRepositoryBO>): void {
        this.currentRepositoryId = row.item.id;

        this.router.navigate(
            [
                '/' + AdminUIModuleRoutes.MESH_BROWSER,
                this.currentRepositoryId,
                {
                    outlets: {
                        [ROUTE_MESH_BROWSER_OUTLET]: [
                            'list',
                        ],
                    },
                },
            ],
            {
                relativeTo: this.route, state: {
                    [ROUTE_DATA_MESH_REPO_ID]: this.currentRepositoryId,
                    [ROUTE_DATA_MESH_REPO_ITEM]: row.item,
                },
            },
        );
    }
}

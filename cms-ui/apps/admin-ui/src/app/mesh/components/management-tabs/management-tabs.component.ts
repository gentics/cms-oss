import { MeshMangementTabs, ROUTE_MANAGEMENT_OUTLET, ROUTE_PARAM_MESH_TAB, ROUTE_PATH_MESH } from '@admin-ui/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
    selector: 'gtx-mesh-management-tabs',
    templateUrl: './management-tabs.component.html',
    styleUrls: ['./management-tabs.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ManagementTabsComponent {

    public readonly MeshMangementTabs = MeshMangementTabs;

    @Input({ alias: ROUTE_PARAM_MESH_TAB })
    public activeTab: MeshMangementTabs;

    constructor(
        protected router: Router,
        protected route: ActivatedRoute,
    ) {}

    changeTab(tab: MeshMangementTabs): void {
        this.router.navigate([{ outlets: { [ROUTE_MANAGEMENT_OUTLET]: [ROUTE_PATH_MESH, tab] } }], { relativeTo: this.route.parent.parent });
    }
}

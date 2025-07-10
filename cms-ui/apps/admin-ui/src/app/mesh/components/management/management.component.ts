import { ROUTE_DETAIL_OUTLET, ROUTE_MANAGEMENT_OUTLET, ROUTE_PATH_MESH } from '@admin-ui/common';
import { getUserDisplayName } from '@admin-ui/mesh/utils';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input } from '@angular/core';
import { ActivatedRoute, PRIMARY_OUTLET, Router, UrlTree } from '@angular/router';
import { ContentRepository } from '@gentics/cms-models';
import { User } from '@gentics/mesh-models';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';

@Component({
    selector: 'gtx-mesh-management',
    templateUrl: './management.component.html',
    styleUrls: ['./management.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ManagementComponent {

    public readonly ROUTE_MANAGEMENT_OUTLET = ROUTE_MANAGEMENT_OUTLET;

    @Input()
    public repository: ContentRepository;

    public me: User;
    public meName: string;
    public loggedIn = false;

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected router: Router,
        protected route: ActivatedRoute,
        protected mesh: MeshRestClientService,
    ) {}

    public handleMeshLogin(event: { loggedIn: boolean, user?: User }): void {
        this.loggedIn = event.loggedIn;

        if (event.loggedIn) {
            if (event.user) {
                this.me = event.user;
                this.meName = getUserDisplayName(this.me);
            } else {
                // eslint-disable-next-line @typescript-eslint/no-unsafe-call
                this.mesh.auth.me().send().then(res => {
                    this.me = res;
                    this.meName = getUserDisplayName(this.me);
                    this.changeDetector.markForCheck();
                });
            }
        }

        const tree: UrlTree = (this.router as any).rawUrlTree;
        let isManagement = false;
        let group = tree.root;

        while (Object.keys(group.children || {}).length > 0) {
            if (group.children[ROUTE_DETAIL_OUTLET]) {
                group = group.children[ROUTE_DETAIL_OUTLET];
                continue;
            }
            if (group.children[PRIMARY_OUTLET]) {
                group = group.children[PRIMARY_OUTLET];
                continue;
            }
            if (group.children[ROUTE_MANAGEMENT_OUTLET]) {
                group = group.children[ROUTE_MANAGEMENT_OUTLET];
                isManagement = true;
                break;
            }
        }

        const parts = [
            ROUTE_PATH_MESH,
        ];
        if (isManagement && group.segments.length >= 2) {
            parts.push(group.segments[1].path);
        }
        this.router.navigate([{ outlets: { [ROUTE_MANAGEMENT_OUTLET]: parts } }], { relativeTo: this.route });
    }

    public logout(): void {
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.mesh.auth.logout().send().then(() => {
            this.loggedIn = false;
            this.changeDetector.markForCheck();
        });
    }
}

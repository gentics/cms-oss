import { ROUTE_MANAGEMENT_OUTLET, ROUTE_PATH_MESH } from '@admin-ui/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ContentRepository } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';

@Component({
    selector: 'gtx-mesh-management',
    templateUrl: './management.component.html',
    styleUrls: ['./management.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ManagementComponent {

    public readonly ROUTE_MANAGEMENT_OUTLET = ROUTE_MANAGEMENT_OUTLET;

    @Input()
    public repository: ContentRepository;

    public loggedIn = false;

    constructor(
        protected router: Router,
        protected route: ActivatedRoute,
        protected cmsClient: GcmsApi,
    ) {}

    public handleMeshLogin(isLoggedIn: boolean): void {
        this.loggedIn = isLoggedIn;
        this.router.navigate([{ outlets: { [ROUTE_MANAGEMENT_OUTLET]: [ROUTE_PATH_MESH] } }], { relativeTo: this.route });
    }

    public logout(): void {
        // this.cmsClient.contentrepositories.logoutFromMeshInstance(this.repository.id).subscribe(() => {
            this.loggedIn = false;
        // });
    }
}

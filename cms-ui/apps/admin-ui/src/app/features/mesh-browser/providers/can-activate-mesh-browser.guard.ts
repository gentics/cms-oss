import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot } from '@angular/router';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';

@Injectable()
export class MeshBrowserCanActivateGuard {

    constructor(
        protected client: MeshRestClientService,
    ) { }

    async canActivate(route: ActivatedRouteSnapshot): Promise<boolean> {
        const projectName = route.params.project;

        if (!projectName) {
            return false;
        }

        try {
            // route can be activated if user has access to the project
            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
            const projects = (await this.client.projects.list().send()).data;
            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
            const foundProject = projects.find(projectName => projectName);
            if (foundProject) {
                return true;
            }

            return false;
        }
        catch (err) {
            return false;
        }
    }


}

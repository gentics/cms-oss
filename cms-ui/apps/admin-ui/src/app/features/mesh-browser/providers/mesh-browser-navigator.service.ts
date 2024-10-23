import {
    AdminUIEntityDetailRoutes,
    AdminUIModuleRoutes,
    ContentRepositoryBO,
    ROUTE_DATA_MESH_REPO_ITEM,
    ROUTE_DETAIL_OUTLET,
    ROUTE_MESH_BROWSER_OUTLET,
} from '@admin-ui/common';
import { AppStateService, FocusEditor } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ActivatedRoute, NavigationExtras, Router } from '@angular/router';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';
import { getFullPrimaryPath } from '@gentics/ui-core';
import {
    MeshSchemaListParams,
    NavigationEntry,
} from '../models/mesh-browser-models';

export interface ListRouteParameters {
    crId: number;
    project: string;
    branch: string;
    node: string;
    language: string;
}

export interface DetailRouteParamers {
    project: string;
    branch: string;
    node: string;
    language: string;
}

@Injectable()
export class MeshBrowserNavigatorService {

    constructor(
        protected meshClient: MeshRestClientService,
        protected router: Router,
        protected appState: AppStateService,
    ) {}

    public handleNavigation(
        route: ActivatedRoute,
        params: ListRouteParameters,
        cr?: ContentRepositoryBO,
    ): void {
        const commands = this.getRouteCommand(params);

        const extras: NavigationExtras = { relativeTo: route };
        const snapshot = route.snapshot;

        if (cr) {
            extras.state = {
                [ROUTE_DATA_MESH_REPO_ITEM]: cr,
            };
        } else if (snapshot[ROUTE_DATA_MESH_REPO_ITEM]) {
            extras.state = {
                [ROUTE_DATA_MESH_REPO_ITEM]: snapshot[ROUTE_DATA_MESH_REPO_ITEM],
            };
        }

        this.router.navigate(commands, extras);
    }

    private getRouteCommand(params: ListRouteParameters): any[] {
        const command = [
            '/' + AdminUIModuleRoutes.MESH_BROWSER,
            params.crId,
            {
                outlets: {
                    [ROUTE_MESH_BROWSER_OUTLET]: [
                        AdminUIEntityDetailRoutes.MESH_BROWSER_LIST,
                        params.project,
                        params.branch,
                        params.node,
                        params.language,
                    ],
                },
            },
        ];

        return command;
    }

    public navigateToModuleRoot(route: ActivatedRoute): void {
        this.router.navigate([`/${AdminUIModuleRoutes.MESH_BROWSER}`], {
            relativeTo: route,
        });
    }

    public async navigateToDetails(
        route: ActivatedRoute,
        params: DetailRouteParamers,
        cr?: ContentRepositoryBO,
    ): Promise<void> {
        const fullUrl = getFullPrimaryPath(route);

        const commands: any[] = [
            fullUrl,
            {
                outlets: {
                    [ROUTE_DETAIL_OUTLET]:  [
                        AdminUIEntityDetailRoutes.MESH_BROWSER_NODE,
                        params.project,
                        params.branch,
                        params.node,
                        params.language,
                    ],
                },
            },
        ] ;
        const extras: NavigationExtras = { relativeTo: route };
        const snapshot = route.snapshot;

        if (cr) {
            extras.state = {
                [ROUTE_DATA_MESH_REPO_ITEM]: cr,
            };
        } else if (snapshot[ROUTE_DATA_MESH_REPO_ITEM]) {
            extras.state = {
                [ROUTE_DATA_MESH_REPO_ITEM]: snapshot[ROUTE_DATA_MESH_REPO_ITEM],
            };
        }

        await this.router.navigate(commands, extras);
        this.appState.dispatch(new FocusEditor());
    }
}

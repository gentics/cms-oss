import {
    AdminUIEntityDetailRoutes,
    AdminUIModuleRoutes,
    ROUTE_DETAIL_OUTLET,
    ROUTE_MESH_BROWSER_OUTLET,
} from '@admin-ui/common';
import { BreadcrumbsService } from '@admin-ui/core';
import { AppStateService, FocusEditor } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ActivatedRoute, NavigationExtras, Router } from '@angular/router';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';
import { IBreadcrumbRouterLink, getFullPrimaryPath } from '@gentics/ui-core';
import {
    MeshSchemaListParams,
    NavigationEntry,
} from '../models/mesh-browser-models';


@Injectable()
export class MeshBrowserNavigatorService {
    constructor(
        protected meshClient: MeshRestClientService,
        protected breadcrumbService: BreadcrumbsService,
        protected router: Router,
        protected appState: AppStateService,
    ) {}

    public handleNavigation(
        route: ActivatedRoute,
        selectedRepositoryId: number,
        currentProject: string,
        currentBranchUuid: string,
        parentNodeUuid: string,
        currentLanguage: string,
    ): void {
        this.router.navigate(
            this.getRouteCommand(
                selectedRepositoryId,
                currentProject,
                currentBranchUuid,
                parentNodeUuid,
                currentLanguage,
            ),
            { relativeTo: route },
        );
    }

    private getRouteCommand(
        selectedRepositoryId: number,
        currentProject: string,
        currentBranchUuid: string,
        parentNodeUuid: string,
        currentLanguage: string,
    ): any[] {
        const command = [
            '/' + AdminUIModuleRoutes.MESH_BROWSER,
            selectedRepositoryId,
            {
                outlets: {
                    [ROUTE_MESH_BROWSER_OUTLET]: [
                        AdminUIEntityDetailRoutes.MESH_BROWSER_LIST,
                        currentProject,
                        currentBranchUuid,
                        parentNodeUuid,
                        currentLanguage,
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
        currentNodeUuid: string,
        currentProject: string,
        currentBranchUuid: string,
        currentLanguage: string,
    ): Promise<void> {
        const fullUrl = getFullPrimaryPath(route);

        const commands: any[] = [
            fullUrl,
            {
                outlets: {
                    [ROUTE_DETAIL_OUTLET]:  [
                        AdminUIEntityDetailRoutes.MESH_BROWSER_NODE,
                        currentProject,
                        currentBranchUuid,
                        currentNodeUuid,
                        currentLanguage,
                    ],
                },
            },
        ] ;
        const extras: NavigationExtras = { relativeTo: route };

        await this.router.navigate(commands, extras);
        this.appState.dispatch(new FocusEditor());
    }

    public async handleTopLevelBreadcrumbNavigation(
        currentProject: string,
        currentBranchUuid: string,
        currentNodeUuid: string,
    ): Promise<void> {
        const breadcrumbEntries = await this.getBreadcrumbNavigationEntries(
            currentProject,
            { nodeUuid: currentNodeUuid },
            currentBranchUuid,
        );

        if (!breadcrumbEntries) {
            return;
        }

        const breadcrumbPath: IBreadcrumbRouterLink[] = [
            {
                route: ['/'],
                text: 'Dashboard',
            },
            {
                route: ['/' + AdminUIModuleRoutes.MESH_BROWSER],
                text: 'Mesh Browser',
            },
        ];

        this.breadcrumbService.setBreadcrumbs(breadcrumbPath);
    }

    public async getBreadcrumbNavigationEntries(
        project: string,
        params: MeshSchemaListParams,
        branchUuid: string,
    ): Promise<NavigationEntry[]> {
        const response = await this.meshClient.graphql(
            project,
            {
                query: `
                query($nodeUuid: String) {
                    node(uuid: $nodeUuid) { 
                        breadcrumb {
                            parent { 
                                displayName
                                node {uuid}
                            }
                            node {
                                displayName
                                uuid
                                isContainer
                            }
                        }
                    }
                }      
            `,
                variables: params,
            },
            {
                branch: branchUuid,
                version: 'draft',
            },
        );

        return response.data.node?.breadcrumb;
    }
}

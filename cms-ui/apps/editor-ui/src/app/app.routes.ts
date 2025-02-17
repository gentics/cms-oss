import { Route } from '@angular/router';
import { EditorOutlet } from './common/models';
import { NoNodesComponent, TagEditorRouteComponent } from './core/components';
import { ProjectEditorComponent } from './core/components/project-editor/project-editor.component';
import { AuthGuard } from './core/providers/guards/auth-guard';
import { OpenModalGuard } from './core/providers/guards/open-modal-guard';
import { ToolOverviewComponent } from './embedded-tools/components/tool-overview/tool-overview.component';
import { ToolProxyComponent } from './embedded-tools/components/tool-proxy/tool-proxy.component';
import { ProxyRouteComponent, RessourceProxyComponent } from './shared/components';

export const APP_ROUTES: Route[] = [
    {
        path: '',
        redirectTo: '/login',
        pathMatch: 'full',
    },
    {
        path: 'login',
        canActivate: [AuthGuard],
        pathMatch: 'full',
        loadChildren: () => import('./login/login.module').then(m => m.LoginModule),
    },
    {
        path: 'no-nodes',
        canActivate: [AuthGuard],
        pathMatch: 'full',
        component: NoNodesComponent,
    },
    {
        path: 'editor',
        component: ProjectEditorComponent,
        canActivate: [AuthGuard],
        children: [
            {
                path: '',
                component: ProxyRouteComponent,
                children: [
                    {
                        path: '',
                        loadChildren: () => import('./list-view/list-view.module').then(m => m.ListViewModule),
                    },
                ],
                outlet: EditorOutlet.LIST,
                canDeactivate: [OpenModalGuard],
                canActivate: [OpenModalGuard],
                canActivateChild: [OpenModalGuard],
            },
            {
                path: 'node',
                component: ProxyRouteComponent,
                children: [
                    {
                        path: '',
                        loadChildren: () => import('./editor-overlay/editor-overlay-routing.module').then(m => m.EditorOverlayRoutingModule),
                    },
                ],
                outlet: EditorOutlet.MODAL,
                canDeactivate: [OpenModalGuard],
                canActivate: [OpenModalGuard],
                canActivateChild: [OpenModalGuard],
            },
            {
                path: 'type',
                component: ProxyRouteComponent,
                children: [
                    {
                        path: '',
                        loadChildren: () => import('./editor-overlay/editor-overlay-routing.module').then(m => m.EditorOverlayRoutingModule),
                    },
                ],
                outlet: EditorOutlet.MODAL,
                canDeactivate: [OpenModalGuard],
                canActivate: [OpenModalGuard],
                canActivateChild: [OpenModalGuard],
            },
            {
                path: 'node',
                component: ProxyRouteComponent,
                children: [
                    {
                        path: '',
                        loadChildren: () => import('./content-frame/content-frame.module').then(m => m.ContentFrameModule),
                    },
                ],
                outlet: EditorOutlet.DETAIL,
                canDeactivate: [OpenModalGuard],
                canActivate: [OpenModalGuard],
                canActivateChild: [OpenModalGuard],

            },
        ],
    },
    {
        path: 'tools',
        component: ProxyRouteComponent,
        children: [
            {
                path: '',
                component: ToolOverviewComponent,
            },
            {
                path: '**',
                component: ToolProxyComponent,
            },
        ],
        canActivate: [AuthGuard],
    },
    {
        path: 'tag-editor/:nodeId/:entityType/:entityId/:tagName',
        component: TagEditorRouteComponent,
        canActivate: [AuthGuard],
    },
    {
        path: 'proxy',
        canActivate: [AuthGuard],
        pathMatch: 'prefix',
        children: [
            {
                path: '**',
                component: RessourceProxyComponent,
            },
        ],
    },
];

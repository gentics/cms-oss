import { Route } from '@angular/router';
import { ProjectEditor } from './core/components/project-editor/project-editor.component';
import { TagEditorRouteComponent } from './core/components/tag-editor-route/tag-editor-route.component';
import { AuthGuard } from './core/providers/guards/auth-guard';
import { OpenModalGuard } from './core/providers/guards/open-modal-guard';
import { ToolOverviewComponent } from './embedded-tools/components/tool-overview/tool-overview.component';
import { ToolProxyComponent } from './embedded-tools/components/tool-proxy/tool-proxy.component';
import { ProxyRouteComponent } from './shared/components/proxy-route/proxy-route.component';
import { RessourceProxyComponent } from './shared/components/ressource-proxy/ressource-proxy.component';

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
        path: 'editor',
        component: ProjectEditor,
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
                outlet: 'list',
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
                outlet: 'modal',
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
                outlet: 'modal',
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
                outlet: 'detail',
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

import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Node } from '@gentics/cms-models';
import { IBreadcrumbRouterLink } from '@gentics/ui-core';
import { TranslateService } from '@ngx-translate/core';
import { isEqual } from 'lodash-es';
import { Observable, combineLatest } from 'rxjs';
import { map, distinctUntilChanged } from 'rxjs/operators';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { NavigationService } from '../../../core/providers/navigation/navigation.service';
import { ApplicationStateService, FolderActionsService } from '../../../state';
import { EmbeddedToolsService } from '../../providers/embedded-tools/embedded-tools.service';

@Component({
    selector: 'tool-breadcrumb',
    templateUrl: './tool-breadcrumb.component.html',
    styleUrls: ['./tool-breadcrumb.component.scss'],
    standalone: false
})
export class ToolBreadcrumbComponent implements OnInit {

    nodes$: Observable<Array<Node>>;
    routerLinks$: Observable<Array<IBreadcrumbRouterLink>>;
    toolIsVisible$: Observable<boolean>;

    private TOOLS_NODE_ID = -1;

    constructor(
        private state: ApplicationStateService,
        private embeddedTools: EmbeddedToolsService,
        private entityResolver: EntityResolver,
        private folderActions: FolderActionsService,
        private navigationService: NavigationService,
        private router: Router,
        private translate: TranslateService) { }

    ngOnInit(): void {
        this.nodes$ = this.state.select(state => state.folder.nodes.list).pipe(
            map(nodeIds => nodeIds.map(id => this.entityResolver.getNode(id)))
        );

        this.routerLinks$ = combineLatest([
            this.state.select(state => state.ui.language),
            this.state.select(state => state.tools),
        ]).pipe(
            map(() => this.generateBreadcrumbRouterLinks()),
            distinctUntilChanged(isEqual),
        );

        this.toolIsVisible$ = this.state.select(state => !!state.tools.visible);
    }

    closeTool(): void {
        this.embeddedTools.close(this.state.now.tools.visible)
            .then(closed => {
                if (closed) {
                    this.folderActions.navigateListAndEditorFromState();
                }
            });
    }

    generateBreadcrumbRouterLinks(): IBreadcrumbRouterLink[] {
        const appState = this.state.now;
        const toolKey = appState.tools.visible;
        const tool = appState.tools.available.find(tool => tool.key === toolKey);

        const overviewLink: IBreadcrumbRouterLink = {
            route: ['/tools'],
            text: this.translate.instant('common.tools'),
        };

        if (!tool) {
            return [overviewLink];
        }

        const toolPath = appState.tools.subpath[toolKey] || '';

        const toolRootLink: IBreadcrumbRouterLink = {
            text: typeof tool.name === 'string' ? tool.name : tool.name[appState.ui.language],
            route: toolPath ? ['/tools', toolKey, toolPath] : ['/tools', toolKey],
        };

        const toolSuppliedBreadcrumbs = appState.tools.breadcrumbs[toolKey];
        if (!toolSuppliedBreadcrumbs || !toolSuppliedBreadcrumbs.length) {
            return [overviewLink, toolRootLink];
        }

        const toolSuppliedBreadcrumbsAsRouterLink = toolSuppliedBreadcrumbs
            .map((crumb, index) => ({
                text: index ? crumb.text : toolRootLink.text,
                route: ['/tools', toolKey, crumb.url || ''],
            }));

        return [overviewLink, ...toolSuppliedBreadcrumbsAsRouterLink];
    }

    navigateToNodeFolder(node: Node): void {
        this.navigationService.instruction({
            list: {
                folderId: node.folderId,
                nodeId: node.id,
            },
        }).navigate();
    }

    navigateToOverview(): void {
        this.router.navigateByUrl('/tools');
    }

}

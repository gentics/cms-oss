import { Injectable, OnDestroy } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { KeycloakService, SKIP_KEYCLOAK_PARAMETER_NAME, WindowRef } from '@gentics/cms-components';
import { EmbeddedTool } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { I18nService } from '@gentics/cms-components';
import { Subscription } from 'rxjs';
import { filter, map, pairwise, startWith, switchMap, take, tap } from 'rxjs/operators';
import { ADMIN_UI_LINK } from '../../../common/config/config';
import { Api } from '../../../core/providers/api/api.service';
import {
    ApplicationStateService,
    CloseToolAction,
    HideToolsAction,
    OpenToolAction,
    StartToolsFetchingAction,
    ToolNavigationAction,
    ToolsFetchingErrorAction,
    ToolsFetchingSuccessAction,
} from '../../../state';
import { ToolApiChannelService } from '../tool-api-channel/tool-api-channel.service';
import { TabbedTool } from './tabbed-tool';

/** Matches "/tools/:toolname[/:tool-sub-path]" */
const toolsPathRegex = /^\/tools(?:\/([^/]+)(?:\/(.+))?)?$/;

@Injectable()
export class EmbeddedToolsService implements OnDestroy {

    private subscriptions: Subscription[] = [];
    private tabbedTools = new Map<string, TabbedTool>();

    adminUITabWindow: Window;

    constructor(
        private api: Api,
        private channelService: ToolApiChannelService,
        private state: ApplicationStateService,
        private modalService: ModalService,
        private router: Router,
        private translate: I18nService,
        private windowRef: WindowRef,
        private keycloak : KeycloakService,
    ) {
        // Required to prevent cyclic dependencies.
        channelService.registerToolsService(this);
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    loadAvailableToolsWhenLoggedIn(): void {
        const sub = this.state.select(state => state.auth.currentUserId).pipe(
            filter(id => id != null),
            tap(() => this.state.dispatch(new StartToolsFetchingAction())),
            switchMap(() => this.api.admin.getAvailableEmbeddedTools()),
        ).subscribe(response => {
            this.state.dispatch(new ToolsFetchingSuccessAction(response.tools));
        }, () => {
            this.state.dispatch(new ToolsFetchingErrorAction());
        });

        this.subscriptions.push(sub);
    }

    updateStateWhenRouteChanges(): void {
        const sub = this.router.events.pipe(
            filter(event => event instanceof NavigationEnd),
            map((event: NavigationEnd) => event.url),
            startWith(this.router.url),
        ).subscribe(url => {
            const urlParts = toolsPathRegex.exec(url);

            if (!urlParts) {
                this.state.dispatch(new HideToolsAction());
                return;
            }

            const toolToOpen = urlParts[1];
            const subpath = urlParts[2] || '';
            const currentTool = this.state.now.tools.visible;

            if (!toolToOpen) {
                this.state.dispatch(new HideToolsAction());
                return;
            }

            if (toolToOpen === currentTool) {
                this.navigateToolFromRoute(toolToOpen, subpath);
                this.state.dispatch(new ToolNavigationAction(toolToOpen, subpath));
                return;
            }

            // if a tool does not exist, redirect to overview
            this.state.select(state => state.tools).pipe(
                filter(tools => tools.received),
                take(1),
            ).subscribe(tools => {
                if (tools.available.find(tool => tool.key === toolToOpen && !tool.newtab)) {
                    const isActive = this.state.now.tools.active.indexOf(toolToOpen) >= 0;
                    if (isActive) {
                        this.navigateToolFromRoute(toolToOpen, subpath);
                    }
                    this.state.dispatch(new OpenToolAction(toolToOpen, subpath));
                } else {
                    this.router.navigateByUrl('/tools', { replaceUrl: true });
                }
            });
        });

        this.subscriptions.push(sub);
    }

    manageTabbedToolsWhenStateChanges(): void {
        const openOrCloseSubscription = this.state.select(state => state.tools.active).pipe(
            pairwise(),
        ).subscribe(([lastActiveTools, activeTools]) => {
            const availableTools = this.state.now.tools.available;

            const opened = activeTools.filter(key => lastActiveTools.indexOf(key) < 0)
                .map(key => availableTools.find(tool => tool.key === key))
                .filter(tool => tool.newtab);
            const closed = lastActiveTools.filter(key => activeTools.indexOf(key) < 0)
                .map(key => availableTools.find(tool => tool.key === key))
                .filter(tool => tool.newtab);

            for (const tool of opened) {
                this.openToolInNewTab(tool);
            }

            for (const tool of closed) {
                this.closeTab(tool.key);
            }
        });

        this.subscriptions.push(openOrCloseSubscription);
    }

    openOrFocusAdminUI(): void {
        const window: Window = this.windowRef.nativeWindow;

        if (!this.adminUITabWindow) {
            this.adminUITabWindow = window.open(ADMIN_UI_LINK + (this.keycloak.ssoSkipped() ? '?' + SKIP_KEYCLOAK_PARAMETER_NAME : ''), '_blank');
            this.adminUITabWindow.addEventListener('beforeunload', () => {
                this.adminUITabWindow = null;
            });
        } else {
            this.adminUITabWindow.focus();
        }
    }

    closeAdminUI(): void {
        this.adminUITabWindow.close();
    }

    isAdminUIOpen(): boolean {
        return !!this.adminUITabWindow;
    }

    openOrFocus(toolKey: string): void {
        this.state.dispatch(new OpenToolAction(toolKey));

        const tab = this.tabbedTools.get(toolKey);
        if (tab) {
            tab.focus();
        }
    }

    close(toolKey: string, force: true): Promise<true>;
    close(toolKey: string, force?: false): Promise<boolean>;
    close(toolKey: string, force: boolean = false): Promise<boolean> {
        const closeTool = () => {
            const isCurrentlyOpen = this.state.now.tools.visible === toolKey;
            this.state.dispatch(new CloseToolAction(toolKey));

            if (isCurrentlyOpen) {
                this.router.navigateByUrl('/tools');
            }

            return Promise.resolve(true);
        };

        const api = this.channelService.getApi(toolKey);
        if (force || !api || !api.hasUnsavedChanges) {
            return closeTool();
        }

        return Promise.resolve(api.hasUnsavedChanges()).then(unsavedChanges => {
            if (!unsavedChanges) {
                return closeTool();
            }

            this.askUserToDiscardChanges(toolKey).then(userChoice => {
                if (userChoice === 'discard') {
                    return closeTool();
                } else if (userChoice === 'open') {
                    this.openOrFocus(toolKey);
                    return false;
                }
            });
        });
    }

    toolWantsToNavigate(toolKey: string, subpath: string, replaceUrl: boolean = false): void {
        this.state.dispatch(new ToolNavigationAction(toolKey, subpath));
        const newUrl = `/tools/${toolKey}/${subpath}`.replace(/\/+$|(\/)\/+/g, '$1');
        this.router.navigateByUrl(newUrl, { replaceUrl });
    }

    private navigateToolFromRoute(toolKey: string, subpath: string): void {
        const api = this.channelService.getApi(toolKey);
        const currentSubpath = this.state.now.tools.subpath[toolKey];
        if (api && api.navigate && subpath !== currentSubpath) {
            Promise.resolve(api.navigate(subpath))
                .catch(() => {
                    // Ignore all errors
                });
        }
    }

    private openToolInNewTab(tool: EmbeddedTool): TabbedTool {
        const window: Window = this.windowRef.nativeWindow;

        const tabWindow = window.open(tool.toolUrl, '_blank');
        const subscription = new Subscription();
        subscription.add(this.channelService.connect(tool.key, tabWindow));

        const tabbedTool: TabbedTool = {
            key: tool.key,
            window: tabWindow,
            api: this.channelService.getApi(tool.key),
            subscription,
            close: () => {
                tabWindow.close();
                subscription.unsubscribe();
            },
            focus: () => tabWindow.focus(),
        };

        const checkClosedInterval = setInterval(() => {
            if (tabWindow.closed) {
                subscription.unsubscribe();
                this.tabClosedByUser(tool.key);
            }
        }, 500);
        subscription.add(() => clearInterval(checkClosedInterval));
        this.subscriptions.push(subscription);

        this.tabbedTools.set(tool.key, tabbedTool);
        return tabbedTool;
    }

    private closeTab(toolKey: string): void {
        const tool = this.tabbedTools.get(toolKey);
        if (tool) {
            tool.close();
            tool.subscription.unsubscribe();
        }
    }

    private tabClosedByUser(toolKey: string): void {
        this.state.dispatch(new CloseToolAction(toolKey));
    }

    private askUserToDiscardChanges(toolKey: string): Promise<'open' | 'discard'> {
        let toolName = this.state.now.tools.available.find(tool => tool.key === toolKey).name;
        if (typeof toolName !== 'string') {
            toolName = toolName[this.state.now.ui.language];
        }

        return this.modalService
            .dialog({
                title: this.translate.instant('modal.tool_unsaved_title'),
                body: this.translate.instant('modal.tool_unsaved_body', { toolName }),
                buttons: [
                    {
                        label: this.translate.instant('modal.tool_unsaved_open_button'),
                        type: 'secondary',
                        // flat: true,
                        returnValue: 'open',
                    },
                    {
                        label: this.translate.instant('modal.tool_unsaved_discard_button'),
                        type: 'alert',
                        returnValue: 'discard',
                    },
                ],
            })
            .then(modal => modal.open());
    }
}

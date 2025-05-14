import { ChangeDetectionStrategy, ChangeDetectorRef, Component, ContentChild, EventEmitter, OnDestroy, OnInit, Output } from '@angular/core';
import { ADMIN_UI_LINK } from '@editor-ui/app/common/config/config';
import { EmbeddedToolsService } from '@editor-ui/app/embedded-tools/providers/embedded-tools/embedded-tools.service';
import { ApplicationStateService, ContentStagingActionsService } from '@editor-ui/app/state';
import { AccessControlledType, EmbeddedTool, GcmsPermission } from '@gentics/cms-models';
import { DropdownListComponent } from '@gentics/ui-core';
import { isEqual } from 'lodash-es';
import { Observable, Subscription, combineLatest, of } from 'rxjs';
import { debounceTime, distinctUntilChanged, map, startWith, switchMap, tap } from 'rxjs/operators';
import {
    ADMIN_TOOL_KEY,
    ActionButton,
    ActionButtonGroup,
    ActionButtonIconType,
    ActionButtonType,
    PRODUCT_TOOL_KEYS,
} from '../../../common/models/actions';
import { I18nService } from '../../providers/i18n/i18n.service';
import { PermissionService } from '../../providers/permissions/permission.service';
import { KeycloakService, SKIP_KEYCLOAK_PARAMETER_NAME } from '@gentics/cms-components';

@Component({
    selector: 'gtx-actions-selector',
    templateUrl: './actions-selector.component.html',
    styleUrls: ['./actions-selector.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ActionsSelectorComponent implements OnInit, OnDestroy {

    @Output()
    public actionClick = new EventEmitter<{ action: string, event: MouseEvent }>();

    @Output()
    public toolClick = new EventEmitter<{ tool: EmbeddedTool, event: MouseEvent }>();

    @ContentChild('dropdown')
    public dropdown: DropdownListComponent;

    public readonly ActionButtonType = ActionButtonType;
    public readonly ActionButtonIconType = ActionButtonIconType;

    public buttonGroups: ActionButtonGroup[] = [];

    protected subscriptions: Subscription[] = [];

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected appState: ApplicationStateService,
        protected permissions: PermissionService,
        protected stagingActions: ContentStagingActionsService,
        protected toolsService: EmbeddedToolsService,
        protected i18n: I18nService,
        protected keycloak: KeycloakService,
    ) { }

    ngOnInit(): void {
        this.subscriptions.push(combineLatest([
            this.createProductToolsGroup(),
            this.createCustomToolsGroup(),
        ]).subscribe(groups => {
            this.buttonGroups = groups.filter(group => group != null);
            this.changeDetector.markForCheck();

            if (this.dropdown && this.dropdown.isOpen) {
                this.dropdown.resize();
            }
        }, err => {
            console.error('Error while generating action-button groups!', err);
        }));
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    triggerClick(button: ActionButton, event: MouseEvent): void {
        this.closeDropdown();

        if (button.type === ActionButtonType.ACTION) {
            this.actionClick.emit({ action: button.id, event });
            return;
        }

        const tool = this.appState.now.tools.available.find(tool => tool.key === button.toolKey);
        this.toolClick.emit({ tool, event });

        if (button.newTab) {
            return;
        }

        event.preventDefault();
        event.stopImmediatePropagation();
        event.stopPropagation();

        if (button.id === ADMIN_TOOL_KEY) {
            this.toolsService.openOrFocusAdminUI();
        } else {
            this.toolsService.openOrFocus(button.toolKey);
        }
    }

    closeDropdown(): void {
        if (this.dropdown) {
            this.dropdown.closeDropdown();
        }
    }

    protected createProductToolsGroup(): Observable<ActionButtonGroup> {
        // Content Staging setup
        const showContentStaging$ = this.appState.select(state => state.features.content_staging).pipe(
            switchMap(isEnabled => {
                if (!isEnabled) {
                    return of(false);
                }

                // Check if the user has the required permissions
                return this.permissions.getTypePermissions(AccessControlledType.CONTENT_STAGING_ADMIN).pipe(
                    map(perm => perm.hasPermission(GcmsPermission.READ) && perm.hasPermission(GcmsPermission.MODIFY_CONTENT)),
                );
            }),
            tap(enabledAndPermission => {
                // Load the packages if it's enabled and the user got permissions
                if (enabledAndPermission) {
                    this.stagingActions.loadPackages();
                }
            }),
        );

        const userCanSeeWastebin$ = combineLatest([
            this.permissions.wastebin$,
            this.appState.select(state => state.features.wastebin),
        ]).pipe(
            debounceTime(50),
            startWith([false, false]),
            map(([wastebinFeature, wastebinPermission]) => wastebinFeature && wastebinPermission),
            distinctUntilChanged(isEqual),
        );

        const userCanSeePublishQueue$ = this.permissions.viewPublishQueue$;

        const isAdmin$ = this.appState.select(state => state.auth.isAdmin);
        const productTools = this.appState.select(state => state.tools.available).pipe(
            map(tools => tools.filter(tool => PRODUCT_TOOL_KEYS.includes(tool.key))),
            startWith([]),
        );

        return combineLatest([
            this.appState.select(state => state.ui.language),
            showContentStaging$,
            userCanSeeWastebin$,
            userCanSeePublishQueue$,
            isAdmin$,
            productTools,
        ]).pipe(map(([language, contentStaging, wastebin, publishQueue, admin, otherTools]) => {
            const buttons: ActionButton[] = [];

            if (contentStaging) {
                buttons.push({
                    id: 'content-staging',
                    type: ActionButtonType.ACTION,
                    i18nLabel: 'editor.content_staging_label',
                    iconType: ActionButtonIconType.FONT,
                    icon: 'inventory',
                });
            }

            if (wastebin) {
                buttons.push({
                    id: 'wastebin',
                    type: ActionButtonType.ACTION,
                    i18nLabel: 'editor.wastebin_label',
                    iconType: ActionButtonIconType.FONT,
                    icon: 'delete_sweep',
                });
            }

            if (publishQueue) {
                buttons.push({
                    id: 'publish-queue',
                    type: ActionButtonType.ACTION,
                    i18nLabel: 'editor.publish_queue_label',
                    iconType: ActionButtonIconType.FONT,
                    icon: 'playlist_add_check',
                });
            }

            if (admin) {
                // Put the admin-tool at the very beginning
                buttons.push({
                    id: ADMIN_TOOL_KEY,
                    type: ActionButtonType.TOOL,
                    i18nLabel: 'editor.administration_tool_label',
                    toolLink: ADMIN_UI_LINK + (this.keycloak.ssoSkipped() ? '?' + SKIP_KEYCLOAK_PARAMETER_NAME : ''),
                    newTab: true,
                    iconType: ActionButtonIconType.FONT,
                    icon: 'tune',
                    toolKey: null,
                });
            }

            if (Array.isArray(otherTools) && otherTools.length > 0) {
                buttons.push(...otherTools.map(tool => this.toolToButton(tool, language)));
            }

            if (buttons.length === 0) {
                return null;
            }

            buttons.forEach(btn => this.normalizeActionButton(btn));
            buttons.sort((a, b) => a.label.localeCompare(b.label));

            return {
                i18nLabel: 'editor.product_tools_label',
                buttons: [buttons],
            };
        }));
    }

    protected createCustomToolsGroup(): Observable<ActionButtonGroup> {
        return combineLatest([
            this.appState.select(state => state.ui.language),
            this.appState.select(state => state.tools.available).pipe(
                map(tools => tools.filter(tool => !PRODUCT_TOOL_KEYS.includes(tool.key))),
            ),
        ]).pipe(
            map(([language, tools]) => {
                const buttons: ActionButton[] = (tools || [])
                    .map(tool => this.toolToButton(tool, language))
                    .map(btn => this.normalizeActionButton(btn));

                if (buttons.length === 0) {
                    return null;
                }

                buttons.sort((a, b) => a.label.localeCompare(b.label));

                return {
                    i18nLabel: 'editor.custom_tools_label',
                    buttons: [buttons],
                };
            }),
        );
    }

    protected toolToButton(tool: EmbeddedTool, language: string): ActionButton {
        const btn: ActionButton = {
            id: `tool_${tool.key}`,
            toolKey: tool.key,
            type: ActionButtonType.TOOL,
            label: tool.name[language],
            iconType: null,
            icon: tool.iconUrl || '',
            toolLink: tool.toolUrl,
            newTab: tool.newtab ?? false,
        };

        return btn;
    }

    protected normalizeActionButton(btn: ActionButton): ActionButton {
        if (!btn.label) {
            if (btn.i18nLabel) {
                btn.label = this.i18n.translate(btn.i18nLabel);
            }
        }
        if (!btn.label) {
            btn.label = btn.id;
        }

        // Determine the proper icon-type
        if (/^https?:\/\//.test(btn.icon)) {
            btn.iconType = ActionButtonIconType.URL;
        } else if (/^[a-z0-9_]+$/.test(btn.icon)) {
            btn.iconType = ActionButtonIconType.FONT;
        } else {
            btn.iconType = ActionButtonIconType.TEXT;
            btn.icon = (btn.label || '').trim().substring(0, 1);
        }

        return btn;
    }
}

import {
    AdminUIEntityDetailRoutes,
    AdminUIModuleRoutes,
    BO_PERMISSIONS,
    EntityTableActionClickEvent,
    NodeBO,
    ROUTE_DETAIL_OUTLET,
    ROUTE_ENTITY_LOADED,
    ROUTE_ENTITY_RESOLVER_KEY,
    TemplateBO,
} from '@admin-ui/common';
import {
    ErrorHandler,
    I18nNotificationService,
    I18nService,
    NodeOperations,
    PermissionsService,
    TemplateOperations,
    TemplateTableLoaderService,
} from '@admin-ui/core';
import { BaseTableMasterComponent } from '@admin-ui/shared';
import { AppStateService, FocusEditor } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActivatedRoute, NavigationExtras, Router } from '@angular/router';
import {
    AnyModelType,
    Feature,
    GcmsPermission,
    LocalizeRequest,
    Node,
    NormalizableEntityTypesMap,
    Raw,
    Response,
    Template,
    UnlocalizeRequest,
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { ModalService, TableAction, TableRow, getFullPrimaryPath } from '@gentics/ui-core';
import { Observable, of } from 'rxjs';
import { distinctUntilChanged, map, switchMap } from 'rxjs/operators';
import { CopyTemplateService } from '../../providers/copy-template/copy-template.service';
import { AssignTemplatesToFoldersModalComponent } from '../assign-templates-to-folders-modal/assign-templates-to-folders-modal.component';
import { AssignTemplatesToNodesModalComponent } from '../assign-templates-to-nodes-modal/assign-templates-to-nodes-modal.component';
import { CreateTemplateModalComponent } from '../create-template-modal/create-template-modal.component';

const NODE_ID_PARAM = 'nodeId';
const OPERATION_FOREGROUND_TIME_MS = 2_0000;

enum Action {
    LINK_TO_FOLDER = 'linkToFolder',
    LINK_TO_NODE = 'linkToNode',
    COPY = 'copy',
    LOCALIZE = 'localize',
    UNLOCALIZE = 'unlocalize',
}


@Component({
    selector: 'gtx-template-master',
    templateUrl: './template-master.component.html',
    styleUrls: ['./template-master.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class TemplateMasterComponent extends BaseTableMasterComponent<Template, TemplateBO> implements OnInit {

    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'template';

    public activeNode?: Node;
    public selected: string[] = [];
    public actions: TableAction<TemplateBO>[] = [];

    constructor(
        changeDetector: ChangeDetectorRef,
        router: Router,
        route: ActivatedRoute,
        appState: AppStateService,
        protected api: GcmsApi,
        protected nodeOperations: NodeOperations,
        protected modalService: ModalService,
        protected loader: TemplateTableLoaderService,
        protected operations: TemplateOperations,
        protected i18n: I18nService,
        protected notification: I18nNotificationService,
        protected permissions: PermissionsService,
        protected errorHandler: ErrorHandler,
        protected copyTemplateComponent: CopyTemplateService,
    ) {
        super(
            changeDetector,
            router,
            route,
            appState,
        );
    }

    public ngOnInit(): void {
        super.ngOnInit();

        this.subscriptions.push(this.route.paramMap.pipe(
            map(params => params.get(NODE_ID_PARAM)),
            distinctUntilChanged(),
            switchMap(nodeId => {
                if (nodeId == null || nodeId.trim().length === 0) {
                    return of(null);
                }
                const nodeIdNum = Number(nodeId);
                if (!Number.isInteger(nodeIdNum)) {
                    return of(null);
                }

                return this.nodeOperations.get(nodeIdNum);
            }),
        ).subscribe(node => {
            this.selected = [];
            this.activeNode = node;
            this.changeDetector.markForCheck();
        }));

        this.actions = [
            {
                id: Action.COPY,
                icon: 'content_copy',
                iconHollow: true,
                label: this.i18n.instant('shared.copy'),
                type: 'secondary',
                enabled: (template) => template[BO_PERMISSIONS].includes(GcmsPermission.EDIT),
                multiple: false,
                single: true,
            },
            {
                id: Action.LINK_TO_NODE,
                icon: 'device_hub',
                label: this.i18n.instant('template.assign_to_nodes'),
                type: 'primary',
                enabled: (template) => template == null || template[BO_PERMISSIONS].includes(GcmsPermission.EDIT),
                multiple: true,
                single: true,
            },
            {
                id: Action.LINK_TO_FOLDER,
                icon: 'folder',
                label: this.i18n.instant('template.assign_to_folders'),
                type: 'secondary',
                enabled: (template) => template == null || template[BO_PERMISSIONS].includes(GcmsPermission.EDIT),
                multiple: true,
                single: true,
            },
        ];

        if (this.appState.now.features.global[Feature.MULTICHANNELLING]) {
            this.actions.push({
                id: Action.LOCALIZE,
                icon: 'insert_drive_file',
                label: this.i18n.instant('template.localize'),
                type: 'secondary',
                enabled: template => template == null || (template.inherited),
                multiple: true,
                single: true,
            },
            {
                id: Action.UNLOCALIZE,
                icon: 'restore_page',
                label: this.i18n.instant('template.unlocalize'),
                type: 'warning',
                enabled: template => template == null || (!template.inherited && !template.master),
                multiple: true,
                single: true,
            })
        }
    }

    public handleNodeSelect(row: TableRow<NodeBO>): void {
        this.router.navigate([`/${AdminUIModuleRoutes.TEMPLATES}/${row.item.id}`], { relativeTo: this.route });
    }

    public navigateBack(): void {
        this.router.navigate([`/${AdminUIModuleRoutes.TEMPLATES}`], { relativeTo: this.route });
    }

    public async handleCreate(): Promise<void> {
        const dialog = await this.modalService.fromComponent(CreateTemplateModalComponent, {
            closeOnEscape: false,
            closeOnOverlayClick: false,
        }, {
            node: this.activeNode,
        });
        const created = await dialog.open();

        if (created) {
            this.loader.reload();
        }
    }

    public handleAction(event: EntityTableActionClickEvent<TemplateBO>): void {
        const items = event.selection ? event.selectedItems : [event.item];

        switch (event.actionId as Action) {
            case Action.COPY:
                this.copyTemplate(event.item);
                return;

            case Action.LINK_TO_FOLDER:
                this.linkTemplatesToFolders(items);
                return;

            case Action.LINK_TO_NODE:
                this.linkTemplatesToNodes(items).then(didChange => {
                    if (didChange) {
                        this.loader.reload();
                    }
                });
                return;

            case Action.LOCALIZE:
                this.localizeTemplate(items);
                return;

            case Action.UNLOCALIZE:
                this.unlocalizeTemplate(items);
                return;
        }
    }

    protected override async navigateToEntityDetails(row: TableRow<TemplateBO>): Promise<void> {
        const fullUrl = getFullPrimaryPath(this.route);
        const commands: any[] = [
            fullUrl,
            { outlets: { [ROUTE_DETAIL_OUTLET]: [AdminUIEntityDetailRoutes.TEMPLATE, this.activeNode?.id, row.id] } },
        ];
        const extras: NavigationExtras = { relativeTo: this.route };

        if (this.navigateWithEntity()) {
            extras.state = {
                [ROUTE_ENTITY_LOADED]: true,
                [ROUTE_ENTITY_RESOLVER_KEY]: row.item,
            };
        }

        await this.router.navigate(commands, extras);
        this.appState.dispatch(new FocusEditor());
    }

    protected async copyTemplate(template: TemplateBO): Promise<void> {
        let loadedTemplate: Template<Raw>;

        try {
            const loadResponse = await this.api.template.getTemplate(template.id, {
                nodeId: this.activeNode.id,
            }).toPromise();
            loadedTemplate = loadResponse.template;
        } catch (err) {
            this.errorHandler.notifyAndReturnErrorMessage(err);
            return;
        }

        const created = await this.copyTemplateComponent.createCopy(this.activeNode, loadedTemplate);

        if (created) {
            this.loader.reload();
            this.navigateToEntityDetails({ item: created, id: `${created?.id as string}` });
        }
    }

    protected async linkTemplatesToNodes(templates: TemplateBO[]): Promise<boolean> {
        // Can't open without selection
        if (templates.length === 0) {
            return false;
        }

        let doAbort = false;
        templates.forEach(t => {
            if (!t[BO_PERMISSIONS].includes(GcmsPermission.EDIT)) {
                this.notification.show({
                    type: 'alert',
                    message: 'template.assign_template_to_node_permission_required',
                    translationParams: {
                        templateName: t.name,
                    },
                });
                doAbort = true;
            }
        });

        if (doAbort) {
            return false;
        }

        // Close the details, as we have no way of reloading the assignment inside the details right now.
        if (this.appState.now.ui.editorIsOpen) {
            const closed = await this.router.navigate(
                [{ outlets: { detail: null } }],
                { relativeTo: this.route },
            );

            if (!closed) {
                return false;
            }
        }

        const dialog = await this.modalService.fromComponent(AssignTemplatesToNodesModalComponent, {
            closeOnEscape: false,
            closeOnOverlayClick: false,
        }, {
            templates,
        });

        return dialog.open();
    }

    protected async linkTemplatesToFolders(templates: TemplateBO[]): Promise<void> {
        // Can't open without selection
        if (templates.length === 0) {
            return;
        }

        let doAbort = false;
        templates.forEach(t => {
            if (!t[BO_PERMISSIONS].includes(GcmsPermission.EDIT)) {
                this.notification.show({
                    type: 'alert',
                    message: 'template.assign_template_to_folder_permission_required',
                    translationParams: {
                        templateName: t.name,
                    },
                });
                doAbort = true;
            }
        });

        if (doAbort) {
            return;
        }

        // Close the details, as we have no way of reloading the assignment inside the details right now.
        if (this.appState.now.ui.editorIsOpen) {
            const closed = await this.router.navigate(
                [{ outlets: { detail: null } }],
                { relativeTo: this.route },
            );

            if (!closed) {
                return;
            }
        }

        const dialog = await this.modalService.fromComponent(AssignTemplatesToFoldersModalComponent, {
            closeOnEscape: false,
            closeOnOverlayClick: false,
        }, {
            nodeId: this.activeNode.id,
            rootFolderId: this.activeNode.folderId,
            templates,
        });

        await dialog.open();
    }

    protected localizeTemplate(templates: TemplateBO[]): void {
        this.executeTemplateOperation(templates.filter(template => template.inherited),
            (templateId, options) => this.operations.localizeTemplate(templateId, options), 'template.localize_success');
    }

    protected unlocalizeTemplate(templates: TemplateBO[]): void {
        this.executeTemplateOperation(templates.filter(template => !template.inherited && !template.master),
            (templateId, options) => this.operations.unlocalizeTemplate(templateId, options), 'template.unlocalize_success');
    }

    private executeTemplateOperation(
        templates: TemplateBO[],
        operation: (id: number, options: LocalizeRequest | UnlocalizeRequest) => Observable<Response>,
        i18nMessage: string,
    ): void {
        if (!this.isChannel()) {
            return;
        }
        const channelId = this.activeNode.id;

        Promise.all(
            templates.map(template => {
                return operation(template.id, {
                    channelId,
                    foregroundTime: OPERATION_FOREGROUND_TIME_MS,
                }).toPromise().then(_success => {
                    this.notification.show({
                        type: 'success',
                        message: i18nMessage,
                        translationParams: {
                            templateName: template.name,
                        },
                    });
                })
            }),
        ).then(_success => {
            this.selected = [];
            this.loader.reload();
        });
    }

    private isChannel(): boolean {
        if (this.activeNode?.masterNodeId === this.activeNode?.id) {
            return false;
        }

        return true;
    }
}

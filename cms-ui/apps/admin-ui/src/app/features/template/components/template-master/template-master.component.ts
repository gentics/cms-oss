import {
    AdminUIEntityDetailRoutes,
    AdminUIModuleRoutes,
    BO_PERMISSIONS,
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
    TemplateTableLoaderService,
} from '@admin-ui/core';
import { BaseTableMasterComponent } from '@admin-ui/shared';
import { AppStateService, FocusEditor } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActivatedRoute, NavigationExtras, Router } from '@angular/router';
import { AnyModelType, GcmsPermission, Node, NormalizableEntityTypesMap, Raw, Template } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { ModalService, TableAction, TableActionClickEvent, TableRow, getFullPrimaryPath } from '@gentics/ui-core';
import { of } from 'rxjs';
import { distinctUntilChanged, map, switchMap } from 'rxjs/operators';
import { AssignTemplatesToFoldersModalComponent } from '../assign-templates-to-folders-modal/assign-templates-to-folders-modal.component';
import { AssignTemplatesToNodesModalComponent } from '../assign-templates-to-nodes-modal/assign-templates-to-nodes-modal.component';
import { CopyTemplateService } from '../../providers/copy-template/copy-template.service';
import { CreateTemplateModalComponent } from '../create-template-modal/create-template-modal.component';

const NODE_ID_PARAM = 'nodeId';
const LINK_TO_FOLDER_ACTION = 'linkToFolder';
const LINK_TO_NODE_ACTION = 'linkToNode';
const COPY_ACTION = 'copy';

@Component({
    selector: 'gtx-template-master',
    templateUrl: './template-master.component.html',
    styleUrls: ['./template-master.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
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
        protected i18n: I18nService,
        protected notification: I18nNotificationService,
        protected permissions: PermissionsService,
        protected templateTableLoader: TemplateTableLoaderService,
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
                id: COPY_ACTION,
                icon: 'content_copy',
                label: this.i18n.instant('shared.copy'),
                type: 'secondary',
                enabled: (template) => template[BO_PERMISSIONS].includes(GcmsPermission.EDIT),
                multiple: false,
                single: true,
            },
            {
                id: LINK_TO_NODE_ACTION,
                icon: 'device_hub',
                label: this.i18n.instant('template.assign_to_nodes'),
                type: 'primary',
                enabled: (template) => template == null || template[BO_PERMISSIONS].includes(GcmsPermission.EDIT),
                multiple: true,
                single: true,
            },
            {
                id: LINK_TO_FOLDER_ACTION,
                icon: 'folder',
                label: this.i18n.instant('template.assign_to_folders'),
                type: 'secondary',
                enabled: (template) => template == null || template[BO_PERMISSIONS].includes(GcmsPermission.EDIT),
                multiple: true,
                single: true,
            },
        ];
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

    public handleAction(event: TableActionClickEvent<TemplateBO>): void {
        const getTemplates = () => {
            if (!event.selection) {
                return [event.item];
            }

            return this.loader.getEntitiesByIds(this.selected)
                .map(template => this.loader.mapToBusinessObject(template));
        }

        switch (event.actionId) {
            case COPY_ACTION:
                this.copyTemplate(event.item);
                return;

            case LINK_TO_FOLDER_ACTION:
                this.linkTemplatesToFolders(getTemplates());
                return;

            case LINK_TO_NODE_ACTION:
                this.linkTemplatesToNodes(getTemplates());
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
            this.templateTableLoader.reload();
            this.navigateToEntityDetails({ item: created as any, id: `${created.id}` });
        }
    }

    protected async linkTemplatesToNodes(templates: TemplateBO[]): Promise<void> {
        // Can't open without selection
        if (templates.length === 0) {
            return;
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

        const dialog = await this.modalService.fromComponent(AssignTemplatesToNodesModalComponent, {
            closeOnEscape: false,
            closeOnOverlayClick: false,
        }, {
            templates,
        });

        await dialog.open();
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
}

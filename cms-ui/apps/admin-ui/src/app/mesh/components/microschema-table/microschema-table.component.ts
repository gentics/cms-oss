import { BO_PERMISSIONS } from '@admin-ui/common';
import { I18nService } from '@admin-ui/core';
import { MeshMicroschemaBO, MeshProjectBO } from '@admin-ui/mesh/common';
import { MicroschemaHandlerService, MicroschemaTableLoaderService } from '@admin-ui/mesh/providers';
import { BaseEntityTableComponent, DELETE_ACTION } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { Microschema, Permission } from '@gentics/mesh-models';
import { ModalService, TableAction, TableActionClickEvent, TableColumn } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { MicroschemaModal } from '../microschema-modal/microschema-modal.component';
import { MicroschemaPropertiesMode } from '../microschema-properties/microschema-properties.component';
import { SelectProjectModal } from '../select-project-modal/select-project-modal.component';

const EDIT_ACTION = 'edit';
const MANAGE_PROJECT_ASSIGNMENT_ACTION = 'manageProjects';
const ASSIGN_TO_PROJECTS_ACTION = 'assignToProjects';
const UNASSIGN_FROM_PROJECTS_ACTION = 'unassignFromProjects';

@Component({
    selector: 'gtx-mesh-microschema-table',
    templateUrl: './microschema-table.component.html',
    styleUrls: ['./microschema-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class MicroschemaTableComponent extends BaseEntityTableComponent<Microschema, MeshMicroschemaBO> {

    protected rawColumns: TableColumn<MeshMicroschemaBO>[] = [
        {
            id: 'name',
            label: 'common.name',
            fieldPath: 'name',
            sortable: true,
        },
        {
            id: 'description',
            label: 'common.description',
            fieldPath: 'description',
        },
    ];
    protected entityIdentifier = 'microschema' as any;

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: MicroschemaTableLoaderService,
        modalService: ModalService,
        protected handler: MicroschemaHandlerService,
    ) {
        super(
            changeDetector,
            appState,
            i18n,
            loader,
            modalService,
        );
    }

    protected override createTableActionLoading(): Observable<TableAction<MeshMicroschemaBO>[]> {
        return this.actionRebuildTrigger$.pipe(
            map(() => {
                const actions: TableAction<MeshMicroschemaBO>[] = [
                    {
                        id: EDIT_ACTION,
                        icon: 'edit',
                        label: this.i18n.instant('common.edit'),
                        enabled: (item) => item[BO_PERMISSIONS].includes(Permission.UPDATE),
                        type: 'primary',
                        single: true,
                    },
                    {
                        id: ASSIGN_TO_PROJECTS_ACTION,
                        icon: 'link',
                        label: this.i18n.instant('mesh.assign_microschemas_to_projects'),
                        enabled: (item) => item == null || item[BO_PERMISSIONS].includes(Permission.UPDATE),
                        type: 'secondary',
                        multiple: true,
                    },
                    {
                        id: UNASSIGN_FROM_PROJECTS_ACTION,
                        icon: 'link_off',
                        label: this.i18n.instant('mesh.unassign_microschemas_from_projects'),
                        enabled: (item) => item == null || item[BO_PERMISSIONS].includes(Permission.UPDATE),
                        type: 'secondary',
                        multiple: true,
                    },
                    {
                        id: DELETE_ACTION,
                        icon: 'delete',
                        label: this.i18n.instant('shared.delete'),
                        enabled: (item) => item == null || item[BO_PERMISSIONS].includes(Permission.DELETE),
                        type: 'alert',
                        multiple: true,
                        single: true,
                    },
                ];

                return actions;
            }),
        );
    }

    public override handleCreateButton(): void {
        this.openModal(MicroschemaPropertiesMode.CREATE);
    }

    public override handleAction(event: TableActionClickEvent<MeshMicroschemaBO>): void {
        const items = this.getEntitiesByIds(this.getAffectedEntityIds(event));

        switch (event.actionId) {
            case EDIT_ACTION:
                this.openModal(MicroschemaPropertiesMode.EDIT, event.item);
                return;

                // case MANAGE_PROJECT_ASSIGNMENT_ACTION:
                //     this.manageProjectAssignment(event.item);
                //     return;

            case ASSIGN_TO_PROJECTS_ACTION:
                this.handleAssignToProjectsAction(items);
                return;

            case UNASSIGN_FROM_PROJECTS_ACTION:
                this.handleUnassignFromProjectsAction(items);
                return;
        }

        super.handleAction(event);
    }

    // async manageProjectAssignment(schema: MeshMicroschemaBO): Promise<void> {
    //     // TODO: Find a way to get these
    //     const assignedProjects: string[] = [];

    //     const dialog = await this.modalService.fromComponent(SelectGroupModal, {}, {
    //         title: 'mesh.manage_group_assignment',
    //         multiple: true,
    //         selected: (schema.groups || []).map(group => group.uuid),
    //     });

    //     const groups: MeshGroupBO[] = await dialog.open();
    //     const newGroupIds = groups.map(group => group.uuid);

    //     const toAssign = groups.filter(group => !assignedProjects.includes(group.uuid));
    //     const toRemove = schema.groups.filter(group => !newGroupIds.includes(group.uuid));

    //     // Nothing to do
    //     if (toAssign.length === 0 && toRemove.length === 0) {
    //         return;
    //     }

    //     for (const group of toAssign) {
    //         this.handler.assignRole(group, schema);
    //     }
    //     for (const group of toRemove) {
    //         this.handler.unassignRole(group, schema);
    //     }

    //     this.reload();
    // }

    async handleAssignToProjectsAction(microschemas: MeshMicroschemaBO[]): Promise<void> {
        const dialog = await this.modalService.fromComponent(SelectProjectModal, {}, {
            title: 'mesh.assign_microschemas_to_projects',
            multiple: true,
        });

        const projects: MeshProjectBO[] = await dialog.open();
        if (projects.length === 0) {
            return;
        }

        for (const project of projects) {
            for (const microschema of microschemas) {
                this.handler.assignToProject(project, microschema);
            }
        }

        this.reload();
    }

    async handleUnassignFromProjectsAction(microschemas: MeshMicroschemaBO[]): Promise<void> {
        const dialog = await this.modalService.fromComponent(SelectProjectModal, {}, {
            title: 'mesh.unassign_microschemas_from_projects',
            multiple: true,
        });

        const projects: MeshProjectBO[] = await dialog.open();
        if (projects.length === 0) {
            return;
        }

        for (const project of projects) {
            for (const microschema of microschemas) {
                this.handler.unassignFromProject(project, microschema);
            }
        }

        this.reload();
    }

    async openModal(mode: MicroschemaPropertiesMode, microschema?: Microschema): Promise<void> {
        const dialog = await this.modalService.fromComponent(MicroschemaModal, {}, {
            mode,
            microschema,
        });
        const res = await dialog.open();
        if (res) {
            this.reload();
        }
    }
}

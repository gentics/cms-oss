import { BO_PERMISSIONS } from '@admin-ui/common';
import { I18nService } from '@admin-ui/core';
import { MeshProjectBO, MeshSchemaBO } from '@admin-ui/mesh/common';
import { SchemaHandlerService, SchemaTableLoaderService } from '@admin-ui/mesh/providers';
import { BaseEntityTableComponent, DELETE_ACTION } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { Permission, Schema } from '@gentics/mesh-models';
import { ModalService, TableAction, TableActionClickEvent, TableColumn } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { SchemaModal } from '../schema-modal/schema-modal.component';
import { SchemaPropertiesMode } from '../schema-properties/schema-properties.component';
import { SelectProjectModal } from '../select-project-modal/select-project-modal.component';

const EDIT_ACTION = 'edit';
const MANAGE_PROJECT_ASSIGNMENT_ACTION = 'manageProjects';
const ASSIGN_TO_PROJECTS_ACTION = 'assignToProjects';
const UNASSIGN_FROM_PROJECTS_ACTION = 'unassignFromProjects';

@Component({
    selector: 'gtx-mesh-schema-table',
    templateUrl: './schema-table.component.html',
    styleUrls: ['./schema-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SchemaTableComponent extends BaseEntityTableComponent<Schema, MeshSchemaBO> {

    protected rawColumns: TableColumn<MeshSchemaBO>[] = [
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
    protected entityIdentifier = 'schema' as any;

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: SchemaTableLoaderService,
        modalService: ModalService,
        protected handler: SchemaHandlerService,
    ) {
        super(
            changeDetector,
            appState,
            i18n,
            loader,
            modalService,
        );
    }

    protected override createTableActionLoading(): Observable<TableAction<MeshSchemaBO>[]> {
        return this.actionRebuildTrigger$.pipe(
            map(() => {
                const actions: TableAction<MeshSchemaBO>[] = [
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
                        label: this.i18n.instant('mesh.assign_schemas_to_projects'),
                        enabled: (item) => item == null || item[BO_PERMISSIONS].includes(Permission.UPDATE),
                        type: 'secondary',
                        multiple: true,
                    },
                    {
                        id: UNASSIGN_FROM_PROJECTS_ACTION,
                        icon: 'link_off',
                        label: this.i18n.instant('mesh.unassign_schemas_from_projects'),
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
        this.openModal(SchemaPropertiesMode.CREATE);
    }

    public override handleAction(event: TableActionClickEvent<MeshSchemaBO>): void {
        const items = this.getEntitiesByIds(this.getAffectedEntityIds(event));

        switch (event.actionId) {
            case EDIT_ACTION:
                this.openModal(SchemaPropertiesMode.EDIT, event.item);
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

    // async manageProjectAssignment(schema: MeshSchemaBO): Promise<void> {
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

    async handleAssignToProjectsAction(schemas: MeshSchemaBO[]): Promise<void> {
        const dialog = await this.modalService.fromComponent(SelectProjectModal, {}, {
            title: 'mesh.assign_schemas_to_projects',
            multiple: true,
        });

        const projects: MeshProjectBO[] = await dialog.open();
        if (projects.length === 0) {
            return;
        }

        for (const project of projects) {
            for (const schema of schemas) {
                this.handler.assignToProject(project, schema);
            }
        }

        this.reload();
    }

    async handleUnassignFromProjectsAction(schemas: MeshSchemaBO[]): Promise<void> {
        const dialog = await this.modalService.fromComponent(SelectProjectModal, {}, {
            title: 'mesh.unassign_schemas_from_projects',
            multiple: true,
        });

        const projects: MeshProjectBO[] = await dialog.open();
        if (projects.length === 0) {
            return;
        }

        for (const project of projects) {
            for (const schema of schemas) {
                this.handler.unassignFromProject(project, schema);
            }
        }

        this.reload();
    }

    async openModal(mode: SchemaPropertiesMode, schema?: Schema): Promise<void> {
        const dialog = await this.modalService.fromComponent(SchemaModal, {}, {
            mode,
            schema,
        });
        const res = await dialog.open();
        if (res) {
            this.reload();
        }
    }
}

import {
    BO_PERMISSIONS,
    DELETE_ACTION,
    EDIT_ACTION,
    MANAGE_MICROSCHEMA_ASSIGNMENT_ACTION,
    MANAGE_SCHEMA_ASSIGNMENT_ACTION,
    MANAGE_TAGS_ACTIONS,
} from '@admin-ui/common';
import { I18nService } from '@admin-ui/core';
import { MeshMicroschemaBO, MeshProjectBO, MeshSchemaBO } from '@admin-ui/mesh/common';
import { MicroschemaHandlerService, SchemaHandlerService } from '@admin-ui/mesh/providers';
import { ProjectTableLoaderService } from '@admin-ui/mesh/providers/project-table-loader/project-table-loader.service';
import { BaseEntityTableComponent } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { Permission, Project } from '@gentics/mesh-models';
import { ModalService, TableAction, TableActionClickEvent, TableColumn } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ManageTagFamiliesModal } from '../manage-tag-families-modal/manage-tag-families-modal.component';
import { ProjectModal } from '../project-modal/project-modal.component';
import { ProjectPropertiesMode } from '../project-properties/project-properties.component';
import { SelectMicroschemaModal } from '../select-microschema-modal/select-microschema-modal.component';
import { SelectSchemaModal } from '../select-schema-modal/select-schema-modal.component';

@Component({
    selector: 'gtx-mesh-project-table',
    templateUrl: './project-table.component.html',
    styleUrls: ['./project-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectTableComponent extends BaseEntityTableComponent<Project, MeshProjectBO> {

    protected rawColumns: TableColumn<MeshProjectBO>[] = [
        {
            id: 'name',
            label: 'common.name',
            fieldPath: 'name',
            sortable: true,
        },
    ];
    protected entityIdentifier = 'project' as any;

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: ProjectTableLoaderService,
        modalService: ModalService,
        protected schemaHandler: SchemaHandlerService,
        protected microschemaHandler: MicroschemaHandlerService,
    ) {
        super(
            changeDetector,
            appState,
            i18n,
            loader,
            modalService,
        );
    }

    protected override createTableActionLoading(): Observable<TableAction<MeshProjectBO>[]> {
        return this.actionRebuildTrigger$.pipe(
            map(() => {
                const actions: TableAction<MeshProjectBO>[] = [
                    {
                        id: EDIT_ACTION,
                        icon: 'edit',
                        label: this.i18n.instant('common.edit'),
                        enabled: (item) => item[BO_PERMISSIONS].includes(Permission.UPDATE),
                        type: 'primary',
                        single: true,
                    },
                    {
                        id: MANAGE_TAGS_ACTIONS,
                        icon: 'local_offer',
                        label: this.i18n.instant('mesh.manage_tags'),
                        enabled: (item) => item[BO_PERMISSIONS].includes(Permission.UPDATE),
                        type: 'secondary',
                        single: true,
                    },
                    {
                        id: MANAGE_SCHEMA_ASSIGNMENT_ACTION,
                        icon: 'view_compact',
                        label: this.i18n.instant('mesh.manage_schema_assignment'),
                        enabled: (item) => item[BO_PERMISSIONS].includes(Permission.UPDATE),
                        type: 'secondary',
                        single: true,
                    },
                    {
                        id: MANAGE_MICROSCHEMA_ASSIGNMENT_ACTION,
                        icon: 'view_module',
                        label: this.i18n.instant('mesh.manage_microschema_assignment'),
                        enabled: (item) => item[BO_PERMISSIONS].includes(Permission.UPDATE),
                        type: 'secondary',
                        single: true,
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
        this.openModal(ProjectPropertiesMode.CREATE);
    }

    public override handleAction(event: TableActionClickEvent<MeshProjectBO>): void {
        switch (event.actionId) {
            case EDIT_ACTION:
                this.openModal(ProjectPropertiesMode.EDIT, event.item);
                return;

            case MANAGE_TAGS_ACTIONS:
                this.manageTagFamilies(event.item);
                return;

            case MANAGE_SCHEMA_ASSIGNMENT_ACTION:
                this.manageSchemaAssignment(event.item);
                return;

            case MANAGE_MICROSCHEMA_ASSIGNMENT_ACTION:
                this.manageMicroschemaAssignment(event.item);
                return;
        }

        super.handleAction(event);
    }

    async manageTagFamilies(project: MeshProjectBO): Promise<void> {
        const dialog = await this.modalService.fromComponent(ManageTagFamiliesModal, {
            closeOnEscape: false,
            closeOnOverlayClick: false,
        }, {
            project,
        });
        await dialog.open();
    }

    async manageSchemaAssignment(project: MeshProjectBO): Promise<void> {
        const assignedSchemas = ((await this.schemaHandler.listFromProject(project.name))?.data || []);
        const assignedSchemaIds = assignedSchemas.map(schema => schema.uuid);

        const dialog = await this.modalService.fromComponent(SelectSchemaModal, {}, {
            title: 'mesh.manage_schema_assignment',
            multiple: true,
            selected: assignedSchemaIds,
        });

        const schemas: MeshSchemaBO[] = await dialog.open();
        const newSchemaIds = schemas.map(schema => schema.uuid);

        const toAssign = schemas.filter(schema => !assignedSchemaIds.includes(schema.uuid));
        const toRemove = assignedSchemas.filter(schema => !newSchemaIds.includes(schema.uuid));

        // Nothing to do
        if (toAssign.length === 0 && toRemove.length === 0) {
            return;
        }

        for (const schema of toAssign) {
            this.schemaHandler.assignToProject(project, schema);
        }
        for (const schema of toRemove) {
            this.schemaHandler.unassignFromProject(project, schema);
        }

        this.reload();
    }

    async manageMicroschemaAssignment(project: MeshProjectBO): Promise<void> {
        const assignedMicroschemas = ((await this.microschemaHandler.listFromProject(project.name))?.data || []);
        const assignedMicroschemaIds = assignedMicroschemas.map(schema => schema.uuid);

        const dialog = await this.modalService.fromComponent(SelectMicroschemaModal, {}, {
            title: 'mesh.manage_microschema_assignment',
            multiple: true,
            selected: assignedMicroschemaIds,
        });

        const microschemas: MeshMicroschemaBO[] = await dialog.open();
        const newMicroschemaIds = microschemas.map(microschema => microschema.uuid);

        const toAssign = microschemas.filter(microschema => !assignedMicroschemaIds.includes(microschema.uuid));
        const toRemove = assignedMicroschemas.filter(microschema => !newMicroschemaIds.includes(microschema.uuid));

        // Nothing to do
        if (toAssign.length === 0 && toRemove.length === 0) {
            return;
        }

        for (const microschema of toAssign) {
            this.microschemaHandler.assignToProject(project, microschema);
        }
        for (const microschema of toRemove) {
            this.microschemaHandler.unassignFromProject(project, microschema);
        }

        this.reload();
    }

    async openModal(mode: ProjectPropertiesMode, project?: Project): Promise<void> {
        const dialog = await this.modalService.fromComponent(ProjectModal, {}, {
            mode,
            project,
        });
        const res = await dialog.open();
        if (res) {
            this.reload();
        }
    }
}

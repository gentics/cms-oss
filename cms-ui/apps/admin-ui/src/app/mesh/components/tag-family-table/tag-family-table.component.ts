import { BO_PERMISSIONS } from '@admin-ui/common';
import { I18nService } from '@admin-ui/core';
import { MeshTagBO, MeshTagFamilyBO } from '@admin-ui/mesh/common';
import { TagFamilyTableLoaderOptions, TagFamilyTableLoaderService, TagHandlerService } from '@admin-ui/mesh/providers';
import { BaseEntityTableComponent, DELETE_ACTION } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input } from '@angular/core';
import { Permission, TagFamily, TagResponse } from '@gentics/mesh-models';
import { ModalService, TableAction, TableActionClickEvent, TableColumn } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { TagFamilyModal } from '../tag-family-modal/tag-family-modal.component';
import { TagFamilyPropertiesMode } from '../tag-family-properties/tag-family-properties.component';
import { TagModal } from '../tag-modal/tag-modal.component';
import { TagPropertiesMode } from '../tag-properties/tag-properties.component';

const EDIT_ACTION = 'edit';

@Component({
    selector: 'gtx-mesh-tag-family-table',
    templateUrl: './tag-family-table.component.html',
    styleUrls: ['./tag-family-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TagFamilyTableComponent extends BaseEntityTableComponent<TagFamily, MeshTagFamilyBO, TagFamilyTableLoaderOptions> {

    public readonly Permission = Permission;
    public readonly BO_PERMISSIONS = BO_PERMISSIONS;

    @Input()
    public project: string;

    @Input()
    public manageTags = true;

    protected rawColumns: TableColumn<MeshTagFamilyBO>[] = [
        {
            id: 'name',
            label: 'common.name',
            fieldPath: 'name',
            sortable: true,
        },
        {
            id: 'tags',
            label: 'mesh.tags',
            clickable: false,
        },
    ];
    protected entityIdentifier = 'tag-family' as any;

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: TagFamilyTableLoaderService,
        modalService: ModalService,
        protected tagHandler: TagHandlerService,
    ) {
        super(
            changeDetector,
            appState,
            i18n,
            loader,
            modalService,
        );
    }

    protected override createAdditionalLoadOptions(): TagFamilyTableLoaderOptions {
        return {
            project: this.project,
        };
    }

    protected override createTableActionLoading(): Observable<TableAction<MeshTagFamilyBO>[]> {
        return this.actionRebuildTrigger$.pipe(
            map(() => {
                const actions: TableAction<MeshTagFamilyBO>[] = [
                    {
                        id: EDIT_ACTION,
                        icon: 'edit',
                        label: this.i18n.instant('common.edit'),
                        enabled: (item) => item[BO_PERMISSIONS].includes(Permission.UPDATE),
                        type: 'primary',
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
        this.openModal(TagFamilyPropertiesMode.CREATE);
    }

    public override handleAction(event: TableActionClickEvent<MeshTagFamilyBO>): void {
        switch (event.actionId) {
            case EDIT_ACTION:
                this.openModal(TagFamilyPropertiesMode.EDIT, event.item);
                return;
        }

        super.handleAction(event);
    }

    public async createNewTag(family: MeshTagBO): Promise<void> {
        const dialog = await this.modalService.fromComponent(TagModal, {}, {
            mode: TagPropertiesMode.CREATE,
            project: this.project,
            family: family.uuid,
        });
        const res = await dialog.open();
        if (res) {
            this.reload();
        }
    }

    public async editTag(family: MeshTagFamilyBO, tag: TagResponse): Promise<void> {
        const dialog = await this.modalService.fromComponent(TagModal, {}, {
            mode: TagPropertiesMode.EDIT,
            project: this.project,
            family: family.uuid,
            tag,
        });
        const res = await dialog.open();
        if (res) {
            this.reload();
        }
    }

    public async deleteTag(family: MeshTagFamilyBO, tag: TagResponse): Promise<void> {
        const dialog = await this.modalService.dialog({
            title: this.i18n.instant('mesh.delete_tag', { entityName: tag.name }),
            body: this.i18n.instant('mesh.delete_tag_warning'),
            buttons: [
                {
                    label: this.i18n.instant('shared.confirm_button'),
                    type: 'alert',
                    returnValue: true,
                },
                {
                    label: this.i18n.instant('common.cancel_button'),
                    type: 'secondary',
                    returnValue: false,
                },
            ],
        });
        const doDelete = await dialog.open();

        if (doDelete) {
            await this.tagHandler.delete(this.project, family.uuid, tag.uuid);
        }
    }

    async openModal(mode: TagFamilyPropertiesMode, family?: TagFamily): Promise<void> {
        const dialog = await this.modalService.fromComponent(TagFamilyModal, {}, {
            mode,
            project: this.project,
            family,
        });
        const res = await dialog.open();
        if (res) {
            this.reload();
        }
    }
}

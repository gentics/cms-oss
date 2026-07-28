import { BO_PERMISSIONS } from '@admin-ui/common';
import { MeshTagBO } from '@admin-ui/mesh/common';
import { TagHandlerService, TagTableLoaderOptions, TagTableLoaderService } from '@admin-ui/mesh/providers';
import { BaseEntityTableComponent, DELETE_ACTION } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input } from '@angular/core';
import { Permission, Tag, TagFamilyReference } from '@gentics/mesh-models';
import { ModalService, TableAction, TableActionClickEvent, TableColumn } from '@gentics/ui-core';
import { I18nService } from '@gentics/cms-components';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { TagModal } from '../tag-modal/tag-modal.component';
import { TagPropertiesMode } from '../tag-properties/tag-properties.component';

const EDIT_ACTION = 'edit';

@Component({
    selector: 'gtx-mesh-tag-table',
    templateUrl: './tag-table.component.html',
    styleUrls: ['./tag-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class TagTableComponent extends BaseEntityTableComponent<Tag, MeshTagBO, TagTableLoaderOptions> {

    @Input()
    public project: string;

    @Input()
    public family: TagFamilyReference;

    protected rawColumns: TableColumn<MeshTagBO>[] = [
        {
            id: 'name',
            label: 'common.name',
            fieldPath: 'name',
            sortable: true,
        },
    ];

    protected entityIdentifier = 'tag' as any;

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: TagTableLoaderService,
        modalService: ModalService,
        protected handler: TagHandlerService,
    ) {
        super(
            changeDetector,
            appState,
            i18n,
            loader,
            modalService,
        );
    }

    protected override createAdditionalLoadOptions(): TagTableLoaderOptions {
        return {
            project: this.project,
            family: this.family,
        };
    }

    protected override createTableActionLoading(): Observable<TableAction<MeshTagBO>[]> {
        return this.actionRebuildTrigger$.pipe(
            map(() => {
                const actions: TableAction<MeshTagBO>[] = [
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
        this.openModal(TagPropertiesMode.CREATE);
    }

    public override handleAction(event: TableActionClickEvent<MeshTagBO>): void {
        switch (event.actionId) {
            case EDIT_ACTION:
                this.openModal(TagPropertiesMode.EDIT, event.item);
                return;
        }

        super.handleAction(event);
    }

    async openModal(mode: TagPropertiesMode, tag?: Tag): Promise<void> {
        const dialog = await this.modalService.fromComponent(TagModal, {}, {
            mode,
            project: this.project,
            family: this.family.uuid,
            tag,
        });
        const res = await dialog.open();
        if (res) {
            this.reload();
        }
    }
}

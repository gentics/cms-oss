import { BusinessObject } from '@admin-ui/common';
import { BaseTableLoaderService, DevToolPackageTableLoaderService, I18nService, PackageOperations } from '@admin-ui/core';
import { ContextMenuService } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectorRef, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { ModalService, TableActionClickEvent } from '@gentics/ui-core';
import { BaseEntityTableComponent } from '../base-entity-table/base-entity-table.component';

export const UNASSIGN_FROM_PACKAGE_ACTION = 'unassignFromPackage';

@Component({ template: '' })
export abstract class BasePackageEntityTableComponent<T, O = T & BusinessObject, A = never>
    extends BaseEntityTableComponent<T, O, A>
    implements OnChanges {

    @Input()
    public packageName: string;

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: BaseTableLoaderService<T, O, A>,
        modalService: ModalService,
        protected contextMenu: ContextMenuService,
        protected packageOperations: PackageOperations,
        protected packageTableLoader: DevToolPackageTableLoaderService,
    ) {
        super(
            changeDetector,
            appState,
            i18n,
            loader,
            modalService,
        );
    }

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.packageName) {
            this.loadTrigger.next();
            this.actionRebuildTrigger.next();
        }
    }

    public handleAssignToPackageButton(): void {
        this.contextMenu.assignEntityToPackageModalOpen(this.packageName, this.entityIdentifier).then(() => {
            this.loader.reload();
            // Also update the package table, as the count has updated
            this.packageTableLoader.reload();
        });
    }

    public override handleAction(event: TableActionClickEvent<O>): void {
        switch (event.actionId) {
            case UNASSIGN_FROM_PACKAGE_ACTION:
                this.unassignFromPackage(this.getAffectedEntityIds(event));
                return;
        }

        super.handleAction(event);
    }

    protected async unassignFromPackage(ids: string[]): Promise<void> {
        await this.packageOperations.removeEntitiesFromPackage(this.packageName, this.entityIdentifier as any, ids).toPromise();
        this.removeFromSelection(ids);
        this.loader.reload();
        // Also update the package table, as the count has updated
        this.packageTableLoader.reload();
    }
}

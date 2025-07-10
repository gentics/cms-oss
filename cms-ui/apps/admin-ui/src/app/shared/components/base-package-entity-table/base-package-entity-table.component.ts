import { BusinessObject, PackageTableEntityLoader } from '@admin-ui/common';
import { DevToolPackageTableLoaderService, I18nService } from '@admin-ui/core/providers';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectorRef, Component, Input, OnChanges } from '@angular/core';
import { ChangesOf, ModalService, TableActionClickEvent } from '@gentics/ui-core';
import { ContextMenuService } from '../../providers/context-menu/context-menu.service';
import { BaseEntityTableComponent } from '../base-entity-table/base-entity-table.component';

export const UNASSIGN_FROM_PACKAGE_ACTION = 'unassignFromPackage';

@Component({
    template: '',
    standalone: false
})
export abstract class BasePackageEntityTableComponent<T, O = T & BusinessObject, A = never>
    extends BaseEntityTableComponent<T, O, A>
    implements OnChanges {

    @Input()
    public packageName: string;

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: BasePackageEntityTableComponent<T, O, A>,
        modalService: ModalService,
        protected contextMenu: ContextMenuService,
        protected packageTableLoader: DevToolPackageTableLoaderService,
    ) {
        super(
            changeDetector,
            appState,
            i18n,
            loader as any,
            modalService,
        );
    }

    public override ngOnChanges(changes: ChangesOf<this>): void {
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
        for (const id of ids) {
            await (this.loader as any as PackageTableEntityLoader<O, A>).removeFromDevToolPackage(this.packageName, id).toPromise();
            this.removeFromSelection(id);
        }

        this.loader.reload();
        // Also update the package table, as the count has updated
        this.packageTableLoader.reload();
    }
}

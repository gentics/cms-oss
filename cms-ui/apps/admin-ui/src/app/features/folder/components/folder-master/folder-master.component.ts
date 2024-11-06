import { FolderBO, ROUTE_ENTITY_LOADED, ROUTE_ENTITY_RESOLVER_KEY } from '@admin-ui/common';
import { BaseTableMasterComponent } from '@admin-ui/shared/components/base-table-master/base-table-master.component';
import { FocusEditor } from '@admin-ui/state';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { NavigationExtras } from '@angular/router';
import { Folder, NormalizableEntityType } from '@gentics/cms-models';
import { TableRow, getFullPrimaryPath } from '@gentics/ui-core';

@Component({
    selector: 'gtx-folder-master',
    templateUrl: './folder-master.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FolderMasterComponent extends BaseTableMasterComponent<Folder, FolderBO> {

    protected entityIdentifier: NormalizableEntityType = 'folder';

    protected override async navigateToEntityDetails(row: TableRow<FolderBO>): Promise<void> {
        const fullUrl = getFullPrimaryPath(this.route);
        const commands: any[] = [
            fullUrl,
            { outlets: { detail: [this.detailPath || this.entityIdentifier, row.item.id] } },
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
}

import { BO_PERMISSIONS } from '@admin-ui/common';
import { GcmsPermission } from '@gentics/cms-models';
import { TableAction } from '@gentics/ui-core';
import { firstValueFrom, of } from 'rxjs';
import { DELETE_ACTION } from '../base-entity-table/base-entity-table.component';
import { PortalImplementationTableComponent } from './portal-implementation-table.component';

describe('PortalImplementationTableComponent', () => {
    function createComponent(): PortalImplementationTableComponent {
        const permissions = {
            getUserActionPermsForId: () => ({ typePermissions: [] }),
            checkPermissions: () => of(true),
        };
        const i18n = { instant: (key: string) => key };

        return new PortalImplementationTableComponent(
            { markForCheck: () => {} } as any,
            null,
            i18n as any,
            null,
            null,
            null,
            null,
            permissions as any,
        );
    }

    async function getActions(component: PortalImplementationTableComponent): Promise<TableAction<any>[]> {
        const actions = firstValueFrom((component as any).createTableActionLoading());
        (component as any).actionRebuildTrigger.next();
        return actions as Promise<TableAction<any>[]>;
    }

    function isEnabled(action: TableAction<any>, item: any): boolean {
        return typeof action.enabled === 'function' ? action.enabled(item) : action.enabled;
    }

    it('enables the delete action only for portal implementations with delete permission', async () => {
        const deleteAction = (await getActions(createComponent())).find(action => action.id === DELETE_ACTION);

        expect(deleteAction).toBeDefined();
        expect(isEnabled(deleteAction, { [BO_PERMISSIONS]: [GcmsPermission.VIEW, GcmsPermission.DELETE] })).toBe(true);
        expect(isEnabled(deleteAction, { [BO_PERMISSIONS]: [GcmsPermission.VIEW, GcmsPermission.EDIT] })).toBe(false);
    });
});

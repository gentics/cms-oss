import { BO_PERMISSIONS } from '@admin-ui/common';
import { GcmsPermission } from '@gentics/cms-models';
import { firstValueFrom, of } from 'rxjs';
import { PortalImplementationTableLoaderService } from './portal-implementation-table-loader.service';

describe('PortalImplementationTableLoaderService', () => {
    it('loads the portal implementations with their permissions', async () => {
        const response = {
            items: [
                { id: 1, keyword: 'one', name: 'One' },
                { id: 2, keyword: 'two', name: 'Two' },
            ],
            numItems: 2,
            perms: {
                1: [GcmsPermission.VIEW, GcmsPermission.DELETE],
                2: [GcmsPermission.VIEW],
            },
        };
        const api = {
            portalImplementations: {
                getPortalImplementations: jasmine.createSpy('getPortalImplementations').and.returnValue(of(response)),
            },
        };
        const entityManager = { addEntities: jasmine.createSpy('addEntities') };
        const loader = new PortalImplementationTableLoaderService(entityManager as any, null, api as any, null, null);

        const page = await firstValueFrom(loader.loadTablePage({ page: 1, perPage: 10 }));

        expect(api.portalImplementations.getPortalImplementations).toHaveBeenCalledWith(jasmine.objectContaining({ perms: true }));
        expect(page.rows.map(row => row.item[BO_PERMISSIONS])).toEqual([
            [GcmsPermission.VIEW, GcmsPermission.DELETE],
            [GcmsPermission.VIEW],
        ]);
    });
});

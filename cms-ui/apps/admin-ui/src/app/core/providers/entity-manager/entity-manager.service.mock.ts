import { InterfaceOf } from '@admin-ui/common';
import { EntityManagerService } from './entity-manager.service';

/**
 * Mocks the most commonly used parts of the `EntityManager` service.
 */
export class MockEntityManagerService implements InterfaceOf<Omit<EntityManagerService, 'ngOnDestroy'>> {
    addEntities = jasmine.createSpy('addEntities');
    addEntity = jasmine.createSpy('addEntity');
    deleteEntities = jasmine.createSpy('deleteEntities');
    getEntity = jasmine.createSpy('getEntity');
    denormalizeEntity = jasmine.createSpy('denormalizeEntity');
    init = jasmine.createSpy('init');
    watchDenormalizedEntitiesList = jasmine.createSpy('watchDenormalizedEntitiesList');
    watchNormalizedEntitiesList = jasmine.createSpy('watchNormalizedEntitiesList');
    deleteAllEntitiesInBranch = jasmine.createSpy('deleteAllEntitiesInBranch');
}

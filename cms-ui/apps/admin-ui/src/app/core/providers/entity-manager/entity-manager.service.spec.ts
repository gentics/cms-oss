import { Injectable } from '@angular/core';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import {
    GcmsNormalizer,
    IS_NORMALIZED,
    NormalizableEntity,
    NormalizableEntityType,
    NormalizableEntityTypesMapBO,
    Normalized,
    NormalizedEntityStore,
    Page,
    Raw,
    User,
} from '@gentics/cms-models';
import {
    getExampleEntityStore,
    getExampleFolderData,
    getExampleFolderDataNormalized,
    getExamplePageData,
    getExamplePageDataNormalized,
} from '@gentics/cms-models/testing';
import { deepFreeze } from '@gentics/ui-core/utils/deep-freeze/deep-freeze';
import { cloneDeep } from 'lodash-es';
import { Observable } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ObservableStopper } from '../../../common/utils/observable-stopper/observable-stopper';
import { AddEntities, UpdateEntities } from '../../../state/entity/entity.actions';
import { EntityStateModel, INITIAL_ENTITY_STATE } from '../../../state/entity/entity.state';
import { AppStateService } from '../../../state/providers/app-state/app-state.service';
import { TEST_APP_STATE, TestAppState, assembleTestAppStateImports } from '../../../state/utils/test-app-state/test-app-state.mock';
import { EntityManagerService } from './entity-manager.service';

interface NormalizationWorkerRequest<T extends NormalizableEntityType, E extends NormalizableEntityTypesMapBO<Raw>[T]> {
    id: number;
    entityType: T;
    rawEntities: E[];
}

@Injectable()
class EntityManagerWithAccessibleInternals extends EntityManagerService {
    normalizer: GcmsNormalizer;
    createDenormalizedCacheSpy;

    constructor(appState: AppStateService) {
        super(appState);
        this.createDenormalizedCacheSpy = spyOn(this, 'createDenormalizedCache' as any).and.callThrough();
    }

    readonly CLEANUP_DEBOUNCE: number;

    createDenormalizedCache(): any {
        return super.createDenormalizedCache();
    }
}

function isObservable<T>(value: any): value is Observable<T> {
    return value != null
      && typeof value === 'object'
      && typeof value.subscribe === 'function';
}

interface EntityNormalizationPair<T extends NormalizableEntity<Raw>> {
    raw: T[];
    normalized: Partial<NormalizedEntityStore>;
}

interface EntityIds {
    pages: number[];
    users: number[];
    folders: number[];
}

const PAGE_A_ID = 3;
const PAGE_B_ID = 37;
const NEW_PAGE_ID = 4321;
const USER_A_ID = 1;
const NEW_USER_ID = 2;
const FILE_A_ID = 10;
const NEW_FOLDER_ID = 4321;

describe('EntityManagerService', () => {

    const MOCK_ENTITIES = getExampleEntityStore();
    deepFreeze(MOCK_ENTITIES);

    let appState: TestAppState;
    let entityManager: EntityManagerWithAccessibleInternals;
    let stopper: ObservableStopper;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                assembleTestAppStateImports(),
            ],
            providers: [
                { provide: EntityManagerService, useClass: EntityManagerWithAccessibleInternals },
                TEST_APP_STATE,
            ],
        });

        stopper = new ObservableStopper();
        appState = TestBed.inject(AppStateService) as TestAppState;
        entityManager = TestBed.inject(EntityManagerService) as EntityManagerWithAccessibleInternals;
    });

    afterEach(() => {
        stopper.stop();

        // For some reason the services are not automatically destroyed after the tests,
        // so we need to do it manually to make sure that we do not leak web worker threads.
        entityManager.ngOnDestroy();
        entityManager = null;
    });

    describe('EntityManagerService', () => {
        beforeEach(() => {
            entityManager.init();
        });

        it('test data is set up correctly', () => {
            // Make sure that the entity IDs used for testing exist or don't exist in the test data as expected.
            // This guards us against problems from changes to getExampleEntityStore().

            expect(MOCK_ENTITIES.page[PAGE_A_ID]).toBeTruthy();
            expect(MOCK_ENTITIES.page[PAGE_B_ID]).toBeTruthy();
            expect(MOCK_ENTITIES.page[NEW_PAGE_ID]).toBeUndefined();

            expect(MOCK_ENTITIES.user[USER_A_ID]).toBeTruthy();
            expect(MOCK_ENTITIES.user[NEW_USER_ID]).toBeUndefined();

            expect(MOCK_ENTITIES.file[FILE_A_ID]).toBeTruthy();

            expect(MOCK_ENTITIES.folder[NEW_FOLDER_ID]).toBeUndefined();
        });

        describe('getEntity() returns an observable, which', () => {

            beforeEach(() => {
                appState.mockState({
                    entity: getExampleEntityStore(),
                });
            });

            it('emits the correct page entity', () => {
                let emissionCount = 0;
                let page: Page<Normalized>;

                const result$ = entityManager.getEntity('page', PAGE_A_ID);
                expect(isObservable(result$)).toBe(true, 'getEntity() did not return an Observable');

                result$.pipe(
                    takeUntil(stopper.stopper$),
                ).subscribe((entity) => {
                    ++emissionCount;
                    page = entity;
                });

                expect(emissionCount).toBe(1);
                expect(page).toEqual(MOCK_ENTITIES.page[PAGE_A_ID]);
            });

            it('emits the correct user entity', () => {
                let emissionCount = 0;
                let user: User<Normalized>;

                const result$ = entityManager.getEntity('user', USER_A_ID);
                result$.pipe(
                    takeUntil(stopper.stopper$),
                ).subscribe((entity) => {
                    ++emissionCount;
                    user = entity;
                });

                expect(emissionCount).toBe(1);
                expect(user).toEqual(MOCK_ENTITIES.user[USER_A_ID]);
            });

            it('emits when the entity is changed', () => {
                let emissionCount = 0;
                let page: Page<Normalized>;

                const page$ = entityManager.getEntity('page', PAGE_A_ID);
                page$.pipe(
                    takeUntil(stopper.stopper$),
                ).subscribe((entity) => {
                    ++emissionCount;
                    page = entity;
                });

                expect(emissionCount).toBe(1);
                expect(page).toEqual(MOCK_ENTITIES.page[PAGE_A_ID]);

                const newName = 'This is a new name';
                appState.dispatch(new UpdateEntities({
                    page: {
                        [PAGE_A_ID]: {
                            name: newName,
                        },
                    },
                }));

                expect(emissionCount).toBe(2, 'Observable did not emit when the entity was changed.');
                expect(page).toEqual({
                    ...MOCK_ENTITIES.page[PAGE_A_ID],
                    name: newName,
                });
            });

            it('emits undefined if no entity with that ID exists and emits again when the entity is added', () => {
                let emissionCount = 0;
                let page: Page<Normalized>;

                const page$ = entityManager.getEntity('page', NEW_PAGE_ID);
                page$.pipe(
                    takeUntil(stopper.stopper$),
                ).subscribe((entity) => {
                    ++emissionCount;
                    page = entity;
                });

                expect(emissionCount).toBe(1);
                expect(page).toBe(undefined);

                const newPage = getExamplePageDataNormalized({ id: NEW_PAGE_ID });
                appState.dispatch(new AddEntities({
                    page: {
                        [NEW_PAGE_ID]: newPage,
                    },
                }));

                expect(emissionCount).toBe(2, 'Observable did not emit when the entity was added.');
                expect(page).toEqual(newPage);
            });

            it('does not emit when another entity of the same type is changed', () => {
                let emissionCount = 0;

                const page$ = entityManager.getEntity('page', PAGE_A_ID);
                page$.pipe(
                    takeUntil(stopper.stopper$),
                ).subscribe(() => ++emissionCount);

                expect(emissionCount).toBe(1);

                const newName = 'This is a new name';
                appState.dispatch(new UpdateEntities({
                    page: {
                        [PAGE_B_ID]: {
                            name: newName,
                        },
                    },
                }));

                expect(emissionCount).toBe(1, 'Observable emitted when another entity of the same type was changed.');
            });

            it('does not emit when an entity of the same type is added', () => {
                let emissionCount = 0;

                const page$ = entityManager.getEntity('page', PAGE_A_ID);
                page$.pipe(
                    takeUntil(stopper.stopper$),
                ).subscribe(() => ++emissionCount);

                expect(emissionCount).toBe(1);

                const newPage = getExamplePageDataNormalized({ id: NEW_PAGE_ID });
                appState.dispatch(new AddEntities({
                    page: {
                        [NEW_PAGE_ID]: newPage,
                    },
                }));

                expect(emissionCount).toBe(1, 'Observable emitted when an entity of the same type was added.');
            });

            it('does not emit when an entity of another type is changed', () => {
                let emissionCount = 0;

                const page$ = entityManager.getEntity('page', PAGE_A_ID);
                page$.pipe(
                    takeUntil(stopper.stopper$),
                ).subscribe(() => ++emissionCount);

                expect(emissionCount).toBe(1);

                const newName = 'This is a new name';
                appState.dispatch(new UpdateEntities({
                    file: {
                        [FILE_A_ID]: {
                            name: newName,
                        },
                    },
                }));

                expect(emissionCount).toBe(1, 'Observable emitted when an entity of a differenty type was changed.');
            });

            it('does not emit when an entity of a different type is added', () => {
                let emissionCount = 0;

                const page$ = entityManager.getEntity('page', PAGE_A_ID);
                page$.pipe(
                    takeUntil(stopper.stopper$),
                ).subscribe(() => ++emissionCount);

                expect(emissionCount).toBe(1);

                const newFolder = getExampleFolderDataNormalized({ id: NEW_FOLDER_ID });
                appState.dispatch(new AddEntities({
                    folder: {
                        [NEW_FOLDER_ID]: newFolder,
                    },
                }));

                expect(emissionCount).toBe(1, 'Observable emitted when an entity of a different type was added.');
            });

            it('throws an error if an invalid entity type is specified', () => {
                expect(() => entityManager.getEntity('invalid' as any, 10))
                    .toThrow(jasmine.objectContaining({ message: 'The EntityState branch "invalid" does not exist.' }));
            });

        });
    });

    describe('denormalizeEntity()', () => {

        let normalizer: GcmsNormalizer;
        let denormalizeSpy: jasmine.Spy;

        beforeEach(() => {
            entityManager.init();
            normalizer = entityManager.normalizer;
            denormalizeSpy = spyOn(normalizer, 'denormalize').and.callThrough();

            appState.mockState({
                entity: getExampleEntityStore(),
            });
        });

        it('uses GcmsNormalizer to denormalize an entity', () => {
            const page: Page<Normalized> = appState.now.entity.page[PAGE_A_ID] as any;
            const result = entityManager.denormalizeEntity('page', page);

            expect(result).toBeTruthy();
            expect(denormalizeSpy).toHaveBeenCalledTimes(1);
            expect(denormalizeSpy).toHaveBeenCalledWith('page', page, appState.now.entity);
            expect(result).toBe(denormalizeSpy.calls.all()[0].returnValue);
        });

    });

    describe('watchDenormalizedEntitiesList()', () => {

        const ENTITIES_COUNT = 100;
        let cleanupDebounceTime: number;
        let normalizer: GcmsNormalizer;
        let denormalizeEntitySpy: jasmine.Spy;
        let expectedDenormalizedPages: Page<Raw>[];

        function generateTestPages(count: number): EntityNormalizationPair<Page<Raw>> {
            const rawPages: Page<Raw>[] = new Array(count);
            for (let i = 0; i < count; ++i) {
                rawPages[i] = getExamplePageData({ id: i + 1 });
            }
            const normalized = normalizer.normalize('page', rawPages).entities;
            return {
                raw: rawPages,
                normalized,
            };
        }

        function getDenormalizedPages(normalizedEntities: Partial<NormalizedEntityStore>): Page<Raw>[] {
            return Object.keys(normalizedEntities.page).map(
                (key) => normalizer.denormalize('page', normalizedEntities.page[key], normalizedEntities),
            );
        }

        beforeEach(() => {
            normalizer = new GcmsNormalizer();
            entityManager.init();
            denormalizeEntitySpy = spyOn(entityManager, 'denormalizeEntity').and.callThrough();
            cleanupDebounceTime = (entityManager).CLEANUP_DEBOUNCE + 1;

            const entities = generateTestPages(ENTITIES_COUNT);
            appState.mockState({
                entity: entities.normalized,
            });
            expectedDenormalizedPages = getDenormalizedPages(appState.now.entity);
        });

        it('denormalizes all entities in the state branch upon subscription', fakeAsync(() => {
            let emissionCount = 0;
            let actualResult: Page<Raw>[] = [];

            entityManager.watchDenormalizedEntitiesList('page').pipe(
                takeUntil(stopper.stopper$),
            ).subscribe((result) => {
                ++emissionCount;
                actualResult = result;
            });

            expect(emissionCount).toBe(1);
            expect(denormalizeEntitySpy).toHaveBeenCalledTimes(ENTITIES_COUNT);
            Object.keys(appState.now.entity.page).forEach((key, index) => {
                const callArgs = denormalizeEntitySpy.calls.argsFor(index);
                expect(callArgs[0]).toEqual('page');
                expect(callArgs[1]).toBe(appState.now.entity.page[key]);
            });

            expect(actualResult).toEqual(expectedDenormalizedPages);

            tick(cleanupDebounceTime);
        }));

        it('denormalizes only changed entities upon a change in the state branch and emits all denormalized entities', fakeAsync(() => {
            let emissionCount = 0;
            let actualResult: Page<Raw>[] = [];

            entityManager.watchDenormalizedEntitiesList('page').pipe(
                takeUntil(stopper.stopper$),
            ).subscribe((result) => {
                ++emissionCount;
                actualResult = result;
            });

            expect(emissionCount).toBe(1);
            expect(actualResult).toEqual(expectedDenormalizedPages);
            const firstResult = [...actualResult];
            denormalizeEntitySpy.calls.reset();

            const changedEntity = cloneDeep(actualResult[0]);
            changedEntity.name = changedEntity.name + ' changed';
            delete (changedEntity as any).languageVariants;
            entityManager.addEntity('page', changedEntity);

            // Get a denormalized version of the changed entity with the language variants.
            const expectedChangedEntityComplete = normalizer.denormalize('page', appState.now.entity.page[changedEntity.id], appState.now.entity);
            expect(expectedChangedEntityComplete.name).toEqual(changedEntity.name);

            // Only the changed entity should have a different reference, the others should be the same.
            expect(emissionCount).toBe(2);
            expect(denormalizeEntitySpy.calls.count()).toBe(1);
            expect(denormalizeEntitySpy).toHaveBeenCalledWith('page', appState.now.entity.page[changedEntity.id]);
            expect(actualResult[0]).not.toBe(firstResult[0]);
            expect(actualResult[0]).toEqual(expectedChangedEntityComplete);
            for (let i = 1; i < firstResult.length; ++i) {
                expect(actualResult[i]).toBe(firstResult[i]);
            }

            tick(cleanupDebounceTime);
        }));

        it('denormalizes only the new entities upon addition to the state branch and emits all denormalized entities', fakeAsync(() => {
            let emissionCount = 0;
            let actualResult: Page<Raw>[] = [];

            entityManager.watchDenormalizedEntitiesList('page').pipe(
                takeUntil(stopper.stopper$),
            ).subscribe((result) => {
                ++emissionCount;
                actualResult = result;
            });

            expect(emissionCount).toBe(1);
            expect(actualResult).toEqual(expectedDenormalizedPages);
            const firstResult = [...actualResult];
            denormalizeEntitySpy.calls.reset();

            const firstNewEntityId = actualResult.length + 1;
            const secondNewEntityId = actualResult.length + 2;
            const newEntities = [
                getExamplePageData({ id: firstNewEntityId, idVariant1: firstNewEntityId }),
                getExamplePageData({ id: secondNewEntityId, idVariant1: secondNewEntityId }),
            ];
            entityManager.addEntities('page', newEntities);
            tick();

            // Get the denormalized versions of the new entities.
            const newEntitiesDenormalized = newEntities
                .map((entity) => normalizer.denormalize('page', appState.now.entity.page[entity.id], appState.now.entity));

            // Only the new entities should have been denormalized, the others should have the same references as before.
            expect(emissionCount).toBe(2);
            expect(denormalizeEntitySpy.calls.count()).toBe(2);
            expect(actualResult.length).toBe(ENTITIES_COUNT + 2);
            firstResult.forEach(
                (expectedPage, index) => expect(actualResult[index]).toBe(expectedPage),
            );
            expect(actualResult[firstResult.length]).toEqual(newEntitiesDenormalized[0]);
            expect(actualResult[firstResult.length + 1]).toEqual(newEntitiesDenormalized[1]);

            tick(cleanupDebounceTime);
        }));

        it('does not denormalize entities again if a second observer subscribes', fakeAsync(() => {
            let emissionCountSubA = 0;
            let emissionCountSubB = 0;
            let actualResultSubA: Page<Raw>[];
            let actualResultSubB: Page<Raw>[];

            // Subscription A
            entityManager.watchDenormalizedEntitiesList('page').pipe(
                takeUntil(stopper.stopper$),
            ).subscribe((result) => {
                ++emissionCountSubA;
                actualResultSubA = result;
            });

            expect(emissionCountSubA).toBe(1);
            expect(actualResultSubA).toEqual(expectedDenormalizedPages);
            denormalizeEntitySpy.calls.reset();

            // Subscription B
            entityManager.watchDenormalizedEntitiesList('page').pipe(
                takeUntil(stopper.stopper$),
            ).subscribe((result) => {
                ++emissionCountSubB;
                actualResultSubB = result;
            });

            expect(emissionCountSubA).toBe(1);
            expect(emissionCountSubB).toBe(1);
            expect(denormalizeEntitySpy.calls.count()).toBe(0);
            actualResultSubA.forEach(
                (expectedPageFromA, index) => expect(actualResultSubB[index]).toBe(expectedPageFromA),
            );

            tick(cleanupDebounceTime);
        }));

        it('emits if an entity is removed', fakeAsync(() => {
            let emissionCount = 0;
            let actualResult: Page<Raw>[] = [];

            entityManager.watchDenormalizedEntitiesList('page').pipe(
                takeUntil(stopper.stopper$),
            ).subscribe((result) => {
                ++emissionCount;
                actualResult = result;
            });

            expect(emissionCount).toBe(1);
            expect(actualResult).toEqual(expectedDenormalizedPages);
            denormalizeEntitySpy.calls.reset();

            // Remove the first and the last page.
            const expectedSecondResult = actualResult.slice(1, actualResult.length - 1);
            entityManager.deleteEntities('page', [actualResult[0].id, actualResult[actualResult.length - 1].id]);

            expect(emissionCount).toBe(2);
            expect(denormalizeEntitySpy.calls.count()).toBe(0);
            expect(actualResult.length).toBe(expectedSecondResult.length);
            expectedSecondResult.forEach(
                (expectedPage, index) => expect(actualResult[index]).toBe(expectedPage),
            );

            tick(cleanupDebounceTime);
        }));

        it('continues to emit if one subscriber unsubscribes', fakeAsync(() => {
            let emissionCountSubA = 0;
            let emissionCountSubB = 0;

            // Subscription A
            const subA = entityManager.watchDenormalizedEntitiesList('page').pipe(
                takeUntil(stopper.stopper$),
            ).subscribe((result) => ++emissionCountSubA);
            expect(emissionCountSubA).toBe(1);

            // Subscription B
            entityManager.watchDenormalizedEntitiesList('page').pipe(
                takeUntil(stopper.stopper$),
            ).subscribe((result) => ++emissionCountSubB);
            expect(emissionCountSubB).toBe(1);

            subA.unsubscribe();
            entityManager.deleteEntities('page', [expectedDenormalizedPages[0].id]);

            // Subscription B should continue to emit, but subscription A should not.
            expect(emissionCountSubB).toBe(2);
            expect(emissionCountSubA).toBe(1);

            tick(cleanupDebounceTime);
        }));

        it('clears the cache after the last observer has unsubscribed and denormalized all entities again if there is a new observer', fakeAsync(() => {
            let emissionCountSubB = 0;
            let actualResultSubA: Page<Raw>[];
            let actualResultSubB: Page<Raw>[];

            // Subscription A
            const subA = entityManager.watchDenormalizedEntitiesList('page').pipe(
                takeUntil(stopper.stopper$),
            ).subscribe((result) => actualResultSubA = result);

            expect(actualResultSubA).toEqual(expectedDenormalizedPages);
            denormalizeEntitySpy.calls.reset();

            expect(entityManager.createDenormalizedCacheSpy).toHaveBeenCalled();
            const cache: Map<any, any> = entityManager.createDenormalizedCacheSpy.calls.mostRecent().returnValue;
            expect(cache.size).toBe(ENTITIES_COUNT);

            // Unsubscribe and allow the cache to be cleared.
            tick(cleanupDebounceTime);
            subA.unsubscribe();
            tick(cleanupDebounceTime);

            expect(cache.size).toBe(0);

            // Create a new subscription (sub B).
            entityManager.watchDenormalizedEntitiesList('page').pipe(
                takeUntil(stopper.stopper$),
            ).subscribe((result) => {
                ++emissionCountSubB;
                actualResultSubB = result;
            });

            // All entities should have been denormalized again.
            expect(emissionCountSubB).toBe(1, 'Only the new denormalized list should have been emitted.');
            expect(actualResultSubB).toEqual(expectedDenormalizedPages);
            expect(denormalizeEntitySpy.calls.count()).toBe(ENTITIES_COUNT);
            actualResultSubA.forEach(
                (pageFromA, index) => expect(actualResultSubB[index]).not.toBe(pageFromA),
            );
            expect(cache.size).toBe(ENTITIES_COUNT);

            tick(cleanupDebounceTime);
        }));

        it('removes a deleted entity from the cache after the debounce time', fakeAsync(() => {
            let emissionCount = 0;
            let actualResult: Page<Raw>[] = [];

            entityManager.watchDenormalizedEntitiesList('page').pipe(
                takeUntil(stopper.stopper$),
            ).subscribe((result) => {
                ++emissionCount;
                actualResult = result;
            });
            expect(emissionCount).toBe(1);

            const cache: Map<any, any> = (entityManager).createDenormalizedCacheSpy.calls.mostRecent().returnValue;
            expect(cache.size).toBe(ENTITIES_COUNT);

            // Remove the a page.
            entityManager.deleteEntities('page', [actualResult[0].id]);
            expect(emissionCount).toBe(2);
            expect(cache.size).toBe(ENTITIES_COUNT);

            tick(cleanupDebounceTime);
            expect(cache.size).toBe(ENTITIES_COUNT - 1);
        }));

    });

    describe('watchNormalizedEntitiesList', () => {

        const ENTITIES_COUNT = 100;
        let normalizer: GcmsNormalizer;
        let expectedNormalizedPages: Page<Normalized>[];

        function generateTestPages(count: number): EntityNormalizationPair<Page<Raw>> {
            const rawPages: Page<Raw>[] = new Array(count);
            for (let i = 0; i < count; i += 2) {
                rawPages[i] = getExamplePageData({ id: i + 1, idVariant1: i + 2 });
                rawPages[i + 1] = getExamplePageData({ id: i + 2, idVariant1: i + 1 });
            }
            const normalized = normalizer.normalize('page', rawPages).entities;
            return {
                raw: rawPages,
                normalized,
            };
        }

        function getNormalizedPages(normalizedEntities: Partial<NormalizedEntityStore>): Page<Normalized>[] {
            return Object.keys(normalizedEntities.page).map(
                (key) => normalizedEntities.page[key],
            );
        }

        beforeEach(() => {
            normalizer = new GcmsNormalizer();
            entityManager.init();

            const entities = generateTestPages(ENTITIES_COUNT);
            appState.mockState({
                entity: entities.normalized,
            });
            expectedNormalizedPages = getNormalizedPages(appState.now.entity);
        });

        it('emits all entities in the state branch upon subscription', fakeAsync(() => {
            let emissionCount = 0;
            let actualResult: Page<Normalized>[] = [];

            entityManager.watchNormalizedEntitiesList('page').pipe(
                takeUntil(stopper.stopper$),
            ).subscribe((result) => {
                ++emissionCount;
                actualResult = result;
            });

            expect(emissionCount).toBe(1);
            expect(actualResult).toEqual(expectedNormalizedPages);
        }));

        it('emits all entities when an entity is changed', fakeAsync(() => {
            let emissionCount = 0;
            let actualResult: Page<Normalized>[] = [];

            entityManager.watchNormalizedEntitiesList('page').pipe(
                takeUntil(stopper.stopper$),
            ).subscribe((result) => {
                ++emissionCount;
                actualResult = result.slice();
            });

            expect(emissionCount).toBe(1);
            expect(actualResult).toEqual(expectedNormalizedPages);

            // Change the name of an entity.
            const changedEntity = normalizer.denormalize('page', expectedNormalizedPages[10], appState.now.entity);
            changedEntity.name = changedEntity.name + ' changed';
            entityManager.addEntity('page', changedEntity);
            expectedNormalizedPages[10] = normalizer.normalize('page', changedEntity).result;

            expect(emissionCount).toBe(2);
            expect(actualResult).toEqual(expectedNormalizedPages);
        }));

        it('emits all entities when an entity is added', fakeAsync(() => {
            let emissionCount = 0;
            let actualResult: Page<Normalized>[] = [];

            entityManager.watchNormalizedEntitiesList('page').pipe(
                takeUntil(stopper.stopper$),
            ).subscribe((result) => {
                ++emissionCount;
                actualResult = result;
            });

            expect(emissionCount).toBe(1);
            expect(actualResult).toEqual(expectedNormalizedPages);

            // Add a new entity.
            const newEntityId = actualResult.length + 1;
            const newEntity = getExamplePageData({ id: newEntityId, idVariant1: newEntityId });
            newEntity.name = 'newEntity';
            entityManager.addEntity('page', newEntity);
            expectedNormalizedPages.push(normalizer.normalize('page', newEntity).result);

            expect(emissionCount).toBe(2);
            expect(actualResult).toEqual(expectedNormalizedPages);
        }));

        it('emits all entities when an entity is removed', fakeAsync(() => {
            let emissionCount = 0;
            let actualResult: Page<Normalized>[] = [];

            entityManager.watchNormalizedEntitiesList('page').pipe(
                takeUntil(stopper.stopper$),
            ).subscribe((result) => {
                ++emissionCount;
                actualResult = result;
            });

            expect(emissionCount).toBe(1);
            expect(actualResult).toEqual(expectedNormalizedPages);

            // Remove an entity.
            const removedEntity = expectedNormalizedPages[10];
            entityManager.deleteEntities('page', [removedEntity.id]);
            expectedNormalizedPages.splice(10, 1);
            expect(expectedNormalizedPages.length).toBe(ENTITIES_COUNT - 1);

            expect(emissionCount).toBe(2);
            expect(actualResult).toEqual(expectedNormalizedPages);
        }));

    });

    describe('entity addition', () => {

        let rawPages: Page<Raw>[];
        let pageIds: number[];
        let folderIds: number[];
        let userIds: number[];
        let expectedEntityState: EntityStateModel;
        const maxSyncBatchSize = 20;

        /**
         * Generates `count` raw pages, each with a folder and a page variant;
         * the user IDs alter between two values.
         */
        function generateTestPages(count: number): void {
            const normalizer = new GcmsNormalizer();
            const userIdsSet = new Set<number>();

            for (let i = 0; i < count; ++i) {
                const pageId = (i + 1) * 2;
                const variantId = pageId + 1;
                const folderId = pageId;
                const userId = i % 2 === 0 ? USER_A_ID : NEW_USER_ID;

                pageIds.push(pageId, variantId);
                userIdsSet.add(userId);
                folderIds.push(folderId);

                const page = getExamplePageData({
                    id: pageId,
                    userId,
                    idVariant1: variantId,
                });
                (page as any).folder = getExampleFolderData({ id: pageId, userId });
                Object.freeze(page);
                rawPages.push(page);
            }
            Object.freeze(rawPages);

            userIds = Array.from(userIdsSet);

            expectedEntityState = cloneDeep(INITIAL_ENTITY_STATE);

            // Incredible stupid workaround. Normalized entities have a symbol set (IS_NORMALIZED),
            // which would cause tests to fail because they don't expect them to be present.
            const normalizedEntities = normalizer.normalize('page', rawPages).entities;
            Object.entries(normalizedEntities).forEach(([entityType, entityMap]) => {
                const entityList: Page<Normalized>[] = Object.values(entityMap);
                for (const entity of entityList) {
                    expectedEntityState[entityType][entity.id] = jasmine.objectContaining(entity);
                }
            });
        }

        function assertEntitiesCountInState(testPagesCount: number): void {
            expect(Object.keys(appState.now.entity.page).length).toBe(testPagesCount * 2);
            expect(Object.keys(appState.now.entity.user).length).toBe(2);
            expect(Object.keys(appState.now.entity.folder).length).toBe(testPagesCount);
        }

        function assertIdsAreInEntityState(
            ids: EntityIds = { pages: pageIds, users: userIds, folders: folderIds },
        ): void {
            ids.pages.forEach((id) => {
                const page = appState.now.entity.page[id];
                expect(page).toBeTruthy();
                expect(page[IS_NORMALIZED]).toBe(true, `Page ${id} does not have IS_NORMALIZED symbol.`);
            });
            ids.users.forEach((id) => {
                const user = appState.now.entity.user[id];
                expect(user).toBeTruthy();
                expect(user[IS_NORMALIZED]).toBe(true, `User ${id} does not have IS_NORMALIZED symbol.`);
            });
            ids.folders.forEach((id) => {
                const folder = appState.now.entity.folder[id];
                expect(folder).toBeTruthy();
                expect(folder[IS_NORMALIZED]).toBe(true, `Folder ${id} does not have IS_NORMALIZED symbol.`);
            });
        }

        beforeEach(() => {
            rawPages = [];
            pageIds = [];
            folderIds = [];
            userIds = null;
            expectedEntityState = null;

            entityManager.init();
        });

        it('test data is initialized correctly', () => {
            const testPagesCount = 4 * maxSyncBatchSize;
            generateTestPages(testPagesCount);

            expect(rawPages.length).toBe(testPagesCount);
            expect(pageIds.length).toBe(testPagesCount * 2);
            expect(userIds.length).toBe(2);
            expect(folderIds.length).toBe(testPagesCount);

            expect(Object.keys(expectedEntityState.page).length).toBe(testPagesCount * 2);
            expect(Object.keys(expectedEntityState.user).length).toBe(2);
            expect(Object.keys(expectedEntityState.folder).length).toBe(testPagesCount);
        });

        describe('addEntity()', () => {

            it('works for undefined', () => {
                entityManager.addEntity('page', undefined);
                expect(appState.now.entity).toEqual(INITIAL_ENTITY_STATE);
            });

            it('works for null', () => {
                entityManager.addEntity('page', null);
                expect(appState.now.entity).toEqual(INITIAL_ENTITY_STATE);
            });

            it('works for a single entity', () => {
                generateTestPages(1);
                entityManager.addEntity('page', rawPages[0]);

                expect(Object.keys(appState.now.entity.page).length).toBe(2);
                expect(Object.keys(appState.now.entity.user).length).toBe(1);
                expect(Object.keys(appState.now.entity.folder).length).toBe(1);
                assertIdsAreInEntityState();
                expect(appState.now.entity).toEqual(expectedEntityState);
            });

        });

        describe('addEntities()', () => {

            it('works for undefined', (done) => {
                const done$ = entityManager.addEntities('page', undefined);

                done$.then(() => {
                    expect(appState.now.entity).toEqual(INITIAL_ENTITY_STATE);
                    done();
                });
            });

            it('works for null', (done) => {
                const done$ = entityManager.addEntities('page', null);

                done$.then(() => {
                    expect(appState.now.entity).toEqual(INITIAL_ENTITY_STATE);
                    done();
                });
            });

            it('works for an empty array', (done) => {
                const done$ = entityManager.addEntities('page', []);

                done$.then(() => {
                    expect(appState.now.entity).toEqual(INITIAL_ENTITY_STATE);
                    done();
                });
            });

            it('works for a single entity without using the web worker', (done) => {
                generateTestPages(1);
                const done$ = entityManager.addEntities('page', rawPages);

                done$.then(() => {
                    expect(Object.keys(appState.now.entity.page).length).toBe(2);
                    expect(Object.keys(appState.now.entity.user).length).toBe(1);
                    expect(Object.keys(appState.now.entity.folder).length).toBe(1);
                    assertIdsAreInEntityState();
                    expect(appState.now.entity).toEqual(expectedEntityState);
                    done();
                });
            });

            it('works for an array that is smaller than maxSyncBatchSize without using the web worker', (done) => {
                const count = 10;
                expect(count).toBeLessThan(maxSyncBatchSize);
                generateTestPages(count);

                const done$ = entityManager.addEntities('page', rawPages);

                done$.then(() => {
                    assertEntitiesCountInState(count);
                    assertIdsAreInEntityState();
                    expect(appState.now.entity).toEqual(expectedEntityState);
                    done();
                });
            });

            it('works for an array that has exactly maxSyncBatchSize', (done) => {
                generateTestPages(maxSyncBatchSize);
                const done$ = entityManager.addEntities('page', rawPages);

                done$.then(() => {
                    assertEntitiesCountInState(maxSyncBatchSize);
                    assertIdsAreInEntityState();
                    expect(appState.now.entity).toEqual(expectedEntityState);
                    done();
                });
            });

            it('works for an array that is larger than maxSyncBatchSize and uses the web worker for it', (done) => {
                const count = 3 * maxSyncBatchSize;
                generateTestPages(count);
                const done$ = entityManager.addEntities('page', rawPages);

                const expectedRequest: NormalizationWorkerRequest<'page', Page<Raw>> = {
                    id: 0,
                    entityType: 'page',
                    rawEntities: rawPages,
                };

                const expectedEntityStateIds: EntityIds = { pages: pageIds, users: userIds, folders: folderIds };

                done$.then(() => {
                    assertEntitiesCountInState(count);
                    assertIdsAreInEntityState(expectedEntityStateIds);
                    expect(appState.now.entity).toEqual(expectedEntityState);
                    done();
                });
            });

            it('increments the IDs of requests to the web worker', (done) => {
                const count = 4 * maxSyncBatchSize;
                generateTestPages(count);
                const rawPages1 = rawPages.slice(0, 2 * maxSyncBatchSize);
                const rawPages2 = rawPages.slice(2 * maxSyncBatchSize, rawPages.length);

                const done1$ = entityManager.addEntities('page', rawPages1);
                const expectedRequest1: NormalizationWorkerRequest<'page', Page<Raw>> = {
                    id: 0,
                    entityType: 'page',
                    rawEntities: rawPages1,
                };

                const done2$ = entityManager.addEntities('page', rawPages2);
                const expectedRequest2: NormalizationWorkerRequest<'page', Page<Raw>> = {
                    id: 1,
                    entityType: 'page',
                    rawEntities: rawPages2,
                };

                const expectedFinalEntityStateIds: EntityIds = { pages: pageIds, users: userIds, folders: folderIds };

                let done1Resolved = false;
                done1$.then(() => done1Resolved = true);

                done2$.then(() => {
                    expect(done1Resolved).toBe(true, 'The first request should have completed first.');
                    assertEntitiesCountInState(count);
                    assertIdsAreInEntityState(expectedFinalEntityStateIds);
                    expect(appState.now.entity).toEqual(expectedEntityState);
                    done();
                });
            });

            it('forwards errors during normalization when not using the web worker', (done) => {
                const count = maxSyncBatchSize;
                generateTestPages(count);

                const expectedError = new Error('Expected Error');
                spyOn(entityManager.normalizer, 'normalize').and.callFake(() => {
                    throw expectedError;
                });

                const done$ = entityManager.addEntities('page', rawPages);

                done$
                    .then(() => fail('normalization should have failed here.'))
                    .catch((error) => {
                        expect(error).toBe(expectedError);
                        done();
                    });
            });
        });

        describe('deleteEntities()', () => {

            it('works for undefined', () => {
                generateTestPages(1);
                entityManager.addEntities('page', rawPages);
                entityManager.deleteEntities('page', undefined);
                expect(appState.now.entity).toEqual(expectedEntityState);
            });

            it('works for null', () => {
                generateTestPages(1);
                entityManager.addEntities('page', rawPages);
                entityManager.deleteEntities('page', null);
                expect(appState.now.entity).toEqual(expectedEntityState);
            });

            it('works for a single entity', () => {
                generateTestPages(1);
                entityManager.addEntities('page', rawPages);
                expect(Object.keys(appState.now.entity.page).length).toBe(2);
                entityManager.deleteEntities('page', [rawPages[0].id]);

                expect(Object.keys(appState.now.entity.page).length).toBe(1);
                expect(Object.keys(appState.now.entity.user).length).toBe(1);
                expect(Object.keys(appState.now.entity.folder).length).toBe(1);
                const pageIdsModified = pageIds.filter((id) => id !== rawPages[0].id);
                const expectedEntityStateIds: EntityIds = { pages: pageIdsModified, users: userIds, folders: folderIds };
                assertIdsAreInEntityState(expectedEntityStateIds);
            });

            it('works for multiple entities', () => {
                generateTestPages(2);
                entityManager.addEntities('page', rawPages);
                expect(Object.keys(appState.now.entity.page).length).toBe(4);
                entityManager.deleteEntities('page', [rawPages[0].id, rawPages[1].id]);

                expect(Object.keys(appState.now.entity.page).length).toBe(2);
                expect(Object.keys(appState.now.entity.user).length).toBe(2);
                expect(Object.keys(appState.now.entity.folder).length).toBe(2);
                const pageIdsModified = pageIds.filter((id) => id !== rawPages[0].id).filter((id) => id !== rawPages[1].id);
                const expectedEntityStateIds: EntityIds = { pages: pageIdsModified, users: userIds, folders: folderIds };
                assertIdsAreInEntityState(expectedEntityStateIds);
            });

        });

    });

});

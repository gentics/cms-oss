import { TestBed, waitForAsync } from '@angular/core/testing';
import { Normalized, Page, RecursivePartial } from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';
import * as _ from'lodash-es'

import { AppStateService } from '../providers/app-state/app-state.service';
import { TestAppState, TEST_APP_STATE } from '../utils/test-app-state';
import { AddEntities, ClearAllEntities, DeleteEntities, UpdateEntities } from './entity.actions';
import { EntityStateModel, EntityStateModule, INITIAL_ENTITY_STATE } from './entity.state';

describe('EntityStateModule', () => {

    let appState: TestAppState;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot([EntityStateModule])],
            providers: [TEST_APP_STATE],
        }).compileComponents();
        appState = TestBed.get(AppStateService);
    }));

    it('sets the correct initial state', () => {
        appState.selectOnce(state => state.entity).subscribe(entityState => {
            expect(entityState).toEqual(INITIAL_ENTITY_STATE);
        });
    });

    // ADD
    describe('AddEntities', () => {

        it('adds non-existing entities', () => {
            const initialState = appState.snapshot().entity;
            const newEntities: RecursivePartial<EntityStateModel> = {
                page: {
                    1: {
                        id: 1,
                        name: 'Page 1',
                        type: 'page',
                    },
                    2: {
                        id: 2,
                        name: 'Page 2',
                        type: 'page',
                    },
                },
            };

            appState.dispatch(new AddEntities(_.cloneDeep(newEntities as any)));
            const result = appState.snapshot().entity;

            expect(result).not.toBe(initialState);
            expect(result.page).toEqual(newEntities.page);
        });

        it('adds non-existing entities to multiple branches', () => {
            const initialState = appState.snapshot().entity;
            const newEntities: RecursivePartial<EntityStateModel> = {
                page: {
                    1: {
                        id: 1,
                        name: 'Page 1',
                        type: 'page',
                    },
                    2: {
                        id: 2,
                        name: 'Page 2',
                        type: 'page',
                    },
                },
                user: {
                    1: {
                        id: 1,
                        login: 'admin',
                        firstName: 'John',
                        lastName: 'Doe',
                    },
                },
            };

            appState.dispatch(new AddEntities(_.cloneDeep(newEntities as any)));
            const result = appState.snapshot().entity;

            expect(result).not.toBe(initialState);
            expect(result.page).toEqual(newEntities.page);
            expect(result.user).toEqual(newEntities.user);
        });

        it('keeps unchanged entity branches', () => {
            const initialState = appState.snapshot().entity;
            const newEntities: RecursivePartial<EntityStateModel> = {
                page: {
                    1: {
                        id: 1,
                        name: 'Page 1',
                        type: 'page',
                    },
                },
            };

            appState.dispatch(new AddEntities(newEntities as any));
            const result = appState.snapshot().entity;

            expect(result).not.toBe(initialState);
            expect(result.page).not.toBe(initialState.page);

            const branches = Object.keys(result);
            expect(branches.length).toBe(Object.keys(INITIAL_ENTITY_STATE).length);
            branches.forEach(branchKey => {
                if (branchKey !== 'page') {
                    expect(result[branchKey]).toBe(initialState[branchKey], `Branch ${branchKey} has changed unexpectedly`);
                }
            });
        });

        it('ignores empty change branches', () => {
            appState.mockState({
                entity: {
                    page: {
                        1: {
                            id: 1,
                            name: 'Page 1',
                            type: 'page',
                        },
                        2: {
                            id: 2,
                            name: 'Page 2',
                            type: 'page',
                        },
                    },
                },
            });
            const initialState = appState.snapshot().entity;
            const initialStateClone = _.cloneDeep(initialState);

            const changes: Partial<EntityStateModel> = {
                page: { },
            };

            appState.dispatch(new AddEntities(changes));
            const result = appState.snapshot().entity;

            expect(result).toBe(initialState);
            expect(result).toEqual(initialStateClone);
        });

        it('overwrites existing entities (and changes reference)', () => {
            appState.mockState({
                entity: {
                    page: {
                        1: {
                            id: 1,
                            name: 'Page 1 before change',
                            type: 'page',
                        },
                        2: {
                            id: 2,
                            name: 'Page 2',
                            type: 'page',
                        },
                    },
                },
            });
            const initialState = appState.snapshot().entity;
            const origPage2 = _.cloneDeep(initialState.page[2]);

            const changes: RecursivePartial<EntityStateModel> = {
                page: {
                    1: {
                        id: 1,
                        name: 'Page 1 after change',
                        type: 'page',
                    },
                },
            };

            appState.dispatch(new AddEntities(changes as any));
            const result = appState.snapshot().entity;

            expect(initialState.page[1].name).toBe('Page 1 before change');
            expect(result.page[1].name).toBe('Page 1 after change');
            expect(result.page[1]).not.toBe(initialState.page[1], 'page[1] is still the same reference');
            expect(result.page[2]).toEqual(origPage2, 'page[2] has been changed');
            expect(result.page[2]).toBe(initialState.page[2], 'the reference of page[2] has been changed');
            expect(result.page).not.toBe(initialState.page, 'pages is still the same reference');
        });

        it('keeps existing entities with the same values (and keeps reference)', () => {
            const PAGE1: Partial<Page<Normalized>> = {
                id: 1,
                name: 'Page 1, no changes',
                type: 'page',
            };
            appState.mockState({
                entity: {
                    page: {
                        1: { ...PAGE1 },
                    },
                },
            });
            const initialState = appState.snapshot().entity;

            const changes: Partial<EntityStateModel> = {
                page: {
                    1: { ...PAGE1 } as any,
                },
            };

            appState.dispatch(new AddEntities(changes));
            const result = appState.snapshot().entity;

            expect(result.page[1]).toEqual(PAGE1, 'the values of entityState.page[1] have been changed');
            expect(result.page[1]).toBe(initialState.page[1], 'entityState.page[1] reference has been changed');
            expect(result.page).toBe(initialState.page, 'entityState.page reference has been changed');
            expect(result).toBe(initialState, 'entityState reference has been changed');
        });

        it('keeps existing entities with extra properties if nothing changes (and keeps reference)', () => {
            appState.mockState({
                entity: {
                    page: {
                        1: {
                            id: 1,
                            name: 'Page 1, no changes',
                            type: 'page',
                            description: 'description not in new entity',
                        },
                    },
                },
            });
            const initialState = appState.snapshot().entity;

            const changes: RecursivePartial<EntityStateModel> = {
                page: {
                    1: {
                        id: 1,
                        name: 'Page 1, no changes',
                        type: 'page',
                    },
                },
            };

            appState.dispatch(new AddEntities(changes as any));
            const result = appState.snapshot().entity;

            expect(result.page[1]).toBe(initialState.page[1], 'entityState.page[1] reference has been changed');
            expect(result.page[1].description).toBe('description not in new entity');
            expect(result.page).toBe(initialState.page, 'entityState.page reference has been changed');
            expect(result).toBe(initialState, 'entityState reference has been changed');
        });

        it('keeps extra properties of existing entities if other properties change (and changes reference)', () => {
            appState.mockState({
                entity: {
                    page: {
                        1: {
                            id: 1,
                            name: 'Page 1, no changes',
                            type: 'page',
                            description: 'description not in new entity',
                        },
                    },
                },
            });
            const initialState = appState.snapshot().entity;

            const changes: RecursivePartial<EntityStateModel> = {
                page: {
                    1: {
                        id: 1,
                        name: 'Page 1 after change',
                        type: 'page',
                    },
                },
            };

            appState.dispatch(new AddEntities(changes as any));
            const result = appState.snapshot().entity;

            expect(result.page[1]).not.toBe(initialState.page[1], 'entityState.page[1] is still the same reference');
            expect(result.page[1].description).toBe('description not in new entity');
            expect(result.page).not.toBe(initialState.page, 'entityState.page is still the same reference');
            expect(result).not.toBe(initialState, 'entityState is still the same reference');
        });

        it('adds new properties to existing entities (and changes reference)', () => {
            appState.mockState({
                entity: {
                    page: {
                        1: {
                            id: 1,
                            name: 'Page 1',
                            type: 'page',
                            description: 'description not in new entity',
                        },
                    },
                },
            });
            const initialState = appState.snapshot().entity;

            const changes: RecursivePartial<EntityStateModel> = {
                page: {
                    1: {
                        id: 1,
                        name: 'Page 1',
                        type: 'page',
                        editor: 2,
                    },
                },
            };

            appState.dispatch(new AddEntities(changes as any));
            const result = appState.snapshot().entity;

            expect(result.page[1]).not.toBe(initialState.page[1], 'entityState.page[1] is still the same reference');
            expect(result.page[1].description).toBe('description not in new entity');
            expect(result.page[1].editor).toBe(2);
            expect(result.page).not.toBe(initialState.page, 'entityState.page is still the same reference');
            expect(result).not.toBe(initialState, 'entityState is still the same reference');
        });

        it('overwrites non-empty arrays with empty arrays (and changes reference)', () => {
            appState.mockState({
                entity: {
                    page: {
                        1: {
                            id: 1,
                            name: 'Page 1',
                            type: 'page',
                            versions: [
                                {
                                    number: '1.5',
                                    timestamp: 123456789,
                                },
                            ],
                        },
                    },
                },
            });
            const initialState = appState.snapshot().entity;
            expect(initialState.page[1].versions.length).toBe(1);

            const changes: RecursivePartial<EntityStateModel> = {
                page: {
                    1: {
                        versions: [],
                    },
                },
            };
            appState.dispatch(new AddEntities(changes as any));
            const result = appState.snapshot().entity;

            expect(result.page[1]).not.toBe(initialState.page[1], 'entityState.page[1] is still the same reference');
            expect(result.page[1].versions).not.toBe(initialState.page[1].versions, 'entityState.page[1].versions is still the same reference');
            expect(result.page[1].versions.length).toBe(0);
            expect(result.page[1].name).toEqual('Page 1');
            expect(result.page).not.toBe(initialState.page, 'entityState.page is still the same reference');
            expect(result).not.toBe(initialState, 'entityState is still the same reference');
        });

        it('overwrites nested objects (and changes reference)', () => {
            appState.mockState({
                entity: {
                    page: {
                        1: {
                            id: 1,
                            name: 'Page 1',
                            type: 'page',
                            timeManagement: {
                                at: 1234,
                                offlineAt: 1234,
                            },
                        },
                    },
                },
            });
            const initialState = appState.snapshot().entity;

            const changes: RecursivePartial<EntityStateModel> = {
                page: {
                    1: {
                        timeManagement: {
                            at: 4711,
                        },
                    },
                },
            };
            appState.dispatch(new AddEntities(changes as any));
            const result = appState.snapshot().entity;

            expect(result.page[1]).not.toBe(initialState.page[1], 'entityState.page[1] is still the same reference');
            expect(result.page[1].name).toEqual('Page 1');
            expect(result.page[1].timeManagement).not.toBe(initialState.page[1].timeManagement,
                'entityState.page[1].timeManagement is still the same reference');
            expect(result.page[1].timeManagement.at).toBe(4711);
            expect(result.page[1].timeManagement.offlineAt).toBeUndefined();
            expect(result.page).not.toBe(initialState.page, 'entityState.page is still the same reference');
            expect(result).not.toBe(initialState, 'entityState is still the same reference');
        });

    });

    // UPDATE
    describe('UpdateEntities', () => {

        it('updates properties of existing entities', () => {
            appState.mockState({
                entity: {
                    page: {
                        1: {
                            id: 1,
                            locked: false,
                            name: 'Page 1',
                            type: 'page',
                        },
                    },
                },
            });

            const updates: RecursivePartial<EntityStateModel> = {
                page: {
                    1: {
                        locked: true,
                    },
                },
            };
            appState.dispatch(new UpdateEntities(updates));
            const result = appState.snapshot().entity;

            expect(result.page[1].locked).toBe(true);
        });

        it('does not modify unchanged properties or unchanged entities', () => {
            const origEntities: RecursivePartial<EntityStateModel> = {
                page: {
                    1: {
                        id: 1,
                        locked: false,
                        name: 'Page 1',
                        type: 'page',
                    },
                    2: {
                        id: 2,
                        locked: false,
                        name: 'Page 2',
                        type: 'page',
                    },
                },
            };
            appState.mockState({
                entity: _.cloneDeep(origEntities),
            });
            const initialState = appState.snapshot().entity;
            const expectedState = _.cloneDeep(initialState);
            expectedState.page[1].locked = true;

            const updates: RecursivePartial<EntityStateModel> = {
                page: {
                    1: {
                        locked: true,
                    },
                },
            };
            appState.dispatch(new UpdateEntities(updates));
            const result = appState.snapshot().entity;

            expect(result).toEqual(expectedState);
            expect(result.page[2]).toBe(initialState.page[2], 'entityState.page[2] reference has been changed');
        });

        it('does not add non-existing entities', () => {
            const initialState = appState.snapshot().entity;

            const updates = {
                page: {
                    1: {
                        locked: true,
                    },
                },
            };
            appState.dispatch(new UpdateEntities(updates));
            const result = appState.snapshot().entity;

            expect(result).toBe(initialState, 'entityState reference has been changed');
            expect(result).toEqual(INITIAL_ENTITY_STATE);
        });

        it('changes references of changed entity branches', () => {
            appState.mockState({
                entity: {
                    page: {
                        1: {
                            id: 1,
                            locked: false,
                            name: 'Page 1',
                            type: 'page',
                        },
                    },
                },
            });
            const initialState = appState.snapshot().entity;

            const updates: RecursivePartial<EntityStateModel> = {
                page: {
                    1: {
                        locked: true,
                    },
                },
            };
            appState.dispatch(new UpdateEntities(updates));
            const result = appState.snapshot().entity;

            expect(result.page[1]).not.toBe(initialState.page[1], 'entityState.page[1] is still the same reference');
            expect(result.page).not.toBe(initialState.page, 'entityState.page is still the same reference');
            expect(result).not.toBe(initialState, 'entityState is still the same reference');
        });

        it('keeps unchanged entity branches', () => {
            appState.mockState({
                entity: {
                    page: {
                        1: {
                            id: 1,
                            locked: false,
                            name: 'Page 1',
                            type: 'page',
                        },
                    },
                },
            });
            const initialState = appState.snapshot().entity;
            const expectedUnchangedBranches = _.cloneDeep(INITIAL_ENTITY_STATE);
            delete expectedUnchangedBranches.page;

            const updates: RecursivePartial<EntityStateModel> = {
                page: {
                    1: {
                        locked: true,
                    },
                },
            };
            appState.dispatch(new UpdateEntities(updates));
            const result = appState.snapshot().entity;

            expect(result).not.toBe(initialState);
            expect(result.page).not.toBe(initialState.page);
            const resultWithoutPages = { ...result };
            delete resultWithoutPages.page;
            expect(resultWithoutPages).toEqual(expectedUnchangedBranches);
            Object.keys(resultWithoutPages).forEach(
                branchKey => expect(result[branchKey]).toBe(initialState[branchKey], `reference of entityState.${branchKey} has been changed`),
            );
        });

        it('ignores empty entity branches in the input', () => {
            appState.mockState({
                entity: {
                    page: {
                        1: {
                            id: 1,
                            name: 'Page 1',
                            type: 'page',
                        },
                        2: {
                            id: 2,
                            name: 'Page 2',
                            type: 'page',
                        },
                    },
                },
            });
            const initialState = appState.snapshot().entity;
            const initialStateClone = _.cloneDeep(initialState);

            const changes: Partial<EntityStateModel> = {
                page: { },
            };

            appState.dispatch(new UpdateEntities(changes));
            const result = appState.snapshot().entity;

            expect(result).toBe(initialState);
            expect(result).toEqual(initialStateClone);
        });

        it('can change single nested properties without affecting unchanged properties', () => {
            const origEntities: RecursivePartial<EntityStateModel> = {
                page: {
                    1: {
                        id: 1,
                        type: 'page',
                        name: 'Page 1',
                        translationStatus: {
                            name: 'Seite 1',
                            inSync: true,
                            language: 'de',
                        },
                    },
                },
            };
            appState.mockState({
                entity: _.cloneDeep(origEntities),
            });
            const initialState = appState.snapshot().entity;
            const expectedPage = _.cloneDeep(origEntities.page[1]);
            expectedPage.translationStatus.inSync = false;

            const updates = {
                page: {
                    1: {
                        translationStatus: {
                            inSync: false,
                        },
                    },
                },
            };
            appState.dispatch(new UpdateEntities(updates));
            const result = appState.snapshot().entity;

            expect(result.page[1]).toEqual(expectedPage);
        });

        it('can overwrite a simple property with null', () => {
            const origEntities: RecursivePartial<EntityStateModel> = {
                page: {
                    1: {
                        id: 1,
                        type: 'page',
                        name: 'Page 1',
                        translationStatus: {
                            name: 'Seite 1',
                            inSync: true,
                            language: 'de',
                        },
                    },
                },
            };
            appState.mockState({
                entity: _.cloneDeep(origEntities),
            });
            const expectedPage = _.cloneDeep(origEntities.page[1]);
            expectedPage.name = null;

            const updates = {
                page: {
                    1: {
                        name: null,
                    },
                },
            };
            appState.dispatch(new UpdateEntities(updates));
            const result = appState.snapshot().entity;

            expect(result.page[1]).toEqual(expectedPage);
        });

        it('can overwrite a complex property with null', () => {
            const origEntities: RecursivePartial<EntityStateModel> = {
                page: {
                    1: {
                        id: 1,
                        type: 'page',
                        name: 'Page 1',
                        translationStatus: {
                            name: 'Seite 1',
                            inSync: true,
                            language: 'de',
                        },
                    },
                },
            };
            appState.mockState({
                entity: _.cloneDeep(origEntities),
            });
            const expectedPage = _.cloneDeep(origEntities.page[1]);
            expectedPage.translationStatus = null;

            const updates = {
                page: {
                    1: {
                        translationStatus: null,
                    },
                },
            };
            appState.dispatch(new UpdateEntities(updates));
            const result = appState.snapshot().entity;

            expect(result.page[1]).toEqual(expectedPage);
        });

        it('can overwrite non-empty arrays with empty arrays', () => {
            appState.mockState({
                entity: {
                    page: {
                        1: {
                            id: 1,
                            type: 'page',
                            versions: [
                                {
                                    number: '1.5',
                                    timestamp: 123456789,
                                },
                            ],
                        },
                    },
                },
            });
            const initialState = appState.snapshot().entity;
            expect(initialState.page[1].versions.length).toBe(1);

            const updates = {
                page: {
                    1: {
                        versions: [],
                    },
                },
            };
            appState.dispatch(new UpdateEntities(updates));
            const result = appState.snapshot().entity;

            expect(result.page[1].versions.length).toBe(0);
        });

    });

    // DELETE
    describe('DeleteEntities', () => {

        it('removes a single entity', () => {
            // prepare test data
            const initialState = appState.snapshot().entity;
            const testEntities: RecursivePartial<EntityStateModel> = {
                page: {
                    1: {
                        id: 1,
                        name: 'Page 1',
                        type: 'page',
                    },
                    2: {
                        id: 2,
                        name: 'Page 2',
                        type: 'page',
                    },
                },
                user: {
                    1: {
                        id: 1,
                        login: 'admin',
                        firstName: 'John',
                        lastName: 'Doe',
                    },
                },
            };
            const resultEntities: RecursivePartial<EntityStateModel> = _.cloneDeep(testEntities as any);
            delete resultEntities.page[1];
            // add test data to state
            appState.dispatch(new AddEntities(_.cloneDeep(testEntities as any)));
            const originalStateEntities = appState.snapshot().entity;
            // assure that correct test data in state
            expect(originalStateEntities).not.toBe(initialState);
            expect(originalStateEntities.page).toEqual(testEntities.page);
            expect(originalStateEntities.user).toEqual(testEntities.user);

            // delete a page
            appState.dispatch(new DeleteEntities('page', [1]));
            const changedStateEntities = appState.snapshot().entity;
            // check state if correctlydeleted
            expect(changedStateEntities.page).toEqual(resultEntities.page);
            expect(originalStateEntities.user).toEqual(resultEntities.user);
        });

        it('removes multiple entities', () => {
            // prepare test data
            const initialState = appState.snapshot().entity;
            const testEntities: RecursivePartial<EntityStateModel> = {
                page: {
                    1: {
                        id: 1,
                        name: 'Page 1',
                        type: 'page',
                    },
                    2: {
                        id: 2,
                        name: 'Page 2',
                        type: 'page',
                    },
                    3: {
                        id: 3,
                        name: 'Page 3',
                        type: 'page',
                    },
                    4: {
                        id: 4,
                        name: 'Page 4',
                        type: 'page',
                    },
                },
                user: {
                    1: {
                        id: 1,
                        login: 'admin',
                        firstName: 'John',
                        lastName: 'Doe',
                    },
                },
            };
            const resultEntities: RecursivePartial<EntityStateModel> = _.cloneDeep(testEntities as any);
            delete resultEntities.page[2];
            delete resultEntities.page[4];
            // add test data to state
            appState.dispatch(new AddEntities(_.cloneDeep(testEntities as any)));
            const originalStateEntities = appState.snapshot().entity;
            // assure that correct test data in state
            expect(originalStateEntities).not.toBe(initialState);
            expect(originalStateEntities.page).toEqual(testEntities.page);
            expect(originalStateEntities.user).toEqual(testEntities.user);

            // delete a page
            appState.dispatch(new DeleteEntities('page', [2, 4]));
            const changedStateEntities = appState.snapshot().entity;
            // check state if correctlydeleted
            expect(changedStateEntities.page).toEqual(resultEntities.page);
            expect(originalStateEntities.user).toEqual(resultEntities.user);
        });

        it('does nothing if entity to be removed doesn\'t exist', () => {
            // prepare test data
            const initialState = appState.snapshot().entity;
            const testEntities: RecursivePartial<EntityStateModel> = {
                page: {
                    1: {
                        id: 1,
                        name: 'Page 1',
                        type: 'page',
                    },
                    2: {
                        id: 2,
                        name: 'Page 2',
                        type: 'page',
                    },
                },
                user: {
                    1: {
                        id: 1,
                        login: 'admin',
                        firstName: 'John',
                        lastName: 'Doe',
                    },
                },
            };
            // add test data to state
            appState.dispatch(new AddEntities(_.cloneDeep(testEntities as any)));
            const originalStateEntities = appState.snapshot().entity;
            // assure that correct test data in state
            expect(originalStateEntities).not.toBe(initialState);
            expect(originalStateEntities.page).toEqual(testEntities.page);
            expect(originalStateEntities.user).toEqual(testEntities.user);

            // delete a page
            appState.dispatch(new DeleteEntities('page', [3, 4]));
            const changedStateEntities = appState.snapshot().entity;
            // check state if correctlydeleted
            expect(changedStateEntities.page).toEqual(originalStateEntities.page);
            expect(changedStateEntities.user).toEqual(originalStateEntities.user);
        });

    });

    // CLEAR_ALL
    describe('ClearAllEntities', () => {

        it('clears all entities from the state', () => {
            appState.mockState({
                entity: {
                    page: {
                        1: {
                            id: 1,
                            locked: false,
                            name: 'Page 1',
                            type: 'page',
                        },
                    },
                    user: {
                        1: {
                            id: 1,
                            login: 'admin',
                            firstName: 'John',
                            lastName: 'Doe',
                        },
                    },
                },
            });

            appState.dispatch(new ClearAllEntities());
            expect(appState.now.entity).toEqual(INITIAL_ENTITY_STATE);
        });

    });

});

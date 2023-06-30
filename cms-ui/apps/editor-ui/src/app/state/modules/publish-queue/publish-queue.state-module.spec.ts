import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NgxsModule } from '@ngxs/store';
import {
    getExamplePageData,
    getExamplePageDataNormalized,
    getExampleUserData,
    getExampleUserDataNormalized,
} from '../../../../testing/test-data.mock';
import { ItemsInfo, PublishQueueState } from '../../../common/models';
import { ApplicationStateService } from '../../providers';
import { TestApplicationState } from '../../test-application-state.mock';
import { STATE_MODULES } from '../state-modules';
import {
    AssigningUsersToPagesErrorAction,
    AssigningUsersToPagesSuccessAction,
    PublishQueueFetchingErrorAction,
    PublishQueuePagesFetchingSuccessAction,
    PublishQueueUsersFetchingSuccessAction,
    StartAssigningUsersToPagesAction,
    StartPublishQueueFetchingAction,
} from './publish-queue.actions';

describe('PublishQueueStateModule', () => {

    let appState: TestApplicationState;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
            ],
        });
        appState = TestBed.get(ApplicationStateService);
    });

    it('sets the correct initial state', () => {
        expect(appState.now.publishQueue).toEqual({
            pages: {
                creating: false,
                deleting: [],
                fetchAll: false,
                fetching: false,
                hasMore: false,
                list: [],
                selected: [],
                saving: false,
                showPath: true,
                sortBy: 'name',
                sortOrder: 'asc',
                total: 0,
                currentPage: 1,
                itemsPerPage: 10,
            },
            users: [],
            fetching: false,
            assigning: false,
        } as PublishQueueState);
    });

    it('assignToUsersStart works', fakeAsync(() => {
        expect(appState.now.publishQueue.assigning).toBe(false);
        appState.dispatch(new StartAssigningUsersToPagesAction());
        tick();

        expect(appState.now.publishQueue.assigning).toBe(true);
    }));

    it('assignToUsersSuccess works', fakeAsync(() => {
        appState.mockState({
            publishQueue: {
                assigning: true,
            },
            entities: {
                page: {
                    1: getExamplePageDataNormalized({ id: 1 }),
                    2: getExamplePageDataNormalized({ id: 2 }),
                    3: getExamplePageDataNormalized({ id: 3 }),
                },
            },
        });
        appState.dispatch(new AssigningUsersToPagesSuccessAction([1, 2, 3]));
        tick();

        expect(appState.now.publishQueue.assigning).toEqual(false);
    }));

    it('assignToUsersError works', fakeAsync(() => {
        appState.mockState({
            publishQueue: {
                assigning: true,
                pages: {
                    creating: false,
                    deleting: [],
                    fetchAll: false,
                    fetching: false,
                    hasMore: false,
                    list: [],
                    selected: [],
                    saving: false,
                    showPath: false,
                    sortBy: 'name',
                    sortOrder: 'asc',
                    total: 0,
                    currentPage: 1,
                    itemsPerPage: 10,
                },
            },
        });
        appState.dispatch(new AssigningUsersToPagesErrorAction());
        tick();

        expect(appState.now.publishQueue.assigning).toEqual(false);
    }));

    it('fetchQueueStart works', fakeAsync(() => {
        expect(appState.now.publishQueue.fetching).toBe(false);
        appState.dispatch(new StartPublishQueueFetchingAction());
        tick();

        expect(appState.now.publishQueue.fetching).toBe(true);
    }));

    it('fetchQueueSuccess works', fakeAsync(() => {
        appState.mockState({
            publishQueue: {
                fetching: true,
                pages: {
                    creating: false,
                    deleting: [],
                    fetchAll: false,
                    fetching: false,
                    hasMore: false,
                    list: [],
                    selected: [],
                    saving: false,
                    showPath: false,
                    sortBy: 'name',
                    sortOrder: 'asc',
                    total: 0,
                    currentPage: 1,
                    itemsPerPage: 10,
                },
            },
            entities: {
                page: {
                    1: getExamplePageDataNormalized({ id: 1 }),
                },
                user: {},
            },
        });
        appState.dispatch(new PublishQueuePagesFetchingSuccessAction([
            getExamplePageData({ id: 3 }),
            getExamplePageData({ id: 5 }),
        ]));
        tick();

        expect(appState.now.publishQueue.fetching).toBe(false);
        expect(appState.now.publishQueue.pages).toEqual(
            {
                creating: false,
                deleting: [],
                fetchAll: false,
                fetching: false,
                hasMore: false,
                list: [3, 5],
                selected: [],
                saving: false,
                showPath: false,
                sortBy: 'name',
                sortOrder: 'asc',
                total: 0,
                currentPage: 1,
                itemsPerPage: 10,
            } as ItemsInfo,
        );

        // List extended with the default languageVariant id: 48, which is auto-loaded
        expect(Object.keys(appState.now.entities.page)).toEqual(['1', '3', '5', '48']);
    }));

    it('fetchQueueError works', fakeAsync(() => {
        appState.mockState({
            publishQueue: {
                fetching: true,
                pages: {
                    creating: false,
                    deleting: [],
                    fetchAll: false,
                    fetching: false,
                    hasMore: false,
                    list: [],
                    selected: [],
                    saving: false,
                    showPath: false,
                    sortBy: 'name',
                    sortOrder: 'asc',
                    total: 0,
                    currentPage: 1,
                    itemsPerPage: 10,
                },
            },
        });
        appState.dispatch(new PublishQueueFetchingErrorAction());
        tick();

        expect(appState.now.publishQueue.fetching).toBe(false);
    }));

    it('fetchUsersStart works', fakeAsync(() => {
        expect(appState.now.publishQueue.fetching).toBe(false);
        appState.dispatch(new StartPublishQueueFetchingAction());
        tick();

        expect(appState.now.publishQueue.fetching).toBe(true);
    }));

    it('fetchUsersSuccess works', fakeAsync(() => {
        appState.mockState({
            publishQueue: {
                fetching: true,
                pages: {
                    creating: false,
                    deleting: [],
                    fetchAll: false,
                    fetching: false,
                    hasMore: false,
                    list: [],
                    selected: [],
                    saving: false,
                    showPath: false,
                    sortBy: 'name',
                    sortOrder: 'asc',
                    total: 0,
                    currentPage: 1,
                    itemsPerPage: 10,
                },
            },
            entities: {
                user: {
                    1: getExampleUserDataNormalized({ id: 1 }),
                },
            },
        });
        appState.dispatch(new PublishQueueUsersFetchingSuccessAction([
            getExampleUserData({ id: 3 }),
            getExampleUserData({ id: 9 }),
        ]));
        tick();

        expect(appState.now.publishQueue.fetching).toBe(false);
        expect(appState.now.publishQueue.users).toEqual([3, 9]);
        expect(Object.keys(appState.now.entities.user)).toEqual(['1', '3', '9']);
    }));

    it('fetchUsersError works', fakeAsync(() => {
        appState.mockState({
            publishQueue: {
                fetching: true,
            },
        });
        appState.dispatch(new PublishQueueFetchingErrorAction());
        tick();

        expect(appState.now.publishQueue.fetching).toBe(false);
    }));

});

import { TestBed } from '@angular/core/testing';
import { NgxsModule } from '@ngxs/store';
import { getExampleFolderData } from '../../../../testing/test-data.mock';
import { WastebinState } from '../../../common/models';
import { ApplicationStateService } from '../../providers';
import { TestApplicationState } from '../../test-application-state.mock';
import { STATE_MODULES } from '../state-modules';
import {
    RestoreWasteBinItemsAction,
    StartWasteBinItemsDeletionAction,
    StartWasteBinItemsFetchingAction,
    WasteBinItemsDeletionErrorAction,
    WasteBinItemsDeletionSuccessAction,
    WasteBinItemsFetchingErrorAction,
    WasteBinItemsFetchingSuccessAction,
} from './wastebin.actions';

describe('WastebinStateModule', () => {

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
        expect(appState.now.wastebin).toEqual({
            folder: {
                list: [],
                requesting: false,
            },
            form: {
                list: [],
                requesting: false,
            },
            page: {
                list: [],
                requesting: false,
            },
            file: {
                list: [],
                requesting: false,
            },
            image: {
                list: [],
                requesting: false,
            },
            sortBy: 'name',
            sortOrder: 'asc',
            lastError: undefined,
        } as WastebinState);
    });

    it('fetchItemsStart works', () => {
        appState.dispatch(new StartWasteBinItemsFetchingAction('page'));
        expect(appState.now.wastebin.page.requesting).toBe(true);
    });

    it('fetchItemsSuccess works', () => {
        appState.mockState({
            wastebin: {
                page: {
                    list: [],
                    requesting: true,
                },
            },
        });

        appState.dispatch(new WasteBinItemsFetchingSuccessAction('page', [
            getExampleFolderData({ id: 3, userId: 1 }),
            getExampleFolderData({ id: 7, userId: 3 }),
            getExampleFolderData({ id: 23, userId: 3 }),
        ]));

        expect(appState.now.wastebin.page).toEqual({
            list: [3, 7, 23],
            requesting: false,
        });

        const numericKeys = (obj: any): number[] => Object.keys(obj).map(k => parseInt(k, 10));

        expect(numericKeys(appState.now.entities.page)).toEqual([3, 7, 23]);
        expect(numericKeys(appState.now.entities.user)).toEqual([1, 3]);
    });

    it('fetchItemsError works', () => {
        appState.mockState({
            wastebin: {
                folder: {
                    list: [],
                    requesting: true,
                },
                lastError: undefined,
            },
        });

        const errorMessage = 'Some error occurred';
        appState.dispatch(new WasteBinItemsFetchingErrorAction('folder', errorMessage));

        expect(appState.now.wastebin.folder.requesting).toBe(false);
        expect(appState.now.wastebin.lastError).toBe(errorMessage);
    });

    it('deleteFromWastebinStart works', () => {
        appState.mockState({
            wastebin: {
                page: {
                    list: [1, 2, 3, 4, 5],
                    requesting: false,
                },
            },
        });

        appState.dispatch(new StartWasteBinItemsDeletionAction('page', [2, 3, 4]));
        expect(appState.now.wastebin.page.requesting).toBe(true);
        expect(appState.now.wastebin.page.list).toEqual([1, 5]);
    });

    it('deleteFromWastebinSuccess works', () => {
        appState.mockState({
            wastebin: {
                folder: {
                    list: [1, 2, 3, 4, 5],
                    requesting: true,
                },
            },
        });

        appState.dispatch(new WasteBinItemsDeletionSuccessAction('folder', [2, 3]));

        expect(appState.now.wastebin.folder).toEqual({
            list: [1, 4, 5],
            requesting: false,
        });
    });

    it('deleteFromWastebinError works', () => {
        appState.mockState({
            wastebin: {
                folder: {
                    list: [1, 2, 3, 4, 5],
                    requesting: true,
                },
            },
        });

        const errorMessage = 'Some error occurred';
        appState.dispatch(new WasteBinItemsDeletionErrorAction('folder', [6, 7], errorMessage));

        expect(appState.now.wastebin.folder.requesting).toBe(false);
        expect(appState.now.wastebin.folder.list).toEqual([1, 2, 3, 4, 5, 6, 7]);
        expect(appState.now.wastebin.lastError).toBe(errorMessage);
    });

    it('restoreItemsFromWastebin works', () => {
        appState.mockState({
            wastebin: {
                folder: {
                    list: [1, 2, 3, 4, 5],
                    requesting: true,
                },
            },
        });

        appState.dispatch(new RestoreWasteBinItemsAction('folder', [2, 3]));

        expect(appState.now.wastebin.folder.list).toEqual([1, 4, 5]);
    });

});

import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ApplicationStateService, STATE_MODULES } from '@editor-ui/app/state';
import { Favourite, FavouriteWithDisplayDetails } from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { EntityResolver } from '../entity-resolver/entity-resolver';
import { UserSettingsService } from '../user-settings/user-settings.service';
import { FavouritesService } from './favourites.service';
import { AuthenticationModule } from '@gentics/cms-components/auth';

describe('FavouritesService', () => {

    let service: FavouritesService;
    let state: TestApplicationState;
    let userSettings: MockUserSettingsService;
    let exampleStarrable: Omit<Favourite, 'nodeId'>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                NgxsModule.forRoot(STATE_MODULES),
                AuthenticationModule.forRoot(),
            ],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: UserSettingsService, useClass: MockUserSettingsService },
                EntityResolver,
                FavouritesService,
            ],
        });

        userSettings = TestBed.inject(UserSettingsService) as any;
        state = TestBed.inject(ApplicationStateService) as any;
        service = TestBed.inject(FavouritesService);

        state.mockState({ auth: { user: { id: 123 } as any } });

        exampleStarrable = { type: 'folder', id: 17, globalId: 'some-folder-id', name: 'Folder 17' };
    });

    describe('list$', () => {
        it('cleans up its subscriptions when destroyed', () => {
            const subscription = service.list$.subscribe();
            subscription.unsubscribe();
            service.ngOnDestroy();
        });

        it('is a subscription to the app state when subscribed to', fakeAsync(() => {
            const exampleItem: Favourite = { nodeId: 7, name: 'My folder', type: 'folder', id: 5, globalId: 'TestID' };
            state.mockState({ favourites: { list: [exampleItem] } });

            let received: any;
            const subscription = service.list$.subscribe(val => { received = val; });
            tick(500);
            expect(received).toEqual([jasmine.objectContaining(exampleItem)]);

            subscription.unsubscribe();
        }));

        it('adds the name of the node to the favourites item', fakeAsync(() => {
            const exampleItem: Favourite = { nodeId: 7, name: 'My folder', type: 'folder', id: 5, globalId: 'TestID' };
            state.mockState({
                entities: {
                    node: {
                        7: { id: 7, name: 'Node #7' },
                    },
                },
                favourites: {
                    list: [exampleItem],
                    loaded: true,
                },
            });

            let received: FavouriteWithDisplayDetails[];
            const subscription = service.list$.subscribe(val => { received = val; });
            tick(500);
            expect(received.length).toBeGreaterThan(0);
            expect(received[0].nodeName).toBe('Node #7');

            subscription.unsubscribe();
        }));

    });

    describe('getList()', () => {

        it('returns the "favourites.list" branch of the app state', () => {
            const fav: Favourite = { name: 'My folder', type: 'folder', id: 17, globalId: 'folder-17', nodeId: 1 };
            state.mockState({ favourites: { list: [fav] } });
            expect(service.getList()).toEqual([fav]);
        });

    });

    describe('add()', () => {

        it('updates the app state via favourites.add', async () => {
            await service.add([exampleStarrable], { nodeId: 7 });
            expect(state.now.favourites.list).toEqual([{ ...exampleStarrable, nodeId: 7 } as any]);
        });

        it('calls favourites.add regardless of the input', async () => {
            await service.add([exampleStarrable], { nodeId: 7 });
            await service.add([exampleStarrable], { nodeId: 7 });
            await service.add([]);
            await service.add([]);
            expect(state.now.favourites.list).toEqual([{ ...exampleStarrable, nodeId: 7 } as any]);
        });

        it('works when no value was set before', () => {
            expect(() => service.add([exampleStarrable], { nodeId: 7 })).not.toThrow();
        });

        it('stores the result in the user storage', async () => {
            state.mockState({ folder: { activeNode: 1 } });
            expect(userSettings.saveFavourites).not.toHaveBeenCalled();

            await service.add([exampleStarrable]);

            expect(state.now.favourites.list).toEqual([{ ...exampleStarrable, nodeId: 1 } as any]);
            expect(userSettings.saveFavourites)
                .toHaveBeenCalledWith([{ ...exampleStarrable, nodeId: 1 }]);
        });

        it('adds the current node id to the saved data if none is provided', async () => {
            state.mockState({
                entities: {
                    node: {
                        10: { id: 10, name: 'Node number 10' },
                    },
                },
                folder: {
                    activeNode: 10,
                },
            });

            expect('nodeId' in exampleStarrable).toBe(false);
            await service.add([exampleStarrable]);

            expect(state.now.favourites.list).toEqual([{ ...exampleStarrable, nodeId: 10 } as any]);
            expect(userSettings.saveFavourites)
                .toHaveBeenCalledWith([{ ...exampleStarrable, nodeId: 10 }]);
        });

        it('keeps the node id if one is provided', async () => {
            state.mockState({
                entities: {
                    node: {
                        1: { id: 1, name: 'One' },
                        10: { id: 10, name: 'Ten' },
                    },
                },
                folder: {
                    activeNode: 1,
                },
            });

            expect('nodeId' in exampleStarrable).toBe(false);
            await service.add([exampleStarrable], { nodeId: 10 });

            expect(state.now.favourites.list).toEqual([{ ...exampleStarrable, nodeId: 10 } as any]);
            expect(userSettings.saveFavourites)
                .toHaveBeenCalledWith([{ ...exampleStarrable, nodeId: 10 }]);
        });

        it('keeps the node id if one is in the provided favourite', async () => {
            state.mockState({
                entities: {
                    node: {
                        1: { id: 1, name: 'One' },
                        10: { id: 10, name: 'Ten' },
                    },
                },
                folder: {
                    activeNode: 1,
                },
            });

            const item = Object.assign({}, exampleStarrable, { nodeId: 10 });
            expect('nodeId' in item).toBe(true);
            await service.add([item]);

            expect(state.now.favourites.list).toEqual([{ ...exampleStarrable, nodeId: 10 } as any]);
            expect(userSettings.saveFavourites)
                .toHaveBeenCalledWith([{ ...exampleStarrable, nodeId: 10 }]);
        });
    });

    describe('remove()', () => {

        it('updates the app state via favourites.remove', async () => {
            const fav: Favourite = { nodeId: 7, type: 'folder', id: 17, name: 'myfolder', globalId: 'SomeGlobalID' };
            state.mockState({
                favourites: {
                    list: [fav],
                },
            });

            await service.remove([fav]);
            const currentState = state.now.favourites;
            expect(currentState.list).toEqual([]);
        });

        it('works when no value was set before', () => {
            expect(() => {
                service.remove([{ nodeId: 7, type: 'folder', id: 17, name: 'myfolder', globalId: 'SomeGlobalID' }]);
            }).not.toThrow();
        });

        it('stores the result in the user settings', async () => {
            expect(userSettings.saveFavourites).not.toHaveBeenCalled();
            const itemToDelete: Favourite = { nodeId: 7, type: 'folder', id: 17, name: 'some name', globalId: 'GlobalIdOne' };
            const remainingItem: Favourite = { nodeId: 7, type: 'page', id: 999, name: 'remaining', globalId: 'GlobalIdTwo' };
            const initialState = [remainingItem, itemToDelete];
            state.mockState({ favourites: { list: initialState } });

            await service.remove([itemToDelete]);
            expect(userSettings.saveFavourites).toHaveBeenCalledWith([remainingItem]);
        });

    });

    describe('reorder()', () => {

        it('updates the app state via favourites.reorder', async () => {
            const folderFav: Favourite = { nodeId: 7, type: 'folder', id: 17, name: 'a folder', globalId: 'global-folder-id' };
            const pageFav: Favourite = { nodeId: 8, type: 'page', id: 65, name: 'a page', globalId: 'global-page-id' };

            state.mockState({
                favourites: {
                    list: [
                        pageFav,
                        folderFav,
                    ],
                },
            });

            await service.reorder([
                folderFav,
                pageFav,
            ]);

            expect(state.now.favourites.list).toEqual([
                folderFav,
                pageFav,
            ]);
        });

    });

});

class MockUserSettingsService implements Partial<UserSettingsService> {
    constructor() {
        spyOn(this as any, 'saveFavourites');
    }
    saveFavourites(favourites: Favourite[]): Promise<void> {
        return Promise.resolve();
    }
}

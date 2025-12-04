import { TestBed } from '@angular/core/testing';
import { Favourite } from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';
import { FavouritesState } from '../../../common/models';
import { ApplicationStateService } from '../../providers';
import { TestApplicationState } from '../../test-application-state.mock';
import { STATE_MODULES } from '../state-modules';
import { AddFavouritesAction, FavouritesLoadedAction, RemoveFavouritesAction, ReorderFavouritesAction } from './favourites.actions';

describe('FavouritesStateModule', () => {

    let appState: TestApplicationState;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
            ],
        });
        appState = TestBed.inject(ApplicationStateService) as any;
    });

    it('sets the correct initial state', () => {
        expect(appState.now.favourites).toEqual({
            list: [],
            loaded: false,
        } as FavouritesState);
    });

    it('loaded works', () => {
        const favs: Favourite[] = [
            {
                globalId: '554F.c3e0184e-b579-11e6-ac1d-0242ac120003',
                id: 61,
                name: 'Some folder',
                nodeId: 4,
                type: 'folder',
            },
            {
                globalId: '554F.1dfe20a2-b6f4-11e6-ac1d-0242ac120003',
                id: 120,
                name: 'Some page',
                nodeId: 4,
                type: 'page',
            },
        ];

        appState.dispatch(new FavouritesLoadedAction(favs));
        expect(appState.now.favourites.loaded).toBe(true);
        expect(appState.now.favourites.list).toEqual(favs);
    });

    it('add works', () => {
        const initialFav: Favourite = {
            globalId: '554F.c3e0184e-b579-11e6-ac1d-0242ac120003',
            id: 61,
            name: 'Some folder',
            nodeId: 4,
            type: 'folder',
        };

        appState.mockState({
            favourites: {
                list: [initialFav],
                loaded: true,
            },
        });

        const favToAdd: Favourite = {
            globalId: '554F.1dfe20a2-b6f4-11e6-ac1d-0242ac120003',
            id: 120,
            name: 'Some page',
            nodeId: 4,
            type: 'page',
        };
        appState.dispatch(new AddFavouritesAction([favToAdd]));

        expect(appState.now.favourites.list.length).toBe(2);
        expect(appState.now.favourites.list).toEqual([
            initialFav,
            favToAdd,
        ]);
    });

    it('remove works', () => {
        const favToStay: Favourite = {
            globalId: '554F.1dfe20a2-b6f4-11e6-ac1d-0242ac120003',
            id: 120,
            name: 'Some page',
            nodeId: 4,
            type: 'page',
        };

        const favToRemove: Favourite = {
            globalId: '554F.c3e0184e-b579-11e6-ac1d-0242ac120003',
            id: 61,
            name: 'Some folder',
            nodeId: 4,
            type: 'folder',
        };

        appState.mockState({
            favourites: {
                list: [
                    favToRemove,
                    favToStay,
                ],
                loaded: true,
            },
        });

        appState.dispatch(new RemoveFavouritesAction([favToRemove]));
        let currentState = appState.now.favourites;

        expect(currentState.list.length).toBe(1);
        expect(currentState.list).toEqual([
            favToStay,
        ]);

        // Removing a favourite from a different node
        appState.dispatch(new RemoveFavouritesAction([{
            globalId: '554F.different-id-11e6-ac1d-0242ac120003',
            id: 61,
            name: 'Some folder',
            nodeId: 999999999999,
            type: 'folder',
        }]));

        expect(appState.now.favourites.list.length).toBe(1, 'removed favourite from different node');
    });

    it('reorder works (by reference)', () => {
        const fav1: Favourite = {
            globalId: '554F.c3e0184e-b579-11e6-ac1d-0242ac120003',
            id: 61,
            name: 'Some folder',
            nodeId: 4,
            type: 'folder',
        };
        const fav2: Favourite = {
            globalId: '554F.1dfe20a2-b6f4-11e6-ac1d-0242ac120003',
            id: 120,
            name: 'Some page',
            nodeId: 4,
            type: 'page',
        };
        const fav3: Favourite = {
            globalId: '554F.592d00f3-bac5-11e6-b8f6-0242ac120003',
            id: 188,
            name: 'example-image.jpg',
            nodeId: 4,
            type: 'image',
        };

        appState.mockState({
            favourites: {
                list: [fav1, fav2, fav3],
                loaded: true,
            },
        });

        appState.dispatch(new ReorderFavouritesAction([fav2, fav1]));

        expect(appState.now.favourites.list.map(fav => fav.id)).toEqual([120, 61, 188]);
        expect(appState.now.favourites.list).toEqual([fav2, fav1, fav3]);
    });


    it('reorder works (by value)', () => {
        const folderFav: Favourite = {
            globalId: '554F.c3e0184e-b579-11e6-ac1d-0242ac120003',
            id: 61,
            name: 'Some folder',
            nodeId: 4,
            type: 'folder',
        };
        const pageFav: Favourite = {
            globalId: '554F.1dfe20a2-b6f4-11e6-ac1d-0242ac120003',
            id: 120,
            name: 'Some page',
            nodeId: 4,
            type: 'page',
        };
        const imageFav: Favourite = {
            globalId: '554F.592d00f3-bac5-11e6-b8f6-0242ac120003',
            id: 188,
            name: 'example-image.jpg',
            nodeId: 4,
            type: 'image',
        };

        appState.mockState({
            favourites: {
                list: [
                    folderFav,
                    pageFav,
                    imageFav,
                ],
                loaded: true,
            },
        });

        appState.dispatch(new ReorderFavouritesAction([pageFav, folderFav]));

        expect(appState.now.favourites.list).toEqual([
            pageFav,
            folderFav,
            imageFav,
        ]);
    });
});

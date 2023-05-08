import { Injectable } from '@angular/core';
import { Favourite } from '@gentics/cms-models';
import { StateContext } from '@ngxs/store';
import { FavouritesState } from '../../../common/models';
import { ActionDefinition, AppStateBranch, concatUnique, removeEntries } from '../../state-utils';
import {
    AddFavouritesAction,
    FAVOURITES_STATE_KEY,
    FavouritesLoadedAction,
    RemoveFavouritesAction,
    ReorderFavouritesAction,
} from './favourites.actions';

const hashFavourite = (fav: Favourite): string => `${fav.nodeId}-${fav.id}-${fav.type}`;

const INITIAL_FAVOURITES_STATE: FavouritesState = {
    list: [],
    loaded: false,
};

@AppStateBranch<FavouritesState>({
    name: FAVOURITES_STATE_KEY,
    defaults: INITIAL_FAVOURITES_STATE,
})
@Injectable()
export class FavouritesStateModule {

    @ActionDefinition(FavouritesLoadedAction)
    handleFavouritesLoadedAction(ctx: StateContext<FavouritesState>, action: FavouritesLoadedAction): void {
        ctx.patchState({
            list: action.favourites.slice(),
            loaded: true,
        });
    }

    @ActionDefinition(AddFavouritesAction)
    handleAddFavouritesAction(ctx: StateContext<FavouritesState>, action: AddFavouritesAction): void {
        const state = ctx.getState();
        const newList = concatUnique(state.list, action.favourites, hashFavourite);

        ctx.patchState({
            list: newList,
        });
    }

    @ActionDefinition(RemoveFavouritesAction)
    handleRemoveFavouritesAction(ctx: StateContext<FavouritesState>, action: RemoveFavouritesAction): void {
        const state = ctx.getState();
        const newList = removeEntries(state.list, action.favourites, hashFavourite);

        ctx.patchState({
            list: newList,
        });
    }

    @ActionDefinition(ReorderFavouritesAction)
    handleReorderFavouritesAction(ctx: StateContext<FavouritesState>, action: ReorderFavouritesAction): void {
        const state = ctx.getState();
        const oldFavourites = state.list;
        const oldEntries: { [k: string]: number } = {};
        oldFavourites.forEach((fav, index) => {
            let hashed = hashFavourite(fav);
            oldEntries[hashed] = index;
        });

        const used: { [index: number]: boolean } = {};
        let changed = false;

        // Reuse existing objects whenever possible
        const newList: Favourite[] = action.favourites.map((newFav, index) => {
            let oldIndex = oldFavourites.indexOf(newFav);
            if (oldIndex >= 0) {
                used[oldIndex] = true;
                changed = changed || oldIndex !== index;
                return oldFavourites[oldIndex];
            }

            oldIndex = oldEntries[hashFavourite(newFav)];
            if (oldIndex != null) {
                used[oldIndex] = true;
                changed = changed || oldIndex !== index;
                return oldFavourites[oldIndex];
            }

            changed = true;
            return newFav;
        });

        if (changed || newList.length < oldFavourites.length) {
            changed = true;
            newList.push(...oldFavourites.filter((fav, index) => !used[index]));
        }

        if (changed || newList.some((fav, index) => oldFavourites[index] !== fav)) {
            ctx.patchState({
                list: newList,
            });
        }
    }
}

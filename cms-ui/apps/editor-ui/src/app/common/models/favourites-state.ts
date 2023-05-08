import { Favourite } from '@gentics/cms-models';

export interface FavouritesState {
    list: Favourite[];
    loaded: boolean;
}

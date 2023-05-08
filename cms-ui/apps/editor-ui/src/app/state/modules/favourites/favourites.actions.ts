import { Favourite } from '@gentics/cms-models';
import { AppState } from '../../../common/models';
import { ActionDeclaration } from '../../state-utils';

export const FAVOURITES_STATE_KEY: keyof AppState = 'favourites';

@ActionDeclaration(FAVOURITES_STATE_KEY)
export class FavouritesLoadedAction {
    constructor(
        public favourites: Favourite[],
    ) {}
}

@ActionDeclaration(FAVOURITES_STATE_KEY)
export class AddFavouritesAction {
    constructor(
        public favourites: Favourite[],
    ) {}
}

@ActionDeclaration(FAVOURITES_STATE_KEY)
export class RemoveFavouritesAction {
    constructor(
        public favourites: Favourite[],
    ) {}
}

@ActionDeclaration(FAVOURITES_STATE_KEY)
export class ReorderFavouritesAction {
    constructor(
        public favourites: Favourite[],
    ) {}
}

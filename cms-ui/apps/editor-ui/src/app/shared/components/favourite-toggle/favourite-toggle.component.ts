import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { Item } from '@gentics/cms-models';
import { FavouritesService } from '../../../core/providers/favourites/favourites.service';
import { ApplicationStateService } from '../../../state';

/**
 * A star icon for toggling an item's favorite status.
 */
@Component({
    selector: 'favourite-toggle',
    templateUrl: './favourite-toggle.tpl.html',
    styleUrls: ['./favourite-toggle.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
    })
export class FavouriteToggle {

    @Input()
    item: Item | Item[];

    constructor(
        private appState: ApplicationStateService,
        private favourites: FavouritesService,
    ) { }

    add(item: Item): void {
        let items: Item[] = (Array.isArray(item) ? item : [item]) as any;
        this.favourites.add(items);
    }

    remove(item: Item): void {
        let items: Item[] = (Array.isArray(item) ? item : [item]) as any;
        const nodeId = this.appState.now.folder.activeNode;
        this.favourites.remove(items, { nodeId });
    }
}

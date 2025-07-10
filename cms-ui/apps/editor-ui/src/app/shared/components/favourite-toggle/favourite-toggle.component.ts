import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { Item } from '@gentics/cms-models';
import { FavouritesService } from '../../../core/providers/favourites/favourites.service';
import { ApplicationStateService } from '../../../state';

/**
 * A star icon for toggling an item's favorite status.
 */
@Component({
    selector: 'favourite-toggle',
    templateUrl: './favourite-toggle.component.html',
    styleUrls: ['./favourite-toggle.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class FavouriteToggleComponent {

    @Input()
    public item: Item | Item[];

    constructor(
        private appState: ApplicationStateService,
        private favourites: FavouritesService,
    ) { }

    public toggle(item: Item, active: boolean): void {
        if (active) {
            this.remove(item);
        } else {
            this.add(item);
        }
    }

    add(item: Item): void {
        const items: Item[] = (Array.isArray(item) ? item : [item]) as any;
        this.favourites.add(items);
    }

    remove(item: Item): void {
        const items: Item[] = (Array.isArray(item) ? item : [item]) as any;
        const nodeId = this.appState.now.folder.activeNode;
        this.favourites.remove(items, { nodeId });
    }
}

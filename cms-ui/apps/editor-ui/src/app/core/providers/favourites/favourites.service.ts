import { Injectable, OnDestroy } from '@angular/core';
import { ApplicationStateService, AddFavouritesAction, RemoveFavouritesAction, ReorderFavouritesAction } from '@editor-ui/app/state';
import { Favourite, FavouriteWithDisplayDetails, Starrable } from '@gentics/cms-models';
import { Observable, Subscription, combineLatest } from 'rxjs';
import { debounceTime, map } from 'rxjs/operators';
import { EntityResolver } from '../entity-resolver/entity-resolver';
import { UserSettingsService } from '../user-settings/user-settings.service';

/**
 * A service that provides a list of the user's favourites, persisted to LocalStorage.
 */
@Injectable()
export class FavouritesService implements OnDestroy {

    /**
     * Returns the favourites list in the app store @see {@link AppStore}.
     *
     * If items with the same name and type are added from different nodes,
     * allow to distinguish between them by their node name.
     */
    get list$(): Observable<FavouriteWithDisplayDetails[]> { return this.favList$; }
    private favList$: Observable<FavouriteWithDisplayDetails[]>;

    private changeSubscription: Subscription;
    private userSubscription: Subscription;
    private userId: number;

    constructor(
        private userSettings: UserSettingsService,
        private appState: ApplicationStateService,
        private entityResolver: EntityResolver,
    ) {

        // Add node names to the favourite objects
        this.favList$ = combineLatest([
            appState.select(state => state.favourites.list),
            appState.select(state => state.entities),
        ]).pipe(
            debounceTime(50),
            map(([favourites, nodes]) => {
                return favourites.map(fav => {
                    const node = entityResolver.getNode(fav.nodeId);
                    const entity = this.entityResolver.getEntity(fav.type, fav.id);
                    let breadcrumbs = this.entityToBreadcrumb(entity);
                    const nodeName = node ? node.name : '';
                    return Object.assign({}, fav, { nodeName, breadcrumbs });
                });
            }),
        );

        this.userSubscription = appState
            .select(state => state.auth.currentUserId)
            .subscribe(userId => this.userId = userId);
    }

    entityToBreadcrumb(entity: any): string[] {
        let breadcrumbs: string[] = [];
        if (entity && entity.path) {
            const entityPath = entity.path.split('/');
            entityPath.pop();
            entityPath.shift();
            if (entity.type && entity.type === 'folder') {
                entityPath.pop();
            }
            breadcrumbs = entityPath;
        }
        return breadcrumbs;
    }

    ngOnDestroy(): void {
        if (this.changeSubscription) {
            this.changeSubscription.unsubscribe();
        }

        if (this.userSubscription) {
            this.userSubscription.unsubscribe();
        }
    }

    /**
     * Returns the current state of the favourites list in the app store.
     *
     * @see {@link AppStore}
     */
    getList(): Favourite[] {
        return this.appState.now.favourites.list;
    }

    /**
     * Add items to the user's favourites.
     */
    add(items: Starrable[], nodeObj?: { nodeId: number }): void {
        let nodeId: number = nodeObj
            ? nodeObj.nodeId
            : this.appState.now.folder.activeNode;

        let toAdd = items.map((item: any) => (<Favourite> {
            id: item.id,
            type: item.type,
            name: item.name,
            globalId: item.globalId,
            nodeId: !nodeObj && item.nodeId || nodeId,
        }));

        this.appState.dispatch(new AddFavouritesAction(toAdd));

        let favsNow = this.appState.now.favourites.list;
        this.userSettings.saveFavourites(favsNow);
    }

    /**
     * Remove items from the user's favourites.
     */
    remove(items: Favourite[]): void;
    remove(items: Starrable[], nodeInfo: { nodeId: number }): void;
    remove(items: Starrable[], nodeInfo?: { nodeId: number }): void {
        let toRemove: Favourite[];

        if (nodeInfo) {
            let nodeId = nodeInfo.nodeId;
            toRemove = items.map(item => ({
                ...item,
                nodeId,
            } as Favourite));
        } else {
            toRemove = items as Favourite[];
        }

        this.appState.dispatch(new RemoveFavouritesAction(toRemove));

        let favsNow = this.appState.now.favourites.list;
        this.userSettings.saveFavourites(favsNow);
    }

    reorder(reorderedFavourites: Favourite[]): void {
        this.appState.dispatch(new ReorderFavouritesAction(reorderedFavourites));
        let favsNow = this.appState.now.favourites.list;
        this.userSettings.saveFavourites(favsNow);
    }
}

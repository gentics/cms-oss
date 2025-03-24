import { BO_ID, BusinessObject, TrableRowReloadOptions } from '@admin-ui/common';
import { TrableRow } from '@gentics/ui-core';
import { BehaviorSubject, forkJoin, Observable, of } from 'rxjs';
import { map, skip, switchMap } from 'rxjs/operators';

/**
 * Base class for entity/type specific implementations to load and manage trable-components.
 */
export abstract class BaseTrableLoaderService<T, O = T & BusinessObject, A = never> {

    /**
     * Subject which should be triggered whenever a full reload of the trable should be performed.
     * Actual implementation of the reload is done in the `BaseEntityTrableComponent` or of the
     * entity specific implementation (override).
     */
    private reloadSubject = new BehaviorSubject<void>(null);

    public reload$ = this.reloadSubject.asObservable().pipe(
        skip(1),
    );

    /**
     * Function to load the children of the specified entity.
     *
     * @param parent The parent of which the children should be loaded from. May be null, which indicates it should load the root elements.
     * @param options The options which are to be used when loading.
     */
    protected abstract loadEntityChildren(parent: O | null, options?: A): Observable<O[]>;

    /**
     * Function to load the specified entity (usually for reloading).
     * Does not have to include the children information, as they are already fetched via `loadEntityChildren`.
     *
     * @param id The ID (`BO_ID`) of the entity to load.
     * @param options The options which are to be used when loading.
     */
    protected abstract loadEntityRow(entity: O, options?: A): Observable<O>;

    /**
     * Basic hook to create a hash for a entity when it's loaded to determine it's state.
     * Used for trable updates to determine if the row changed or not.
     * Returns `null` without any actual implementation/override from the entity-specific loader.
     *
     * @param entity The entity for which the hash should be created.
     * @returns A hash of the entity or `null` if it isn't needed/supported.
     */
    public createRowHash(entity: O): string | null {
        return null;
    }

    /**
     * Hook to determine if a entity (can) have children or not.
     *
     * @param entity Entity to check if it has children or not.
     * @param options The options which were used to load this entity.
     * @returns If the entity has children to load.
     */
    protected hasChildren(entity: O, options?: A): boolean {
        return true;
    }

    /**
     * Hook to determine if a entity can be selected or not.
     *
     * @param entity Entity to check if it can be selected in the trable.
     * @param options The options which were used to load this entity.
     * @returns If the entity is supposed to be selectable in the trable.
     */
    protected canBeSelected(entity: O, options?: A): boolean {
        return true;
    }

    /**
     * Reloads the specified row by updating the row state and updating the item on success.
     *
     * @param row The row to reload.
     * @param options The options which will be forwarded to `loadEntityRow` as parameters.
     * @param reloadOptions Options for how to reload the row.
     * @returns An Observable which returns the row, updated with the new value (does it in place/per reference).
     */
    public reloadRow(row: TrableRow<O>, options?: A, reloadOptions?: TrableRowReloadOptions): Observable<TrableRow<O>> {
        return of(null).pipe(
            switchMap(() => this.loadEntityRow(row.item, options)),
            switchMap(loadedEntity => {
                const newRow = this.mapToTrableRow(loadedEntity, row.parent, options);
                // Copy state from original row
                newRow.expanded = row.expanded;
                newRow.loaded = row.loaded;
                newRow.children = row.children;
                newRow.hasChildren = row.hasChildren;

                // If the row has been loaded, then the descendants reload can be done
                if (row.loaded && reloadOptions?.reloadDescendants) {
                    return this.reloadDescendants(newRow, options);
                }

                return of(newRow);
            }),
        );
    }

    public reloadDescendants(row: TrableRow<O>, options?: A): Observable<TrableRow<O>> {
        // If it has no children, or wasn't loaded yet, we can simply skip this
        if (!row.hasChildren || !row.loaded) {
            return of(row);
        }

        return of(null).pipe(
            switchMap(() => this.loadEntityChildren(row.item, options)),
            switchMap(newChildren => {
                const newRow = this.mapToTrableRow(row.item, row.parent, options);
                const newChildRows = newChildren.map(child => this.mapToTrableRow(child, row, options));
                const newChildIds = new Set(newChildRows.map(child => child.id));
                const oldChildMap: Record<string, TrableRow<O>> = {};

                if (row.children) {
                    for (const child of row.children) {
                        if (newChildIds.has(child.id)) {
                            oldChildMap[child.id] = child;
                        }
                    }
                }

                const toLoad: TrableRow<O>[] = [];
                newRow.children = newChildRows.map(childRow => {
                    const old = oldChildMap[childRow.id];
                    if (old) {
                        // Restore the state of the old row
                        childRow.expanded = old.expanded;
                        childRow.loaded = old.loaded;
                        childRow.children = old.children;
                    }

                    if (childRow.loaded) {
                        toLoad.push(childRow);
                    }

                    return childRow;
                });

                newRow.loaded = true;
                newRow.hasChildren = newRow.children.length > 0;
                newRow.expanded = row.expanded;
                newRow.loading = false;

                // If none of the children need further loading, then we're done
                if (toLoad.length === 0) {
                    return of(newRow);
                }

                // Recursively load the descendant data otherwise
                return forkJoin(toLoad.map(child => this.reloadDescendants(child, options))).pipe(
                    map(refreshedChildren => {
                        const map = refreshedChildren.reduce((acc, child) => {
                            acc[child.id] = child;
                            return acc;
                        }, {});
                        newRow.children = newRow.children.map(child => map[child.id] ? map[child.id] : child);

                        return newRow;
                    }),
                );
            }),
        );
    }

    /**
     * Loads the children of a row and sets them correctly into the row.
     * Also updates the state of the row an properly manages the relationship.
     *
     * @param row The row from which the children should be loaded from.
     * @param options The options which are forwarded to `loadEntityChildren` when loading the children.
     * @returns An Observable which emits one or multiple children which have been loaded.
     */
    public loadRowChildren(row: TrableRow<O> | null, options?: A): Observable<TrableRow<O>[]> {
        return of(null).pipe(
            switchMap(() => this.loadEntityChildren(row?.item, options)),
            map(children => children.map(child => this.mapToTrableRow(child, row, options))),
        );
    }

    /**
     * Trigger a reload for all trables which are currently active and use this trable loader.
     */
    public reload(): void {
        this.reloadSubject.next(null);
    }

    /**
     * Maps the provided entity to a TrableRow to be displayed in a trable component.
     *
     * @param entity The entity/item to map to a row
     * @param parent The parent of the row, or absent when it's a root element.
     * @param options The additional options used to load this entity
     * @returns A trable-row with the entity stored as item and all data resolved to be able to display the row correctly.
     */
    protected mapToTrableRow(entity: O, parent?: TrableRow<O>, options?: A): TrableRow<O> {
        return {
            id: entity[BO_ID],
            item: entity,
            expanded: false,
            selectable: this.canBeSelected(entity, options),
            hasChildren: this.hasChildren(entity, options),
            level: parent == null ? 0 : parent.level + 1,
            hash: this.createRowHash(entity),
            loading: false,
            loaded: false,
            children: [],
            parent,
        }
    }
}

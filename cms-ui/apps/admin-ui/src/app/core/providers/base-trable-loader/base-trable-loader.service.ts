import { BO_ID, BusinessObject } from '@admin-ui/common';
import { TrableRow } from '@gentics/ui-core';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { map, switchMap, skip, tap } from 'rxjs/operators';

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
    /**
     * Subject which should be triggered whenever the view of the trable should be refreshed.
     * This is usefull when reloading/modifying a row purely via this service.
     */
    private refreshViewSubject = new BehaviorSubject<void>(null);

    public reload$ = this.reloadSubject.asObservable().pipe(
        skip(1),
    );

    public refreshView$ = this.refreshViewSubject.asObservable().pipe(
        skip(1),
    );

    /**
     * Store which keeps all loaded rows in memory for later lookups.
     */
    public flatStore: { [id: string]: TrableRow<O> } = {};

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
     * @param refreshOnLoad If this should trigger the `refreshView$` observable when the row is updated.
     * (Once on the beginning, setting it to loading, and once at the end with the updated item and new state).
     * @returns An Observable which returns the row, updated with the new value (does it in place/per reference).
     */
    public reloadRow(row: TrableRow<O>, options?: A, refreshOnLoad: boolean = false): Observable<TrableRow<O>> {
        return of(null).pipe(
            tap(() => {
                if (this.flatStore[row.id]) {
                    this.flatStore[row.id].loading = true;
                }
                row.loading = true;
                if (refreshOnLoad) {
                    this.refreshView();
                }
            }),
            switchMap(() => this.loadEntityRow(row.item, options)),
            map(loadedEntity => {
                row.item = loadedEntity;
                row.loading = false;
                row.hash = this.createRowHash(loadedEntity);
                this.flatStore[row.id] = row;
                if (refreshOnLoad) {
                    this.refreshView();
                }
                return row;
            }),
        );
    }

    /**
     * Loads the children of a row and sets them correctly into the row.
     * Also updates the state of the row an properly manages the relationship.
     *
     * @param row The row from which the children should be loaded from.
     * @param options The options which are forwarded to `loadEntityChildren` when loading the children.
     * @param refreshOnLoad If this should trigger the `refreshView$` observable when the children are loaded.
     * @returns An Observable which emits one or multiple children which have been loaded.
     */
    public loadRowChildren(row: TrableRow<O> | null, options?: A, refreshOnLoad: boolean = false): Observable<TrableRow<O>[]> {
        return of(null).pipe(
            tap(() => {
                if (row) {
                    if (this.flatStore[row.id]) {
                        row = this.flatStore[row.id];
                    }
                    row.loading = true;
                    if (refreshOnLoad) {
                        this.refreshView();
                    }
                }
            }),
            switchMap(() => this.loadEntityChildren(row?.item, options)),
            map(children => children.map(child => this.mapToTrableRow(child, row, options))),
            map(children => {
                children.forEach(child => {
                    this.flatStore[child.id] = child;
                });

                // If a row/parent exists, then update the state of it.
                if (row) {
                    row.children = children;
                    row.hasChildren = row.children?.length > 0;
                    row.loaded = true;
                    row.loading = false;
                    row.expanded = true;
                }

                if (refreshOnLoad) {
                    this.refreshView();
                }

                return children;
            }),
        );
    }

    public getEntityById(entityId: string | number): O {
        return this.flatStore[entityId]?.item;
    }

    public getEntitiesByIds(entityIds: (string | number)[]): O[] {
        return entityIds.map(id => this.flatStore[id]?.item)
            .filter(item => item != null);
    }

    /**
     * Trigger a reload for all trables which are currently active and use this trable loader.
     */
    public reload(): void {
        this.reloadSubject.next(null);
    }

    /**
     * Trigger a view-refresh for all trables which are currently active and use this trable loader.
     */
    public refreshView(): void {
        this.refreshViewSubject.next();
    }

    public deleteFromStore(id: string): string[] {
        const toDelete = new Set<string>();
        toDelete.add(id);
        this.getChildIds(id, toDelete);
        const arr = Array.from(toDelete);

        for (const id of arr) {
            delete this.flatStore[id];
        }

        return arr;
    }

    public resetStore(): void {
        this.flatStore = {};
    }

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

    protected getChildIds(id: string, buffer: Set<string>): void {
        const parent: TrableRow<O> = this.flatStore[id];
        if (parent) {
            buffer.add(parent.id);
        }
        while (parent?.children != null) {
            for (const child of parent.children) {
                this.getChildIds(child.id, buffer);
            }
        }
    }
}

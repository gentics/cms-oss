import { BO_ID, BusinessObject } from '@admin-ui/common';
import { TrableRow } from '@gentics/ui-core';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { map, switchMap, tap, delay } from 'rxjs/operators';

export abstract class BaseTrableLoaderService<T, O = T & BusinessObject, A = never> {

    private reloadSubject = new BehaviorSubject<void>(null);

    public reload$ = this.reloadSubject.asObservable();

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

    public createRowHash(entity: O): string | null {
        return null;
    }

    public reloadRow(row: TrableRow<O>, options?: A): Observable<TrableRow<O>> {
        return of(null).pipe(
            tap(() => {
                if (this.flatStore[row.id]) {
                    this.flatStore[row.id].loading = true;
                }
                row.loading = true;
            }),
            switchMap(() => this.loadEntityRow(row.item, options)),
            map(loadedEntity => {
                row.item = loadedEntity;
                row.loading = false;
                row.hash = this.createRowHash(loadedEntity);
                this.flatStore[row.id] = row;
                return row;
            }),
        );
    }

    public loadRowChildren(row: TrableRow<O> | null, options?: A): Observable<TrableRow<O> | TrableRow<O>[]> {
        return of(null).pipe(
            tap(() => {
                if (row) {
                    if (this.flatStore[row.id]) {
                        row = this.flatStore[row.id];
                    }
                    row.loading = true;
                }
            }),
            switchMap(() => this.loadEntityChildren(row?.item, options)),
            map(children => children.map(child => this.mapToTrableRow(child, row, options))),
            map(children => {
                children.forEach(child => {
                    this.flatStore[child.id] = child;
                });

                if (!row) {
                    return children;
                }

                row.children = children;
                row.hasChildren = row.children?.length > 0;
                row.loaded = true;
                row.loading = false;
                row.expanded = true;
                return row;
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

    protected hasChildren(entity: O, options?: A): boolean {
        return true;
    }

    protected canBeSelected(entity: O, options?: A): boolean {
        return true;
    }

    public reload(): void {
        this.reloadSubject.next(null);
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

    protected getChildIds(id: string, buffer: Set<string>): void {
        let parent: TrableRow<O> = this.flatStore[id];
        if (parent) {
            buffer.add(parent.id);
        }
        while (parent?.children != null) {
            for (const child of parent.children) {
                this.getChildIds(child.id, buffer);
            }
        }
    }

    public resetStore(): void {
        this.flatStore = {};
    }
}

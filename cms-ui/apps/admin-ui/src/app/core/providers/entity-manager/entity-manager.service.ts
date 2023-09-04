import { batchedMap } from '@admin-ui/common';
import { deepFreeze } from '@admin-ui/common/utils/deep-freeze/deep-freeze';
import { InitializableServiceBase } from '@admin-ui/shared/providers/initializable-service-base';
import { AppStateService, SelectState } from '@admin-ui/state';
import { AddEntities, DeleteAllEntitiesInBranch, DeleteEntities } from '@admin-ui/state/entity/entity.actions';
import { EntityStateModel } from '@admin-ui/state/entity/entity.state';
import { Injectable } from '@angular/core';
import {
    AnyModelType,
    ArrayNormalizationResult,
    EntityIdType,
    GcmsNormalizer,
    IndexById,
    NormalizableEntity,
    NormalizableEntityType,
    NormalizableEntityTypesMapBO,
    Normalized,
    Raw,
} from '@gentics/cms-models';
import { debounce as _debounce, values as _values, isEqual } from 'lodash-es';
import { Observable, ReplaySubject, Subject, of as observableOf, throwError } from 'rxjs';
import {
    distinctUntilChanged,
    finalize,
    map,
    multicast,
    refCount,
    switchMap,
    take,
    takeUntil,
    tap,
} from 'rxjs/operators';
import { environment } from '../../../../environments/environment';

interface CachedDenormalizedValue<
    T extends keyof EntityStateModel,
    R extends NormalizableEntityTypesMapBO<Raw>[T],
    E = NormalizableEntityTypesMapBO<Normalized>[T],
> {
    iterationLastUsed: number;
    normalized: E;
    denormalized: R;
}

/**
 * The EntityManagerService is used to
 * - retrieve entities from the AppState using their IDs,
 * - denormalize entities if required,
 * - add new entities to the EntityState, and
 * - delete entities from the EntityState.
 */
@Injectable()
export class EntityManagerService extends InitializableServiceBase {

    @SelectState(state => state.entity)
    protected entityState: Observable<EntityStateModel>;

    protected normalizer = new GcmsNormalizer();

    protected denormalizedEntityBranches = new Map<keyof EntityStateModel, Observable<NormalizableEntity<Raw>[]>>();

    protected nextRequestId = 0;
    protected pendingNormalizations: Map<number, Subject<ArrayNormalizationResult>> = new Map();

    /** Time in ms how long the debounce should be for. */
    protected readonly CLEANUP_DEBOUNCE: number = 200;

    constructor(
        private appState: AppStateService,
    ) {
        super();
    }

    protected onServiceInit(): void {
    }

    protected onServiceDestroy(): void {
    }

    /**
     * Gets an observable for the specified entity that emits
     * whenever the entity changes.
     * @param type The type of entity.
     * @param id The id of the entity.
     */
    getEntity<
        T extends keyof EntityStateModel,
        R extends NormalizableEntityTypesMapBO<Normalized>[T]
    >(type: T, id: EntityIdType): Observable<R> {
        if (!this.appState.now.entity[type]) {
            throw new Error(`The EntityState branch "${type}" does not exist.`);
        }

        return this.entityState.pipe(
            map(allEntities => allEntities[type]),
            distinctUntilChanged(isEqual),
            map(destBranch => destBranch[id] as R),
            distinctUntilChanged(isEqual),
        );
    }

    /**
     * Denormalizes an entity using the entities available in the AppState.
     * @param type The type of entity.
     * @param entity The normalized entity that should be denormalized.
     * For ease of use, also a denormalized or partially denormalized entity may be passed here.
     * @returns The denormalized version of the entity.
     */
    denormalizeEntity<
        T extends keyof EntityStateModel,
        E extends NormalizableEntityTypesMapBO<AnyModelType>[T],
        R extends NormalizableEntityTypesMapBO<Raw>[T]
    >(
        type: T,
        entity: E | null | undefined,
    ): R {
        return this.normalizer.denormalize(type as any, entity, this.appState.now.entity);
    }

    /**
     * Returns an observable, which emits all denormalized entities of a branch and
     * updates when one or more entities change.
     *
     * When one or more entities change, the newly emitted value is again the entire array
     * of denormalized entities, but only the changed entities have been denormalized again.
     *
     * All denormalized entities should be treated as immutable, since they are
     * shared among all subscribers of a certain type.
     *
     * **Important:** If an entity contains references to other entities of the same type,
     * the parent entity is not denormalized again if any of its children change.
     * E.g., suppose we have to pages: `pageA = { id: 1, languageVariants: [ 1, 2 ] }`
     * and `pageB = { id: 2, languageVariants: [ 1, 2 ] }`.
     * If `pageA` changes, the second object in the languageVariants of `pageB` should
     * also change, but currently it does not.
     * We will see if this causes problems, e.g., with groups and subgroups.
     *
     * @param type The type of entity.
     */
    watchDenormalizedEntitiesList<
        T extends keyof EntityStateModel,
        R = NormalizableEntityTypesMapBO<Raw>[T]
    >(
        type: T,
    ): Observable<R[]> {
        let denormalizedBranch$ = this.denormalizedEntityBranches.get(type) as Observable<R[]>;
        if (!denormalizedBranch$) {
            denormalizedBranch$ = this.createBranchDenormalizer(type) as any;
            this.denormalizedEntityBranches.set(type, denormalizedBranch$ as any);
        }
        return denormalizedBranch$;
    }

    /**
     * Returns an observable, which emits all normalized entities of a branch and
     * updates when one or more entities change.
     *
     * When one or more entities change, the newly emitted value is again the entire array.
     *
     * All normalized entities should be treated as immutable, since they are
     * shared among all subscribers of a certain type.
     *
     * @param type The type of entity.
     */
    watchNormalizedEntitiesList<
        T extends keyof EntityStateModel,
        R = NormalizableEntityTypesMapBO<Normalized>[T]
    >(
        type: T,
    ): Observable<R[]> {
        return this.appState.select(state => state.entity[type] as any).pipe(
            map((entities: IndexById<R>) => _values(entities)),
        );
    }

    /**
     * Normalizes the specified rawEntity and adds all resulting normalized entities to the EntityState.
     * @param type The type of entity.
     * @param rawEntity The raw entity.
     */
    addEntity<
        T extends keyof EntityStateModel,
        E extends NormalizableEntityTypesMapBO<Raw>[T],
    >(
        type: T,
        rawEntity: E,
    ): void {
        if (rawEntity) {
            const normalizationResult = this.normalizer.normalize(type, rawEntity);
            this.appState.dispatch(new AddEntities(normalizationResult.entities));
        }
    }

    /**
     * Removes identified entities from the EntityState.
     * @param type The type of entity.
     * @param entityIds The ids referring to entities to be removed.
     */
    deleteEntities(
        type: NormalizableEntityType,
        entityIds: (EntityIdType)[],
    ): void {
        if (Array.isArray(entityIds) && entityIds.length > 0) {
            this.appState.dispatch(new DeleteEntities(type, entityIds));
        }
    }

    /**
     * Removes all entities in specified entity branch.
     * @param type The type of entity.
     */
    deleteAllEntitiesInBranch(
        type: NormalizableEntityType,
    ): void {
        this.appState.dispatch(new DeleteAllEntitiesInBranch(type));
    }

    /**
     * Normalizes the specified rawEntities and adds them to the EntityState.
     *
     * To keep the UI from freezing, entities are normalized by a web worker if
     * the number exceeds a certain threshold. The promise returned by this method resolves when all
     * entities have been normalized and added to the EntityState.
     *
     * We cannot use the batch processing with setTimeout() like we do in the editor UI,
     * because here we are dealing with much larger quantities of entities at a time.
     * Thus using setTimeout() repeatedly would result in a too long waiting time for the user,
     * so we go for the much more efficient web worker.
     *
     * @param type The type of entities in the `rawEntities` array.
     * @param rawEntities The array of raw entities.
     */
    addEntities<
        T extends keyof EntityStateModel,
        E extends NormalizableEntityTypesMapBO<Raw>[T],
    >(
        type: T,
        rawEntities: E[],
    ): Promise<void> {
        if (rawEntities) {
            return new Promise<void>((resolve, reject) => {
                this.addEntitiesInternal(type, rawEntities, resolve, reject);
            });
        } else {
            return Promise.resolve();
        }
    }

    private addEntitiesInternal<
        T extends keyof EntityStateModel,
        E extends NormalizableEntityTypesMapBO<Raw>[T],
    >(
        type: T,
        rawEntities: E[],
        resolveFn: (value?: void | PromiseLike<void>) => void,
        rejectFn: (reason?: any) => void,
    ): void {
        const normalizationResult$ = this.normalizeAsync(type, rawEntities);
        normalizationResult$.pipe(
            take(1),
            takeUntil(this.stopper.stopper$),
        ).subscribe(
            normalizationResult => {
                this.appState.dispatch(new AddEntities(normalizationResult.entities));
                resolveFn();
            },
            error => rejectFn(error),
        );
    }

    protected normalizeAsync<
        T extends keyof EntityStateModel,
        E extends NormalizableEntityTypesMapBO<Raw>[T]
    >(
        type: T,
        rawEntities: E[],
    ): Observable<ArrayNormalizationResult> {
        // If there are not many entities to normalize or
        // if the current platform does not support web workers, normalize synchronously.
        try {
            return observableOf(this.normalizer.normalize(type, rawEntities));
        } catch (error) {
            return throwError(error);
        }
    }

    /**
     * Creates a new denormalizer observable for an entity branch.
     */
    protected createBranchDenormalizer<
        T extends keyof EntityStateModel,
        R extends NormalizableEntityTypesMapBO<Raw>[T],
    >(
        type: T,
    ): Observable<R[]> {
        const denormalizedCache = this.createDenormalizedCache<T, R>();
        let iteration = 0;

        const doCleanup = _debounce((lastIteration: number) => {
            denormalizedCache.forEach((cachedEntry, id) => {
                if (cachedEntry.iterationLastUsed < lastIteration) {
                    denormalizedCache.delete(id);
                }
            });
        }, this.CLEANUP_DEBOUNCE, { trailing: true });

        const denormalizer$ = this.appState.select(state => state.entity[type]).pipe(
            switchMap(entityBranch => {
                ++iteration;
                return this.denormalizeBranchUsingCache(type, entityBranch, iteration, denormalizedCache);
            }),
            tap(() => doCleanup(iteration)),
            finalize(() => {
                // Clear the cache when the last observer unsubscribes to save memory.
                // If someone subscribes again, we will denormalize everything again.
                denormalizedCache.clear();
                doCleanup.cancel();
            }),
            // We cannot use publishReplay() because the ReplaySubject needs to be cleared when the
            // last subscriber unsubscribes.
            multicast(() => new ReplaySubject<R[]>(1)),
            refCount(),
        );

        return denormalizer$;
    }

    /**
     * Creates a cache for storing denormalized entities.
     *
     * This is done in an extra method to aid unit testing.
     */
    protected createDenormalizedCache<
        T extends keyof EntityStateModel,
        R extends NormalizableEntityTypesMapBO<Raw>[T],
    >(): Map<number, CachedDenormalizedValue<T, R>> {
        return new Map<number, CachedDenormalizedValue<T, R>>();
    }

    private denormalizeBranchUsingCache<
        T extends keyof EntityStateModel,
        R extends NormalizableEntityTypesMapBO<Raw>[T],
    >(
        type: T,
        entityBranch: EntityStateModel[T],
        iteration: number,
        cache: Map<number, CachedDenormalizedValue<T, R>>,
    ): Observable<R[]> {
        const ids: number[] = Object.keys(entityBranch) as any[];
        const ids$ = observableOf(ids);

        return ids$.pipe(
            batchedMap(id => {
                const normalized: NormalizableEntityTypesMapBO<Normalized>[T] = entityBranch[id] as any;
                let cachedEntry = cache.get(id);

                if (!cachedEntry) {
                    cachedEntry = { iterationLastUsed: -1, normalized: null, denormalized: null };
                    cache.set(id, cachedEntry);
                }

                if (cachedEntry.normalized !== normalized) {
                    cachedEntry.denormalized = this.denormalizeEntity(type, normalized);
                    cachedEntry.normalized = normalized;

                    // In the debug build freeze the denormalized entity to reveal illegal modifications during testing.
                    if (!environment.production) {
                        deepFreeze(cachedEntry.denormalized);
                    }
                }

                cachedEntry.iterationLastUsed = iteration;
                return cachedEntry.denormalized;
            }),
        );
    }
}

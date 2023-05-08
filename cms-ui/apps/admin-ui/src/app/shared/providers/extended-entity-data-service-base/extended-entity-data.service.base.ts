import { EntityManagerService, ExtendedEntityOperationsBase, I18nNotificationService, I18nService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import {
    NormalizableEntityType,
    NormalizableEntityTypesMapBO,
    Normalized,
    Raw,
} from '@gentics/cms-models';
import { BehaviorSubject, Observable } from 'rxjs';
import {
    map,
    switchMap,
    tap,
} from 'rxjs/operators';
import { EntityGridDataProvider } from '../../../common';
import { EntityDataServiceBase } from '../entity-data-service-base/entity-data.service.base';

/**
 * Superclass for EntityDataService classes for admin entities.
 *
 * @see /architecture/data-loading.png
 */
export abstract class ExtendedEntityDataServiceBase<
    T extends NormalizableEntityType,
    O extends ExtendedEntityOperationsBase<T, T_RAW>,
    T_RAW extends NormalizableEntityTypesMapBO<Raw>[T] = NormalizableEntityTypesMapBO<Raw>[T],
    T_NORM extends NormalizableEntityTypesMapBO<Normalized>[T] = NormalizableEntityTypesMapBO<Normalized>[T],
> extends EntityDataServiceBase<T, O, T_RAW, T_NORM> implements EntityGridDataProvider<T_RAW> {

    public hasLoadedDelay = 250;

    get amountTotal$(): Observable<number> {
        return this.getRawEntitiesFromState().pipe(
            map((entities: T_RAW[]) => entities.length),
        );
    }

    get hasLoaded$(): Observable<boolean> {
        return this._hasLoaded$.asObservable();
    }

    private _hasLoaded$ = new BehaviorSubject<boolean>(false);
    private reload$ = new BehaviorSubject<any>(null);
    private loaderInitialized = false;

    constructor(
        entityIdentifier: T,
        state: AppStateService,
        entityManager: EntityManagerService,
        entityOperations: O,
        notification: I18nNotificationService,
        i18n: I18nService,
    ) {
        super(entityIdentifier, state, entityManager, entityOperations, notification, i18n);
    }

    /**
     * In order to guarantee class construction logic this must be called explicitly.
     *
     * @Note Call from using component
     */
    ensureEntitiesLoaded(options?: any): Observable<boolean> {
        if (!this.loaderInitialized) {
            this.loaderInitialized = true;
            return this.getEntitiesFromApi(options).pipe(
                tap(() => this._hasLoaded$.next(true)),
                switchMap(() => this.hasLoaded$),
            );
        }

        return this.hasLoaded$;
    }

    watchAllEntities(options?: any): Observable<T_RAW[]> {
        return this.reload$.pipe(
            switchMap(reloadOptions => this.getEntitiesFromApi({ ...options, ...reloadOptions })),
            tap(() => {
                this.loaderInitialized = true;
                this._hasLoaded$.next(true);
            }),
        );
    }

    getEntitiesFromApi(options?: any): Observable<T_RAW[]> {
        return this.entityOperations.getAll(options).pipe(
            this.getLoadingOperator(options),
        );
    }

    getRawEntitiesFromState(): Observable<T_RAW[]> {
        return this.ensureEntitiesLoaded().pipe(
            switchMap(() => this.entityManager.watchDenormalizedEntitiesList<T, T_RAW>(this.entityIdentifier) ),
        );
    }

    getNormalizedEntitiesFromState(): Observable<T_NORM[]> {
        return this.ensureEntitiesLoaded().pipe(
            switchMap(() => this.entityManager.watchNormalizedEntitiesList<T, T_NORM>(this.entityIdentifier) ),
        );
    }

    reloadEntities(options?: any): void {
        this.reload$.next(options);
    }

    protected convertDate(unixTimestamp: number): any {
        return unixTimestamp > 0 ? new Date(unixTimestamp * 1000).toLocaleString() as any : null;
    }

}

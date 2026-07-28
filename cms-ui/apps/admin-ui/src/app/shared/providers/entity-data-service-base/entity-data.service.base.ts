import { EntityManagerService, EntityOperationsBase } from '@admin-ui/core';
import {
    AppStateService,
} from '@admin-ui/state';
import { I18nNotificationService } from '@gentics/cms-components';
import {
    EntityIdType,
    NormalizableEntityType,
    NormalizableEntityTypesMapBO,
    Normalized,
    Raw,
} from '@gentics/cms-models';
import { ColorThemes } from '@gentics/ui-core';
import { I18nService } from '@gentics/cms-components';
import { Observable, of, OperatorFunction, throwError } from 'rxjs';
import { catchError, mergeMap, switchMap } from 'rxjs/operators';
import { InitializableServiceBase } from '../initializable-service-base';

/**
 * Superclass for EntityDataService classes.
 *
 * An EntityDataService is intended to be used by a component to fetch entities for
 * display. The service encapsulates the logic used for obtaining the data either from
 * the AppState or the REST API (through an `Operations` service).
 * @see /architecture/data-loading.png
 */
export abstract class EntityDataServiceBase<
    T extends NormalizableEntityType,
    O extends EntityOperationsBase<T, T_RAW>,
    T_RAW extends NormalizableEntityTypesMapBO<Raw>[T] = NormalizableEntityTypesMapBO<Raw>[T],
    T_NORM extends NormalizableEntityTypesMapBO<Normalized>[T] = NormalizableEntityTypesMapBO<Normalized>[T],
> extends InitializableServiceBase {

    constructor(
        public readonly entityIdentifier: T,
        protected state: AppStateService,
        protected entityManager: EntityManagerService,
        protected entityOperations: O,
        protected notification: I18nNotificationService,
        protected i18n: I18nService,
    ) {
        super();
    }

    onServiceInit(): void { }

    /**
     * Gets the ID of the specified `entity`.
     *
     * This method is necessary, because not all entity types use the same property name for their ID.
     */
    abstract getEntityId(entity: T_RAW): EntityIdType;

    /**
     * Gets the RxJS operator that should be used by this service to trigger
     * a loading spinner while performing REST operations.
     */
    protected getLoadingOperator<U>(options?: any): OperatorFunction<U, U> {
        return (source) => source;
    }

    getEntity(id: EntityIdType): T_RAW {
        const entity = this.state.now.entity[this.entityIdentifier][id];
        return entity ? this.entityManager.denormalizeEntity(this.entityIdentifier, entity) : undefined;
    }

    getEntityFromApi(entityId: EntityIdType): Observable<T_RAW> {
        return this.entityOperations.get(entityId);
    }

    /**
     * Try getting entity from state, otherwise fetch
     */
    getEntityFromState(entityId: EntityIdType): Observable<T_NORM | void> {
        const getEntityFromState = (id: EntityIdType): Observable<T_NORM> => {
            return this.state.select((state) => state.entity[this.entityIdentifier][id] as T_NORM);
        };
        return getEntityFromState(entityId).pipe(
            mergeMap((entity: T_NORM) => {
                if (entity !== undefined && entity !== null) {
                    return of(entity);
                } else {
                    // if entity is not in state, fetch it
                    return this.entityOperations.get(entityId).pipe(
                        // if not existing
                        catchError((err) => {
                            // remove from state
                            this.entityManager.deleteEntities(this.entityIdentifier, [entityId]);
                            return throwError(err);
                        }),
                        // if exist, get normalized entity from state
                        switchMap(() => getEntityFromState(entityId)),
                    );
                }
            }),
        );
    }

    protected displayNotificationSuccess(i18nMessageString: string, entityName?: string): void {
        this.displayNotification(i18nMessageString, 'success', entityName);
    }

    protected displayNotificationError(i18nMessageString: string, entityName?: string): void {
        this.displayNotification(i18nMessageString, 'alert', entityName);
    }

    private displayNotification(i18nMessageString: string, type: ColorThemes | 'default', entityName?: string): void {
        this.notification.show({
            type,
            message: i18nMessageString,
            translationParams: { entityName: entityName || this.i18n.instant(`common.${this.entityIdentifier}_singular`) },
        });
    }

}

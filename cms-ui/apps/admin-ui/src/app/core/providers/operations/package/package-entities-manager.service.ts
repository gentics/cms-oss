import { discard } from '@admin-ui/common';
import { ServiceBase } from '@admin-ui/shared/providers/service-base/service.base';
import { Injectable, Injector } from '@angular/core';
import { NormalizableEntityType } from '@gentics/cms-models';
import { forkJoin, Observable } from 'rxjs';
import { catchError, first, map, switchMap, tap } from 'rxjs/operators';
import { EntityManagerService } from '../../entity-manager';
import { ErrorHandler } from '../../error-handler';
import { I18nNotificationService } from '../../i18n-notification';

@Injectable()
export class PackageEntitiesManagerService extends ServiceBase {

    protected readonly errorHandler: ErrorHandler;

    constructor(
        injector: Injector,
        private entityManager: EntityManagerService,
        private notification: I18nNotificationService,
    ) {
        super();
        this.errorHandler = injector.get(ErrorHandler);
    }

    /**
     * Add an existing entity to a package
     * @param packageName of parent package entity
     * @param identifier of package child entity
     * @param globalIds of package child entities to be assigned to package
     * @param preselected of package child entities already assigned to package
     * @param addFn Method to assign package child entities to package
     * @param i18nSuccessAdd to display if addFn Method succeeded
     * @param removeFn Method to unassign package child entities from package
     * @param i18nSuccessRemove to display if removeFn Method succeeded
     * @param refreshFn Method to perform after addF and removeFn method execution
     */
    changeEntitiesOfPackage<T extends NormalizableEntityType>(
        packageName: string,
        identifier: T,
        globalIds: string[],
        preselected: string[],
        addFn: (pName: string, gId: string) => Observable<void>,
        i18nSuccessAdd: string,
        removeFn: (pName: string, gId: string) => Observable<void>,
        i18nSuccessRemove: string,
        refreshFn: (packageName: string) => Observable<any>,
    ): Observable<void> {

        // calculate minimal amount of requests required
        const entitiesShallBeLinked = globalIds.filter(id => !preselected.includes(id));
        const entitiesShallNotBeLinked = preselected.filter(id => !globalIds.includes(id));

        return forkJoin([
            entitiesShallBeLinked.length > 0
                ? this.addEntityToPackage(
                    packageName,
                    identifier,
                    entitiesShallBeLinked,
                    addFn,
                    i18nSuccessAdd,
                )
                : Promise.resolve(),
            entitiesShallNotBeLinked.length > 0
                ? this.removeEntityFromPackage(
                    packageName,
                    identifier,
                    entitiesShallNotBeLinked,
                    removeFn,
                    i18nSuccessRemove,
                )
                : Promise.resolve(),
        ]).pipe(
            map(() => { return; }),
            // refresh
            tap(() => refreshFn(packageName).toPromise()),
        );
    }

    /**
     * Add an existing entity to a package
     * @param packageId of parent package entity
     * @param identifier of package child entity
     * @param globalId (s) of package child entity(ies) to be assigned to package
     * @param addFn Method to assign package child entities to package
     * @param i18nSuccessAdd to display if addFn Method succeeded
     * @param refreshFn Method to perform after addF and removeFn method execution
     */
    addEntityToPackage<T extends NormalizableEntityType>(
        packageId: string,
        identifier: T,
        globalId: string | string[],
        addFn: (packageId: string, entityId: string) => Observable<void>,
        i18nSuccess: string,
        refreshFn?: (packageId: string) => Observable<any>,
    ): Observable<void> {
        const request = (pName: string, entityId: string): Observable<void> => {
            return this.entityManager.getEntity(identifier, entityId).pipe(
                first(),
                switchMap(entity => addFn(pName, entityId).pipe(
                    map(() => entity),
                )),
                discard(entity => {
                    this.notification.show({
                        type: 'success',
                        message: i18nSuccess,
                        translationParams: {
                            name: this.getEntityName(identifier, entity, entityId),
                        },
                    });
                }),
            );
        };

        let stream: Observable<void>;

        if (Array.isArray(globalId) && globalId.length > 0) {
            stream = forkJoin(globalId.map(gid => request(packageId, gid))).pipe(
                discard(),
            );
        } else if (typeof globalId === 'string') {
            stream = request(packageId, globalId);
        } else {
            console.error(`globalId is invalid:`, globalId);
            return;
        }

        return stream.pipe(
            // refresh
            tap(() => refreshFn && refreshFn(packageId).toPromise()),
            catchError(error => this.errorHandler.notifyAndRethrow(error)),
        );
    }

    /**
     * Remove an existing entity from a package
     * @param packageId of parent package entity
     * @param identifier of package child entity
     * @param globalId (s) of package child entity(ies) to be assigned to package
     * @param removeFn Method to unassign package child entities from package
     * @param i18nSuccessRemove to display if removeFn Method succeeded
     * @param refreshFn Method to perform after addF and removeFn method execution
     */
    removeEntityFromPackage<T extends NormalizableEntityType>(
        packageId: string,
        identifier: T,
        globalId: string | string[],
        removeFn: (packageId: string, entityId: string) => Observable<void>,
        i18nSuccess: string,
        refreshFn?: (packageId: string) => Observable<any>,
    ): Observable<void> {
        const request = (pName: string, entityId: string): Observable<void> => {
            return this.entityManager.getEntity(identifier, entityId).pipe(
                first(),
                switchMap(entity => removeFn(pName, entityId).pipe(
                    map(() => entity),
                )),
                discard(entity => {
                    this.notification.show({
                        type: 'success',
                        message: i18nSuccess,
                        translationParams: {
                            name: this.getEntityName(identifier, entity, entityId),
                        },
                    });
                }),
            );
        };

        let stream: Observable<void>;
        if (Array.isArray(globalId) && globalId.length > 0) {
            stream = forkJoin(globalId.map(gid => request(packageId, gid))).pipe(
                discard(),
            );
        } else if (typeof globalId === 'string') {
            stream = request(packageId, globalId);
        } else {
            console.error(`globalId is invalid:`, globalId);
            return;
        }

        return stream.pipe(
            // refresh
            tap(() => refreshFn && refreshFn(packageId).toPromise()),
            catchError(error => this.errorHandler.notifyAndRethrow(error)),
        );
    }

    protected getEntityName(identifier: NormalizableEntityType, entity: any, entityId: string): string {
        return entity?.name || entity?.id || entityId;
    }
}

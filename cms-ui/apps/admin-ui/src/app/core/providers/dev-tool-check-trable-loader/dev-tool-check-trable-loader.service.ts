import { Injectable } from '@angular/core';
import {
    PackageCheckResult,
    PackageDependency,
    PackageDependencyEntity,
    ReferenceDependency,
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { TrableRow } from '@gentics/ui-core';
import { Observable, Subject, from, interval, of } from 'rxjs';
import {
    BO_DISPLAY_NAME,
    BO_ID,
    BO_PERMISSIONS,
    PackageDependencyEntityBO,
} from '@admin-ui/common';
import { catchError, filter, map, mergeMap, retry, startWith, switchMap, take, takeUntil, tap, toArray } from 'rxjs/operators';
import { BaseTrableLoaderService } from '../base-trable-loader/base-trable-loader.service';

export interface PackageCheckTrableLoaderOptions {
    packageName: string;
    checkAll?: boolean;
    triggerNewCheck?: boolean;
}

@Injectable()
export class PackageCheckTrableLoaderService extends BaseTrableLoaderService<
PackageDependencyEntity,
PackageDependencyEntityBO,
PackageCheckTrableLoaderOptions
> {
    constructor(protected api: GcmsApi) {
        super();
    }

    protected loadEntityRow(
        entity: PackageDependencyEntity,
        options?: PackageCheckTrableLoaderOptions,
    ): Observable<PackageDependencyEntityBO> {
        return null;
    }

    protected loadEntityChildren(
        parent: PackageDependencyEntityBO | null,
        options?: PackageCheckTrableLoaderOptions,
    ): Observable<PackageDependencyEntityBO[]> {
        if (!parent) {
            if (options?.triggerNewCheck) {
                return this.triggerNewCheck(options)
            }

            return this.api.devTools.getCheckResult(options.packageName)
                .pipe(
                    map((checkResult: PackageCheckResult) =>
                        checkResult.items.map((packageDependency) =>
                            this.mapToBusinessObject(packageDependency),
                        ),
                    ),
                );
        } else if ((parent as PackageDependency).referenceDependencies) {
            return from(
                (parent as PackageDependency).referenceDependencies,
            ).pipe(
                map((referenceDependency) =>
                    this.mapToBusinessObject(referenceDependency, true),
                ),
                toArray(),
            );
        }
    }

    public isCheckResultAvailable(options: PackageCheckTrableLoaderOptions): Observable<boolean> {
        return this.api.devTools.getCheckResult(options.packageName)
            .pipe(
                switchMap(() => of(true)),
                catchError(() => {
                    return of(false);
                }),
            )
    }


    public triggerNewCheck(options?: PackageCheckTrableLoaderOptions): Observable<PackageDependencyEntityBO[]> {
        return this.api.devTools.check(options.packageName, {
            wait: 1, // todo: fix
            checkAll: options.checkAll,
        }).pipe(
            map((checkResult: PackageCheckResult) =>
                checkResult.items.map((packageDependency) =>
                    this.mapToBusinessObject(packageDependency),
                ),
            ),
            catchError(()=> {
                this.pollUntilResultIsAvailable(options)
                return of(null)
            }),
        )
    }

    private pollUntilResultIsAvailable(options: PackageCheckTrableLoaderOptions) {
        const pollStop = new Subject();
        interval(1000).pipe(
            startWith(0),
            mergeMap(() => this.isCheckResultAvailable(options)),
            take(10),
            filter(isAvailable => isAvailable === true),
            tap(() => {
                pollStop.next();
                pollStop.complete();
            }),
            takeUntil(pollStop),
        ).subscribe()
    }


    protected override mapToTrableRow(
        entity: PackageDependencyEntityBO,
        parent?: TrableRow<PackageDependencyEntityBO>,
        options?: PackageCheckTrableLoaderOptions,
    ): TrableRow<PackageDependencyEntityBO> {
        const row = super.mapToTrableRow(entity, parent, options);
        row.hasChildren = this.hasChildren(entity);
        if (!row.hasChildren) {
            row.loaded = true;
        }

        return row;
    }

    protected override hasChildren(entity: PackageDependencyEntity): boolean {
        if ((entity as PackageDependency).referenceDependencies) {
            return (
                (entity as PackageDependency).referenceDependencies?.length > 0
            );
        }
        return false;
    }

    private mapToBusinessObject(
        packageDependency: PackageDependencyEntity,
        addFlag?: boolean,
    ): PackageDependencyEntityBO {
        const packageEntity: PackageDependencyEntityBO = {
            ...packageDependency,
            [BO_ID]: packageDependency.globalId.toString(),
            [BO_DISPLAY_NAME]: packageDependency.name,
            [BO_PERMISSIONS]: [],
        }

        if (addFlag) {
            const reference = packageDependency as ReferenceDependency;
            packageEntity['isContained'] = (reference.isInPackage ?? false) || (reference.isInOtherPackage ?? false);
        }


        return packageEntity;
    }
}

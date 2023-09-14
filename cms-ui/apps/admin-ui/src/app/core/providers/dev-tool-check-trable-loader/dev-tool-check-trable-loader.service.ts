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
import { catchError, first, map, switchMap, toArray } from 'rxjs/operators';
import { BaseTrableLoaderService } from '../base-trable-loader/base-trable-loader.service';
import { PackageOperations } from '../operations';

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
    constructor(
        protected api: GcmsApi,
        private packageOperations: PackageOperations) {
        super();
    }

    protected loadEntityRow(
        entity: PackageDependencyEntity,
        options?: PackageCheckTrableLoaderOptions,
    ): Observable<PackageDependencyEntityBO> {
        return of(this.mapToBusinessObject(entity))
    }

    protected loadEntityChildren(
        parent: PackageDependencyEntityBO | null,
        options: PackageCheckTrableLoaderOptions,
    ): Observable<PackageDependencyEntityBO[]> {
        if (!parent) {
            if (options?.triggerNewCheck) {
                return this.triggerNewCheck(options);
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

    public triggerNewCheck(options: PackageCheckTrableLoaderOptions): Observable<PackageDependencyEntityBO[]> {
        return this.api.devTools.check(options.packageName, {
            checkAll: options.checkAll ?? false,
        }).pipe(
            switchMap((apiResponse)=> {
                if (apiResponse.items) {
                    return of(apiResponse);
                }
                return this.packageOperations.pollCheckResultUntilResultIsAvailable(options).pipe(
                    switchMap(() => this.api.devTools.getCheckResult(options.packageName)),
                )
            }),
            map((checkResult: PackageCheckResult) =>
                checkResult.items.map((packageDependency) =>
                    this.mapToBusinessObject(packageDependency),
                ),
            ),
        )
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

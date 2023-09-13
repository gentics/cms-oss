import { Injectable } from '@angular/core';
import {
    PackageCheckResult,
    PackageDependency,
    PackageDependencyEntity,
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { TrableRow } from '@gentics/ui-core';
import { Observable, from, of } from 'rxjs';
import { catchError, map, switchMap, toArray } from 'rxjs/operators';
import {
    BO_DISPLAY_NAME,
    BO_ID,
    BO_PERMISSIONS,
    PackageDependencyEntityBO,
} from '@admin-ui/common';
import { BaseTrableLoaderService } from '../base-trable-loader/base-trable-loader.service';

export interface PackageCheckTrableLoaderOptions {
    packageName: string;
    checkAll?: boolean;
    shouldReload?: boolean;
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
                    this.mapToBusinessObject(referenceDependency),
                ),
                toArray(),
            );
        }
    }

    public isCheckResultAvailable(options?: PackageCheckTrableLoaderOptions): Observable<boolean> {
        return this.api.devTools.getCheckResult(options.packageName)
            .pipe(
                switchMap(() => of(true)),
                catchError(() => {
                    return of(false);
                }),
            )
    }


    public getNewCheckResult(options?: PackageCheckTrableLoaderOptions): Observable<PackageDependencyEntityBO[]> {
        return this.api.devTools.check(options.packageName).pipe(
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

    public mapToBusinessObject(
        packageDependency: PackageDependencyEntity,
    ): PackageDependencyEntityBO {
        return {
            ...packageDependency,
            [BO_ID]: packageDependency.globalId.toString(),
            [BO_DISPLAY_NAME]: packageDependency.name,
            [BO_PERMISSIONS]: [],
        };
    }
}

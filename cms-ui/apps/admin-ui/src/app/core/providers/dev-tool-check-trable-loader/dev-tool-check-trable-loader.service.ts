import { Injectable } from '@angular/core';
import {
    PackageCheckCompletenessFilter,
    PackageCheckResult,
    PackageDependency,
    PackageDependencyEntity,
    ReferenceDependency,
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { TrableRow } from '@gentics/ui-core';
import { Observable, from, of } from 'rxjs';
import {
    BO_DISPLAY_NAME,
    BO_ID,
    BO_PERMISSIONS,
    PackageDependencyEntityBO,
} from '@admin-ui/common';
import { map, switchMap, toArray } from 'rxjs/operators';
import { BaseTrableLoaderService } from '../base-trable-loader/base-trable-loader.service';
import { DevToolPackageHandlerService } from '../dev-tool-package-handler/dev-tool-package-handler.service';


export interface PackageCheckTrableLoaderOptions {
    packageName: string;
    checkAll?: boolean;
    triggerNewCheck?: boolean;
    wait?: number;
}

@Injectable()
export class PackageCheckTrableLoaderService extends BaseTrableLoaderService<
PackageDependencyEntity,
PackageDependencyEntityBO,
PackageCheckTrableLoaderOptions
> {
    constructor(
        protected api: GcmsApi,
        private handler: DevToolPackageHandlerService) {
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
                        checkResult.items.map(packageDependency =>
                            this.mapToBusinessObject(packageDependency),
                        ),
                    ),
                    map(dependencies => dependencies.sort((a,b) => {
                        return a.dependencyType > b.dependencyType ? 1  : -1;
                    })),
                );
        } else if ((parent as PackageDependency).referenceDependencies) {
            return from(
                (parent as PackageDependency).referenceDependencies,
            ).pipe(
                map((referenceDependency) =>
                    this.mapToBusinessObject(referenceDependency, options),
                ),
                toArray(),
            );
        }
    }

    public triggerNewCheck(options: PackageCheckTrableLoaderOptions): Observable<PackageDependencyEntityBO[]> {
        return this.api.devTools.check(options.packageName, {
            checkAll: options.checkAll ?? true,
            wait: options.wait ?? 5_000,
            filter: PackageCheckCompletenessFilter.INCOMPLETE.toString(),
        }).pipe(
            switchMap((apiResponse)=> {
                if (apiResponse.items) {
                    return of(apiResponse);
                }
                return this.handler.pollCheckResultUntilResultIsAvailable(options).pipe(
                    switchMap(() => this.api.devTools.getCheckResult(options.packageName)),
                )
            }),
            map((checkResult: PackageCheckResult) =>
                checkResult.items.map((packageDependency) =>
                    this.mapToBusinessObject(packageDependency),
                ),
            ),
            map(dependencies =>  dependencies.sort((a,b) => {
                return a.dependencyType > b.dependencyType ? 1  : -1;
            })),
        )
    }

    protected override mapToTrableRow(
        entity: PackageDependencyEntityBO,
        parent?: TrableRow<PackageDependencyEntityBO>,
        options?: PackageCheckTrableLoaderOptions,
    ): TrableRow<PackageDependencyEntityBO> {
        const row = super.mapToTrableRow(entity, parent, options);
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
        options?: PackageCheckTrableLoaderOptions,
    ): PackageDependencyEntityBO {
        const packageEntity: PackageDependencyEntityBO = {
            ...packageDependency,
            [BO_ID]: packageDependency.globalId,
            [BO_DISPLAY_NAME]: packageDependency.name,
            [BO_PERMISSIONS]: [],
        }

        if (!this.isReferenceDependency(packageDependency)) {
            // row parent
            packageEntity['isContained'] = !this.containsMissingReferences(packageEntity);
        }
        else {
            // row child (=> expand row)
            const reference = packageDependency as ReferenceDependency;
            packageEntity['isContained'] = (reference.isInPackage ?? false) || (reference.isInOtherPackage ?? false);
            packageEntity['foundInPackage'] = (packageEntity['isContained'] && !reference.foundInPackage) ? options.packageName : reference.foundInPackage;
        }

        return packageEntity;
    }

    private containsMissingReferences(packageEntity: PackageDependencyEntityBO): boolean {
        const packageDependency = packageEntity as PackageDependency;

        return packageDependency.referenceDependencies.some(dependency =>
            !dependency.isInPackage && !dependency.isInOtherPackage)
    }

    private isReferenceDependency(packageEntity: PackageDependencyEntity): boolean {
        return 'isInPackage' in packageEntity || 'isInOtherPackage' in packageEntity
    }

}

import { Injectable } from '@angular/core';
import { PackageCheckResult, PackageDependency } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { TrableRow } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
    BO_DISPLAY_NAME,
    BO_ID,
    BO_PERMISSIONS,
    PackageDependencyBO,
} from '@admin-ui/common';
import { BaseTrableLoaderService } from '../base-trable-loader/base-trable-loader.service';

export interface PackageCheckTrableLoaderOptions {
    packageName: string;
}

@Injectable()
export class PackageCheckTrableLoaderService extends BaseTrableLoaderService<
PackageDependency,
PackageDependencyBO,
PackageCheckTrableLoaderOptions
> {
    constructor(protected api: GcmsApi) {
        super();
    }

    protected loadEntityRow(
        entity: PackageDependency,
        options?: PackageCheckTrableLoaderOptions,
    ): Observable<PackageDependencyBO> {
        // todo: remove; merely needed for reload and not needed?
        return this.api.devTools.getCheckResult(options.packageName).pipe(
            map((checkResult: PackageCheckResult) => {
                const packageDependency = checkResult.items.find(
                    (packageDependency) =>
                        packageDependency.globalId ===entity.globalId,
                );

                return this.mapToBusinessObject(packageDependency);
            }),
        );
    }

    protected loadEntityChildren(
        parent: PackageDependencyBO | null,
        options?: PackageCheckTrableLoaderOptions,
    ): Observable<PackageDependencyBO[]> {
        let packageDependencies: Observable<PackageDependencyBO[]>;

        if (!parent) {
            packageDependencies = this.api.devTools
                .getCheckResult(options.packageName)
                .pipe(
                    map((checkResult: PackageCheckResult) =>
                        checkResult.items.map((packageDependency) =>
                            this.mapToBusinessObject(packageDependency),
                        ),
                    ),
                );
        }
        else {
            packageDependencies = packageDependencies.pipe(
                map((packageDependencies) =>
                    packageDependencies.flatMap((packageDependency) =>
                        packageDependency.referenceDependencies.map((referenceDependency) =>
                            this.mapToBusinessObject(referenceDependency),
                        ),
                    ),
                ),
            );
        }

        // todo: remove me
        packageDependencies.toPromise().then((res) => console.log(res));

        return packageDependencies;
    }

    protected override mapToTrableRow(
        entity: PackageDependencyBO,
        parent?: TrableRow<PackageDependencyBO>,
        options?: PackageCheckTrableLoaderOptions,
    ): TrableRow<PackageDependencyBO> {
        const row = super.mapToTrableRow(entity, parent, options);
        row.hasChildren = this.hasChildren(entity);
        row.loaded = true;

        return row;
    }

    protected override hasChildren(entity: PackageDependency): boolean {
        return entity.referenceDependencies?.length > 0;
    }

    public mapToBusinessObject(
        packageDependency: PackageDependency,
    ): PackageDependencyBO {
        return {
            ...packageDependency,
            [BO_ID]: packageDependency.globalId.toString(),
            [BO_DISPLAY_NAME]: packageDependency.name,
            [BO_PERMISSIONS]: [],
        };
    }
}

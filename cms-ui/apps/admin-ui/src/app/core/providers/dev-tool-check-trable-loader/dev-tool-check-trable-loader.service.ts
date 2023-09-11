import { Injectable } from '@angular/core';
import {
    PackageCheckResult, PackageDependency,
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { TrableRow } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, PackageDependencyBO } from '@admin-ui/common';
import { BaseTrableLoaderService } from '../base-trable-loader/base-trable-loader.service';

export interface PackageCheckLoaderOptions {
    packageName: string;
}

@Injectable()
export class PackageCheckTrableLoaderService extends BaseTrableLoaderService<
PackageDependency,
PackageDependencyBO,
PackageCheckLoaderOptions
> {
    constructor(protected api: GcmsApi) {
        super();
    }

    protected loadEntityRow(
        entity: PackageDependency,
        options?: PackageCheckLoaderOptions,
    ): Observable<PackageDependencyBO> {
        return null;
    }

    protected loadEntityChildren(
        parent: PackageDependencyBO | null,
        options?: PackageCheckLoaderOptions,
    ): Observable<PackageDependencyBO[]> {

        return this.api.devTools
            .getCheckResult(options.packageName)
            .pipe(
                map((checkResult: PackageCheckResult) =>
                    checkResult.items.map((packageDependency) =>
                        this.mapToBusinessObject(packageDependency),
                    ),
                ),
            )


    }

    public override createRowHash(
        entity: PackageDependencyBO,
    ): string | null {
        return new Date().toISOString();
    }

    protected override mapToTrableRow(
        entity: PackageDependencyBO,
        parent?: TrableRow<PackageDependencyBO>,
        options?: PackageCheckLoaderOptions,
    ): TrableRow<PackageDependencyBO> {
        const row = super.mapToTrableRow(entity, parent, options);
        row.hasChildren = false
        row.loaded = true

        return row;
    }

    public mapToBusinessObject(
        packageDependency: PackageDependency,
    ): PackageDependencyBO {
        return {
            ...packageDependency,
            [BO_ID]: packageDependency.globalId.toString(),
            [BO_DISPLAY_NAME]: packageDependency.name,
            [BO_PERMISSIONS]: [], // TODO: permissions needed?
        };
    }

}

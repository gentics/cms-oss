import { PackageOperations } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { PackageDataService } from '@admin-ui/shared';
import { AbstractCanActivateEntityGuard } from '@admin-ui/shared/providers/abstract-can-activate-entity/abstract-can-activate-entity.guard';

/**
 * A route guard checking for changes made by the package in an componennt with changable elements.
 */
@Injectable()
export class CanActivatePackageGuard extends AbstractCanActivateEntityGuard<'package', PackageOperations> {
    constructor(
        packageData: PackageDataService,
        appState: AppStateService,
    ) {
        super(
            'package',
            packageData,
            appState,
        );
    }

}

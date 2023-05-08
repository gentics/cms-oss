import { DataSourceOperations } from '@admin-ui/core';
import { DataSourceDataService } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { AbstractCanActivateEntityGuard } from '@admin-ui/shared/providers/abstract-can-activate-entity/abstract-can-activate-entity.guard';

/**
 * A route guard checking for changes made by the user in an componennt with changable elements.
 */
@Injectable()
export class CanActivateDataSourceGuard extends AbstractCanActivateEntityGuard<'dataSource', DataSourceOperations> {
    constructor(
        dataSourceData: DataSourceDataService,
        appState: AppStateService,
    ) {
        super(
            'dataSource',
            dataSourceData,
            appState,
        );
    }

}

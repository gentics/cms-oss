
import { NodeOperations } from '@admin-ui/core/providers/operations/node/node.operations';
import { NodeDataService } from '@admin-ui/shared/providers/node-data/node-data.service';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { AbstractCanActivateEntityGuard } from '@admin-ui/shared/providers/abstract-can-activate-entity/abstract-can-activate-entity.guard';

/**
 * A route guard checking for changes made by the user in an componennt with changable elements.
 */
@Injectable()
export class CanActivateNodeGuard extends AbstractCanActivateEntityGuard<'node', NodeOperations> {

    constructor(
        nodeData: NodeDataService,
        appState: AppStateService,
    ) {
        super(
            'node',
            nodeData,
            appState,
        );
    }

}

import { Injectable } from '@angular/core';
import { tap } from 'rxjs/operators';
import { Api } from '../../../core/providers/api/api.service';
import { ApplicationStateService } from '../../providers';
import { NodeSettingsFetchingSuccessAction } from '../../modules';

@Injectable()
export class NodeSettingsActionsService {

    constructor(
        private appState: ApplicationStateService,
        private api: Api,
    ) {}

    /**
     * Loads the node settings for the specified node and updates the app state with the result.
     */
    loadNodeSettings(nodeId: number): Promise<any> {
        return this.api.folders.getNodeSettings(nodeId).pipe(
            tap(response => {
                if (response && response.data) {
                    this.appState.dispatch(new NodeSettingsFetchingSuccessAction(nodeId, response.data, response.global))
                }
            }),
        ).toPromise();
    }
}

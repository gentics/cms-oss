import { AppStateService, GlobalFeaturesMap, NodeFeaturesMap, SetGlobalFeature, SetNodeFeatures } from '@admin-ui/state';
import { Injectable, Injector } from '@angular/core';
import { Feature } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { forkJoin, Observable, of, throwError } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { OperationsBase } from '../operations.base';

/** An array of all global CMS features known to the UI. */
export const ALL_GLOBAL_FEATURES: Feature[] = Object.keys(Feature).map(key => Feature[key]);
Object.freeze(ALL_GLOBAL_FEATURES);

@Injectable()
export class FeatureOperations extends OperationsBase {

    constructor(
        injector: Injector,
        private api: GcmsApi,
        private appState: AppStateService,
    ) {
        super(injector);
    }

    /**
     * Check the activation status of all global features. Note that only a subset of total CMS features are
     * checked in the UI.
     */
    checkAllGlobalFeatures(): Observable<GlobalFeaturesMap> {
        const allChecks$: Observable<boolean>[] = ALL_GLOBAL_FEATURES
            .map(key => this.checkGlobalFeatureInternal(key));

        return forkJoin(allChecks$).pipe(
            map(() => this.appState.now.features.global),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Checks the CMS for whether the given global feature is activated and updates the app state with the result.
     */
    checkGlobalFeature(key: Feature): Observable<boolean> {
        return this.checkGlobalFeatureInternal(key).pipe(
            this.catchAndRethrowError(),
        );
    }

    /**
     * Loads the activated node features for the specified node and updates the app state with the result.
     */
    getNodeFeatures(nodeId: number): Observable<NodeFeaturesMap> {
        return this.api.folders.getNodeFeatures(nodeId).pipe(
            switchMap(response => {
                return this.appState.dispatch(new SetNodeFeatures(nodeId, response.features)).pipe(
                    map(data => this.appState.now.features.node[nodeId]),
                );
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Checks the CMS for whether the given global feature is activated and updates the app state with the result.
     *
     * Does not handle errors.
     */
    private checkGlobalFeatureInternal(key: Feature): Observable<boolean> {
        return this.api.admin.getFeature(key).pipe(
            map(response => response.activated),
            catchError(err => {
                console.error('error while loading feature', key, err);
                return of(false);
            }),
            tap(isActive => this.appState.dispatch(new SetGlobalFeature(key, isActive))),
        );
    }

}

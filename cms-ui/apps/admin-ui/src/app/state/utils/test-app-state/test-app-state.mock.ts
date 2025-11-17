import { ConstructorOf, ObservableStopper } from '@admin-ui/common';
import { Injectable, Injector } from '@angular/core';
import { IS_NORMALIZED, RecursivePartial } from '@gentics/cms-models';
import { ActionContext, Actions, NgxsModule, Store } from '@ngxs/store';
import { cloneDeep as _cloneDeep, merge as _merge } from'lodash-es'
import { Observable, OperatorFunction } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AppState } from '../../app-state';
import { EntityStateModel } from '../../entity/entity.state';
import { AppStateService } from '../../providers/app-state/app-state.service';
import { STATE_MODULES } from '../../state.module';

export interface TrackedActions<T> {

    /** Gets the number of actions that have been tracked. */
    readonly count: number;

    /** Gets the tracked action at the specified index, */
    get(index: number): T;

    /** Resets the tracked actions. */
    reset(): void;

}

class TrackedActionsImpl<T> implements TrackedActions<T> {

    private trackedActions: T[] = [];

    get count(): number {
        return this.trackedActions.length;
    }
    get(index: number): T {
        return this.trackedActions[index];
    }
    reset(): void {
        this.trackedActions = [];
    }
    addTrackedAction(action: T): void {
        this.trackedActions.push(action);
    }

}

/**
 * An RxJS operator for filtering actions.
 *
 * @see https://ngxs.gitbook.io/ngxs/advanced/action-handlers
 */
export type ActionFilterOperator = (...allowedTypes: ConstructorOf<any>[]) => OperatorFunction<ActionContext<any>, any>;

/**
 * Helper class for unit tests involving the AppState. Use `TEST_APP_STATE` in `configureTestModule()`.
 */
@Injectable()
export class TestAppState extends AppStateService {

    private stopper = new ObservableStopper();

    constructor(store: Store, private injector: Injector) {
        super(store);
    }

    onServiceDestroy(): void {
        if (super.onServiceDestroy) {
            super.onServiceDestroy();
        }
        this.stopper.stop();
    }

    /**
     * Merges the specified `partialState` into the current state.
     */
    mockState(partialState: RecursivePartial<AppState>): void {
        const newState: AppState = _cloneDeep(this.store.snapshot());
        _merge(newState, partialState);

        // Since _.merge() ignores symbols, we need to manually copy [IS_NORMALIZED] for entities.
        if (partialState.entity) {
            this.applyNormalizationStatuses(newState, partialState.entity);
        }

        this.store.reset(newState);
    }

    /**
     * Allows tracking of state actions.
     *
     * @see https://ngxs.gitbook.io/ngxs/advanced/action-handlers
     */
    trackActions(): Observable<any> {
        return this.injector.get(Actions);
    }

    /**
     * Automatically tracks state actions using the specified `filter` operator.
     *
     * @param actionFilter The `ActionFilterOperator` used for filtering out the actions to be tracked.
     * **Important:** If using `ofActionCompleted()` the return type must be changed to `ActionCompletion<>`,
     * because this operator will wrap the filtered actions - unfortunately I could not find a way to do this filtering
     * with generics, because the other action filter operators return Observable<any>.
     * @param allowedTypes The types of actions that should be tracked.
     */
    trackActionsAuto<A>(actionFilter: ActionFilterOperator, allowedType: ConstructorOf<A>): TrackedActions<A>;
    trackActionsAuto<A, B>(actionFilter: ActionFilterOperator, allowedTypeA: ConstructorOf<A>, allowedTypeB: ConstructorOf<B>): TrackedActions<A | B>;
    trackActionsAuto<A, B, C>(
        actionFilter: ActionFilterOperator, allowedTypeA: ConstructorOf<A>, allowedTypeB: ConstructorOf<B>, allowedTypeC: ConstructorOf<C>,
    ): TrackedActions<A | B | C>;
    trackActionsAuto(actionFilter: ActionFilterOperator, ...allowedTypes: ConstructorOf<any>[]): TrackedActions<any>;
    trackActionsAuto(actionFilter: ActionFilterOperator, ...allowedTypes: ConstructorOf<any>[]): TrackedActions<any> {
        const trackedActions = new TrackedActionsImpl<any>();

        this.trackActions().pipe(
            actionFilter(...allowedTypes),
            takeUntil(this.stopper.stopper$),
        ).subscribe(action => trackedActions.addTrackedAction(action));

        return trackedActions;
    }

    private applyNormalizationStatuses(newState: AppState, modifiedEntities: RecursivePartial<EntityStateModel>): void {
        Object.keys(modifiedEntities).forEach(type => {
            const srcEntityBranch = modifiedEntities[type];
            const destEntityBranch = newState.entity[type];

            Object.keys(srcEntityBranch).forEach(id => {
                const entityChanges = srcEntityBranch[id];
                if (entityChanges[IS_NORMALIZED]) {
                    destEntityBranch[id][IS_NORMALIZED] = entityChanges[IS_NORMALIZED];
                }
            });
        });
    }

}

/**
 * Imports for setting up unit tests that require the AppState.
 */
export function assembleTestAppStateImports(): any[] {
    return [
        NgxsModule.forRoot(STATE_MODULES),
    ];
}

/**
 * Provider for overriding the `AppStateService` with `TestAppState`.
 */
export const TEST_APP_STATE = { provide: AppStateService, useClass: TestAppState };

import { FormTabHandle, ObservableStopper, OnDiscardChanges } from '@admin-ui/common';
import { ExtendedEntityOperationsBase } from '@admin-ui/core';
import { AppStateService, SelectState, SetUIFocusEntity, UIStateModel } from '@admin-ui/state';
import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import {
    NormalizableEntityType,
    NormalizableEntityTypesMapBO,
    Normalized,
    Raw,
} from '@gentics/cms-models';
import { isEqual } from 'lodash-es';
import { Observable } from 'rxjs';
import {
    catchError,
    distinctUntilChanged,
    filter,
    map,
    publishReplay,
    refCount,
} from 'rxjs/operators';
import { ExtendedEntityDataServiceBase } from '../../providers';

/**
 * Superclass for a component that is displayed on the details side of a master-detail view.
 */
 @Component({
    template: '',
    standalone: false
})
export abstract class BaseDetailComponent<
    T extends NormalizableEntityType,
    O extends ExtendedEntityOperationsBase<T, T_RAW>,
    T_RAW extends NormalizableEntityTypesMapBO<Raw>[T] = NormalizableEntityTypesMapBO<Raw>[T],
    T_NORM extends NormalizableEntityTypesMapBO<Normalized>[T] = NormalizableEntityTypesMapBO<Normalized>[T],
> implements OnDestroy, OnDiscardChanges, OnInit {

    /** Name of the entity */
    abstract entityIdentifier: NormalizableEntityType;

    /** current entity observable */
    currentEntity$: Observable<T_RAW>;

    abstract get isLoading(): boolean;

    /**
     * Gets the form handle for the currently active tab.
     */
    abstract get activeFormTab(): FormTabHandle;

    @SelectState(state => state.ui)
    userState$: Observable<UIStateModel>;

    /**
     * Generic subscription stopper, which is inherited in all components.
     */
    protected stopper = new ObservableStopper();

    constructor(
        protected route: ActivatedRoute,
        protected router: Router,
        protected appState: AppStateService,
        protected entityData: ExtendedEntityDataServiceBase<T, O, T_RAW, T_NORM>,
        protected changeDetectorRef: ChangeDetectorRef,
    ) {}

    ngOnInit(): void {
        // get current entity
        this.currentEntity$ = this.userState$.pipe(
            map((userState: UIStateModel) => this.entityData.getEntity(userState.focusEntityId)),
            filter((entity: T_RAW) => typeof entity === 'object' && entity != null),
            distinctUntilChanged(isEqual),
        );

        // assign values and validation of current entity
        this.currentEntity$.pipe(
            publishReplay(1),
            refCount(),
        );
    }

    ngOnDestroy(): void {
        this.stopper.stop();
    }

    /** OnDiscardChanges implementation indicating if user has changed input data */
    userHasEdited(): boolean {
        const formHandle = this.activeFormTab;
        return formHandle ? formHandle.isDirty() : false;
    }

    changesValid(): boolean {
        const formHandle = this.activeFormTab;
        return formHandle ? formHandle.isValid() : false;
    }

    /**
     * User clicks on the top right button with X-icon to close the details split screen
     */
    async detailsClose(): Promise<void> {
        // this.route.parent.parent assumes that the detail outlet routes have a root and that the specific editor tabs are child routes of this root route.
        const relativeToRoute = this.route.parent.parent || this.route.parent;
        const navigationSucceeded = await this.router.navigate([ { outlets: { detail: null } } ], { relativeTo: relativeToRoute });
        if (navigationSucceeded) {
            this.appState.dispatch(new SetUIFocusEntity(null, null, null));
        }
    }

    updateEntity(): Promise<void> {
        const formHandle = this.activeFormTab;
        return formHandle.save();
    }

    resetEntity(): Promise<void> {
        const formHandle = this.activeFormTab;
        if (formHandle.reset) {
            return formHandle.reset();
        }
        return Promise.resolve();
    }

    /**
     * Try getting entity from state, otherwise fetch
     */
    protected getEntity(entityId: number): Observable<T_NORM | void> {
        return this.entityData.getEntityFromState(entityId).pipe(
            // if entity doesn't exist, close details and split view
            catchError(() => this.detailsClose()),
        );
    }

}

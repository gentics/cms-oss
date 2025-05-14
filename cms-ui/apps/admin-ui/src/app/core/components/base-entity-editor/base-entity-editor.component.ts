import {
    EDITABLE_ENTITY_DETAIL_TABS,
    EditableEntity,
    EditableEntityBusinessObjects,
    EditableEntityDetailTabs,
    EditableEntityModels,
    EntityEditorHandler,
    EntityLoadRequestParams,
    EntityUpdateRequestModel,
    FormGroupTabHandle,
    FormTabHandle,
    OnDiscardChanges,
    ROUTE_ENTITY_RESOLVER_KEY,
    ROUTE_PARAM_ENTITY_ID,
    ROUTE_PARAM_NODE_ID,
    discard,
} from '@admin-ui/common';
import { EDITOR_TAB } from '@admin-ui/core/providers';
import { AppStateService, FocusEditor, OpenEditor, SetUIFocusEntity } from '@admin-ui/state';
import { ChangeDetectorRef, Directive, Input, OnChanges, OnDestroy, OnInit, SimpleChanges } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { toValidNumber } from '@gentics/ui-core';
import { BehaviorSubject, Subscription } from 'rxjs';
import { finalize } from 'rxjs/operators';

@Directive({
    standalone: false
})
export abstract class BaseEntityEditorComponent<K extends EditableEntity>
    implements OnInit, OnChanges, OnDestroy, OnDiscardChanges {

    public readonly Tabs: typeof EDITABLE_ENTITY_DETAIL_TABS[K];
    // eslint-disable-next-line @typescript-eslint/naming-convention
    public readonly skipClose = true;

    // eslint-disable-next-line @angular-eslint/no-input-rename
    @Input({ alias: ROUTE_PARAM_ENTITY_ID, transform: toValidNumber })
    public entityId: number;

    // eslint-disable-next-line @angular-eslint/no-input-rename
    @Input({ alias: ROUTE_PARAM_NODE_ID, transform: toValidNumber })
    public nodeId: number;

    @Input({ alias: EDITOR_TAB })
    public editorTab: EditableEntityDetailTabs[K];

    public isLoading = false;
    public entity: EditableEntityBusinessObjects[K] = null;
    public entityIsClean = true;

    public tabHandles: { [key in keyof EditableEntityDetailTabs[K]]: FormTabHandle } = {} as any;
    public activeTabHandle: FormTabHandle;

    protected loaderSubscription: Subscription;
    protected subcriptions: Subscription[] = [];

    constructor(
        protected entityKey: K,
        protected changeDetector: ChangeDetectorRef,
        protected route: ActivatedRoute,
        protected router: Router,
        protected appState: AppStateService,
        protected handler: EntityEditorHandler<K>,
    ) {
        this.Tabs = EDITABLE_ENTITY_DETAIL_TABS[entityKey];
    }

    protected abstract initializeTabHandles(): void;
    protected abstract onEntityChange(): void;

    /* LIFE CYCLE HOOKS
     *************************************************************************/

    ngOnInit(): void {
        this.initializeTabHandles();
        this.updateActiveTabHandle();
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.entityId || changes.nodeId) {
            this.appState.dispatch(new SetUIFocusEntity(this.entityKey, this.entityId, this.nodeId));
            this.appState.dispatch(new OpenEditor());
            this.appState.dispatch(new FocusEditor());
        }

        if (changes.entityId) {
            this.handleEntityLoad(this.route.snapshot.data[ROUTE_ENTITY_RESOLVER_KEY]);
        }

        if (changes.editorTab && !changes.editorTab.firstChange) {
            this.updateActiveTabHandle();
        }
    }

    ngOnDestroy(): void {
        if (this.loaderSubscription != null) {
            this.loaderSubscription.unsubscribe();
        }
        this.subcriptions.forEach(s => s.unsubscribe());
    }

    /* ON DISCARD CHANGES IMPL
     *************************************************************************/

    userHasEdited(): boolean {
        return this.activeTabHandle?.isDirty?.();
    }

    changesValid(): boolean {
        return this.activeTabHandle?.isValid?.();
    }

    updateEntity(): Promise<void> {
        return this.activeTabHandle?.save?.();
    }

    resetEntity(): Promise<void> {
        return this.activeTabHandle?.reset?.();
    }

    /* TEMPLATE ACTIONS
     *************************************************************************/

    async detailsClose(): Promise<void> {
        // this.route.parent.parent assumes that the detail outlet routes have a root and that the specific editor tabs are child routes of this root route.
        const relativeToRoute = this.route.parent.parent || this.route.parent;
        const navigationSucceeded = await this.router.navigate([ { outlets: { detail: null } } ], { relativeTo: relativeToRoute });
        if (navigationSucceeded) {
            this.appState.dispatch(new SetUIFocusEntity(null, null, null));
        }
    }

    /* EDITOR HOOKS
     *************************************************************************/

    protected createLoadOptions(): EntityLoadRequestParams<K> {
        return null;
    }

    protected finalizeEntityToUpdate(entity: EditableEntityModels[K]): EntityUpdateRequestModel<K> {
        return entity;
    }

    protected onEntityUpdate(): void {
        return;
    }

    /* UTIL
     *************************************************************************/

    protected resetTabs(): Promise<void> {
        return Promise.all(Object.values(this.tabHandles).map(handle => handle.reset?.() ?? Promise.resolve()))
            .then(() => {});
    }

    protected loadEntity(): Promise<void> {
        if (this.entityId == null) {
            return;
        }

        if (this.loaderSubscription != null) {
            this.loaderSubscription.unsubscribe();
            this.loaderSubscription = null;
        }

        this.isLoading = true;
        this.changeDetector.markForCheck();

        return new Promise((resolve, reject) => {
            this.loaderSubscription = this.handler.getMapped(this.entityId, this.createLoadOptions()).subscribe(
                loadedEntity => {
                    this.handleEntityLoad(loadedEntity);
                    this.changeDetector.markForCheck();
                    resolve();
                },
                error => {
                    this.isLoading = false;
                    this.changeDetector.markForCheck();
                    reject(error);
                },
            )
        });
    }

    protected handleEntityLoad(loadedEntity: EditableEntityBusinessObjects[K]): void {
        this.entity = loadedEntity;

        // Hacky workaround, but other changes wouldn't work as well.
        // This updates the route-entity with the newly updated/loaded one, without
        // causing a route change, and therefore avoiding additional computation.
        (this.route.data as BehaviorSubject<any>).next({
            ...this.route.snapshot.data,
            [ROUTE_ENTITY_RESOLVER_KEY]: loadedEntity,
        });

        this.entityIsClean = true;
        this.isLoading = false;
        this.changeDetector.markForCheck();

        // For whatever reason, the form doesn't properly update within one tick.
        // Therefore we have to delay this and then the save button is displayed correctly again.
        setTimeout(() => {
            this.onEntityChange();
            // Just to be sure, as `onEntityChange` could change the clean state
            this.entityIsClean = true;
            this.changeDetector.markForCheck();
        });
    }

    protected updateActiveTabHandle(): void {
        this.activeTabHandle = this.tabHandles[this.editorTab as any];
    }

    protected createTabHandle(formControl: UntypedFormControl): FormTabHandle {
        return new FormGroupTabHandle(formControl, {
            save: () => {
                const value = formControl.value;
                formControl.disable();
                const mapped = this.finalizeEntityToUpdate(value);

                return this.handler.updateMapped(this.entityId, mapped).pipe(
                    discard(entity => {
                        this.handleEntityLoad(entity);
                        this.onEntityUpdate();
                    }),
                    finalize(() => formControl.enable()),
                ).toPromise();
            },
        })
    }
}

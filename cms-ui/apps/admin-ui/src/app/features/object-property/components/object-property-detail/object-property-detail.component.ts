import {
    createFormSaveDisabledTracker,
    detailLoading,
    discard,
    FormTabHandle,
} from '@admin-ui/common';
import {
    BREADCRUMB_RESOLVER,
    ObjectPropertyOperations,
    ObjectPropertyTableLoaderService,
    ResolveBreadcrumbFn,
} from '@admin-ui/core';
import { BaseDetailComponent, ObjectPropertyDataService } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnInit,
    Type,
} from '@angular/core';
import { UntypedFormBuilder, UntypedFormControl } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { createNestedControlValidator } from '@gentics/cms-components';
import {
    NormalizableEntityType,
    ObjectPropertyBO,
    ObjectPropertyUpdateRequest,
    Raw,
} from '@gentics/cms-models';
import { NGXLogger } from 'ngx-logger';
import { Observable, of } from 'rxjs';
import { map, publishReplay, refCount, takeUntil } from 'rxjs/operators';
import { ObjectpropertyPropertiesMode } from '../object-property-properties/object-property-properties.component';

// *************************************************************************************************
/**
 * # ObjectPropertyDetailComponent
 * Display and edit entity objectProperty detail information
 */
// *************************************************************************************************
@Component({
    selector: 'gtx-object-property-detail',
    templateUrl: './object-property-detail.component.html',
    styleUrls: ['./object-property-detail.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ObjectPropertyDetailComponent
    extends BaseDetailComponent<'objectProperty', ObjectPropertyOperations>
    implements OnInit {

    readonly ObjectpropertyPropertiesMode = ObjectpropertyPropertiesMode;
    readonly entityIdentifier: NormalizableEntityType = 'objectProperty';

    /** current entity value */
    currentEntity: ObjectPropertyBO<Raw>;

    fgProperties: UntypedFormControl;

    fgPropertiesSaveDisabled$: Observable<boolean>;

    get isLoading(): boolean {
        return this.currentEntity == null || this.currentEntity.keyword == null;
    }

    get isLoading$(): Observable<boolean> {
        return this.currentEntity$.pipe(
            map((entity: ObjectPropertyBO<Raw>) => entity == null || !this.currentEntity.keyword),
            publishReplay(1),
            refCount(),
        );
    }

    get activeFormTab(): FormTabHandle {
        return {
            isDirty: (): boolean => this.fgProperties.dirty,
            isValid: (): boolean => this.fgProperties.valid,
            save: (): Promise<void> => this.updateEntity(),
            reset: (): Promise<void> => Promise.resolve(this.formInit()),
        };
    }

    constructor(
        logger: NGXLogger,
        route: ActivatedRoute,
        router: Router,
        appState: AppStateService,
        protected entityData: ObjectPropertyDataService,
        changeDetectorRef: ChangeDetectorRef,
        private operations: ObjectPropertyOperations,
        private formBuilder: UntypedFormBuilder,
        private tableLoader: ObjectPropertyTableLoaderService,
    ) {
        super(
            logger,
            route,
            router,
            appState,
            entityData,
            changeDetectorRef,
        );
    }

    static [BREADCRUMB_RESOLVER]: ResolveBreadcrumbFn = (route, injector) => {
        const appState = injector.get<AppStateService>(AppStateService as Type<AppStateService>);
        const gtxObjectProperty = appState.now.entity.objectProperty[route.params.id];
        return of(gtxObjectProperty ? { title: gtxObjectProperty.keyword, doNotTranslate: true } : null);
    }

    ngOnInit(): void {
        super.ngOnInit();

        this.formInit();

        // assign values and validation of current entity
        this.currentEntity$.pipe(
            takeUntil(this.stopper.stopper$),
        ).subscribe((currentEntity: ObjectPropertyBO<Raw>) => {
            this.currentEntity = currentEntity;
            // fill form with entity property values
            this.formInit();
            this.changeDetectorRef.markForCheck();
        });
    }

    btnSavePropertiesOnClick(): void {
        this.updateEntity();
    }

    /**
     * Requests changes of user by id to CMS
     */
    async updateEntity(): Promise<void> {
        // assemble payload with conditional properties
        const payload: ObjectPropertyUpdateRequest = {
            nameI18n: this.fgProperties.value.nameI18n,
            descriptionI18n: this.fgProperties.value.descriptionI18n,
            type: this.fgProperties.value.type,
            constructId: this.fgProperties.value.constructId,
            categoryId: this.fgProperties.value.categoryId,
            required: this.fgProperties.value.required,
            inheritable: this.fgProperties.value.inheritable,
            syncContentset: this.fgProperties.value.syncContentset,
            syncChannelset: this.fgProperties.value.syncChannelset,
            syncVariants: this.fgProperties.value.syncVariants,
        };

        return this.operations.update(this.currentEntity.id, payload).pipe(
            detailLoading(this.appState),
            discard((updatedEntity: ObjectPropertyBO<Raw>) => {
                this.currentEntity = updatedEntity;
                this.entityData.reloadEntities();
                this.tableLoader.reload();
                this.formInit();
            }),
        ).toPromise();
    }

    private formInit(): void {
        if (this.fgProperties) {
            this.fgProperties.setValue(this.currentEntity);
        } else {
            this.fgProperties = this.formBuilder.control(this.currentEntity, createNestedControlValidator());
            this.fgPropertiesSaveDisabled$ = createFormSaveDisabledTracker(this.fgProperties);
        }
        this.fgProperties.markAsPristine();
    }

}

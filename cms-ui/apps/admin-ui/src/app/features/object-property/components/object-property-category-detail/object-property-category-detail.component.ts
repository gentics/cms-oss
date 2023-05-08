import { createFormSaveDisabledTracker, FormTabHandle } from '@admin-ui/common';
import { detailLoading } from '@admin-ui/common/utils/rxjs-loading-operators/detail-loading.operator';
import {
    BREADCRUMB_RESOLVER,
    ObjectPropertyCategoryOperations,
    ResolveBreadcrumbFn,
} from '@admin-ui/core';
import { BaseDetailComponent } from '@admin-ui/shared/components/base-detail/base-detail.component';
import { ObjectPropertyCategoryDataService } from '@admin-ui/shared/providers';
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
    ObjectPropertyCategoryBO,
    ObjectPropertyCategoryUpdateRequest,
    Raw,
} from '@gentics/cms-models';
import { NGXLogger } from 'ngx-logger';
import { Observable, of } from 'rxjs';
import { map, publishReplay, refCount, takeUntil, tap } from 'rxjs/operators';
import { ObjectPropertyCategoryPropertiesMode } from '../object-property-category-properties/object-property-category-properties.component';
import { ObjectPropertyCategoryTableLoaderService } from '../../providers';

// *************************************************************************************************
/**
 * # ObjectPropertyCategoryDetailComponent
 * Display and edit entity objectPropertyCategory detail information
 */
// *************************************************************************************************
@Component({
    selector: 'gtx-object-property-category-detail',
    templateUrl: './object-property-category-detail.component.html',
    styleUrls: ['./object-property-category-detail.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ObjectPropertyCategoryDetailComponent
    extends BaseDetailComponent<'objectPropertyCategory', ObjectPropertyCategoryOperations>
    implements OnInit {

    readonly ObjectPropertyCategoryPropertiesMode = ObjectPropertyCategoryPropertiesMode;
    readonly entityIdentifier: NormalizableEntityType = 'objectPropertyCategory';

    /** current entity value */
    currentEntity: ObjectPropertyCategoryBO<Raw>;

    fgProperties: UntypedFormControl;

    fgPropertiesSaveDisabled$: Observable<boolean>;

    get isLoading(): boolean {
        return this.currentEntity == null || this.currentEntity.globalId == null;
    }

    get isLoading$(): Observable<boolean> {
        return this.currentEntity$.pipe(
            map((entity: ObjectPropertyCategoryBO<Raw>) => entity == null || !this.currentEntity.globalId),
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
        protected entityData: ObjectPropertyCategoryDataService,
        changeDetectorRef: ChangeDetectorRef,
        private operations: ObjectPropertyCategoryOperations,
        private formBuilder: UntypedFormBuilder,
        private tableLoader: ObjectPropertyCategoryTableLoaderService,
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
        const gtxObjectPropertyCategory = appState.now.entity.objectPropertyCategory[route.params.id];
        return of(gtxObjectPropertyCategory ? { title: gtxObjectPropertyCategory.globalId, doNotTranslate: true } : null);
    }

    ngOnInit(): void {
        super.ngOnInit();

        this.formInit();

        // assign values and validation of current entity
        this.currentEntity$.pipe(
            takeUntil(this.stopper.stopper$),
        ).subscribe((currentEntity: ObjectPropertyCategoryBO<Raw>) => {
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
        const payload: ObjectPropertyCategoryUpdateRequest = {
            nameI18n: this.fgProperties.value.nameI18n,
        };
        return this.operations.update(this.currentEntity.id, payload).pipe(
            detailLoading(this.appState),
            tap((updatedEntity: ObjectPropertyCategoryBO<Raw>) => {
                this.currentEntity = updatedEntity;
                this.entityData.reloadEntities();
                this.tableLoader.reload();
                this.formInit();
            }),
            map(() => undefined),
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

import {
    createFormSaveDisabledTracker,
    detailLoading,
    discard,
    FormTabHandle,
} from '@admin-ui/common';
import {
    BREADCRUMB_RESOLVER,
    ConstructCategoryOperations,
    ResolveBreadcrumbFn,
} from '@admin-ui/core';
import { BaseDetailComponent, ConstructCategoryDataService, LanguageDataService } from '@admin-ui/shared';
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
import {
    ConstructCategoryBO,
    ConstructCategoryUpdateRequest,
    Language,
    NormalizableEntityType,
    Raw,
} from '@gentics/cms-models';
import { NGXLogger } from 'ngx-logger';
import { Observable, of } from 'rxjs';
import { map, publishReplay, refCount, takeUntil } from 'rxjs/operators';
import { ConstructCategoryPropertiesMode } from '../construct-category-properties/construct-category-properties.component';
import { ConstructCategoryTableLoaderService } from '../../providers';

// *************************************************************************************************
/**
 * # ConstructCategoryDetailComponent
 * Display and edit entity constructCategory detail information
 */
// *************************************************************************************************
@Component({
    selector: 'gtx-construct-category-detail',
    templateUrl: './construct-category-detail.component.html',
    styleUrls: ['./construct-category-detail.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConstructCategoryDetailComponent
    extends BaseDetailComponent<'constructCategory', ConstructCategoryOperations>
    implements OnInit {

    // tslint:disable-next-line: variable-name
    readonly ConstructCategoryPropertiesMode = ConstructCategoryPropertiesMode;

    entityIdentifier: NormalizableEntityType = 'constructCategory';

    /** current entity value */
    currentEntity: ConstructCategoryBO<Raw>;

    fgProperties: UntypedFormControl;

    fgPropertiesSaveDisabled$: Observable<boolean>;
    supportedLanguages$: Observable<Language[]>;

    get isLoading(): boolean {
        return this.currentEntity == null || this.currentEntity.globalId == null;
    }

    get isLoading$(): Observable<boolean> {
        return this.currentEntity$.pipe(
            map((entity: ConstructCategoryBO<Raw>) => entity == null || !this.currentEntity.globalId),
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
        changeDetectorRef: ChangeDetectorRef,
        entityData: ConstructCategoryDataService,
        private operations: ConstructCategoryOperations,
        private formBuilder: UntypedFormBuilder,
        private languageData: LanguageDataService,
        private tableLoader: ConstructCategoryTableLoaderService,
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
        const gtxConstructCategory = appState.now.entity.constructCategory[route.params.id];
        return of(gtxConstructCategory ? { title: gtxConstructCategory.globalId, doNotTranslate: true } : null);
    }

    ngOnInit(): void {
        super.ngOnInit();

        this.formInit();

        // assign values and validation of current entity
        this.currentEntity$.pipe(
            takeUntil(this.stopper.stopper$),
        ).subscribe((currentEntity: ConstructCategoryBO<Raw>) => {
            this.currentEntity = currentEntity;
            // fill form with entity property values
            this.formInit();
            this.changeDetectorRef.markForCheck();
        });

        // get available system languages for i18n-properties
        this.supportedLanguages$ = this.languageData.watchSupportedLanguages();
    }

    btnSavePropertiesOnClick(): void {
        this.updateEntity();
    }

    /**
     * Requests changes of user by id to CMS
     */
    async updateEntity(): Promise<void> {
        // assemble payload with conditional properties
        const payload: ConstructCategoryUpdateRequest = {
            nameI18n: this.fgProperties.value.nameI18n,
        };

        return this.operations.update(this.currentEntity.id, payload).pipe(
            detailLoading(this.appState),
            discard((updatedEntity: ConstructCategoryBO<Raw>) => {
                this.currentEntity = updatedEntity;
                this.entityData.reloadEntities();
                this.tableLoader.reload();
                this.formInit();
            }),
        ).toPromise();
    }

    private formInit(): void {
        if (this.fgProperties) {
            this.fgProperties.setValue(structuredClone(this.currentEntity));
        } else {
            this.fgProperties = this.formBuilder.control(structuredClone(this.currentEntity));
            this.fgPropertiesSaveDisabled$ = createFormSaveDisabledTracker(this.fgProperties);
        }
        this.fgProperties.markAsPristine();
    }

}

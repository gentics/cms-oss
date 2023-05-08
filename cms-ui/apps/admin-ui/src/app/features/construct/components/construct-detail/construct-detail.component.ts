import {
    createFormSaveDisabledTracker,
    detailLoading,
    FormTabHandle,
    hasInstancePermission,
} from '@admin-ui/common';
import {
    BREADCRUMB_RESOLVER,
    ConstructOperations,
    ConstructTableLoaderService,
    EditorTabTrackerService,
    ResolveBreadcrumbFn,
} from '@admin-ui/core';
import { BaseDetailComponent, ConstructDataService, LanguageDataService } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnDestroy,
    OnInit,
    Type,
} from '@angular/core';
import { AbstractControl, UntypedFormControl, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { CONTROL_INVALID_VALUE } from '@gentics/cms-components';
import {
    ConstructUpdateRequest,
    Index,
    Language,
    NormalizableEntityType,
    Raw,
    SingleInstancePermissionType,
    TagTypeBO,
} from '@gentics/cms-models';
import { isEqual } from 'lodash';
import { NGXLogger } from 'ngx-logger';
import { combineLatest, Observable, of, Subscription } from 'rxjs';
import { delay, first, map, publishReplay, refCount, repeat, takeUntil, tap } from 'rxjs/operators';
import { ConstructPropertiesMode } from '../construct-properties/construct-properties.component';

export enum ConstructDetailTabs {
    PROPERTIES = 'properties',
    PARTS = 'parts',
}

// *************************************************************************************************
/**
 * # ConstructDetailComponent
 * __Note:__ *Construct* is the new term for *TagType*.
 * Display and edit entity construct detail information
 */
// *************************************************************************************************
@Component({
    selector: 'gtx-construct-detail',
    templateUrl: './construct-detail.component.html',
    styleUrls: ['./construct-detail.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConstructDetailComponent
    extends BaseDetailComponent<'construct', ConstructOperations>
    implements OnInit, OnDestroy {

    // tslint:disable-next-line: variable-name
    public readonly ConstructDetailTabs = ConstructDetailTabs;
    // tslint:disable-next-line: variable-name
    public readonly ConstructPropertiesMode = ConstructPropertiesMode;

    public readonly entityIdentifier: NormalizableEntityType = 'construct';

    public activeTabId$: Observable<ConstructDetailTabs>;

    /** current entity value */
    public currentEntity: TagTypeBO<Raw>;
    public currentActiveTabId: ConstructDetailTabs;

    public fgProperties: UntypedFormControl;
    public fgParts: UntypedFormControl;

    public fgPropertiesSaveDisabled$: Observable<boolean>;
    public fgPartsSaveDisabled$: Observable<boolean>
    public supportedLanguages$: Observable<Language[]>;

    get isLoading(): boolean {
        return this.currentEntity == null || this.currentEntity.keyword == null;
    }

    get isLoading$(): Observable<boolean> {
        return this.currentEntity$.pipe(
            map((entity: TagTypeBO<Raw>) => entity == null || !this.currentEntity.keyword),
            publishReplay(1),
            refCount(),
        );
    }

    get activeFormTab(): FormTabHandle {
        return this.tabHandles[this.appState.now.ui.editorTab];
    }

    private tabHandles: Index<ConstructDetailTabs, FormTabHandle> = {} as any;
    private subscriptions: Subscription[] = [];

    constructor(
        logger: NGXLogger,
        route: ActivatedRoute,
        router: Router,
        appState: AppStateService,
        changeDetectorRef: ChangeDetectorRef,
        entityData: ConstructDataService,
        private operations: ConstructOperations,
        private languageData: LanguageDataService,
        private editorTabTracker: EditorTabTrackerService,
        private tableLoader: ConstructTableLoaderService,
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
        const gtxTagType = appState.now.entity.construct[route.params.id];
        return of(gtxTagType ? { title: gtxTagType.name, doNotTranslate: true } : null);
    }

    ngOnInit(): void {
        super.ngOnInit();

        this.initForms();

        // assign values and validation of current entity
        this.currentEntity$.pipe(
            takeUntil(this.stopper.stopper$),
        ).subscribe((currentEntity: TagTypeBO<Raw>) => {
            this.currentEntity = currentEntity;
            // fill form with entity property values
            this.initForms();
            this.changeDetectorRef.markForCheck();
        });

        // Hacky way to make tabs properly select, as they get stuck on the first value and don't
        // render anything.
        this.activeTabId$ = this.editorTabTracker.trackEditorTab(this.route).pipe(
            map((tabId: ConstructDetailTabs) => !Object.values(ConstructDetailTabs).includes(tabId) ? ConstructDetailTabs.PROPERTIES : tabId),
            tap(id => this.currentActiveTabId = id),
            repeat(1),
            delay(10),
        );

        // get available system languages for i18n-properties
        this.supportedLanguages$ = this.languageData.watchSupportedLanguages();
    }

    ngOnDestroy(): void {
        super.ngOnDestroy();
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    btnSavePropertiesOnClick(): void {
        this.updateEntity();
    }

    btnSavePartsOnClick(): void {
        this.updateParts();
    }

    /**
     * Requests changes of user by id to CMS
     */
    async updateEntity(): Promise<void> {
        const payload: ConstructUpdateRequest = {
            nameI18n: this.fgProperties.value.nameI18n,
            descriptionI18n: this.fgProperties.value.descriptionI18n,
            keyword: this.fgProperties.value.keyword,
            icon: this.fgProperties.value.icon,
            newEditor: this.fgProperties.value.newEditor,
            externalEditorUrl: this.fgProperties.value.externalEditorUrl,
            mayBeSubtag: this.fgProperties.value.mayBeSubtag,
            mayContainSubtags: this.fgProperties.value.mayContainSubtags,
            categoryId: this.fgProperties.value.categoryId,
            categorySortorder: this.fgProperties.value.categorySortorder,
            autoEnable: this.fgProperties.value.autoEnable,
        };

        return this.operations.update(this.currentEntity.id, payload).pipe(
            detailLoading(this.appState),
            tap((updatedEntity: TagTypeBO<Raw>) => {
                this.currentEntity = updatedEntity;
                this.entityData.reloadEntities();
                this.tableLoader.reload();
                this.initForms();
            }),
            map(() => undefined),
        ).toPromise();
    }

    /**
     * Requests changes of user by id to CMS
     */
    async updateParts(): Promise<void> {
        const payload: ConstructUpdateRequest = {
            parts: this.fgParts.value,
        };

        return this.operations.update(this.currentEntity.id, payload).pipe(
            detailLoading(this.appState),
            tap((updatedEntity: TagTypeBO<Raw>) => {
                this.currentEntity = updatedEntity;
                this.entityData.reloadEntities();
                this.initForms();
            }),
            map(() => undefined),
        ).toPromise();
    }

    private initForms(): void {
        if (this.currentEntity) {
            this.initPropertiesForm();
            this.initPartsForm();

            this.tabHandles = {
                [ConstructDetailTabs.PROPERTIES]: {
                    isDirty: () => this.fgProperties.dirty,
                    isValid: () => this.fgProperties.valid,
                    save: (): Promise<void> => this.updateEntity(),
                    reset: (): Promise<void> => Promise.resolve(this.initPropertiesForm()),
                },
                [ConstructDetailTabs.PARTS]: {
                    isDirty: () => this.fgParts.dirty,
                    isValid: () => this.fgParts.valid,
                    save: () => this.updateParts(),
                    reset: () => Promise.resolve(this.initPartsForm()),
                },
            };
        }
    }

    private initPropertiesForm(): void {
        if (this.fgProperties) {
            this.fgProperties.setValue({ ...this.currentEntity });
        } else {
            this.fgProperties = new UntypedFormControl({ ...this.currentEntity }, Validators.required);
            this.fgPropertiesSaveDisabled$ = combineLatest([
                this.currentEntity$.pipe(
                    map(item => hasInstancePermission(item, SingleInstancePermissionType.EDIT)),
                ),
                createFormSaveDisabledTracker(this.fgProperties),
            ]).pipe(
                map(([hasPermission, formInvalid]) => !hasPermission || formInvalid),
            );
        }
        this.fgProperties.markAsPristine();
    }

    private initPartsForm(): void {
        if (this.fgParts) {
            this.fgParts.setValue([...this.currentEntity.parts]);
        } else {
            this.fgParts = new UntypedFormControl([...this.currentEntity.parts], (control) => {
                if (control == null || control.value == null) {
                    return { null: true };
                }
                if (control.value === CONTROL_INVALID_VALUE) {
                    return { nestedError: true };
                }
                if (!Array.isArray(control.value)) {
                    return { notArray: true };
                }
                const missingArray: number[] = [];
                const invalidArray: number[] = [];

                control.value.forEach((partValue, index) => {
                    if (partValue == null) {
                        missingArray.push(index);
                    } else if (partValue === CONTROL_INVALID_VALUE) {
                        invalidArray.push(index);
                    }
                });

                if (missingArray.length > 0) {
                    return { missingParts: missingArray };
                }
                if (invalidArray.length > 0) {
                    return { invalidParts: invalidArray };
                }

                return null;
            });

            this.fgPartsSaveDisabled$ = combineLatest([
                this.currentEntity$.pipe(
                    map(item => hasInstancePermission(item, SingleInstancePermissionType.EDIT)),
                ),
                createFormSaveDisabledTracker(this.fgParts),
            ]).pipe(
                map(([hasPermission, formInvalid]) => !hasPermission || formInvalid),
            );
        }

        this.applyDirtCorrection(this.fgParts);
        this.fgParts.markAsPristine();
    }

    private applyDirtCorrection(control: AbstractControl): void {
        let isFirst = true;
        let oldValue = control.value;

        this.subscriptions.push(control.valueChanges.subscribe(value => {
            if (isFirst || isEqual(oldValue, value)) {
                control.markAsPristine();
                isFirst = false;
            }
            oldValue = value;
        }));
    }

}

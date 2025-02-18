import { ContentRepositoryFragmentDetailTabs, createFormSaveDisabledTracker, FormGroupTabHandle, FormTabHandle, NULL_FORM_TAB_HANDLE } from '@admin-ui/common';
import { discard } from '@admin-ui/common/utils/rxjs-discard-operator/discard.opertator';
import { detailLoading } from '@admin-ui/common/utils/rxjs-loading-operators/detail-loading.operator';
import {
    BREADCRUMB_RESOLVER,
    ContentRepositoryFragmentOperations,
    CRFragmentTableLoaderService,
    EditorTabTrackerService,
    ResolveBreadcrumbFn,
} from '@admin-ui/core';
import { BaseDetailComponent, ContentRepositoryFragmentDataService, TagmapEntryDisplayFields } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, Type } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {
    ContentRepositoryFragmentBO,
    ContentRepositoryFragmentUpdateRequest,
    NormalizableEntityType,
    Normalized,
    Raw,
} from '@gentics/cms-models';
import { Observable, of } from 'rxjs';
import { delay, repeat, takeUntil } from 'rxjs/operators';

@Component({
    selector: 'gtx-content-repository-fragment-detail',
    templateUrl: './cr-fragment-detail.component.html',
    styleUrls: ['./cr-fragment-detail.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContentRepositoryFragmentDetailComponent
    extends BaseDetailComponent<'contentRepositoryFragment', ContentRepositoryFragmentOperations>
    implements OnInit {

    public readonly ContentRepositoryFragmentDetailTabs = ContentRepositoryFragmentDetailTabs;
    public readonly TagmapEntryDisplayFields = TagmapEntryDisplayFields;

    entityIdentifier: NormalizableEntityType = 'contentRepositoryFragment';

    /** current entity value */
    currentEntity: ContentRepositoryFragmentBO<Raw>;

    /** form of tab 'Properties' */
    fgProperties: UntypedFormGroup;

    fgPropertiesSaveDisabled$: Observable<boolean>;

    activeTabId$: Observable<string>;

    private tabHandles: Record<ContentRepositoryFragmentDetailTabs, FormTabHandle>;

    constructor(
        route: ActivatedRoute,
        router: Router,
        appState: AppStateService,
        dataProvider: ContentRepositoryFragmentDataService,
        changeDetectorRef: ChangeDetectorRef,
        private entitiyOperations: ContentRepositoryFragmentOperations,
        private editorTabTracker: EditorTabTrackerService,
        private tableLoader: CRFragmentTableLoaderService,
    ) {
        super(
            route,
            router,
            appState,
            dataProvider,
            changeDetectorRef,
        );
    }

    static [BREADCRUMB_RESOLVER]: ResolveBreadcrumbFn = (route, injector) => {
        const appState = injector.get<AppStateService>(AppStateService as Type<AppStateService>);
        const fragment = appState.now.entity.contentRepositoryFragment[route.params.id];
        return of(fragment ? { title: fragment.name, doNotTranslate: true } : null);
    }

    get isLoading(): boolean {
        return this.currentEntity == null || !this.currentEntity.id || this.currentEntity.id === '';
    }

    get activeFormTab(): FormTabHandle {
        return this.tabHandles[this.appState.now.ui.editorTab];
    }

    public ngOnInit(): void {
        super.ngOnInit();

        this.initForms();

        // assign values and validation of current entity
        this.currentEntity$.pipe(
            takeUntil(this.stopper.stopper$),
        ).subscribe((currentEntity: ContentRepositoryFragmentBO<Raw>) => {
            this.currentEntity = this.appState.now.entity.contentRepositoryFragment[currentEntity.id];
            // fill form with entity property values
            this.fgPropertiesUpdate(this.currentEntity);
            this.changeDetectorRef.markForCheck();
        });

        // Hacky way to make tabs properly select, as they get stuck on the first value and don't
        // render anything.
        this.activeTabId$ = this.editorTabTracker.trackEditorTab(this.route).pipe(
            repeat(1),
            delay(10),
        );
    }

    btnSavePropertiesOnClick(): void {
        this.updateEntity();
    }

    /**
     * Requests changes of fragment by id to CMS
     */
    updateEntity(): Promise<void> {
        // assemble payload with conditional properties
        const newEntity: ContentRepositoryFragmentUpdateRequest = {
            ...(this.fgProperties.value.name && { name: this.fgProperties.value.name }),
        };

        return this.entitiyOperations.update(this.currentEntity.id, newEntity).pipe(
            detailLoading(this.appState),
            discard((updated: ContentRepositoryFragmentBO<Raw>) => {
                this.currentEntity = updated;
                this.fgProperties.markAsPristine();
                this.tableLoader.reload();
            }),
        ).toPromise();
    }

    /**
     * Set new value of form 'Properties'
     */
    protected fgPropertiesUpdate(fragment: ContentRepositoryFragmentBO<Normalized | Raw>): void {
        this.fgProperties.setValue({
            name: fragment.name,
        });
        this.fgProperties.markAsPristine();
    }

    private initForms(): void {
        this.fgPropertiesInit();

        this.tabHandles = {
            [ContentRepositoryFragmentDetailTabs.PROPERTIES]: new FormGroupTabHandle(this.fgProperties, {
                save: () => this.updateEntity(),
            }),
            [ContentRepositoryFragmentDetailTabs.ENTRIES]: NULL_FORM_TAB_HANDLE,
        };
    }

    /**
     * Initialize form 'Properties'
     */
    protected fgPropertiesInit(): void {
        this.fgProperties = new UntypedFormGroup({
            name: new UntypedFormControl(''),
        });

        this.fgPropertiesSaveDisabled$ = createFormSaveDisabledTracker(this.fgProperties);
    }
}

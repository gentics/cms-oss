import { ContentRepositoryDetailTabs, createFormSaveDisabledTracker, detailLoading, FormTabHandle, NULL_FORM_TAB_HANDLE } from '@admin-ui/common';
import {
    ContentRepositoryOperations,
    EditorTabTrackerService,
    ResolveBreadcrumbFn,
} from '@admin-ui/core';
import { BREADCRUMB_RESOLVER, ContentRepositoryTableLoaderService } from '@admin-ui/core/providers';
import { ContentRepositoryDataService, TagmapEntryDisplayFields, TagmapEntryPropertiesMode } from '@admin-ui/shared';
import { BaseDetailComponent } from '@admin-ui/shared/components';
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
    ContentRepositoryBO,
    ContentRepositoryType,
    ContentRepositoryUpdateRequest,
    Index,
    NormalizableEntityType,
    Raw,
} from '@gentics/cms-models';
import { NGXLogger } from 'ngx-logger';
import { Observable, of } from 'rxjs';
import { map, publishReplay, refCount, takeUntil, tap } from 'rxjs/operators';


// *************************************************************************************************
/**
 * # ContentRepositoryDetailComponent
 * Display and edit entity contentRepository detail information
 */
// *************************************************************************************************
@Component({
    selector: 'gtx-content-repository-detail',
    templateUrl: './content-repository-detail.component.html',
    styleUrls: ['./content-repository-detail.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContentRepositoryDetailComponent
    extends BaseDetailComponent<'contentRepository', ContentRepositoryOperations>
    implements OnInit {

    public readonly ContentRepositoryDetailTabs = ContentRepositoryDetailTabs;
    public readonly TagmapEntryPropertiesMode = TagmapEntryPropertiesMode;
    public readonly TagmapEntryDisplayFields = TagmapEntryDisplayFields;
    public readonly ContentRepositoryType = ContentRepositoryType;

    public readonly entityIdentifier: NormalizableEntityType = 'contentRepository';

    /** current entity value */
    currentEntity: ContentRepositoryBO<Raw>;

    fgProperties: UntypedFormControl;

    fgPropertiesSaveDisabled$: Observable<boolean>;

    get isLoading(): boolean {
        return this.currentEntity == null || this.currentEntity.name == null;
    }

    get isLoading$(): Observable<boolean> {
        return this.currentEntity$.pipe(
            map((entity: ContentRepositoryBO<Raw>) => entity == null || !this.currentEntity.name),
            publishReplay(1),
            refCount(),
        );
    }

    get activeFormTab(): FormTabHandle {
        return this.tabHandles[this.appState.now.ui.editorTab] || ContentRepositoryDetailTabs.PROPERTIES;
    }

    activeTabId$: Observable<string>;

    private tabHandles: Index<ContentRepositoryDetailTabs, FormTabHandle>;

    constructor(
        logger: NGXLogger,
        route: ActivatedRoute,
        router: Router,
        appState: AppStateService,
        entityData: ContentRepositoryDataService,
        changeDetectorRef: ChangeDetectorRef,
        private operations: ContentRepositoryOperations,
        private editorTabTracker: EditorTabTrackerService,
        private formBuilder: UntypedFormBuilder,
        private tableLoader: ContentRepositoryTableLoaderService,
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
        const gtxContentRepository = appState.now.entity.contentRepository[route.params.id];
        return of(gtxContentRepository ? { title: gtxContentRepository.name, doNotTranslate: true } : null);
    }

    ngOnInit(): void {
        super.ngOnInit();

        this.tabHandles = {
            [ContentRepositoryDetailTabs.PROPERTIES]: NULL_FORM_TAB_HANDLE,
            [ContentRepositoryDetailTabs.TAGMAP]: NULL_FORM_TAB_HANDLE,
            [ContentRepositoryDetailTabs.STURCTURE_CHECK_RESULT]: NULL_FORM_TAB_HANDLE,
            [ContentRepositoryDetailTabs.DATA_CHECK_RESULT]: NULL_FORM_TAB_HANDLE,
        };

        this.formInit();

        // assign values and validation of current entity
        this.currentEntity$.pipe(
            takeUntil(this.stopper.stopper$),
        ).subscribe((currentEntity: ContentRepositoryBO<Raw>) => {
            this.currentEntity = currentEntity;
            // fill form with entity property values
            this.formInit();
            this.changeDetectorRef.markForCheck();
        });

        this.activeTabId$ = this.editorTabTracker.trackEditorTab(this.route);
    }

    btnSavePropertiesOnClick(): void {
        this.updateEntity();
    }

    /**
     * Requests changes of user by id to CMS
     */
    updateEntity(): Promise<void> {
        // assemble payload with conditional properties
        const payload: ContentRepositoryUpdateRequest = {
            name: this.fgProperties.value.name,
            crType: this.fgProperties.value.crType,
            dbType: this.fgProperties.value.dbType,
            username: this.fgProperties.value.username,
            password: this.fgProperties.value.password,
            usePassword: this.fgProperties.value.usePassword,
            url: this.fgProperties.value.url,
            basepath: this.fgProperties.value.basepath,
            instantPublishing: this.fgProperties.value.instantPublishing,
            languageInformation: this.fgProperties.value.languageInformation,
            permissionInformation: this.fgProperties.value.permissionInformation,
            permissionProperty: this.fgProperties.value.permissionProperty,
            defaultPermission: this.fgProperties.value.defaultPermission,
            diffDelete: this.fgProperties.value.diffDelete,
            elasticsearch: this.fgProperties.value.elasticsearch,
            projectPerNode: this.fgProperties.value.projectPerNode,
        };
        return this.operations.update(this.currentEntity.id, payload).pipe(
            detailLoading(this.appState),
            tap((updatedEntity: ContentRepositoryBO<Raw>) => {
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
            this.fgProperties = this.formBuilder.control(this.currentEntity);
            this.fgPropertiesSaveDisabled$ = createFormSaveDisabledTracker(this.fgProperties);
        }
        this.fgProperties.markAsPristine();
    }

}

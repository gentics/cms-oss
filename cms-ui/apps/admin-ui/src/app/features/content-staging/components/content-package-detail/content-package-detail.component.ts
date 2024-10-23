import { ContentPackageDetailTabs, createFormSaveDisabledTracker, FormGroupTabHandle, FormTabHandle } from '@admin-ui/common';
import { BREADCRUMB_RESOLVER, ContentPackageOperations, EditorTabTrackerService, ResolveBreadcrumbFn } from '@admin-ui/core';
import { BaseDetailComponent, ContentPackageDataService } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, Type } from '@angular/core';
import { UntypedFormControl, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ContentPackageBO, NormalizableEntityType, Normalized, Raw } from '@gentics/cms-models';
import { NGXLogger } from 'ngx-logger';
import { Observable, of } from 'rxjs';
import { delay, repeat, takeUntil } from 'rxjs/operators';
import { ContentPackageTableLoaderService } from '../../providers';
import { ContentPackagePropertiesMode } from '../content-package-properties/content-package-properties.component';

@Component({
    selector: 'gtx-content-package-detail',
    templateUrl: './content-package-detail.component.html',
    styleUrls: ['./content-package-detail.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContentPackageDetailComponent extends BaseDetailComponent<'contentPackage', ContentPackageOperations> implements OnInit {

    readonly ContentPackageDetailTabs = ContentPackageDetailTabs;
    readonly ContentPackagePropertiesMode = ContentPackagePropertiesMode;

    entityIdentifier: NormalizableEntityType = 'contentPackage';

    /** current entity value */
    currentEntity: ContentPackageBO<Raw>;

    /** form of tab 'Properties' */
    fgProperties: UntypedFormControl;

    fgPropertiesSaveDisabled$: Observable<boolean>;

    activeTabId$: Observable<string>;

    private tabHandles: Record<ContentPackageDetailTabs, FormTabHandle>;

    constructor(
        logger: NGXLogger,
        route: ActivatedRoute,
        router: Router,
        appState: AppStateService,
        dataProvider: ContentPackageDataService,
        changeDetectorRef: ChangeDetectorRef,
        private entitiyOperations: ContentPackageOperations,
        private editorTabTracker: EditorTabTrackerService,
        private tableLoader: ContentPackageTableLoaderService,
    ) {
        super(
            logger,
            route,
            router,
            appState,
            dataProvider,
            changeDetectorRef,
        );
    }

    static [BREADCRUMB_RESOLVER]: ResolveBreadcrumbFn = (route, injector) => {
        const appState = injector.get<AppStateService>(AppStateService as Type<AppStateService>);
        const pkg = appState.now.entity.contentPackage[route.params.id];
        return of(pkg ? { title: pkg.name, doNotTranslate: true } : null);
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
        ).subscribe((currentEntity: ContentPackageBO<Raw>) => {
            this.currentEntity = this.appState.now.entity.contentPackage[currentEntity.id];
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

    override async updateEntity(): Promise<void> {
        this.entitiyOperations.update(this.currentEntity.id, this.fgProperties.value).subscribe(newEntity => {
            this.currentEntity = newEntity;
            this.fgProperties.markAsPristine();
            this.tableLoader.reload();
        });
    }

    btnSavePropertiesOnClick(): void {
        this.updateEntity();
    }

    private initForms(): void {
        this.fgPropertiesInit();

        this.tabHandles = {
            [ContentPackageDetailTabs.PROPERTIES]: new FormGroupTabHandle(this.fgProperties, {
                save: () => this.updateEntity(),
            }),
            [ContentPackageDetailTabs.IMPORT_ERRORS]: new FormGroupTabHandle(this.fgProperties, {
                save: () => Promise.resolve(),
            }),
        };
    }

    /**
     * Initialize form 'Properties'
     */
    protected fgPropertiesInit(): void {
        this.fgProperties = new UntypedFormControl(this.currentEntity, Validators.required);
        this.fgPropertiesSaveDisabled$ = createFormSaveDisabledTracker(this.fgProperties);
    }

    /**
     * Set new value of form 'Properties'
     */
    protected fgPropertiesUpdate(pkg: ContentPackageBO<Normalized | Raw>): void {
        this.fgProperties.setValue(pkg);
        this.fgProperties.markAsPristine();
    }

    public reloadEntity(): void {
        this.entitiyOperations.get(this.currentEntity.name).subscribe(pkg => {
            this.currentEntity = pkg;
            this.changeDetectorRef.markForCheck();
        });
    }
}

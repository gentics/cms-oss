import { FormTabHandle, NULL_FORM_TAB_HANDLE } from '@admin-ui/common';
import {
    BREADCRUMB_RESOLVER,
    EditorTabTrackerService,
    PackageOperations,
    ResolveBreadcrumbFn,
} from '@admin-ui/core';
import { BaseDetailComponent, PackageDataService } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnInit,
    Type,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import {
    Index,
    NormalizableEntityType,
    PackageBO,
    Raw,
} from '@gentics/cms-models';
import { NGXLogger } from 'ngx-logger';
import { Observable, of } from 'rxjs';
import { map, publishReplay, refCount, takeUntil } from 'rxjs/operators';

export enum PackageDetailTabs {
    CONSTRUCTS = 'constructs',
    CONTENT_REPOSITORIES = 'content-repositories',
    CR_FRAGMENTS = 'cr-fragments',
    DATA_SOURCES = 'data-sources',
    OBJECT_PROPERTIES = 'object-properties',
    TEMPLATES = 'templates',
    CONSISTENCY_CHECK = 'consistency-check',
}

// *************************************************************************************************
/**
 * # PackageDetailComponent
 * Display and edit entity package detail information
 */
// *************************************************************************************************
@Component({
    selector: 'gtx-package-detail',
    templateUrl: './package-detail.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PackageDetailComponent extends BaseDetailComponent<'package', PackageOperations> implements OnInit {

    public readonly PackageDetailTabs = PackageDetailTabs;

    public readonly entityIdentifier: NormalizableEntityType = 'package';

    /** current entity value */
    public currentEntity: PackageBO<Raw>;

    get isLoading(): boolean {
        return this.currentEntity == null || !this.currentEntity.name || this.currentEntity.name === '';
    }

    get isLoading$(): Observable<boolean> {
        return this.currentEntity$.pipe(
            map((entity: PackageBO<Raw>) => entity == null || !this.currentEntity.name),
            publishReplay(1),
            refCount(),
        );
    }

    get activeFormTab(): FormTabHandle {
        return this.tabHandles[this.appState.now.ui.editorTab] || PackageDetailTabs.CONSTRUCTS;
    }

    activeTabId$: Observable<string>;

    private tabHandles: Index<PackageDetailTabs, FormTabHandle>;

    constructor(
        logger: NGXLogger,
        route: ActivatedRoute,
        router: Router,
        appState: AppStateService,
        packageData: PackageDataService,
        changeDetectorRef: ChangeDetectorRef,
        private editorTabTracker: EditorTabTrackerService,
    ) {
        super(
            logger,
            route,
            router,
            appState,
            packageData,
            changeDetectorRef,
        );
    }

    static [BREADCRUMB_RESOLVER]: ResolveBreadcrumbFn = (route, injector) => {
        const appState = injector.get<AppStateService>(AppStateService as Type<AppStateService>);
        const gtxPackage = appState.now.entity.package[route.params.id];
        return of(gtxPackage ? { title: gtxPackage.name, doNotTranslate: true } : null);
    }

    ngOnInit(): void {
        super.ngOnInit();

        // init forms
        this.initForms();

        // assign values and validation of current entity
        this.currentEntity$.pipe(
            takeUntil(this.stopper.stopper$),
        ).subscribe((currentEntity: PackageBO<Raw>) => {
            this.currentEntity = currentEntity;
        });

        this.activeTabId$ = this.editorTabTracker.trackEditorTab(this.route);
    }

    private initForms(): void {

        this.tabHandles = {
            [PackageDetailTabs.CONSTRUCTS]: NULL_FORM_TAB_HANDLE,
            [PackageDetailTabs.CONTENT_REPOSITORIES]: NULL_FORM_TAB_HANDLE,
            [PackageDetailTabs.CR_FRAGMENTS]: NULL_FORM_TAB_HANDLE,
            [PackageDetailTabs.DATA_SOURCES]: NULL_FORM_TAB_HANDLE,
            [PackageDetailTabs.OBJECT_PROPERTIES]: NULL_FORM_TAB_HANDLE,
            [PackageDetailTabs.TEMPLATES]: NULL_FORM_TAB_HANDLE,
            [PackageDetailTabs.CONSISTENCY_CHECK]: NULL_FORM_TAB_HANDLE,
        };
    }

}

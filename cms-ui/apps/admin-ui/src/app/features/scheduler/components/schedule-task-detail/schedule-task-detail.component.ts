import { createFormSaveDisabledTracker, FormTabHandle, hasInstancePermission, ScheduleTaskDetailTabs } from '@admin-ui/common';
import { BREADCRUMB_RESOLVER, EditorTabTrackerService, ResolveBreadcrumbFn, ScheduleTaskOperations } from '@admin-ui/core';
import { BaseDetailComponent, ScheduleTaskDataService } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, Type } from '@angular/core';
import { UntypedFormControl, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { NormalizableEntityType, Raw, ScheduleTaskBO, SingleInstancePermissionType } from '@gentics/cms-models';
import { combineLatest, Observable, of } from 'rxjs';
import { delay, map, repeat, takeUntil } from 'rxjs/operators';
import { ScheduleTableLoaderService, ScheduleTaskTableLoaderService } from '../../providers';
import { ScheduleTaskPropertiesMode } from '../schedule-task-properties/schedule-task-properties.component';

@Component({
    selector: 'gtx-schedule-task-detail',
    templateUrl: './schedule-task-detail.component.html',
    styleUrls: ['./schedule-task-detail.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ScheduleTaskDetailComponent extends BaseDetailComponent<'scheduleTask', ScheduleTaskOperations> implements OnInit {

    readonly ScheduleTaskDetailTabs = ScheduleTaskDetailTabs;
    readonly ScheduleTaskPropertiesMode = ScheduleTaskPropertiesMode;

    entityIdentifier: NormalizableEntityType = 'scheduleTask';

    /** current entity value */
    currentEntity: ScheduleTaskBO<Raw>;

    /** form of tab 'Properties' */
    fgProperties: UntypedFormControl;

    fgPropertiesSaveDisabled$: Observable<boolean>;

    activeTabId$: Observable<string>;

    private tabHandles: Record<ScheduleTaskDetailTabs, FormTabHandle>;

    constructor(
        route: ActivatedRoute,
        router: Router,
        appState: AppStateService,
        dataProvider: ScheduleTaskDataService,
        changeDetectorRef: ChangeDetectorRef,
        private entitiyOperations: ScheduleTaskOperations,
        private editorTabTracker: EditorTabTrackerService,
        private tableLoader: ScheduleTaskTableLoaderService,
        private scheduleLoader: ScheduleTableLoaderService,
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
        const task = appState.now.entity.scheduleTask[route.params.id];
        return of(task ? { title: task.name, doNotTranslate: true } : null);
    }

    get isLoading(): boolean {
        return this.currentEntity == null || !this.currentEntity.id;
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
        ).subscribe((currentEntity: ScheduleTaskBO<Raw>) => {
            this.currentEntity = currentEntity;
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

    public async saveProperties(): Promise<void> {
        await this.entitiyOperations.update(Number(this.currentEntity.id), this.fgProperties.value).toPromise();
        this.tableLoader.reload();
        this.scheduleLoader.reload();
        this.fgProperties.markAsPristine();
    }

    public initForms(): void {
        this.initPropertiesForm();

        this.tabHandles = {
            [ScheduleTaskDetailTabs.PROPERTIES]: {
                isDirty: () => this.fgProperties.dirty,
                isValid: () => this.fgProperties.valid,
                save: (): Promise<void> => this.saveProperties(),
                reset: (): Promise<void> => Promise.resolve(this.fgProperties.reset()),
            },
        };
    }

    public initPropertiesForm(): void {
        if (this.fgProperties) {
            this.fgProperties.setValue(this.currentEntity);
        } else {
            this.fgProperties = new UntypedFormControl(this.currentEntity, Validators.required);
            this.fgPropertiesSaveDisabled$ = combineLatest([
                this.currentEntity$.pipe(
                    map(item => hasInstancePermission(item, SingleInstancePermissionType.EDIT)),
                ),
                createFormSaveDisabledTracker(this.fgProperties),
            ]).pipe(
                map(([hasPermission, formInvalid]) => !hasPermission || formInvalid),
            );
        }
    }

    public fgPropertiesUpdate(entity: ScheduleTaskBO): void {
        if (this.fgProperties) {
            this.fgProperties.setValue(entity);
        }
    }
}

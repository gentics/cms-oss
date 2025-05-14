import { createFormSaveDisabledTracker, FormTabHandle, hasInstancePermission, ScheduleDetailTabs } from '@admin-ui/common';
import { BREADCRUMB_RESOLVER, EditorTabTrackerService, ResolveBreadcrumbFn, ScheduleOperations } from '@admin-ui/core';
import { BaseDetailComponent, ScheduleDataService } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, Type } from '@angular/core';
import { UntypedFormControl, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { NormalizableEntityType, Raw, ScheduleBO, SingleInstancePermissionType } from '@gentics/cms-models';
import { isEqual } from'lodash-es'
import { combineLatest, Observable, of } from 'rxjs';
import { delay, distinctUntilChanged, filter, map, repeat, takeUntil } from 'rxjs/operators';
import { ScheduleTableLoaderService } from '../../providers';
import { SchedulePropertiesMode } from '../schedule-properties/schedule-properties.component';

@Component({
    selector: 'gtx-schedule-detail',
    templateUrl: './schedule-detail.component.html',
    styleUrls: ['./schedule-detail.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ScheduleDetailComponent extends BaseDetailComponent<'schedule', ScheduleOperations> implements OnInit {

    readonly ScheduleDetailTabs = ScheduleDetailTabs;
    readonly SchedulePropertiesMode = SchedulePropertiesMode;

    entityIdentifier: NormalizableEntityType = 'schedule';

    /** current entity value */
    currentEntity: ScheduleBO<Raw>;
    /** Current open execution id */
    currentExecId: number;

    /** form of tab 'Properties' */
    fgProperties: UntypedFormControl;

    fgPropertiesSaveDisabled$: Observable<boolean>;

    activeTabId$: Observable<string>;
    activeExecId$: Observable<number>;

    private tabHandles: Record<ScheduleDetailTabs, FormTabHandle>;

    constructor(
        route: ActivatedRoute,
        router: Router,
        appState: AppStateService,
        dataProvider: ScheduleDataService,
        changeDetectorRef: ChangeDetectorRef,
        private entitiyOperations: ScheduleOperations,
        private editorTabTracker: EditorTabTrackerService,
        private tableLoader: ScheduleTableLoaderService,
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
        const schedule = appState.now.entity.schedule[route.params.id];
        return of(schedule ? { title: schedule.name, doNotTranslate: true } : null);
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
        ).subscribe((currentEntity: ScheduleBO<Raw>) => {
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

        this.activeExecId$ = this.route.queryParamMap.pipe(
            map(params => params.get('execId')),
            filter(id => id != null),
            map(id => Number(id)),
            filter(id => Number.isInteger(id) && id > 0),
            distinctUntilChanged(isEqual),
        );
    }

    saveProperties(): void {
        this.entitiyOperations.update(Number(this.currentEntity.id), this.fgProperties.value).subscribe(newEntity => {
            this.currentEntity = newEntity;
            this.fgPropertiesUpdate(newEntity);
            this.tableLoader.reload();
            this.fgProperties.markAsPristine();
        });
    }

    public initForms(): void {
        this.initPropertiesForm();

        this.tabHandles = {
            [ScheduleDetailTabs.PROPERTIES]: {
                isDirty: () => this.fgProperties.dirty,
                isValid: () => this.fgProperties.valid,
                save: (): Promise<void> => Promise.resolve(this.saveProperties()),
                reset: (): Promise<void> => Promise.resolve(this.fgProperties.reset()),
            },
            [ScheduleDetailTabs.EXECUTIONS]: {
                isDirty: () => false,
                isValid: () => true,
                save: () => Promise.resolve(),
                reset: () => Promise.resolve(),
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

    public fgPropertiesUpdate(entity: ScheduleBO): void {
        if (this.fgProperties) {
            this.fgProperties.setValue(entity);
        }
    }
}

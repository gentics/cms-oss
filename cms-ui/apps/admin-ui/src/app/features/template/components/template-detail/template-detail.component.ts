import { createFormSaveDisabledTracker, discard, FormTabHandle, Tab, TabGroup, TemplateDetailTabs } from '@admin-ui/common';
import { detailLoading } from '@admin-ui/common/utils/rxjs-loading-operators/detail-loading.operator';
import {
    BREADCRUMB_RESOLVER,
    EditorTabTrackerService,
    NodeOperations,
    ResolveBreadcrumbFn,
    TemplateOperations,
} from '@admin-ui/core';
import { BaseDetailComponent, TemplateDataService, TemplateTableLoaderService } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit, Type } from '@angular/core';
import { UntypedFormControl, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Node, NormalizableEntityType, Normalized, Raw, TemplateBO, TemplateSaveRequest } from '@gentics/cms-models';
import { isEqual } from 'lodash-es';
import { Observable, of, Subscription } from 'rxjs';
import { delay, distinctUntilChanged, filter, first, map, repeat, startWith, switchMap, takeUntil } from 'rxjs/operators';

export enum TemplatePropertiesTabs {
    PROPERTIES = 'properties',
}

@Component({
    selector: 'gtx-template-detail',
    templateUrl: './template-detail.component.html',
    styleUrls: ['./template-detail.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TemplateDetailComponent
    extends BaseDetailComponent<'template', TemplateOperations>
    implements OnInit, OnDestroy {

    public readonly TemplateDetailTabs = TemplateDetailTabs;

    public readonly TemplatePropertiesTabs = TemplatePropertiesTabs;

    public readonly entityIdentifier: NormalizableEntityType = 'template';

    public fgPropertiesSaveDisabled$: Observable<boolean>;
    public activeTabId$: Observable<string>;
    public hasEditPermission$: Observable<boolean>;

    /** current entity value */
    public currentEntity: TemplateBO<Raw>;

    /** form of tab 'Properties' */
    public fgProperties: UntypedFormControl;
    public entityIsClean = true;

    public node: Node<Raw>;

    protected nodeId$: Observable<number>;

    protected subscription = new Subscription();

    public propertiesTabs: (Tab | TabGroup)[] = [
        {
            isGroup: false,
            id: TemplatePropertiesTabs.PROPERTIES,
            label: 'shared.title_properties',
        },
    ];

    constructor(
        route: ActivatedRoute,
        router: Router,
        appState: AppStateService,
        dataProvider: TemplateDataService,
        changeDetectorRef: ChangeDetectorRef,
        private entitiyOperations: TemplateOperations,
        private editorTabTracker: EditorTabTrackerService,
        private nodeOperations: NodeOperations,
        private tableLoader: TemplateTableLoaderService,
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
        const entity = appState.now.entity.template[route.params.id];
        return of(entity ? { title: entity.name, doNotTranslate: true } : null);
    }

    get isLoading(): boolean {
        return this.currentEntity == null || !this.currentEntity.id || this.currentEntity.id === '';
    }

    get activeFormTab(): FormTabHandle {
        return {
            isDirty: () => this.fgProperties.dirty,
            isValid: () => this.fgProperties.valid,
            save: () => this.updateEntity(),
            reset: () => Promise.resolve(this.fgPropertiesInit()),
        }
    }

    public ngOnInit(): void {
        super.ngOnInit();

        this.hasEditPermission$ = this.currentEntity$.pipe(
            switchMap(template => this.entitiyOperations.hasEditPermission(template.id)),
            startWith(false),
        );

        this.initForms();

        this.nodeId$ = this.route.paramMap.pipe(
            map(params => Number(params.get('nodeId'))),
            filter(id => typeof id === 'number' && !isNaN(id) && isFinite(id)),
            distinctUntilChanged(isEqual),
        );

        // assign values and validation of current entity
        this.currentEntity$.pipe(
            takeUntil(this.stopper.stopper$),
        ).subscribe((currentEntity: TemplateBO<Raw>) => {
            this.currentEntity = (this.appState.now.entity.template || {})[currentEntity.id];
            // fill form with entity property values
            this.fgPropertiesUpdate(this.currentEntity);
            this.entityIsClean = true;
            this.changeDetectorRef.markForCheck();
        });

        // Hacky way to make tabs properly select, as they get stuck on the first value and don't
        // render anything.
        this.activeTabId$ = this.editorTabTracker.trackEditorTab(this.route).pipe(
            repeat(1),
            delay(10),
        );

        this.subscription.add(this.nodeId$.pipe(
            switchMap(nodeId => this.nodeOperations.get(nodeId)),
        ).subscribe(node => {
            this.node = node;
        }));

        this.subscription.add(this.hasEditPermission$.subscribe(canEdit => {
            if (!canEdit) {
                this.fgProperties.disable({ emitEvent: false });
            } else {
                this.fgProperties.enable({ emitEvent: false });
            }
        }));
    }

    public ngOnDestroy(): void {
        super.ngOnDestroy();

        this.subscription.unsubscribe();
    }

    btnSavePropertiesOnClick(): void {
        this.updateEntity();
    }

    /**
     * Requests changes of fragment by id to CMS
     */
    updateEntity(): Promise<void> {
        // assemble payload with conditional properties
        const { templateTags, ...newTemplate } = this.fgProperties.value;

        const req: TemplateSaveRequest = {
            template: newTemplate,
        }

        return this.entitiyOperations.update(this.currentEntity.id, req, { nodeId: this.node.id, construct: false }).pipe(
            detailLoading(this.appState),
            discard((updated: TemplateBO<Raw>) => {
                this.currentEntity = updated;
                this.entityIsClean = true;
                this.fgProperties.markAsPristine();
                this.tableLoader.reload();
            }),
        ).toPromise();
    }

    override async detailsClose(): Promise<void> {
        try {
            // Attempt to unlock this template
            await this.entitiyOperations.unlock(this.currentEntity.id).pipe(first()).toPromise();
        } catch (err) {
            // Only write a warning in the console
            console.warn(`Could not unlock template ${this.currentEntity?.id} on close!`, err);
        }
        return super.detailsClose();
    }

    /**
     * Set new value of form 'Properties'
     */
    protected fgPropertiesUpdate(template: TemplateBO<Normalized | Raw>): void {
        this.fgProperties.setValue({
            id: template.id,
            name: template.name,
            description: template.description,
            markupLanguage: template.markupLanguage,
            source: template.source,
            objectTags: template.objectTags,
            // templateTags: template.templateTags,
        });
        this.fgProperties.markAsPristine();
    }

    private initForms(): void {
        this.fgPropertiesInit();
    }

    /**
     * Initialize form 'Properties'
     */
    protected fgPropertiesInit(): void {
        this.fgProperties = new UntypedFormControl({}, Validators.required);

        this.fgPropertiesSaveDisabled$ = createFormSaveDisabledTracker(this.fgProperties).pipe(
            switchMap(disabled => this.hasEditPermission$.pipe(
                map(hasPerm => disabled || !hasPerm),
            )),
        );
    }

}

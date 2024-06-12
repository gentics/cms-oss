import {
    EditableEntity,
    EditableEntityModels,
    EntityUpdateRequestModel,
    NULL_FORM_TAB_HANDLE,
} from '@admin-ui/common';
import { ContentRepositoryHandlerService, ContentRepositoryTableLoaderService } from '@admin-ui/core';
import { BaseEntityEditorComponent } from '@admin-ui/core/components';
import { TagmapEntryDisplayFields } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ContentRepositoryType, Feature, TagmapEntryError } from '@gentics/cms-models';
import { Subscription } from 'rxjs';
import { ContentRepositoryPropertiesMode } from '../content-repository-properties/content-repository-properties.component';

@Component({
    selector: 'gtx-content-repository-editor',
    templateUrl: './content-repository-editor.component.html',
    styleUrls: ['./content-repository-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContentRepositoryEditorComponent extends BaseEntityEditorComponent<EditableEntity.CONTENT_REPOSITORY> implements OnInit {

    public readonly ContentRepositoryPropertiesMode = ContentRepositoryPropertiesMode;
    public readonly ContentRepositoryType = ContentRepositoryType;
    public readonly TagmapEntryDisplayFields = TagmapEntryDisplayFields;

    public fgProperties: UntypedFormControl;
    public meshFeatureEnabled = false;
    public tagmapErrors: TagmapEntryError[] = [];

    private tagmapCheckSubscription: Subscription = null;

    constructor(
        changeDetector: ChangeDetectorRef,
        route: ActivatedRoute,
        router: Router,
        appState: AppStateService,
        handler: ContentRepositoryHandlerService,
        protected tableLoader: ContentRepositoryTableLoaderService,
    ) {
        super(
            EditableEntity.CONTENT_REPOSITORY,
            changeDetector,
            route,
            router,
            appState,
            handler,
        );
    }

    ngOnInit(): void {
        super.ngOnInit();

        this.subcriptions.push(this.appState.select(state => state.features.global[Feature.MESH_CR]).subscribe(enabled => {
            this.meshFeatureEnabled = enabled;
            this.changeDetector.markForCheck();
        }));

        this.checkTagmapEntries();
    }

    protected initializeTabHandles(): void {
        this.fgProperties = new UntypedFormControl({
            ...this.entity,
            elasticsearch: this.entity?.elasticsearch ? JSON.stringify(this.entity.elasticsearch, null, 4) : '',
        });
        this.tabHandles[this.Tabs.PROPERTIES] = this.createTabHandle(this.fgProperties);

        this.tabHandles[this.Tabs.TAGMAP] = NULL_FORM_TAB_HANDLE;
        this.tabHandles[this.Tabs.DATA_CHECK_RESULT] = NULL_FORM_TAB_HANDLE;
        this.tabHandles[this.Tabs.STURCTURE_CHECK_RESULT] = NULL_FORM_TAB_HANDLE;
        this.tabHandles[this.Tabs.MANAGEMENT] = NULL_FORM_TAB_HANDLE;
    }

    override onEntityUpdate(): void {
        this.tableLoader.reload();
        this.checkTagmapEntries();
    }

    protected override finalizeEntityToUpdate(
        entity: EditableEntityModels[EditableEntity.CONTENT_REPOSITORY],
    ): EntityUpdateRequestModel<EditableEntity.CONTENT_REPOSITORY> {
        return (this.handler as ContentRepositoryHandlerService).normalizeForREST(entity);
    }

    protected onEntityChange(): void {
        if (this.fgProperties) {
            this.fgProperties.reset({
                ...this.entity,
                elasticsearch: this.entity?.elasticsearch ? JSON.stringify(this.entity.elasticsearch, null, 4) : '',
            });
        }
        this.checkTagmapEntries();
    }

    private clearTagmapCheck(): void {
        if (this.tagmapCheckSubscription != null) {
            this.tagmapCheckSubscription.unsubscribe();
            this.tagmapCheckSubscription = null;
            this.tagmapErrors = [];
        }
    }

    public checkTagmapEntries(): void {
        this.clearTagmapCheck();
        this.tagmapCheckSubscription = (this.handler as ContentRepositoryHandlerService)
            .checkTagmapEntries(this.entityId)
            .subscribe(errors => {
                this.tagmapErrors = errors;
                this.changeDetector.markForCheck();
            });
    }
}

import {
    EditableEntity,
    EditableEntityModels,
    EntityUpdateRequestModel,
    NULL_FORM_TAB_HANDLE,
    ROUTE_MANAGEMENT_OUTLET,
    ROUTE_PATH_MESH,
} from '@admin-ui/common';
import { ContentRepositoryHandlerService, ContentRepositoryTableLoaderService } from '@admin-ui/core';
import { BaseEntityEditorComponent } from '@admin-ui/core/components';
import { TagmapEntryDisplayFields } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { createNestedControlValidator } from '@gentics/cms-components';
import { ContentRepositoryType, Feature } from '@gentics/cms-models';
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
    public readonly ROUTE_MESH_OUTLET = ROUTE_MANAGEMENT_OUTLET;

    public fgProperties: UntypedFormControl;
    public meshFeatureEnabled = false;

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
    }

    public handleMeshLogin(): void {
        this.router.navigate([{ outlets: { [ROUTE_MANAGEMENT_OUTLET]: [ROUTE_PATH_MESH] } }], { relativeTo: this.route });
    }

    protected initializeTabHandles(): void {
        this.fgProperties = new UntypedFormControl({
            ...this.entity,
            elasticsearch: this.entity?.elasticsearch ? JSON.stringify(this.entity.elasticsearch, null, 4) : '',
        }, createNestedControlValidator());
        this.tabHandles[this.Tabs.PROPERTIES] = this.createTabHandle(this.fgProperties);

        this.tabHandles[this.Tabs.TAGMAP] = NULL_FORM_TAB_HANDLE;
        this.tabHandles[this.Tabs.DATA_CHECK_RESULT] = NULL_FORM_TAB_HANDLE;
        this.tabHandles[this.Tabs.STURCTURE_CHECK_RESULT] = NULL_FORM_TAB_HANDLE;
        this.tabHandles[this.Tabs.MANAGEMENT] = NULL_FORM_TAB_HANDLE;
    }

    override onEntityUpdate(): void {
        this.tableLoader.reload();
    }

    protected override finalizeEntityToUpdate(
        entity: EditableEntityModels[EditableEntity.CONTENT_REPOSITORY],
    ): EntityUpdateRequestModel<EditableEntity.CONTENT_REPOSITORY> {
        if (typeof entity.elasticsearch === 'string') {
            try {
                entity.elasticsearch = JSON.parse(entity.elasticsearch as any);
            } catch (err) {
                entity.elasticsearch = null;
            }
        }

        // Don't update the password if it's blank/just whitespace!
        if (typeof entity.password === 'string' && entity.password.trim() === '') {
            entity.password = null;
        }

        return entity;
    }

    protected onEntityChange(): void {
        if (this.fgProperties) {
            this.fgProperties.reset({
                ...this.entity,
                elasticsearch: this.entity?.elasticsearch ? JSON.stringify(this.entity.elasticsearch, null, 4) : '',
            });
        }
    }
}
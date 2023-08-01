import { EditableEntity, EditableEntityModels, EntityUpdateRequestModel, NULL_FORM_TAB_HANDLE } from '@admin-ui/common';
import { ContentRepositoryHandlerService, ContentRepositoryTableLoaderService } from '@admin-ui/core';
import { BaseEntityEditorComponent } from '@admin-ui/core/components';
import { TagmapEntryDisplayFields } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { createNestedControlValidator } from '@gentics/cms-components';
import { ContentRepositoryType } from '@gentics/cms-models';
import { ContentRepositoryPropertiesMode } from '../content-repository-properties/content-repository-properties.component';

@Component({
    selector: 'gtx-content-repository-editor',
    templateUrl: './content-repository-editor.component.html',
    styleUrls: ['./content-repository-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContentRepositoryEditorComponent extends BaseEntityEditorComponent<EditableEntity.CONTENT_REPOSITORY> {

    public readonly ContentRepositoryPropertiesMode = ContentRepositoryPropertiesMode;
    public readonly ContentRepositoryType = ContentRepositoryType;
    public readonly TagmapEntryDisplayFields = TagmapEntryDisplayFields;

    public fgProperties: UntypedFormControl;

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

    protected initializeTabHandles(): void {
        this.fgProperties = new UntypedFormControl({
            ...this.entity,
            elasticsearch: this.entity?.elasticsearch ? JSON.stringify(this.entity.elasticsearch, null, 4) : '',
        }, createNestedControlValidator());
        this.tabHandles[this.Tabs.PROPERTIES] = this.createTabHandle(this.fgProperties);

        this.tabHandles[this.Tabs.TAGMAP] = NULL_FORM_TAB_HANDLE;
        this.tabHandles[this.Tabs.DATA_CHECK_RESULT] = NULL_FORM_TAB_HANDLE;
        this.tabHandles[this.Tabs.STURCTURE_CHECK_RESULT] = NULL_FORM_TAB_HANDLE;
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

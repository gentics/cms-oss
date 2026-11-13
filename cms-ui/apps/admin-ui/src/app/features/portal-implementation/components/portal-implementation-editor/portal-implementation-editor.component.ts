import { EditableEntity, NULL_FORM_TAB_HANDLE } from '@admin-ui/common';
import { BaseEntityEditorComponent } from '@admin-ui/core/components';
import { PortalImplementationHandlerService, PortalImplementationTableLoaderService } from '@admin-ui/core/providers';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { FormControl } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { PortalImplementation, PortalImplementationUpdateRequest } from '@gentics/cms-models';

@Component({
    selector: 'gtx-portal-implementation-editor',
    templateUrl: './portal-implementation-editor.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class PortalImplementationEditorComponent extends BaseEntityEditorComponent<EditableEntity.PORTAL_IMPLEMENTATION> {

    public fgProperties: FormControl<PortalImplementation>;

    constructor(
        changeDetector: ChangeDetectorRef,
        route: ActivatedRoute,
        router: Router,
        appState: AppStateService,
        handler: PortalImplementationHandlerService,
        protected tableLoader: PortalImplementationTableLoaderService,
    ) {
        super(
            EditableEntity.PORTAL_IMPLEMENTATION,
            changeDetector,
            route,
            router,
            appState,
            handler,
        );
    }

    protected initializeTabHandles(): void {
        this.fgProperties = new FormControl(this.entity);
        this.tabHandles[this.Tabs.PROPERTIES] = this.createTabHandle(this.fgProperties);
        // the files are read-only
        this.tabHandles[this.Tabs.FILES] = NULL_FORM_TAB_HANDLE;
    }

    protected onEntityChange(): void {
        if (this.fgProperties) {
            this.fgProperties.setValue(this.entity);
            this.fgProperties.markAsPristine();
        }
    }

    protected override finalizeEntityToUpdate(entity: PortalImplementation): PortalImplementationUpdateRequest {
        // only send the attributes, that can be changed
        return {
            keyword: entity.keyword,
            name: entity.name,
            description: entity.description,
            type: entity.type,
        };
    }

    override onEntityUpdate(): void {
        this.tableLoader.reload();
    }
}

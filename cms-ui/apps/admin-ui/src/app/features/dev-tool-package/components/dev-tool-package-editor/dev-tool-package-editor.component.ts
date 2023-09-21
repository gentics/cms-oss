import { EditableEntity, NULL_FORM_TAB_HANDLE } from '@admin-ui/common';
import { DevToolPackageHandlerService, DevToolPackageTableLoaderService } from '@admin-ui/core';
import { BaseEntityEditorComponent } from '@admin-ui/core/components';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, Component, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
    selector: 'gtx-dev-tool-package-editor',
    templateUrl: './dev-tool-package-editor.component.html',
    styleUrls: ['./dev-tool-package-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DevToolPackageEditorComponent extends BaseEntityEditorComponent<EditableEntity.DEV_TOOL_PACKAGE> {

    constructor(
        changeDetector: ChangeDetectorRef,
        route: ActivatedRoute,
        router: Router,
        appState: AppStateService,
        handler: DevToolPackageHandlerService,
        protected tableLoader: DevToolPackageTableLoaderService,
    ) {
        super(
            EditableEntity.DEV_TOOL_PACKAGE,
            changeDetector,
            route,
            router,
            appState,
            handler,
        );
    }

    override onEntityUpdate(): void {
        this.tableLoader.reload();
    }

    protected initializeTabHandles(): void {
        this.tabHandles[this.Tabs.CONSTRUCTS] = NULL_FORM_TAB_HANDLE;
        this.tabHandles[this.Tabs.CONTENT_REPOSITORIES] = NULL_FORM_TAB_HANDLE;
        this.tabHandles[this.Tabs.CR_FRAGMENTS] = NULL_FORM_TAB_HANDLE;
        this.tabHandles[this.Tabs.DATA_SOURCES] = NULL_FORM_TAB_HANDLE;
        this.tabHandles[this.Tabs.OBJECT_PROPERTIES] = NULL_FORM_TAB_HANDLE;
        this.tabHandles[this.Tabs.TEMPLATES] = NULL_FORM_TAB_HANDLE;
        this.tabHandles[this.Tabs.CONSISTENCY_CHECK] = NULL_FORM_TAB_HANDLE;
    }

    protected onEntityChange(): void {
    }

}

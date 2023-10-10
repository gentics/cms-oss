import { EditableEntity, NULL_FORM_TAB_HANDLE } from '@admin-ui/common';
import { DevToolPackageHandlerService, DevToolPackageTableLoaderService } from '@admin-ui/core';
import { BaseEntityEditorComponent } from '@admin-ui/core/components';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, Component, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
    selector: 'gtx-mesh-browser-editor',
    templateUrl: './mesh-browser-editor.component.html',
    styleUrls: ['./mesh-browser-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeshBrowserEditorComponent extends BaseEntityEditorComponent<EditableEntity.DEV_TOOL_PACKAGE> {

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
        // this.tabHandles[this.Tabs.PROPERTIES] = NULL_FORM_TAB_HANDLE;
    }

    protected onEntityChange(): void {
    }

}

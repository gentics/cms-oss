import { EditableEntity } from '@admin-ui/common';
import { DevToolPackageHandlerService } from '@admin-ui/core';
import { BaseEntityEditorComponent } from '@admin-ui/core/components';
import { AppStateService, SetUIFocusEntity } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
    selector: 'gtx-mesh-browser-editor',
    templateUrl: './mesh-browser-editor.component.html',
    styleUrls: ['./mesh-browser-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeshBrowserEditorComponent implements OnInit {

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected route: ActivatedRoute,
        protected router: Router,
        protected appState: AppStateService,
    ) {

    }

    ngOnInit(): void {
        console.log('init');
    }

    async detailsClose(): Promise<void> {
        // this.route.parent.parent assumes that the detail outlet routes have a root and that the specific editor tabs are child routes of this root route.
        const relativeToRoute = this.route.parent.parent || this.route.parent;
        const navigationSucceeded = await this.router.navigate([ { outlets: { detail: null } } ], { relativeTo: relativeToRoute });
        if (navigationSucceeded) {
            this.appState.dispatch(new SetUIFocusEntity(null, null, null));
        }
    }

}

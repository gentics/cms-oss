import { Component, OnInit } from '@angular/core';
import { EmbeddedTool } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ADMIN_UI_LINK } from '../../../common/config/config';
import { ApplicationStateService } from '../../../state';
import { EmbeddedToolsService } from '../../providers/embedded-tools/embedded-tools.service';
import { KeycloakService, SKIP_KEYCLOAK_PARAMETER_NAME } from '@gentics/cms-components';

@Component({
    selector: 'tool-overview',
    templateUrl: './tool-overview.component.html',
    styleUrls: ['./tool-overview.component.scss']
})
export class ToolOverviewComponent implements OnInit {

    tools$: Observable<Array<EmbeddedTool>>;
    isAdmin$: Observable<boolean>;

    adminUILink: string;

    constructor(
        private state: ApplicationStateService,
        public toolsService: EmbeddedToolsService,
        private keycloak: KeycloakService,
    ) { }

    ngOnInit(): void {
        this.tools$ = this.state.select(state => state.tools.available);
        this.isAdmin$ = this.state.select(state => state.auth.isAdmin);
        this.adminUILink = ADMIN_UI_LINK + (this.keycloak.ssoSkipped() ? '?' + SKIP_KEYCLOAK_PARAMETER_NAME : '');
    }

    adminUIClicked(event: Event): void {
        event.preventDefault();
        this.toolsService.openOrFocusAdminUI();
    }

    closeAdminUI(event: Event): void {
        event.preventDefault();
        event.stopPropagation();
        this.toolsService.closeAdminUI();
    }
}

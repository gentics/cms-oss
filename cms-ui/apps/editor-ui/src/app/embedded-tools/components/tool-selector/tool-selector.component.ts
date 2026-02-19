import { ChangeDetectionStrategy, Component, ElementRef, OnDestroy, ViewChild } from '@angular/core';
import { SKIP_KEYCLOAK_PARAMETER_NAME } from '@gentics/cms-components/auth';
import { EmbeddedTool } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ADMIN_UI_LINK } from '../../../common/config/config';
import { ApplicationStateService } from '../../../state';
import { EmbeddedToolsService } from '../../providers/embedded-tools/embedded-tools.service';

@Component({
    selector: 'tool-selector',
    templateUrl: './tool-selector.component.html',
    styleUrls: ['./tool-selector.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ToolSelectorComponent implements OnDestroy {

    tools$: Observable<Array<EmbeddedTool>>;
    visible$: Observable<boolean>;
    isAdmin$: Observable<boolean>;

    adminUILink: string;

    @ViewChild('dropdownContent', { read: ElementRef })
    content: ElementRef;

    private timeout: number;

    constructor(
        state: ApplicationStateService,
        public toolsService: EmbeddedToolsService,
    ) {
        this.tools$ = state.select(state => state.tools.available);
        this.visible$ = state.select(state =>
            ((state.tools.received && state.tools.available.length > 0) || state.ui.isAdmin),
        );
        this.isAdmin$ = state.select(state => state.ui.isAdmin);
        this.adminUILink = ADMIN_UI_LINK + (state.now.auth.ssoSkipped ? '?' + SKIP_KEYCLOAK_PARAMETER_NAME : '');
    }

    dropdownOpened(): void {
        // Hacky fix to change styles (especially overflow) on the content wrapper element.
        // No other way to do this with ui-core yet, sorry!
        this.timeout = setTimeout(() => {
            const contentWrapper = this.content.nativeElement.parentNode;
            if (contentWrapper) {
                contentWrapper.style.borderRadius = '2px';
                contentWrapper.style.overflow = 'visible';
                contentWrapper.style.marginLeft = '10px';
                contentWrapper.style.marginTop = '10px';
            }
        }) as any;
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

    ngOnDestroy(): void {
        (clearTimeout as any)(this.timeout);
    }
}

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { KeycloakService, SKIP_KEYCLOAK_PARAMETER_NAME } from '@gentics/cms-components';
import { cancelEvent } from '@gentics/ui-core';
import { I18nService } from '@gentics/cms-components';
import { Subscription } from 'rxjs';
import { filter, take } from 'rxjs/operators';
import { ADMIN_UI_LINK } from '../../../common/config/config';
import { ApplicationStateService } from '../../../state/providers/application-state/application-state.service';
import { FolderActionsService } from '../../../state/providers/folder-actions/folder-actions.service';
import { NavigationService } from '../../providers/navigation/navigation.service';

@Component({
    selector: 'gtx-no-nodes',
    templateUrl: './no-nodes.component.html',
    styleUrls: ['./no-nodes.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class NoNodesComponent implements OnInit, OnDestroy {

    // eslint-disable-next-line @typescript-eslint/naming-convention
    public readonly cancelEvent = cancelEvent;

    public isAdmin = false;
    public isLoaded = false;
    public errorMessage: SafeHtml;

    protected subscriptions: Subscription[] = [];

    adminUILink: string;

    constructor(
        private changeDetector: ChangeDetectorRef,
        private sanitizer: DomSanitizer,
        private appState: ApplicationStateService,
        private i18n: I18nService,
        private folderActions: FolderActionsService,
        private navigationService: NavigationService,
        private keycloak: KeycloakService,
    ) {}

    ngOnInit(): void {
        this.adminUILink = ADMIN_UI_LINK + (this.keycloak.ssoSkipped() ? '?' + SKIP_KEYCLOAK_PARAMETER_NAME : '');

        this.subscriptions.push(this.appState.select(state => state.folder.nodesLoaded).pipe(
            filter(loaded => loaded),
            take(1),
        ).subscribe(() => {
            this.isLoaded = true;
            this.changeDetector.markForCheck();
            this.checkForDefaultNode();
        }));

        this.subscriptions.push(this.appState.select(state => state.auth.isAdmin).subscribe(isAdmin => {
            this.isAdmin = isAdmin;
            const msg = this.i18n.instant(`editor.${isAdmin ? 'admin' : 'user'}_no_nodes_message`);
            this.errorMessage = this.sanitizer.bypassSecurityTrustHtml(msg);
            this.changeDetector.markForCheck();
        }));
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    checkForDefaultNode(): void {
        const defaultNode = this.folderActions.resolveDefaultNode();

        if (!defaultNode) {
            return;
        }

        this.navigationService.list(defaultNode.id, defaultNode.folderId).navigate();
    }
}

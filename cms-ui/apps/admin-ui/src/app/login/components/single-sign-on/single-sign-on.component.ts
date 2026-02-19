import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { API_BASE_URL } from '@gentics/cms-components';
import { AuthStateModel, KeycloakService } from '@gentics/cms-components/auth';
import { BaseComponent } from '@gentics/ui-core';
import { isEqual } from 'lodash-es';
import { distinctUntilChanged, filter, take } from 'rxjs/operators';
import { AuthOperations, ErrorHandler } from '../../../core';
import { AppStateService } from '../../../state/providers/app-state/app-state.service';

@Component({
    selector: 'gtx-single-sign-on',
    templateUrl: './single-sign-on.component.html',
    styleUrls: ['./single-sign-on.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class SingleSignOnComponent extends BaseComponent implements OnInit {

    public showLogin = false;
    public url: SafeUrl;
    public available = false;
    public showButton = false;

    constructor(
        changeDetector: ChangeDetectorRef,
        private appState: AppStateService,
        private authOps: AuthOperations,
        private domSanitizer: DomSanitizer,
        private errorHandler: ErrorHandler,
        private keycloakService: KeycloakService,
        private route: ActivatedRoute,
        private router: Router,
    ) {
        super(changeDetector);
    }

    ngOnInit(): void {
        this.subscriptions.push(this.appState.select((state) => state.auth).pipe(
            distinctUntilChanged<AuthStateModel>(isEqual),
        ).subscribe((state) => {
            this.available = state.keycloakAvailable;
            this.showButton = state.showSingleSignOnButton;
            this.changeDetector.markForCheck();

            if (this.available != null) {
                if (!this.showButton) {
                    if (this.available) {
                        this.attemptSsoWithKeycloak();
                    } else {
                        this.attemptSsoWithIframe();
                    }
                }
            }
        }));
    }

    login(): void {
        if (this.available) {
            this.keycloakService.login();
            this.attemptSsoWithKeycloak();
        } else {
            this.attemptSsoWithIframe();
        }
    }

    attemptSsoWithKeycloak(): void {
        this.subscriptions.push(this.keycloakService.attemptCmsLogin().subscribe((result: string) => {
            this.handleSsoResponse(result);
            const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/';
            const onLogin$ = this.appState.select((state) => state.auth.isLoggedIn).pipe(
                filter((isLoggedIn) => !!isLoggedIn),
            );
            onLogin$.subscribe(() => {
                this.router.navigateByUrl(returnUrl);
            });
        }));
    }

    attemptSsoWithIframe(): void {
        this.subscriptions.push(this.appState.select((state) => state.auth).pipe(
            filter((auth) => !!auth),
            take(1),
            filter((auth) => !auth.isLoggedIn),
        ).subscribe(() => {
            this.url = this.domSanitizer.bypassSecurityTrustResourceUrl(`${API_BASE_URL}/auth/ssologin?ts=${Date.now()}`);
            this.showLogin = true;
            this.changeDetector.markForCheck();
        }));
    }

    frameLoaded(frame: HTMLIFrameElement): void {
        try {
            const result = frame.contentDocument.documentElement.textContent;
            this.handleSsoResponse(result);
        } catch (error) {
            this.errorHandler.catch(error, { notification: false });
        } finally {
            this.showLogin = false;
            this.changeDetector.markForCheck();
        }
    }

    private handleSsoResponse(result: string): void {
        if (/^\d+$/.test(result)) {
            console.log('Logging in via Single-Sign-On');
            const sid = Number(result);
            this.authOps.validateSessionId(sid);
        } else {
            console.error('Unsupported SSO result: ' + result);
        }
    }
}

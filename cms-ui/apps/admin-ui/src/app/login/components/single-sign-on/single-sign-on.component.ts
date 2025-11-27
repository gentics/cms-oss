import { ChangeDetectionStrategy, Component, OnDestroy, OnInit } from '@angular/core';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { API_BASE_URL, KeycloakService } from '@gentics/cms-components';
import { BehaviorSubject } from 'rxjs';
import { filter, take, takeUntil } from 'rxjs/operators';
import { ObservableStopper } from '../../../common/utils';
import { AuthOperations, ErrorHandler } from '../../../core/providers';
import { AppStateService } from '../../../state';

@Component({
    selector: 'gtx-single-sign-on',
    templateUrl: './single-sign-on.component.html',
    styleUrls: ['./single-sign-on.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class SingleSignOnComponent implements OnInit, OnDestroy {

    enabled$ = new BehaviorSubject(false);
    url: SafeUrl;

    private stopper = new ObservableStopper();

    constructor(
        private appState: AppStateService,
        private authOps: AuthOperations,
        private domSanitizer: DomSanitizer,
        private errorHandler: ErrorHandler,
        public keycloakService: KeycloakService,
        private route: ActivatedRoute,
        private router: Router,
    ) { }

    ngOnInit(): void {
        if (!this.keycloakService.showSSOButton) {
            if (this.keycloakService.keycloakEnabled) {
                this.attemptSsoWithKeycloak();
            } else {
                this.attemptSsoWithIframe();
            }
        }
    }

    login(): void {
        if (this.keycloakService.keycloakEnabled) {
            this.keycloakService.login();
            this.attemptSsoWithKeycloak();
        } else {
            this.attemptSsoWithIframe();
        }
    }

    ngOnDestroy(): void {
        this.stopper.stop();
    }

    attemptSsoWithKeycloak(): void {
        this.keycloakService.attemptCmsLogin().subscribe((result: string) => {
            this.handleSsoResponse(result);
            const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/';
            const onLogin$ = this.appState.select((state) => state.auth.isLoggedIn).pipe(
                filter((isLoggedIn) => !!isLoggedIn),
                takeUntil(this.stopper.stopper$),
            );
            onLogin$.subscribe(() => {
                this.router.navigateByUrl(returnUrl);
            });
        });
    }

    attemptSsoWithIframe(): void {
        this.appState.select((state) => state.auth).pipe(
            filter((auth) => !!auth),
            take(1),
            filter((auth) => !auth.isLoggedIn),
        ).subscribe(() => {
            this.url = this.domSanitizer.bypassSecurityTrustResourceUrl(`${API_BASE_URL}/auth/ssologin?ts=${Date.now()}`);
            this.enabled$.next(true);
        });
    }

    frameLoaded(frame: HTMLIFrameElement): void {
        try {
            const result = frame.contentDocument.documentElement.textContent;
            this.handleSsoResponse(result);
        } catch (error) {
            this.errorHandler.catch(error, { notification: false });
        } finally {
            this.enabled$.next(false);
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

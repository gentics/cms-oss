import { ChangeDetectionStrategy, Component, OnDestroy, OnInit } from '@angular/core';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Subscription } from 'rxjs';
import { filter, take } from 'rxjs/operators';
import { KeycloakService } from '@gentics/cms-components';
import { API_BASE_URL } from '../../../common/utils/base-urls';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { LocalStorage } from '../../../core/providers/local-storage/local-storage.service';
import { ApplicationStateService, AuthActionsService } from '../../../state';

@Component({
    selector: 'single-sign-on',
    templateUrl: './single-sign-on.component.html',
    styleUrls: ['./single-sign-on.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SingleSignOn implements OnDestroy, OnInit {

    enabled$ = new BehaviorSubject(false);
    url: SafeUrl;

    private subscriptions: Subscription[] = [];

    constructor(
        private appState: ApplicationStateService,
        private authActions: AuthActionsService,
        private domSanitizer: DomSanitizer,
        private errorHandler: ErrorHandler,
        private localStorage: LocalStorage,
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
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    attemptSsoWithKeycloak(): void {
        this.keycloakService.attemptCmsLogin().subscribe((result: string) => {
            this.handleSsoResponse(result);
            const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
            if (!returnUrl) {
                return;
            }
            const onLogin$ = this.appState.select(state => state.auth.isLoggedIn).pipe(
                filter(isLoggedIn => !!isLoggedIn),
            );
            this.subscriptions.push(onLogin$.subscribe(() => {
                this.router.navigateByUrl(returnUrl);
            }));
        });
    }

    attemptSsoWithIframe(): void {
        this.appState.select(state => state.auth).pipe(
            filter(auth => !!auth),
            take(1),
            filter(auth => !auth.isLoggedIn),
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
            const sid = Number.parseInt(result, 10);
            this.localStorage.setSid(sid);
            this.authActions.validateSession();
        }
    }
}

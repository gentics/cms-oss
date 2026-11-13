import { tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { I18nNotificationService } from '@gentics/cms-components';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { of, throwError } from 'rxjs';
import { componentTest, configureComponentTest } from '../../../../testing';
import { ApplicationStateService } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { SetPasswordComponent } from './set-password.component';

const TOKEN = 'the.token';

describe('SetPasswordComponent', () => {
    let client: { auth: { validatePasswordToken: jasmine.Spy; setPassword: jasmine.Spy } };
    let router: { navigate: jasmine.Spy };
    let notification: { show: jasmine.Spy };
    let queryParams: Record<string, string>;

    beforeEach(() => {
        client = {
            auth: {
                validatePasswordToken: jasmine.createSpy('validatePasswordToken').and.returnValue(of({})),
                setPassword: jasmine.createSpy('setPassword').and.returnValue(of({})),
            },
        };
        router = { navigate: jasmine.createSpy('navigate').and.returnValue(Promise.resolve(true)) };
        notification = { show: jasmine.createSpy('show') };
        queryParams = { token: TOKEN };

        configureComponentTest({
            imports: [GenticsUICoreModule, ReactiveFormsModule],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: GCMSRestClientService, useValue: client },
                { provide: Router, useValue: router },
                { provide: I18nNotificationService, useValue: notification },
                {
                    // read when ngOnInit runs, so that a test can still change the query parameters
                    provide: ActivatedRoute,
                    useValue: { snapshot: { get queryParamMap() {
                        return convertToParamMap(queryParams);
                    } } },
                },
            ],
            declarations: [SetPasswordComponent],
        });
    });

    it('shows the form for a valid token', componentTest(() => SetPasswordComponent, (fixture, instance) => {
        fixture.detectChanges();
        tick();
        fixture.detectChanges();

        expect(client.auth.validatePasswordToken).toHaveBeenCalledWith(TOKEN);
        expect(instance.state).toBe('form');
        expect(fixture.nativeElement.querySelector('form')).not.toBeNull();
        expect(fixture.nativeElement.querySelector('[data-context="invalid-token"]')).toBeNull();
    }));

    it('shows that the link is invalid when there is no token', componentTest(() => SetPasswordComponent, (fixture, instance) => {
        queryParams = {};
        fixture.detectChanges();
        tick();
        fixture.detectChanges();

        expect(client.auth.validatePasswordToken).not.toHaveBeenCalled();
        expect(instance.state).toBe('invalid');
        expect(fixture.nativeElement.querySelector('[data-context="invalid-token"]')).not.toBeNull();
        expect(fixture.nativeElement.querySelector('form')).toBeNull();
    }));

    it('shows the message of the backend when the token is rejected', componentTest(() => SetPasswordComponent, (fixture, instance) => {
        client.auth.validatePasswordToken.and.returnValue(throwError(() => new Error('Link expired')));
        fixture.detectChanges();
        tick();
        fixture.detectChanges();

        expect(instance.state).toBe('invalid');
        const message: HTMLElement = fixture.nativeElement.querySelector('[data-context="invalid-token"]');
        expect(message.textContent).toContain('Link expired');
    }));

    it('sets the password and navigates to the login', componentTest(() => SetPasswordComponent, (fixture, instance) => {
        fixture.detectChanges();
        tick();

        instance.password1.setValue('new.pw-1');
        instance.password2.setValue('new.pw-1');
        instance.setPassword();
        tick();

        expect(client.auth.setPassword).toHaveBeenCalledWith({ token: TOKEN, password: 'new.pw-1' });
        expect(notification.show).toHaveBeenCalledWith(jasmine.objectContaining({ type: 'success' }));
        expect(router.navigate).toHaveBeenCalledWith(['/login']);
    }));

    it('does not send passwords which differ', componentTest(() => SetPasswordComponent, (fixture, instance) => {
        fixture.detectChanges();
        tick();

        instance.password1.setValue('new.pw-1');
        instance.password2.setValue('new.pw-2');
        instance.setPassword();
        tick();

        expect(instance.form.hasError('areEqual')).toBe(true);
        expect(client.auth.setPassword).not.toHaveBeenCalled();
    }));

    it('does not send a password violating the rule', componentTest(() => SetPasswordComponent, (fixture, instance) => {
        fixture.detectChanges();
        tick();

        instance.password1.setValue('two words');
        instance.password2.setValue('two words');
        instance.setPassword();
        tick();

        expect(instance.password1.hasError('pattern')).toBe(true);
        expect(client.auth.setPassword).not.toHaveBeenCalled();
    }));

    it('stays on the page and shows the error when setting fails', componentTest(() => SetPasswordComponent, (fixture, instance) => {
        client.auth.setPassword.and.returnValue(throwError(() => new Error('Link expired')));
        fixture.detectChanges();
        tick();

        instance.password1.setValue('new.pw-1');
        instance.password2.setValue('new.pw-1');
        instance.setPassword();
        tick();
        fixture.detectChanges();

        expect(router.navigate).not.toHaveBeenCalled();
        const message: HTMLElement = fixture.nativeElement.querySelector('[data-context="set-password-error"]');
        expect(message.textContent).toContain('Link expired');
    }));
});

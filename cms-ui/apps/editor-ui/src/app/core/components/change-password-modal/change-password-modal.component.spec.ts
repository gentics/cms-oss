import { TestBed, tick } from '@angular/core/testing';
import { ReactiveFormsModule, UntypedFormControl } from '@angular/forms';
import { MockI18nPipe } from '@gentics/cms-components/testing';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { componentTest, configureComponentTest } from '../../../../testing';
import { ApplicationStateService, AuthActionsService } from '../../../state';
import { ChangePasswordModal } from './change-password-modal.component';

describe('ChangePasswordModal', () => {

    beforeEach(() => {
        configureComponentTest({
            imports: [GenticsUICoreModule, ReactiveFormsModule],
            providers: [
                { provide: AuthActionsService, useValue: {} },
                { provide: ApplicationStateService, useValue: {} },
            ],
            declarations: [
                MockI18nPipe,
                ChangePasswordModal,
            ],
        });
    });

    describe('form validation', () => {

        it('should allow valid characters', componentTest(() => ChangePasswordModal, (fixture) => {
            const instance: ChangePasswordModal = fixture.componentInstance;
            const password1 = instance.form.controls['password1'] as UntypedFormControl;

            assertValid(password1, 'aA1._-@.');
            assertValid(password1, '9922325');
            assertValid(password1, 'two.words');
            assertValid(password1, '--@@_._');
        }));

        it('should not allow invalid characters', componentTest(() => ChangePasswordModal, (fixture) => {
            const instance: ChangePasswordModal = fixture.componentInstance;
            const password1 = instance.form.controls['password1'] as UntypedFormControl;

            assertInvalid(password1, 'abcd!');
            assertInvalid(password1, '       ');
            assertInvalid(password1, 'two words');
            assertInvalid(password1, '#hello');
            assertInvalid(password1, '/hello');
            assertInvalid(password1, '\\hello');
        }));

        it('should not allow passwords shorter than 4 characters', componentTest(() => ChangePasswordModal, (fixture) => {
            const instance: ChangePasswordModal = fixture.componentInstance;
            const password1 = instance.form.controls['password1'] as UntypedFormControl;

            assertInvalid(password1, '');
            assertInvalid(password1, 'a');
            assertInvalid(password1, 'ab');
            assertInvalid(password1, 'abc');
            assertValid(password1, 'abcd');
        }));

        it('should not allow passwords longer than 20 characters', componentTest(() => ChangePasswordModal, (fixture) => {
            const instance: ChangePasswordModal = fixture.componentInstance;
            const password1 = instance.form.controls['password1'] as UntypedFormControl;

            assertValid(password1, 'hello.is.it.me.you.r');
            assertInvalid(password1, 'hello.is.it.me.you.re');
            assertInvalid(password1, 'hello.is.it.me.you.re.looking.for');
        }));

        it('should require both passwords to match', componentTest(() => ChangePasswordModal, (fixture) => {
            const instance: ChangePasswordModal = fixture.componentInstance;
            const form = instance.form;
            const password1 = form.get('password1');
            const password2 = form.get('password2');
            fixture.detectChanges();

            form.get('oldPassword').setValue('oldpwd');
            password1.setValue('abcde');
            password2.setValue('abcdZ');
            form.updateValueAndValidity();
            expect(form.valid).toBe(false);

            password2.setValue('abcde');
            form.updateValueAndValidity();
            expect(form.valid).toBe(true);

        }));

        it('should require the old password', componentTest(() => ChangePasswordModal, (fixture) => {
            const instance: ChangePasswordModal = fixture.componentInstance;
            const form = instance.form;

            form.get('password1').setValue('abcde');
            form.get('password2').setValue('abcde');
            form.get('oldPassword').setValue('');
            form.updateValueAndValidity();
            expect(form.valid).toBe(false);

            form.get('oldPassword').setValue('old!pwd with chars not allowed in new passwords');
            form.updateValueAndValidity();
            expect(form.valid).toBe(true);
        }));
    });

    describe('changePassword()', () => {

        let authActions: { changePassword: jasmine.Spy };

        beforeEach(() => {
            authActions = TestBed.inject(AuthActionsService) as any;
        });

        it('should send old and new password and close on success', componentTest(() => ChangePasswordModal, (fixture) => {
            const instance: ChangePasswordModal = fixture.componentInstance;
            authActions.changePassword = jasmine.createSpy('changePassword').and.returnValue(Promise.resolve(true));
            const closeFn = spyOn(instance, 'closeFn');

            instance.form.get('oldPassword').setValue('oldpwd');
            instance.form.get('password1').setValue('newpwd');
            instance.form.get('password2').setValue('newpwd');
            instance.changePassword();
            tick();

            expect(authActions.changePassword).toHaveBeenCalledWith('oldpwd', 'newpwd');
            expect(closeFn).toHaveBeenCalled();
        }));

        it('should stay open on failure', componentTest(() => ChangePasswordModal, (fixture) => {
            const instance: ChangePasswordModal = fixture.componentInstance;
            authActions.changePassword = jasmine.createSpy('changePassword').and.returnValue(Promise.resolve(false));
            const closeFn = spyOn(instance, 'closeFn');

            instance.form.get('oldPassword').setValue('wrongpwd');
            instance.form.get('password1').setValue('newpwd');
            instance.form.get('password2').setValue('newpwd');
            instance.changePassword();
            tick();

            expect(authActions.changePassword).toHaveBeenCalledWith('wrongpwd', 'newpwd');
            expect(closeFn).not.toHaveBeenCalled();
        }));
    });
});

function assertValid(passwordControl: UntypedFormControl, testString: string): void {
    passwordControl.setValue(testString);
    passwordControl.updateValueAndValidity();
    expect(passwordControl.valid).toBe(true, `test string: "${testString}"`);
}

function assertInvalid(passwordControl: UntypedFormControl, testString: string): void {
    passwordControl.setValue(testString);
    passwordControl.updateValueAndValidity();
    expect(passwordControl.valid).toBe(false, `test string: "${testString}"`);
}

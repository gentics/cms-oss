import { ReactiveFormsModule, UntypedFormControl } from '@angular/forms';
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
            declarations: [ChangePasswordModal],
        });
    });

    describe('form validation', () => {

        it('should allow valid characters',  componentTest(() => ChangePasswordModal, fixture => {
            const instance: ChangePasswordModal = fixture.componentInstance;
            const password1 = instance.form.controls['password1'] as UntypedFormControl;

            assertValid(password1, 'aA1._-@.');
            assertValid(password1, '9922325');
            assertValid(password1, 'two.words');
            assertValid(password1, '--@@_._');
        }));

        it('should not allow invalid characters',  componentTest(() => ChangePasswordModal, fixture => {
            const instance: ChangePasswordModal = fixture.componentInstance;
            const password1 = instance.form.controls['password1'] as UntypedFormControl;

            assertInvalid(password1, 'abcd!');
            assertInvalid(password1, '       ');
            assertInvalid(password1, 'two words');
            assertInvalid(password1, '#hello');
            assertInvalid(password1, '/hello');
            assertInvalid(password1, '\\hello');
        }));

        it('should not allow passwords shorter than 4 characters',  componentTest(() => ChangePasswordModal, fixture => {
            const instance: ChangePasswordModal = fixture.componentInstance;
            const password1 = instance.form.controls['password1'] as UntypedFormControl;

            assertInvalid(password1, '');
            assertInvalid(password1, 'a');
            assertInvalid(password1, 'ab');
            assertInvalid(password1, 'abc');
            assertValid(password1, 'abcd');
        }));

        it('should not allow passwords longer than 20 characters',  componentTest(() => ChangePasswordModal, fixture => {
            const instance: ChangePasswordModal = fixture.componentInstance;
            const password1 = instance.form.controls['password1'] as UntypedFormControl;

            assertValid(password1, 'hello.is.it.me.you.r');
            assertInvalid(password1, 'hello.is.it.me.you.re');
            assertInvalid(password1, 'hello.is.it.me.you.re.looking.for');
        }));

        it('should require both passwords to match',  componentTest(() => ChangePasswordModal, fixture => {
            const instance: ChangePasswordModal = fixture.componentInstance;
            const form = instance.form;
            const password1 = form.get('password1');
            const password2 = form.get('password2');
            fixture.detectChanges();

            password1.setValue('abcde');
            password2.setValue('abcdZ');
            form.updateValueAndValidity();
            expect(form.valid).toBe(false);

            password2.setValue('abcde');
            form.updateValueAndValidity();
            expect(form.valid).toBe(true);

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

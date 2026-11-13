import { TestBed, tick } from '@angular/core/testing';
import { ReactiveFormsModule, UntypedFormControl } from '@angular/forms';
import { componentTest, configureComponentTest } from '../../../../testing';
import { AppStateService } from '../../../state';
import { AuthOperations } from '../../providers';
import { ChangePasswordModalComponent } from './change-password-modal.component';

describe('ChangePasswordModal', () => {

    beforeEach(() => {
        configureComponentTest({
            imports: [
                ReactiveFormsModule,
            ],
            providers: [
                { provide: AuthOperations, useValue: {} },
                { provide: AppStateService, useValue: {} },
            ],
            declarations: [ChangePasswordModalComponent],
        });
    });

    describe('form validation', () => {

        it('should allow valid characters',  componentTest(() => ChangePasswordModalComponent, fixture => {
            const instance: ChangePasswordModalComponent = fixture.componentInstance;
            const password1 = instance.form.controls.password1 as UntypedFormControl;

            assertValid(password1, 'aA1._-@.');
            assertValid(password1, '9922325');
            assertValid(password1, 'two.words');
            assertValid(password1, '--@@_._');
        }));

        it('should not allow invalid characters',  componentTest(() => ChangePasswordModalComponent, fixture => {
            const instance: ChangePasswordModalComponent = fixture.componentInstance;
            const password1 = instance.form.controls.password1 as UntypedFormControl;

            assertInvalid(password1, 'abcd!');
            assertInvalid(password1, '       ');
            assertInvalid(password1, 'two words');
            assertInvalid(password1, '#hello');
            assertInvalid(password1, '/hello');
            assertInvalid(password1, '\\hello');
        }));

        it('should not allow passwords shorter than 4 characters',  componentTest(() => ChangePasswordModalComponent, fixture => {
            const instance: ChangePasswordModalComponent = fixture.componentInstance;
            const password1 = instance.form.controls.password1 as UntypedFormControl;

            assertInvalid(password1, '');
            assertInvalid(password1, 'a');
            assertInvalid(password1, 'ab');
            assertInvalid(password1, 'abc');
            assertValid(password1, 'abcd');
        }));

        it('should not allow passwords longer than 20 characters',  componentTest(() => ChangePasswordModalComponent, fixture => {
            const instance: ChangePasswordModalComponent = fixture.componentInstance;
            const password1 = instance.form.controls.password1 as UntypedFormControl;

            assertValid(password1, 'hello.is.it.me.you.r');
            assertInvalid(password1, 'hello.is.it.me.you.re');
            assertInvalid(password1, 'hello.is.it.me.you.re.looking.for');
        }));

        it('should require both passwords to match', componentTest(() => ChangePasswordModalComponent, fixture => {
            const instance: ChangePasswordModalComponent = fixture.componentInstance;
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

        it('should require the old password when changing the own password', componentTest(() => ChangePasswordModalComponent, fixture => {
            const instance: ChangePasswordModalComponent = fixture.componentInstance;
            const form = instance.form;
            fixture.detectChanges();

            expect(instance.ownPassword).toBe(true);
            form.get('password1').setValue('abcde');
            form.get('password2').setValue('abcde');
            form.updateValueAndValidity();
            expect(form.valid).toBe(false);

            form.get('oldPassword').setValue('oldpwd');
            form.updateValueAndValidity();
            expect(form.valid).toBe(true);
        }));

        it('should not ask for the old password when setting the password of another user', componentTest(() => ChangePasswordModalComponent, fixture => {
            const instance: ChangePasswordModalComponent = fixture.componentInstance;
            instance.userId = 42;
            fixture.detectChanges();

            expect(instance.ownPassword).toBe(false);
            expect(instance.form.get('oldPassword')).toBeNull();
        }));
    });

    describe('changePassword()', () => {

        let authOps: { changePassword: jasmine.Spy; changeOwnPassword: jasmine.Spy };

        beforeEach(() => {
            authOps = TestBed.inject(AuthOperations) as any;
            authOps.changePassword = jasmine.createSpy('changePassword').and.returnValue(Promise.resolve());
        });

        it('should change the own password with old and new password and close on success', componentTest(() => ChangePasswordModalComponent, fixture => {
            const instance: ChangePasswordModalComponent = fixture.componentInstance;
            authOps.changeOwnPassword = jasmine.createSpy('changeOwnPassword').and.returnValue(Promise.resolve(true));
            const closeFn = spyOn(instance, 'closeFn');
            fixture.detectChanges();

            instance.form.get('oldPassword').setValue('oldpwd');
            instance.form.get('password1').setValue('newpwd');
            instance.form.get('password2').setValue('newpwd');
            instance.changePassword();
            tick();

            expect(authOps.changeOwnPassword).toHaveBeenCalledWith('oldpwd', 'newpwd');
            expect(authOps.changePassword).not.toHaveBeenCalled();
            expect(closeFn).toHaveBeenCalled();
        }));

        it('should stay open when changing the own password fails', componentTest(() => ChangePasswordModalComponent, fixture => {
            const instance: ChangePasswordModalComponent = fixture.componentInstance;
            authOps.changeOwnPassword = jasmine.createSpy('changeOwnPassword').and.returnValue(Promise.resolve(false));
            const closeFn = spyOn(instance, 'closeFn');
            fixture.detectChanges();

            instance.form.get('oldPassword').setValue('wrongpwd');
            instance.form.get('password1').setValue('newpwd');
            instance.form.get('password2').setValue('newpwd');
            instance.changePassword();
            tick();

            expect(authOps.changeOwnPassword).toHaveBeenCalledWith('wrongpwd', 'newpwd');
            expect(closeFn).not.toHaveBeenCalled();
        }));

        it('should set the password of another user without old password', componentTest(() => ChangePasswordModalComponent, fixture => {
            const instance: ChangePasswordModalComponent = fixture.componentInstance;
            authOps.changeOwnPassword = jasmine.createSpy('changeOwnPassword');
            const closeFn = spyOn(instance, 'closeFn');
            instance.userId = 42;
            fixture.detectChanges();

            instance.form.get('password1').setValue('newpwd');
            instance.form.get('password2').setValue('newpwd');
            instance.changePassword();
            tick();

            expect(authOps.changePassword).toHaveBeenCalledWith(42, 'newpwd');
            expect(authOps.changeOwnPassword).not.toHaveBeenCalled();
            expect(closeFn).toHaveBeenCalled();
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

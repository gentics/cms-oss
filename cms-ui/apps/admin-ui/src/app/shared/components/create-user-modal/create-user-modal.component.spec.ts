import { NO_ERRORS_SCHEMA } from '@angular/core';
import { tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { I18nNotificationService } from '@gentics/cms-components';
import { Normalized, User } from '@gentics/cms-models';
import { of, Subject, throwError } from 'rxjs';
import { componentTest, configureComponentTest } from '../../../../testing';
import { EntityExistsValidator } from '../../providers/entity-exists-validator/entity-exists-validator.service';
import { GroupDataService } from '../../providers/group-data/group-data.service';
import { CreateUserModalComponent } from './create-user-modal.component';

const USER = { id: 42, login: 'linkuser', firstName: 'Link', lastName: 'User' } as User<Normalized>;
const TOKEN = 'the-token';

describe('CreateUserModalComponent', () => {

    let groupData: {
        createUser: jasmine.Spy;
        createUserWithPasswordLink: jasmine.Spy;
        createPasswordLink: jasmine.Spy;
    };

    beforeEach(() => {
        groupData = {
            createUser: jasmine.createSpy('createUser'),
            createUserWithPasswordLink: jasmine.createSpy('createUserWithPasswordLink').and.returnValue(of(USER)),
            createPasswordLink: jasmine.createSpy('createPasswordLink').and.returnValue(of(TOKEN)),
        };

        configureComponentTest({
            imports: [ReactiveFormsModule],
            providers: [
                { provide: GroupDataService, useValue: groupData },
                { provide: EntityExistsValidator, useValue: { configure: () => {}, validate: () => null } },
                { provide: I18nNotificationService, useValue: { show: jasmine.createSpy('show') } },
            ],
            declarations: [CreateUserModalComponent],
            schemas: [NO_ERRORS_SCHEMA],
        });
    });

    /** Fill the form for a user who sets the password with a link, and create it */
    function createWithPasswordLink(instance: CreateUserModalComponent): void {
        instance.form.patchValue({ firstName: 'Link', lastName: 'User', login: 'linkuser', passwordLink: true });
        instance.userGroupIds = [1, 2];
        instance.buttonCreateUserClicked();
        tick();
    }

    it('fetches the password link only after the user was created in all groups', componentTest(() => CreateUserModalComponent,
        (fixture, instance) => {
            const created = new Subject<User<Normalized>>();
            groupData.createUserWithPasswordLink.and.returnValue(created);
            instance.closeFn = jasmine.createSpy('closeFn');
            fixture.detectChanges();

            createWithPasswordLink(instance);
            expect(groupData.createUserWithPasswordLink).toHaveBeenCalledWith(jasmine.objectContaining({ login: 'linkuser' }), [1, 2]);
            expect(groupData.createPasswordLink).not.toHaveBeenCalled();

            created.next(USER);
            created.complete();
            tick();

            expect(groupData.createPasswordLink).toHaveBeenCalledWith(USER.id);
            expect(instance.passwordLinkUrl).toMatch(/\?skip-sso#\/set-password\?token=the-token$/);
        }));

    it('keeps the created user and offers to fetch the link again when fetching it failed', componentTest(() => CreateUserModalComponent,
        (fixture, instance) => {
            groupData.createPasswordLink.and.returnValue(throwError(() => new Error('link failed')));
            instance.closeFn = jasmine.createSpy('closeFn');
            fixture.detectChanges();

            createWithPasswordLink(instance);
            fixture.detectChanges();

            expect(instance.createdUser).toBe(USER);
            expect(instance.passwordLinkFailed).toBe(true);
            expect(instance.passwordLinkUrl).toBeNull();
            expect(instance.closeFn).not.toHaveBeenCalled();
            expect(fixture.debugElement.query(By.css('[data-context="password-link-failed"]'))).not.toBeNull();
            expect(fixture.debugElement.query(By.css('[data-action="retry-link"]'))).not.toBeNull();
        }));

    it('shows the link after fetching it again', componentTest(() => CreateUserModalComponent, (fixture, instance) => {
        groupData.createPasswordLink.and.returnValues(throwError(() => new Error('link failed')), of(TOKEN));
        instance.closeFn = jasmine.createSpy('closeFn');
        fixture.detectChanges();

        createWithPasswordLink(instance);
        instance.buttonRetryPasswordLinkClicked();
        tick();
        fixture.detectChanges();

        expect(groupData.createPasswordLink).toHaveBeenCalledTimes(2);
        expect(groupData.createUserWithPasswordLink).toHaveBeenCalledTimes(1);
        expect(instance.passwordLinkFailed).toBe(false);
        expect(instance.passwordLinkUrl).toMatch(/\?skip-sso#\/set-password\?token=the-token$/);
        expect(fixture.debugElement.query(By.css('[data-context="password-link-failed"]'))).toBeNull();
        expect(fixture.debugElement.query(By.css('[data-context="password-link"]'))).not.toBeNull();
    }));

    it('closes with the created user when the link could not be fetched', componentTest(() => CreateUserModalComponent,
        (fixture, instance) => {
            groupData.createPasswordLink.and.returnValue(throwError(() => new Error('link failed')));
            instance.closeFn = jasmine.createSpy('closeFn');
            fixture.detectChanges();

            createWithPasswordLink(instance);
            instance.buttonCloseClicked();

            expect(instance.closeFn).toHaveBeenCalledWith(USER);
        }));
});

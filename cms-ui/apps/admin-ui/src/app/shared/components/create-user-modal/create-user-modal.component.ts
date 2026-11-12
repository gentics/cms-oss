import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { AbstractControl, UntypedFormControl, UntypedFormGroup, ValidationErrors, Validators } from '@angular/forms';
import { GroupUserCreateRequest, Normalized, User } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { PASSWORD_VALIDATORS } from '../../../shared/utils';
import { EntityExistsValidator } from '../../providers/entity-exists-validator/entity-exists-validator.service';
import { GroupDataService } from '../../providers/group-data/group-data.service';

@Component({
    selector: 'gtx-create-user-modal',
    templateUrl: './create-user-modal.component.html',
    styleUrls: ['./create-user-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class CreateUserModalComponent extends BaseModal<User<Normalized>> implements OnInit, OnDestroy {

    /** Current step (tab) of the entity creation wizzard */
    currentTab = String(1);

    /** form instance */
    form: UntypedFormGroup;

    /**
     * Group ids the new user will be added after creation.
     * This group assignment must be performed in two steps as a user can only be created in one group initially.
     */
    userGroupIds: number[] = [];

    /** Will be set when the create call is sent */
    loading = false;

    /** User created with a password link, set once the link is shown */
    createdUser: User<Normalized> | null = null;

    /** Link with which the created user sets the password */
    passwordLinkUrl: string | null = null;

    private passwordLinkSubscription: Subscription;

    /** Convenience getter for confirm username input */
    get login(): AbstractControl {
        return this.form.get('login');
    }

    /** Convenience getter for set password input */
    get password1(): AbstractControl {
        return this.form.get('password1');
    }

    /** Convenience getter for confirm password input */
    get password2(): AbstractControl {
        return this.form.get('password2');
    }

    /** search term for the table to search for */
    set searchTerm(v: string) {
        this._searchTerm.next(v);
    }

    get searchTerm(): string {
        return this._searchTerm.getValue();
    }

    get searchTerm$(): Observable<string> {
        return this._searchTerm.asObservable();
    }

    private _searchTerm = new BehaviorSubject<string>(null);

    constructor(
        private changeDetector: ChangeDetectorRef,
        private groupData: GroupDataService,
        private entityExistsValidator: EntityExistsValidator<User<Normalized>>,
    ) {
        super();
        entityExistsValidator.configure('user', 'login');
    }

    ngOnInit(): void {
        // instantiate form
        this.form = new UntypedFormGroup({
            firstName: new UntypedFormControl(null),
            lastName: new UntypedFormControl(null),
            email: new UntypedFormControl(null, Validators.email),
            login: new UntypedFormControl(null, this.entityExistsValidator.validate),
            description: new UntypedFormControl(null),
            passwordLink: new UntypedFormControl(false),
            password1: new UntypedFormControl(null, PASSWORD_VALIDATORS),
            password2: new UntypedFormControl(null, PASSWORD_VALIDATORS),
        }, this.passwordsDontMatch);

        // with a link the user sets the password, so the password fields must neither be filled nor validated
        this.passwordLinkSubscription = this.form.get('passwordLink').valueChanges.subscribe((passwordLink: boolean) => {
            for (const control of [this.password1, this.password2]) {
                if (passwordLink) {
                    control.reset(null);
                    control.disable();
                } else {
                    control.enable();
                }
            }
            this.changeDetector.markForCheck();
        });
    }

    ngOnDestroy(): void {
        this.passwordLinkSubscription?.unsubscribe();
    }

    /** Get form validity state */
    formIsValid(): boolean {
        return this.form.valid;
    }

    /** Get form validity state */
    allIsValid(): boolean {
        return this.formIsValid() && this.userGroupIds.length > 0;
    }

    /** Programmatic tab set */
    setActiveTab(index: string): void {
        this.currentTab = String(index);
    }

    /**
     * Returns TRUE if parameter index is index of active tab
     */
    tabIndexIsActive(index: number): boolean {
        return this.currentTab === String(index);
    }

    /**
     * Validator which checks that both passwords contain equal values.
     */
    passwordsDontMatch(control: UntypedFormControl): null | ValidationErrors {
        const valid = control.get('password1').value === control.get('password2').value;
        return valid ? null : { passwordsDontMatch: true };
    }

    /**
     * If user clicks to create a new user
     */
    buttonCreateUserClicked(): void {
        this.form.disable({ emitEvent: false });
        this.loading = true;
        this.changeDetector.markForCheck();

        const passwordLink: boolean = this.form.value.passwordLink;
        const created: Promise<User<Normalized>> = passwordLink
            ? this.groupData.createUserWithPasswordLink(this.userPayload(false), this.userGroupIds).toPromise()
                .then(({ user, passwordToken }) => {
                    // the modal stays open to show the link, it is closed with the created user
                    this.createdUser = user;
                    this.passwordLinkUrl = this.passwordLinkUrlFor(passwordToken);
                    return null;
                })
            : this.groupData.createUser(this.userPayload(true), this.userGroupIds).toPromise();

        created
            .then((entityCreated) => {
                this.loading = false;
                if (entityCreated) {
                    this.closeFn(entityCreated);
                } else {
                    this.changeDetector.markForCheck();
                }
            }, () => {
                // keep the password fields disabled when the link was chosen
                this.form.enable({ emitEvent: false });
                if (passwordLink) {
                    this.password1.disable({ emitEvent: false });
                    this.password2.disable({ emitEvent: false });
                }
                this.loading = false;
                this.changeDetector.markForCheck();
            });
    }

    /** Close the modal after the password link was shown */
    buttonCloseClicked(): void {
        this.closeFn(this.createdUser);
    }

    private userPayload(withPassword: boolean): GroupUserCreateRequest {
        // assemble payload with conditional properties
        return {
            firstName: this.form.value.firstName,
            lastName: this.form.value.lastName,
            login: this.form.value.login,
            ...(withPassword && { password: this.form.value.password1 }),
            ...(this.form.value.email && { email: this.form.value.email }),
            ...(this.form.value.description && { description: this.form.value.description }),
        };
    }

    /**
     * Link to the page of the editor UI where the user sets the password. `skip-sso` keeps Keycloak from
     * redirecting the user, who has no session yet, to its login.
     */
    private passwordLinkUrlFor(token: string): string {
        const editorUrl = new URL(environment.editorUrl, window.location.toString()).href.replace(/\/+$/, '');
        return `${editorUrl}/?skip-sso#/set-password?token=${encodeURIComponent(token)}`;
    }

}

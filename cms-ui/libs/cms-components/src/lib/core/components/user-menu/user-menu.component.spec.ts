import { Component, Pipe, PipeTransform, Type } from '@angular/core';
import { ComponentFixture, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { GtxUserMenuComponent, GtxUserMenuToggleComponent } from '@gentics/cms-components';
import { GcmsUiLanguage } from '@gentics/cms-integration-api-models';
import { I18nLanguage, Normalized, User } from '@gentics/cms-models';
import { ButtonComponent, DropdownItemComponent, GenticsUICoreModule, SideMenuComponent } from '@gentics/ui-core';
import { componentTest, configureComponentTest } from '../../../testing';

@Component({
    template: `
        <gtx-user-menu
            [opened]="userMenuOpened"
            [user]="currentUser"
            [supportedLanguages]="supportedLanguages"
            [currentlanguage]="currentlanguage"
            (toggle)="userMenuOpened = $event"
            (setLanguage)="onSetLanguage($event)"
            (showPasswordModal)="showPasswordModal()"
            (logout)="onLogoutClick()"
        >
        </gtx-user-menu>
        <gtx-overlay-host></gtx-overlay-host>
    `,
    standalone: false,
})
class TestComponent {
    userMenuOpened = false;
    currentUser: Partial<User<Normalized>> = {
        id: 1,
        firstName: 'John',
        lastName: 'Doe',
        description: 'For whom who serves in silence',
    };
    supportedLanguages: I18nLanguage[] = [
        { code: 'en', name: 'English' },
        { code: 'de', name: 'Deutsch' },
        { code: 'it', name: 'Français' },
        { code: 'fr', name: 'Italiano' },
    ];
    currentlanguage: GcmsUiLanguage = 'en';

    onSetLanguage = jasmine.createSpy('TestComponent.onSetLanguage');
    showPasswordModal = jasmine.createSpy('TestComponent.showPasswordModal');
    onShowPasswordModal = jasmine.createSpy('TestComponent.onShowPasswordModal');
    onLogoutClick = jasmine.createSpy('TestComponent.onLogoutClick');
}

@Pipe({
    name: 'i18n',
    standalone: false,
})
class MockI18nPipe implements PipeTransform {
    transform(key: string, params: object): string {
        return key + (params ? ':' + JSON.stringify(params) : '');
    }
}

describe('UserMenu', () => {

    beforeEach(() => {
        configureComponentTest({
            imports: [
                GenticsUICoreModule.forRoot(),
                NoopAnimationsModule,
            ],
            providers: [],
            declarations: [
                TestComponent,
                MockI18nPipe,
                GtxUserMenuComponent,
                GtxUserMenuToggleComponent,
            ],
        });
    });

    function get<T>(componentType: Type<T>, fixture: ComponentFixture<any>): T {
        const debugElement = fixture.debugElement.query(By.directive(componentType));
        return debugElement && debugElement.componentInstance;
    }

    function getAll<T>(componentType: Type<T>, fixture: ComponentFixture<any>): T[] {
        return fixture.debugElement.nativeElement.queryAll(By.directive(componentType))
            .map(debugElement => debugElement && debugElement.componentInstance);
    }

    it('shows a toggle button when collapsed',
        componentTest(() => TestComponent, (fixture, testComponent) => {
            testComponent.userMenuOpened = false;
            fixture.detectChanges();
            tick();

            const button = get(GtxUserMenuToggleComponent, fixture);
            expect(button.active).toBe(false);
        }),
    );

    it('opens the menu when collapsed and toggle button is clicked',
        componentTest(() => TestComponent, (fixture, testComponent) => {
            testComponent.userMenuOpened = false;
            fixture.detectChanges();
            tick();
            const menu = get(SideMenuComponent, fixture);
            expect(menu.opened).toBe(false);

            menu.toggle.emit(true);
            fixture.detectChanges();
            tick();
            expect(testComponent.userMenuOpened).toBe(true);
            expect(menu.opened).toBe(true);
        }),
    );

    it('collapses the menu when opened and toggle button is clicked',
        componentTest(() => TestComponent, (fixture, testComponent) => {
            testComponent.userMenuOpened = true;
            fixture.detectChanges();
            tick();
            const menu = get(SideMenuComponent, fixture);
            expect(menu.opened).toBe(true);

            menu.toggle.emit(false);
            fixture.detectChanges();
            tick();
            expect(menu.opened).toBe(false);
            expect(testComponent.userMenuOpened).toBe(false);
        }),
    );

    describe('language toggle', () => {

        it('displays the current UI language',
            componentTest(() => TestComponent, (fixture, testComponent) => {
                testComponent.userMenuOpened = true;
                fixture.detectChanges();
                tick();
                const toggle: HTMLElement = fixture.nativeElement.querySelector('.language-toggle');
                expect(toggle.innerText).toMatch(/\bEN\b/);

                // parent component has set another language
                testComponent.currentlanguage = 'de';

                fixture.detectChanges();
                expect(toggle.innerText).toMatch(/\bDE\b/);
            }),
        );

        it('displays a list of available languages',
            componentTest(() => TestComponent, (fixture, testComponent) => {
                testComponent.userMenuOpened = true;
                fixture.detectChanges();
                tick();
                const languageToggle: HTMLElement = fixture.nativeElement.querySelector('.language-toggle gtx-dropdown-trigger');
                languageToggle.click();

                fixture.detectChanges();
                tick();

                const menu: GtxUserMenuComponent = fixture.debugElement.query(By.directive(GtxUserMenuComponent)).componentInstance;
                const dropdownItems = fixture.debugElement.queryAll(By.directive(DropdownItemComponent));
                expect(dropdownItems.length).toBe(menu.supportedLanguages.length);

                for (let index = 0; index < dropdownItems.length; index++) {
                    expect(dropdownItems[index].nativeElement.innerText)
                        .toMatch(menu.supportedLanguages[index].name);
                }
            }),
        );

        it('emits correct UI language change',
            componentTest(() => TestComponent, (fixture, testComponent) => {
                testComponent.userMenuOpened = true;
                fixture.detectChanges();
                tick();
                const menu: GtxUserMenuComponent = fixture.debugElement.query(By.directive(GtxUserMenuComponent)).componentInstance;
                const languages = menu.supportedLanguages;
                fixture.detectChanges();

                const languageToggle: HTMLElement = fixture.nativeElement.querySelector('.language-toggle gtx-dropdown-trigger');
                languageToggle.click();
                fixture.detectChanges();
                tick();

                const dropdownItems = fixture.debugElement.queryAll(By.directive(DropdownItemComponent));

                // Language 1 should be ticked
                expect(dropdownItems[0].nativeElement.innerText).toMatch('check');
                expect(dropdownItems[1].nativeElement.innerText).not.toMatch('check');
            }),
        );

        it('emits "onSetLanguageClick" when a language is selected from the dropdown',
            componentTest(() => TestComponent, (fixture, testComponent) => {
                testComponent.userMenuOpened = true;
                fixture.detectChanges();
                tick();
                const menu: GtxUserMenuComponent = fixture.debugElement.query(By.directive(GtxUserMenuComponent)).componentInstance;
                const languages = menu.supportedLanguages;
                fixture.detectChanges();

                // choose other than default language
                const languageToggle: HTMLElement = fixture.nativeElement.querySelector('.language-toggle gtx-dropdown-trigger');
                languageToggle.click();
                fixture.detectChanges();
                tick();

                const dropdownItems = fixture.debugElement.queryAll(By.directive(DropdownItemComponent));
                dropdownItems[1].triggerEventHandler('click', {});
                fixture.detectChanges();
                expect(testComponent.onSetLanguage).toHaveBeenCalledWith(languages[1].code);
            }),
        );

    });

    it('displays first and last name of the current user',
        componentTest(() => TestComponent, (fixture, testComponent) => {
            testComponent.userMenuOpened = true;
            fixture.detectChanges();
            tick();
            fixture.detectChanges();
            tick();
            expect(fixture.nativeElement.innerText).toContain('user.logged_in_as:{"name":"John Doe"}');
        }),
    );

    it('emits password change modal event when "Change password" is clicked',
        componentTest(() => TestComponent, (fixture, testComponent) => {
            testComponent.userMenuOpened = true;
            fixture.detectChanges();
            tick();
            // Click "more" (vertical dots) button
            const buttons = fixture.debugElement.queryAll(By.directive(ButtonComponent));
            const dropdownTrigger = buttons.filter(btn => btn.nativeElement.innerText.indexOf('more_vert') >= 0)[0];
            expect(dropdownTrigger).toBeDefined('dropdown trigger not found');
            dropdownTrigger.nativeElement.click();
            fixture.detectChanges();
            tick();

            // Click "Change Password" dropdown element
            const dropdownItems = fixture.debugElement.queryAll(By.directive(DropdownItemComponent));
            const changeButtonComponent = dropdownItems.filter(btn => btn.nativeElement.innerText.indexOf('change_password') >= 0)[0];
            expect(changeButtonComponent).toBeDefined('change button not found');
            changeButtonComponent.triggerEventHandler('click', {});
        }),
    );

    it('displays the description of the current user',
        componentTest(() => TestComponent, (fixture, testComponent) => {
            testComponent.userMenuOpened = true;
            fixture.detectChanges();
            tick();
            expect(fixture.nativeElement.innerText).toContain(testComponent.currentUser.description);
        }),
    );

    it('emits "logoutClick" when the logout button is clicked',
        componentTest(() => TestComponent, (fixture, testComponent) => {
            testComponent.userMenuOpened = true;
            fixture.detectChanges();
            tick();
            const buttons = fixture.debugElement.queryAll(By.directive(ButtonComponent));
            const logoutButtonComponent = buttons.filter(btn => btn.nativeElement.innerText.toLowerCase().indexOf('user.log_out') >= 0)[0];

            expect(logoutButtonComponent).toBeDefined();
            logoutButtonComponent.triggerEventHandler('click', {});
            expect(testComponent.onLogoutClick).toHaveBeenCalled();
        }),
    );

});

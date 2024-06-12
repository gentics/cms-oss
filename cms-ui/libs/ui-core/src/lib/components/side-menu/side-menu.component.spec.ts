import { Component, DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { SideMenuToggleDirective } from '../../directives/side-menu-toggle/side-menu-toggle.directive';
import { UserAgentProvider } from '../../providers/user-agent/user-agent-ref';
import { componentTest } from '../../testing';
import { SideMenuComponent } from './side-menu.component';

describe('SideMenuComponent', () => {
    beforeEach(() => TestBed.configureTestingModule({
        imports: [NoopAnimationsModule],
        declarations: [SideMenuComponent, TestComponent, SideMenuToggleDirective],
        providers: [UserAgentProvider],
        teardown: { destroyAfterEach: false },
        schemas: [NO_ERRORS_SCHEMA],
    }));

    it('does not have the "opened" class when closed',
        componentTest(() => TestComponent, fixture => {
            const menu: HTMLElement = getMenuElement(fixture);
            fixture.detectChanges();
            expect(menu.classList).not.toContain('opened');
        }),
    );

    it('has the "opened" class when open',
        componentTest(() => TestComponent, (fixture, testInstance) => {
            const menu: HTMLElement = getMenuElement(fixture);
            testInstance.menuVisible = true;
            fixture.detectChanges();
            expect(menu.classList).toContain('opened');
        }),
    );

    it('user content does not exist in DOM when closed',
        componentTest(() => TestComponent, fixture => {
            fixture.detectChanges();
            const userContent = fixture.debugElement.query(By.css('.user-content'));
            expect(userContent).toBeNull();
        }),
    );

    it('user content does exist in DOM when opened',
        componentTest(() => TestComponent, (fixture, testInstance) => {
            testInstance.menuVisible = true;
            fixture.detectChanges();
            const userContent = fixture.debugElement.query(By.css('.user-content'));
            expect(userContent).not.toBeNull();
        }),
    );

    it('emits "toggle" with new state when toggle button is clicked',
        componentTest(() => TestComponent, (fixture, testInstance) => {
            const buttonDel: DebugElement = getToggleButtonDebugElement(fixture);

            testInstance.menuChanged = jasmine.createSpy('menuChanged');
            testInstance.menuVisible = true;
            fixture.detectChanges();
            buttonDel.triggerEventHandler('click', null);
            tick();

            expect(testInstance.menuChanged).toHaveBeenCalledWith(false);
        }),
    );

    it('emits "toggle" with new state when overlay is clicked',
        componentTest(() => TestComponent, (fixture, testInstance) => {
            const overlay: HTMLElement = fixture.nativeElement.querySelector('.side-menu-overlay');
            testInstance.menuChanged = jasmine.createSpy('menuChanged');
            testInstance.menuVisible = true;
            fixture.detectChanges();
            overlay.click();
            tick();

            expect(testInstance.menuChanged).toHaveBeenCalledWith(false);
            tick(500);
        }),
    );

    it('does not emit "toggle" when clicking overlay with opened === false',
        componentTest(() => TestComponent, (fixture, testInstance) => {
            const overlay: HTMLElement = fixture.nativeElement.querySelector('.side-menu-overlay');
            testInstance.menuChanged = jasmine.createSpy('menuChanged');
            fixture.detectChanges();
            overlay.click();
            tick();

            expect(testInstance.menuChanged).not.toHaveBeenCalled();
            tick(500);
        }),
    );

});

@Component({
    template: `
        <gtx-side-menu [opened]="menuVisible" (toggle)="menuChanged($event)">
            <gtx-side-menu-toggle>toggle</gtx-side-menu-toggle>
            <div class="user-content">User Content</div>
        </gtx-side-menu>`,
})
class TestComponent {
    menuVisible = false;
    menuChanged(...args: any[]): void { }
}

// Helper functions

const getMenuElement = (fixture: ComponentFixture<TestComponent>): HTMLElement => fixture.debugElement.query(By.css('gtx-side-menu')).nativeElement;
const getToggleButtonDebugElement = (fixture: ComponentFixture<TestComponent>): DebugElement => fixture.debugElement.query(By.css('.toggle-button'));

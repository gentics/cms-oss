import { Component } from '@angular/core';
import { ComponentFixture, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ApplicationStateService } from '@editor-ui/app/state';
import { TestApplicationState } from '@editor-ui/app/state/test-application-state.mock';
import { CheckboxComponent, GenticsUICoreModule } from '@gentics/ui-core';
import { componentTest, configureComponentTest } from '../../../../testing';
import { UsersList } from './users-list.component';

describe('UsersList', () => {

    beforeEach(() => {
        configureComponentTest({
            imports: [GenticsUICoreModule.forRoot()],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
            ],
            declarations: [TestComponent, UsersList],
        });
    });

    it('starts with none checked', componentTest(() => TestComponent, (fixture) => {
        fixture.detectChanges();

        const checkboxes = getUserCheckboxes(fixture);
        const checked = checkboxes.filter(checkbox => checkbox.value === true);
        expect(checked.length).toBe(0);
    }));

    it('checks those with selected ids', componentTest(() => TestComponent, (fixture, instance) => {
        instance.selected = [1, 3];
        fixture.detectChanges();

        const checkboxes = getUserCheckboxes(fixture);
        expect(checkboxes.map(checkbox => checkbox.value === true)).toEqual([true, false, true, false]);

        instance.selected = [];
        fixture.detectChanges();

        expect(checkboxes.map(checkbox => checkbox.value === true)).toEqual([false, false, false, false]);

        instance.selected = [1, 2, 3, 4];
        fixture.detectChanges();

        expect(checkboxes.map(checkbox => checkbox.value === true)).toEqual([true, true, true, true]);
    }));

    it('emits selectedChange with new selection', componentTest(() => TestComponent, (fixture, instance) => {
        fixture.detectChanges();
        const spy = spyOn(instance, 'selectedChange').and.callThrough();

        clickCheckbox(fixture, 1);
        fixture.detectChanges();
        expect(instance.selectedChange).toHaveBeenCalledWith([1]);
        expect(spy.calls.count()).toBe(1);

        clickCheckbox(fixture, 3);
        fixture.detectChanges();
        expect(instance.selectedChange).toHaveBeenCalledWith([1, 3]);
        expect(spy.calls.count()).toBe(2);

        clickCheckbox(fixture, 1);
        fixture.detectChanges();
        expect(instance.selectedChange).toHaveBeenCalledWith([3]);
        expect(spy.calls.count()).toBe(3);
    }));

    it('emits selectedChange when selectAll clicked', componentTest(() => TestComponent, (fixture, instance) => {
        fixture.detectChanges();
        const spy = spyOn(instance, 'selectedChange').and.callThrough();

        clickCheckbox(fixture, 0);
        fixture.detectChanges();
        expect(instance.selectedChange).toHaveBeenCalledWith([1, 2, 3, 4]);
        expect(spy.calls.count()).toBe(1);

        clickCheckbox(fixture, 0);
        fixture.detectChanges();
        expect(instance.selectedChange).toHaveBeenCalledWith([]);
        expect(spy.calls.count()).toBe(2);
    }));

    it('allows toggling', componentTest(() => TestComponent, (fixture) => {
        fixture.detectChanges();
        const checkboxes = getUserCheckboxes(fixture);

        clickCheckbox(fixture, 1);
        fixture.detectChanges();
        expect(checkboxes[0].value).toBe(true);

        clickCheckbox(fixture, 1);
        fixture.detectChanges();
        expect(checkboxes[0].value).toBe(false);
    }));
});

/**
 * Returns just the checkboxes in the rows, not including the "select all" checkbox.
 */
const getUserCheckboxes = (fixture: ComponentFixture<TestComponent>): CheckboxComponent[] => fixture.debugElement.queryAll(By.css('gtx-checkbox'))
    .slice(1)
    .map(del => del.componentInstance);

function clickCheckbox(fixture: ComponentFixture<TestComponent>, index: number): void {
    fixture.debugElement.queryAll(By.css('gtx-checkbox label'))[index].nativeElement.click();
    tick();
}
@Component({
    selector: 'test-component',
    template: `
        <users-list
            [users]="users"
            [selected]="selected"
            (selectedChange)="selectedChange($event)"
        ></users-list>
    `,
})
class TestComponent {
    users: any[] = [];
    selected: number[] = [];

    constructor() {
        for (let i = 1; i < 5; i++) {
            this.users.push({
                id: i,
                firstName: `firstName_${i}`,
                lastName: `lastName_${i}`,
            });
        }
    }

    selectedChange(newSelection: number[]): void {
        this.selected = newSelection;
    }
}

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
            imports: [GenticsUICoreModule],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
            ],
            declarations: [TestComponent, UsersList],
        });
    });

    it('starts with none checked', componentTest(() => TestComponent, (fixture) => {
        fixture.detectChanges();

        const checkboxes = getUserCheckboxes(fixture);
        const checked = checkboxes.filter(checkbox => checkbox.checked);
        expect(checked.length).toBe(0);
    }));

    it('checks those with selected ids', componentTest(() => TestComponent, (fixture, instance) => {
        instance.selected = [1, 3];
        fixture.detectChanges();

        const checkboxes = getUserCheckboxes(fixture);
        expect(checkboxes.map(checkbox => checkbox.checked)).toEqual([true, false, true, false]);

        instance.selected = [];
        fixture.detectChanges();

        expect(checkboxes.map(checkbox => checkbox.checked)).toEqual([false, false, false, false]);

        instance.selected = [1, 2, 3, 4];
        fixture.detectChanges();

        expect(checkboxes.map(checkbox => checkbox.checked)).toEqual([true, true, true, true]);
    }));

    it('emits selectionChange with new selection', componentTest(() => TestComponent, (fixture, instance) => {
        fixture.detectChanges();
        spyOn(instance, 'selectionChange').and.callThrough();

        clickCheckbox(fixture, 1);
        fixture.detectChanges();
        expect(instance.selectionChange).toHaveBeenCalledWith([1]);
        expect((<any> instance.selectionChange).calls.count()).toBe(1);

        clickCheckbox(fixture, 3);
        fixture.detectChanges();
        expect(instance.selectionChange).toHaveBeenCalledWith([1, 3]);
        expect((<any> instance.selectionChange).calls.count()).toBe(2);

        clickCheckbox(fixture, 1);
        fixture.detectChanges();
        expect(instance.selectionChange).toHaveBeenCalledWith([3]);
        expect((<any> instance.selectionChange).calls.count()).toBe(3);
    }));

    it('emits selectionChange when selectAll clicked', componentTest(() => TestComponent, (fixture, instance) => {
        fixture.detectChanges();
        spyOn(instance, 'selectionChange').and.callThrough();

        clickCheckbox(fixture, 0);
        fixture.detectChanges();
        expect(instance.selectionChange).toHaveBeenCalledWith([1, 2, 3, 4]);
        expect((<any> instance.selectionChange).calls.count()).toBe(1);

        clickCheckbox(fixture, 0);
        fixture.detectChanges();
        expect(instance.selectionChange).toHaveBeenCalledWith([]);
        expect((<any> instance.selectionChange).calls.count()).toBe(2);
    }));

    it('allows toggling', componentTest(() => TestComponent, (fixture) => {
        fixture.detectChanges();
        const checkboxes = getUserCheckboxes(fixture);

        clickCheckbox(fixture, 1);
        fixture.detectChanges();
        expect(checkboxes[0].checked).toBe(true);

        clickCheckbox(fixture, 1);
        fixture.detectChanges();
        expect(checkboxes[0].checked).toBe(false);
    }));
});

/**
 * Returns just the checkboxes in the rows, not including the "select all" checkbox.
 */
const getUserCheckboxes = (fixture: ComponentFixture<TestComponent>): CheckboxComponent[] => fixture.debugElement.queryAll(By.css('gtx-checkbox'))
    .slice(1)
    .map(del => del.componentInstance);

function clickCheckbox(fixture: ComponentFixture<TestComponent>, index: number): void {
    fixture.debugElement.queryAll(By.css('gtx-checkbox input'))[index].nativeElement.click();
    tick();
}


@Component({
    selector: 'test-component',
    template: `
        <users-list [users]="users"
                    [selected]="selected"
                    (selectionChange)="selectionChange($event)">
        </users-list>`,
    standalone: false,
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

    selectionChange(newSelection: number[]): void {
        this.selected = newSelection;
    }
}

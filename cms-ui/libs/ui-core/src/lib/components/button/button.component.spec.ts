import { Component, NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed, tick } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule, UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { componentTest } from '../../testing';
import { ButtonComponent } from './button.component';

describe('ButtonComponent', () => {

    beforeEach(() => TestBed.configureTestingModule({
        imports: [FormsModule, ReactiveFormsModule],
        declarations: [TestComponent, ButtonComponent],
        teardown: { destroyAfterEach: false },
        schemas: [NO_ERRORS_SCHEMA],
    }));

    it('is enabled by default',
        componentTest(() => TestComponent, fixture => {
            const button: HTMLButtonElement = fixture.nativeElement.querySelector('button');
            fixture.detectChanges();

            expect(button.disabled).toBe(false);
        }),
    );

    it('binds the "disabled" property',
        componentTest(() => TestComponent, `
            <gtx-button [disabled]="true"></gtx-button>`,
        fixture => {
            const button: HTMLButtonElement = fixture.nativeElement.querySelector('button');
            fixture.detectChanges();

            expect(button.disabled).toBe(true);
        },
        ),
    );

    it('accepts string values for the "disabled" property',
        componentTest(() => TestComponent, `
            <gtx-button disabled="true"></gtx-button>`,
        fixture => {
            const button: HTMLButtonElement = fixture.nativeElement.querySelector('button');
            fixture.detectChanges();

            expect(button.disabled).toBe(true);
        },
        ),
    );

    it('accepts an empty "disabled" property',
        componentTest(() => TestComponent, `
            <gtx-button disabled></gtx-button>`,
        fixture => {
            const button: HTMLButtonElement = fixture.nativeElement.querySelector('button');
            fixture.detectChanges();
            expect(button.disabled).toBe(true);
        },
        ),
    );

    it('sets the button as form submit button when a "submit" property is present',
        componentTest(() => TestComponent, `
            <gtx-button submit></gtx-button>`,
        fixture => {
            const button: HTMLButtonElement = fixture.nativeElement.querySelector('button');
            fixture.detectChanges();
            expect(button.type).toBe('submit');
        },
        ),
    );

    it('sets the button as form submit button when "submit" is set to a boolean value',
        componentTest(() => TestComponent, `
            <gtx-button [submit]="isSubmit"></gtx-button>`,
        (fixture, testComponent) => {
            testComponent.isSubmit = true;
            fixture.detectChanges();
            const button: HTMLButtonElement = fixture.nativeElement.querySelector('button');
            expect(button.type).toBe('submit');

            testComponent.isSubmit = false;
            fixture.detectChanges();
            expect(button.type).not.toBe('submit');
        },
        ),
    );

    it('submits the parent form when a submit button is clicked',
        componentTest(() => TestComponent, `
            <form [formGroup]="form" (ngSubmit)="onSubmit($event)">
                <gtx-button submit></gtx-button>
            </form>`,
        (fixture, instance) => {
            fixture.detectChanges();
            const button: HTMLButtonElement = fixture.nativeElement.querySelector('button');
            const form: HTMLFormElement = fixture.nativeElement.querySelector('form');
            form.onsubmit = jasmine.createSpy('formSubmit').and.returnValue(false);

            const event = document.createEvent('MouseEvent');
            event.initEvent('click', true, true);
            button.dispatchEvent(event);

            fixture.detectChanges();
            expect(form.onsubmit).toHaveBeenCalled();
        },
        ),
    );

    it('forwards its "click" event when enabled',
        componentTest(() => TestComponent, `
            <gtx-button (click)="onClick($event)"></gtx-button>`,
        fixture => {
            const onClick = fixture.componentRef.instance.onClick = jasmine.createSpy('onClick');
            const button: HTMLButtonElement = fixture.nativeElement.querySelector('button');
            fixture.detectChanges();

            const event = document.createEvent('MouseEvent');
            event.initEvent('click', true, true);

            button.dispatchEvent(event);
            button.parentElement.dispatchEvent(event);
            button.click();

            expect(onClick).toHaveBeenCalledTimes(3);
        },
        ),
    );

    // Disabled elements don't fire mouse events in some browsers, but not all
    // http://stackoverflow.com/a/3100395/5460631
    it('does not forward button "click" events when disabled',
        componentTest(() => TestComponent, `
            <gtx-button [disabled]="true" (click)="onClick($event)"></gtx-button>`,
        fixture => {
            const onClick = fixture.componentRef.instance.onClick = jasmine.createSpy('onClick');
            const button: HTMLButtonElement = fixture.nativeElement.querySelector('button');
            fixture.detectChanges();
            tick();

            const event = document.createEvent('MouseEvent');
            event.initEvent('click', true, true);
            button.dispatchEvent(event);
            button.click();

            expect(event.defaultPrevented).toBe(true, 'default not prevented');
            expect(onClick).not.toHaveBeenCalled();
        },
        ),
    );

    // Other browsers fire mouse events on the parent of disabled <button> elements
    // http://stackoverflow.com/a/3100395/5460631
    it('does not forward bubbled "click" events when disabled',
        componentTest(() => TestComponent, `
            <gtx-button [disabled]="true" (click)="onClick($event)"></gtx-button>`,
        fixture => {
            const onClick = fixture.componentRef.instance.onClick = jasmine.createSpy('onClick');
            const eventParent: HTMLElement = fixture.nativeElement.querySelector('button').parentNode;
            fixture.detectChanges();
            tick();

            const event = document.createEvent('MouseEvent');
            event.initEvent('click', true, true);
            eventParent.dispatchEvent(event);
            eventParent.click();

            expect(event.defaultPrevented).toBe(true, 'default not prevented');
            expect(onClick).not.toHaveBeenCalled();
        },
        ),
    );

});

@Component({
    template: '<gtx-button></gtx-button>',
})
class TestComponent {
    form = new UntypedFormGroup({
        test: new UntypedFormControl('initial value'),
    });
    isSubmit: boolean;
    onClick(): void {}
    onSubmit(): void {}
}

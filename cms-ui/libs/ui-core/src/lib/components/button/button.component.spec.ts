import {Component} from '@angular/core';
import {TestBed, tick} from '@angular/core/testing';
import {UntypedFormControl, UntypedFormGroup, FormsModule, ReactiveFormsModule} from '@angular/forms';

import {componentTest} from '../../testing';
import {ButtonComponent} from './button.component';


describe('Button:', () => {

    beforeEach(() => TestBed.configureTestingModule({
    imports: [FormsModule, ReactiveFormsModule],
    declarations: [TestComponent, ButtonComponent],
    teardown: { destroyAfterEach: false }
}));

    it('is enabled by default',
        componentTest(() => TestComponent, fixture => {
            let button: HTMLButtonElement = fixture.nativeElement.querySelector('button');
            fixture.detectChanges();

            expect(button.disabled).toBe(false);
        })
    );

    it('binds the "disabled" property',
        componentTest(() => TestComponent, `
            <gtx-button [disabled]="true"></gtx-button>`,
            fixture => {
                let button: HTMLButtonElement = fixture.nativeElement.querySelector('button');
                fixture.detectChanges();

                expect(button.disabled).toBe(true);
            }
        )
    );

    it('accepts string values for the "disabled" property',
        componentTest(() => TestComponent, `
            <gtx-button disabled="true"></gtx-button>`,
            fixture => {
                let button: HTMLButtonElement = fixture.nativeElement.querySelector('button');
                fixture.detectChanges();

                expect(button.disabled).toBe(true);
            }
        )
    );

    it('accepts an empty "disabled" property',
        componentTest(() => TestComponent, `
            <gtx-button disabled></gtx-button>`,
            fixture => {
                let button: HTMLButtonElement = fixture.nativeElement.querySelector('button');
                fixture.detectChanges();
                expect(button.disabled).toBe(true);
            }
        )
    );

    it('sets the button as form submit button when a "submit" property is present',
        componentTest(() => TestComponent, `
            <gtx-button submit></gtx-button>`,
            fixture => {
                let button: HTMLButtonElement = fixture.nativeElement.querySelector('button');
                fixture.detectChanges();
                expect(button.type).toBe('submit');
            }
        )
    );

    it('sets the button as form submit button when "submit" is set to a boolean value',
        componentTest(() => TestComponent, `
            <gtx-button [submit]="isSubmit"></gtx-button>`,
            (fixture, testComponent) => {
                testComponent.isSubmit = true;
                fixture.detectChanges();
                let button: HTMLButtonElement = fixture.nativeElement.querySelector('button');
                expect(button.type).toBe('submit');

                testComponent.isSubmit = false;
                fixture.detectChanges();
                expect(button.type).not.toBe('submit');
            }
        )
    );

    it('submits the parent form when a submit button is clicked',
        componentTest(() => TestComponent, `
            <form [formGroup]="form" (ngSubmit)="onSubmit($event)">
                <gtx-button submit></gtx-button>
            </form>`,
            (fixture, instance) => {
                fixture.detectChanges();
                let button: HTMLButtonElement = fixture.nativeElement.querySelector('button');
                let form: HTMLFormElement = fixture.nativeElement.querySelector('form');
                form.onsubmit = jasmine.createSpy('formSubmit').and.returnValue(false);

                let event = document.createEvent('MouseEvent');
                event.initEvent('click', true, true);
                button.dispatchEvent(event);

                fixture.detectChanges();
                expect(form.onsubmit).toHaveBeenCalled();
            }
        )
    );

    it('forwards its "click" event when enabled',
        componentTest(() => TestComponent, `
            <gtx-button (click)="onClick($event)"></gtx-button>`,
            fixture => {
                let onClick = fixture.componentRef.instance.onClick = jasmine.createSpy('onClick');
                let button: HTMLButtonElement = fixture.nativeElement.querySelector('button');
                fixture.detectChanges();

                let event = document.createEvent('MouseEvent');
                event.initEvent('click', true, true);

                button.dispatchEvent(event);
                button.parentElement.dispatchEvent(event);
                button.click();

                expect(onClick).toHaveBeenCalledTimes(3);
            }
        )
    );

    // Disabled elements don't fire mouse events in some browsers, but not all
    // http://stackoverflow.com/a/3100395/5460631
    it('does not forward button "click" events when disabled',
        componentTest(() => TestComponent, `
            <gtx-button [disabled]="true" (click)="onClick($event)"></gtx-button>`,
            fixture => {
                let onClick = fixture.componentRef.instance.onClick = jasmine.createSpy('onClick');
                let button: HTMLButtonElement = fixture.nativeElement.querySelector('button');
                fixture.detectChanges();
                tick();

                let event = document.createEvent('MouseEvent');
                event.initEvent('click', true, true);
                button.dispatchEvent(event);
                button.click();

                expect(event.defaultPrevented).toBe(true, 'default not prevented');
                expect(onClick).not.toHaveBeenCalled();
            }
        )
    );

    // Other browsers fire mouse events on the parent of disabled <button> elements
    // http://stackoverflow.com/a/3100395/5460631
    it('does not forward bubbled "click" events when disabled',
        componentTest(() => TestComponent, `
            <gtx-button [disabled]="true" (click)="onClick($event)"></gtx-button>`,
            fixture => {
                let onClick = fixture.componentRef.instance.onClick = jasmine.createSpy('onClick');
                let eventParent: HTMLElement = fixture.nativeElement.querySelector('button').parentNode;
                fixture.detectChanges();
                tick();

                let event = document.createEvent('MouseEvent');
                event.initEvent('click', true, true);
                eventParent.dispatchEvent(event);
                eventParent.click();

                expect(event.defaultPrevented).toBe(true, 'default not prevented');
                expect(onClick).not.toHaveBeenCalled();
            }
        )
    );

});

@Component({
    template: `<gtx-button></gtx-button>`
})
class TestComponent {
    form = new UntypedFormGroup({
        test: new UntypedFormControl('initial value')
    });
    isSubmit: boolean;
    onClick(): void {}
    onSubmit(): void {}
}

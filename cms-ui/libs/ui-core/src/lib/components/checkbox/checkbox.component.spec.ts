import { Component, NO_ERRORS_SCHEMA, ViewChild } from '@angular/core';
import { TestBed, tick } from '@angular/core/testing';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { componentTest } from '@gentics/ui-core/testing';
import { CHECKBOX_STATE_INDETERMINATE, CheckboxState } from '../../common';
import { CheckboxComponent } from './checkbox.component';

describe('CheckboxComponent', () => {

    beforeEach(() => TestBed.configureTestingModule({
        imports: [FormsModule, ReactiveFormsModule],
        declarations: [TestComponent, CheckboxComponent],
        teardown: { destroyAfterEach: false },
        schemas: [NO_ERRORS_SCHEMA],
    }));

    it('should bind the label',
        componentTest(() => TestComponent, `
            <gtx-checkbox label="testLabel"></gtx-checkbox>`,
        (fixture) => {
            const label: HTMLLabelElement = fixture.nativeElement.querySelector('label');
            fixture.detectChanges();
            expect(label.innerText).toBe('testLabel');
        },
        ),
    );

    it('should bind the id to the label and input',
        componentTest(() => TestComponent, `
            <gtx-checkbox
                label="testLabel"
                id="testId"
            ></gtx-checkbox>`,
        (fixture) => {
            const nativeInput: HTMLInputElement = fixture.nativeElement.querySelector('input');

            fixture.detectChanges();

            expect(nativeInput.id).toBe('testId');
        },
        ),
    );

    it('should use defaults for undefined attributes which have a default',
        componentTest(() => TestComponent, (fixture) => {
            const nativeInput: HTMLInputElement = fixture.nativeElement.querySelector('input');
            fixture.detectChanges();

            expect(nativeInput.checked).toBe(false);
            expect(nativeInput.disabled).toBe(false);
            expect(nativeInput.required).toBe(false);
            expect(nativeInput.value).toBe('');
        }),
    );

    it('should not display undefined attributes',
        componentTest(() => TestComponent, (fixture) => {
            const nativeInput: HTMLInputElement = fixture.nativeElement.querySelector('input');
            const getAttr = (name: string) => nativeInput.attributes.getNamedItem(name);
            fixture.detectChanges();

            expect(getAttr('max')).toBe(null);
            expect(getAttr('min')).toBe(null);
            expect(getAttr('maxLength')).toBe(null);
            expect(getAttr('name')).toBe(null);
            expect(getAttr('pattern')).toBe(null);
            expect(getAttr('placeholder')).toBe(null);
            expect(getAttr('step')).toBe(null);
        }),
    );

    it('should prefill a unique "id" if none is passed in',
        componentTest(() => TestComponent, (fixture) => {
            const nativeInput: HTMLInputElement = fixture.nativeElement.querySelector('input');
            const getAttr = (name: string) => nativeInput.attributes.getNamedItem(name);
            fixture.detectChanges();

            const id: Attr = nativeInput.attributes.getNamedItem('id');
            expect(id).not.toBe(null);
            expect(id.value.length).toBeGreaterThan(0);
        }),
    );

    it('should pass through the native attributes',
        componentTest(() => TestComponent, `
            <gtx-checkbox
                disabled="true"
                [value]="true"
                name="testName"
                required="true"
                formValue="testValue"
            ></gtx-checkbox>`,
        (fixture) => {
            const nativeInput: HTMLInputElement = fixture.nativeElement.querySelector('input');
            fixture.detectChanges();

            expect(nativeInput.disabled).toBe(true);
            expect(nativeInput.checked).toBe(true);
            expect(nativeInput.name).toBe('testName');
            expect(nativeInput.required).toBe(true);
            expect(nativeInput.value).toBe('testValue');
        },
        ),
    );

    it('should not emit a "change" when the native input changes',
        componentTest(() => TestComponent, `
            <gtx-checkbox (valueChange)="onChange($event)">
            </gtx-checkbox>`,
        (fixture) => {
            const nativeInput: HTMLInputElement = fixture.nativeElement.querySelector('input');
            const instance: TestComponent = fixture.componentInstance;
            fixture.detectChanges();
            const spy = instance.onChange = jasmine.createSpy('onChange');

            nativeInput.click();
            tick();
            fixture.detectChanges();

            expect(spy).not.toHaveBeenCalled();
        },
        ),
    );

    describe('ValueAccessor:', () => {

        it('should bind the check state with ngModel (inbound)',
            componentTest(() => TestComponent, `
                <gtx-checkbox
                    [(ngModel)]="boundProperty"
                    formValue="otherValue">
                </gtx-checkbox>`,
            async (fixture, instance) => {
                const nativeInput: HTMLInputElement = fixture.nativeElement.querySelector('input');

                instance.boundProperty = false;

                fixture.changeDetectorRef.markForCheck();
                fixture.detectChanges();
                await fixture.whenRenderingDone();
                fixture.changeDetectorRef.markForCheck();
                fixture.detectChanges();
                await fixture.whenRenderingDone();

                expect(nativeInput.checked).toBe(false);
                expect(nativeInput.indeterminate).toBe(false);

                instance.boundProperty = CHECKBOX_STATE_INDETERMINATE;

                // This is needed, twice, because otherwise the checkbox isn't re-rendered for *whatever* reason.
                fixture.changeDetectorRef.markForCheck();
                fixture.detectChanges();
                await fixture.whenRenderingDone();
                fixture.changeDetectorRef.markForCheck();
                fixture.detectChanges();
                await fixture.whenRenderingDone();

                expect(nativeInput.indeterminate).toBe(true);

                instance.boundProperty = true;

                fixture.changeDetectorRef.markForCheck();
                fixture.detectChanges();
                await fixture.whenRenderingDone();
                fixture.changeDetectorRef.markForCheck();
                fixture.detectChanges();
                await fixture.whenRenderingDone();

                expect(nativeInput.checked).toBe(true);
                expect(nativeInput.indeterminate).toBe(false);
            },
            ),
        );

        it('should bind the value with formControlName (inbound)',
            componentTest(() => TestComponent, `
                <form [formGroup]="testForm">
                    <gtx-checkbox formControlName="testControl">
                    </gtx-checkbox>
                </form>`,
            (fixture, instance) => {
                const nativeInput: HTMLInputElement = fixture.nativeElement.querySelector('input');
                const control = instance.testForm.get('testControl');

                control.setValue(false);
                fixture.detectChanges();
                tick();

                expect(nativeInput.checked).toBe(false);
                expect(nativeInput.indeterminate).toBe(false);

                control.setValue(true);
                fixture.detectChanges();
                tick();
                expect(nativeInput.checked).toBe(true);
                expect(nativeInput.indeterminate).toBe(false);

                control.setValue(CHECKBOX_STATE_INDETERMINATE);
                fixture.detectChanges();
                tick();
                expect(nativeInput.indeterminate).toBe(true);

                control.setValue(false);
                fixture.detectChanges();
                tick();
                expect(nativeInput.checked).toBe(false);
                expect(nativeInput.indeterminate).toBe(false);
            },
            ),
        );
    });

    describe('pure mode:', () => {

        it('stateless mode should be disabled by default',
            componentTest(() => TestComponent, (fixture, instance) => {
                const checkboxComponent = instance.checkboxComponent;
                fixture.detectChanges();
                expect(checkboxComponent.pure).toBe(false);
            }),
        );

        it('stateless mode should be enabled when using "value" attribute',
            componentTest(() => TestComponent, `
                <gtx-checkbox [pure]="true" value="true"></gtx-checkbox>`,
            (fixture, instance) => {
                const checkboxComponent = instance.checkboxComponent;
                fixture.detectChanges();
                // TODO: Testing private properties is really bad - is there a better way?
                expect(checkboxComponent.pure).toBe(true);
            },
            ),
        );

        it('should not change check state on click when marked as "pure"',
            componentTest(() => TestComponent, `
                <gtx-checkbox [pure]="true" [value]="true"></gtx-checkbox>`,
            (fixture, component) => {
                const checkboxComponent = component.checkboxComponent;
                const nativeInput: HTMLInputElement = fixture.nativeElement.querySelector('input');
                fixture.detectChanges();

                expect(checkboxComponent.value).toBe(true);
                expect(nativeInput.checked).toBe(true);

                fixture.nativeElement.querySelector('label').click();
                tick();
                fixture.detectChanges();

                expect(checkboxComponent.value).toBe(true);
                expect(nativeInput.checked).toBe(true);
            },
            ),
        );

        it('should change check state when "value" attribute binding changes',
            componentTest(() => TestComponent, `
                <gtx-checkbox [pure]="true" [value]="checkState"></gtx-checkbox>`,
            (fixture) => {
                const instance: TestComponent = fixture.componentInstance;
                const checkboxComponent = instance.checkboxComponent;
                const nativeInput: HTMLInputElement = fixture.nativeElement.querySelector('input');
                fixture.detectChanges();

                expect(checkboxComponent.value).toBe(false);
                expect(nativeInput.checked).toBe(false);

                instance.checkState = true;
                fixture.detectChanges();

                expect(checkboxComponent.value).toBe(true);
                expect(nativeInput.checked).toBe(true);
            },
            ),
        );

        it('can be disabled via the form control',
            componentTest(() => TestComponent, `
                <form [formGroup]="testForm">
                    <gtx-checkbox [pure]="true" formControlName="testControl"></gtx-checkbox>
                </form>`,
            (fixture, instance) => {
                fixture.detectChanges();

                const input: HTMLInputElement = fixture.nativeElement.querySelector('input');
                expect(input.disabled).toBe(false);

                instance.testForm.get('testControl').disable();
                fixture.detectChanges();
                expect(input.disabled).toBe(true);
            },
            ),
        );

    });
});

@Component({
    template: '<gtx-checkbox></gtx-checkbox>',
    standalone: false,
})
class TestComponent {

    boundProperty: any;
    checkState = false;
    testIndeterminate = false;
    testForm = new FormGroup({
        testControl: new FormControl<CheckboxState>(true),
    });

    @ViewChild(CheckboxComponent, { static: true })
    checkboxComponent: CheckboxComponent;

    onBlur(...args: any[]): void {}
    onFocus(...args: any[]): void {}
    onChange(...args: any[]): void {}
}

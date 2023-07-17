import { Component, NO_ERRORS_SCHEMA } from '@angular/core';
import { tick } from '@angular/core/testing';
import { UntypedFormControl, UntypedFormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { GenticsUICoreModule, InputComponent } from '@gentics/ui-core';
import { MultiValueValidityState } from '../../../common';
import { componentTest, configureComponentTest } from '../../../testing';
import { StringListComponent } from './string-list.component';

describe('StringListComponent', () => {

    describe('StringListComponent via Value', () => {
        @Component({
            template: `<gtx-string-list
                [label]="label"
                [disabled]="disabled"
                [sortable]="sortable"
                [value]="value"
                [errors]="errors"
                (valueChange)="onValueChange($event)"
                (touch)="onTouch($event)"
            ></gtx-string-list>`,
        })
        class TestComponent {
            public label: string;
            public disabled = false;
            public sortable = false;
            public value: string[] = [];
            public errors: MultiValueValidityState;
            public formControl: UntypedFormControl;

            public valueChangeSpy: jasmine.Spy;
            public onTouchSpy: jasmine.Spy;

            constructor() {
                this.valueChangeSpy = spyOn(this, 'onValueChange' as any) as any;
                this.onTouchSpy = spyOn(this, 'onTouch' as any) as any;
            }

            onValueChange(event: any): void { }
            onTouch(): void { }
        }

        beforeEach(async () => {
            configureComponentTest({
                imports: [
                    GenticsUICoreModule.forRoot(),
                    FormsModule,
                    ReactiveFormsModule,
                ],
                providers: [],
                declarations: [
                    TestComponent,
                    StringListComponent,
                ],
                schemas: [NO_ERRORS_SCHEMA],
            });
        });

        it('should display no label if it is not present', componentTest(() => TestComponent, (fixture, instance) => {
            expect(fixture.nativeElement.querySelector('label')).toBeNull();
        }));

        it('should display a label if it is set', componentTest(() => TestComponent, async (fixture, instance) => {
            const label = 'Hello World';
            instance.label = label;
            fixture.detectChanges();
            tick();

            const labelElement: HTMLLabelElement = fixture.nativeElement.querySelector('label');
            expect(labelElement).not.toBeNull();
            expect(labelElement.textContent).toEqual(label);
        }));

        it('should not display a list without values', componentTest(() => TestComponent, (fixture, instance) => {
            const listElement: HTMLDivElement = fixture.nativeElement.querySelector('.list');
            expect(listElement).toBeNull();
        }));

        it('should display the initial value via input', componentTest(() => TestComponent, async (fixture, instance) => {
            const value = ['Hello', 'World'];
            instance.value = value;
            fixture.detectChanges();
            tick();

            const listElement: HTMLDivElement = fixture.nativeElement.querySelector('.list');
            expect(listElement).not.toBeNull();
            const inputElements: NodeListOf<HTMLInputElement> = listElement.querySelectorAll('.input input');
            expect(inputElements).not.toBeNull();
            expect(inputElements.length).toEqual(value.length);
            inputElements.forEach((item, index) => expect(item.value).toEqual(value[index], `Items value doesn't match "${value[index]}": ${item}`));
        }));

        it('should add a new element on click', componentTest(() => TestComponent, (fixture, instance) => {
            expect(instance.value).toEqual([]);

            const spy = instance.valueChangeSpy;
            const addButton: HTMLElement = fixture.nativeElement.querySelector('.add-button');
            expect(addButton).not.toBeNull();

            // First click
            addButton.click();
            tick();
            expect(instance.onValueChange).toHaveBeenCalledTimes(1);
            expect(instance.onValueChange).toHaveBeenCalledWith(['']);

            // Click it a second time
            spy.calls.reset();
            addButton.click();
            tick();
            expect(instance.onValueChange).toHaveBeenCalledTimes(1);
            expect(instance.onValueChange).toHaveBeenCalledWith(['', '']);
        }));

        it('should be possible to edit the values', componentTest(() => TestComponent, async (fixture, instance) => {
            const changeIndex = 2;
            const changeValue = 'something different';
            const initialValue = ['Hello', 'World', 'foo', 'bar'];
            const expectedValue = [
                ...initialValue.slice(0, changeIndex),
                changeValue,
                ...initialValue.slice(changeIndex + 1),
            ];
            instance.value = initialValue;
            fixture.detectChanges();
            tick();

            const listElement: HTMLDivElement = fixture.nativeElement.querySelector('.list');
            expect(listElement).not.toBeNull();
            const inputElements: NodeListOf<HTMLInputElement> = listElement.querySelectorAll('.input input');
            expect(inputElements).not.toBeNull();
            expect(inputElements.length).toEqual(initialValue.length);

            // Jank way to trigger a change from the input-field we want
            const changeInputRef = inputElements.item(changeIndex);
            changeInputRef.value = changeValue;
            const changeInput: InputComponent = fixture.debugElement.query(element => element.nativeElement === changeInputRef).componentInstance;
            changeInput.onInput({ target: changeInputRef } as any);
            fixture.detectChanges();
            tick();
            await fixture.whenRenderingDone();

            expect(instance.onValueChange).toHaveBeenCalledTimes(1);
            expect(instance.onValueChange).toHaveBeenCalledWith(expectedValue);
        }));

        it('should be possible to delete an entry', componentTest(() => TestComponent, async (fixture, instance) => {
            const initialValue = ['Hello', 'foo', 'bar', 'delete', 'me', 'here'];
            const deleteIndex = 3;
            const expectedValue = ['Hello', 'foo', 'bar', 'me', 'here'];
            instance.value = initialValue;
            fixture.detectChanges();
            tick();

            const listElement: HTMLDivElement = fixture.nativeElement.querySelector('.list');
            expect(listElement).not.toBeNull();
            const deleteButtons: NodeListOf<HTMLInputElement> = listElement.querySelectorAll('.delete-button .button-event-wrapper button');
            expect(deleteButtons).not.toBeNull();
            expect(deleteButtons.length).toEqual(initialValue.length);

            deleteButtons.item(deleteIndex).click();
            fixture.detectChanges();
            tick();
            await fixture.whenRenderingDone();

            expect(instance.onValueChange).toHaveBeenCalledTimes(1);
            expect(instance.onValueChange).toHaveBeenCalledWith(expectedValue);
        }));

        it('should not be possible to add values when disabled', componentTest(() => TestComponent, async (fixture, instance) => {
            const value = ['All', 'your', 'base', 'are', 'belong', 'to', 'us'];
            const label = 'sample text';
            instance.value = value;
            instance.label = label;
            instance.disabled = true;
            fixture.detectChanges();
            tick();

            fixture.nativeElement.querySelector('.add-button').click();
            fixture.detectChanges();
            tick();
            expect(instance.onValueChange).toHaveBeenCalledTimes(0);
            expect(instance.onTouch).toHaveBeenCalledTimes(0);
        }));

        // Can't be tested properly, as the input will trigger changes even if it is disabled.
        // Same goes for the underlying form-control/form-array
        it('should not be possible to change values when disabled', componentTest(() => TestComponent, async (fixture, instance) => {
            const value = ['All', 'your', 'base', 'are', 'belong', 'to', 'us'];
            const label = 'sample text';
            instance.value = value;
            instance.label = label;
            instance.disabled = true;
            fixture.detectChanges();
            tick();

            const inputs = fixture.nativeElement.querySelectorAll('.list .input input');
            const inputRef: HTMLInputElement = inputs.item(4);
            inputRef.value = 'change me to something else';
            const changeInput: InputComponent = fixture.debugElement.query(element => element.nativeElement === inputRef).componentInstance;
            changeInput.onInput({ target: inputRef } as any);
            fixture.detectChanges();
            tick();
            expect(instance.onValueChange).toHaveBeenCalledTimes(0);
            expect(instance.onTouch).toHaveBeenCalledTimes(0);
        }));

        it('should not be possible to remove values when disabled', componentTest(() => TestComponent, async (fixture, instance) => {
            const value = ['All', 'your', 'base', 'are', 'belong', 'to', 'us'];
            const label = 'sample text';
            instance.value = value;
            instance.label = label;
            instance.disabled = true;
            fixture.detectChanges();
            tick();

            fixture.nativeElement.querySelectorAll('.list .delete-button .button-event-wrapper button').item(2).click();
            fixture.detectChanges();
            tick();
            expect(instance.onValueChange).toHaveBeenCalledTimes(0);
            expect(instance.onTouch).toHaveBeenCalledTimes(0);
        }));
    });

    describe('StringListComponent via FormControl', () => {
        @Component({
            template: `<ng-container [formGroup]="form">
                <gtx-string-list
                    [label]="label"
                    [sortable]="sortable"
                    [errors]="errors"
                    formControlName="test"
                    (valueChange)="onValueChange($event)"
                    (touch)="onTouch($event)"
                ></gtx-string-list>
            </ng-container>`,
        })
        class TestComponent {
            public label: string;
            public sortable = false;
            public errors: MultiValueValidityState;
            public formControl = new UntypedFormControl([]);
            public form = new UntypedFormGroup({
                test: this.formControl,
            });

            public formChangeSpy: jasmine.Spy;
            public valueChangeSpy: jasmine.Spy;
            public onTouchSpy: jasmine.Spy;

            constructor() {
                this.formChangeSpy = spyOn(this, 'onFormControlChange' as any) as any;
                this.valueChangeSpy = spyOn(this, 'onValueChange' as any) as any;
                this.onTouchSpy = spyOn(this, 'onTouch' as any) as any;
                this.formControl.valueChanges.subscribe(change => this.onFormControlChange(change));
            }

            onFormControlChange(change: any): void { }
            onValueChange(event: any): void { }
            onTouch(): void { }
        }

        beforeEach(() => {
            configureComponentTest({
                imports: [
                    GenticsUICoreModule.forRoot(),
                    FormsModule,
                    ReactiveFormsModule,
                ],
                providers: [],
                declarations: [
                    TestComponent,
                    StringListComponent,
                ],
                schemas: [NO_ERRORS_SCHEMA],
            });
        });

        it('should display no label if it is not present', componentTest(() => TestComponent, (fixture, instance) => {
            expect(fixture.nativeElement.querySelector('label')).toBeNull();
        }));

        it('should display a label if it is set', componentTest(() => TestComponent, async (fixture, instance) => {
            const label = 'Hello World';
            instance.label = label;
            fixture.detectChanges();
            tick();

            const labelElement: HTMLLabelElement = fixture.nativeElement.querySelector('label');
            expect(labelElement).not.toBeNull();
            expect(labelElement.textContent).toEqual(label);
        }));

        it('should not display a list without values', componentTest(() => TestComponent, (fixture, instance) => {
            const listElement: HTMLDivElement = fixture.nativeElement.querySelector('.list');
            expect(listElement).toBeNull();
        }));

        it('should display the initial value', componentTest(() => TestComponent, async (fixture, instance) => {
            const value = ['Hello', 'World'];
            instance.formControl.patchValue(value, { emitEvent: false });
            fixture.detectChanges();
            tick();

            const listElement: HTMLDivElement = fixture.nativeElement.querySelector('.list');
            expect(listElement).not.toBeNull();
            const inputElements: NodeListOf<HTMLInputElement> = listElement.querySelectorAll('.input input');
            expect(inputElements).not.toBeNull();
            expect(inputElements.length).toEqual(value.length);
            inputElements.forEach((item, index) => expect(item.value).toEqual(value[index], `Items value doesn't match "${value[index]}": ${item}`));
        }));

        it('should add a new element on click', componentTest(() => TestComponent, (fixture, instance) => {
            const addButton: HTMLElement = fixture.nativeElement.querySelector('.add-button .button-event-wrapper button');
            expect(addButton).not.toBeNull();
            fixture.detectChanges();
            tick();

            // First click
            addButton.click();
            fixture.detectChanges();
            tick();

            expect(instance.onFormControlChange).toHaveBeenCalledTimes(1);
            expect(instance.onFormControlChange).toHaveBeenCalledWith(['']);
            expect(instance.formControl.value).toEqual(['']);
            // OnValueChange spy is broken for some reason
            // expect(instance.onValueChange).toHaveBeenCalledTimes(1);
            // expect(instance.onValueChange).toHaveBeenCalledWith(['']);

            // Reset the spies
            instance.formChangeSpy.calls.reset();
            instance.valueChangeSpy.calls.reset();

            // Click it a second time
            addButton.click();
            fixture.detectChanges();
            tick();

            expect(instance.formControl.value).toEqual(['', '']);
            expect(instance.onFormControlChange).toHaveBeenCalledTimes(1);
            expect(instance.onFormControlChange).toHaveBeenCalledWith(['', '']);
            // OnValueChange spy is broken for some reason
            // expect(instance.onValueChange).toHaveBeenCalledTimes(1);
            // expect(instance.onValueChange).toHaveBeenCalledWith(['', '']);
        }));

        it('should be possible to edit the values', componentTest(() => TestComponent, async (fixture, instance) => {
            const changeIndex = 2;
            const changeValue = 'something different';
            const initialValue = ['Hello', 'World', 'foo', 'bar'];
            const expectedValue = [
                ...initialValue.slice(0, changeIndex),
                changeValue,
                ...initialValue.slice(changeIndex + 1),
            ];
            instance.formControl.patchValue(initialValue, { emitEvent: false });
            fixture.detectChanges();
            tick();

            const listElement: HTMLDivElement = fixture.nativeElement.querySelector('.list');
            expect(listElement).not.toBeNull();
            const inputElements: NodeListOf<HTMLInputElement> = listElement.querySelectorAll('.input input');
            expect(inputElements).not.toBeNull();
            expect(inputElements.length).toEqual(initialValue.length);

            // Jank way to trigger a change from the input-field we want
            const changeInputRef = inputElements.item(changeIndex);
            changeInputRef.value = changeValue;
            const changeInput: InputComponent = fixture.debugElement.query(element => element.nativeElement === changeInputRef).componentInstance;
            changeInput.onInput({ target: changeInputRef } as any);
            fixture.detectChanges();
            tick();
            await fixture.whenRenderingDone();

            expect(instance.onFormControlChange).toHaveBeenCalledTimes(1);
            expect(instance.onFormControlChange).toHaveBeenCalledWith(expectedValue);
            // OnValueChange spy is broken for some reason
            // expect(instance.onValueChange).toHaveBeenCalledTimes(1);
            // expect(instance.onValueChange).toHaveBeenCalledWith(expectedValue);
        }));

        it('should be possible to edit the values via FormControl', componentTest(() => TestComponent, async (fixture, instance) => {
            const changeIndex = 2;
            const changeValue = 'something different';
            const initialValue = ['Hello', 'World', 'foo', 'bar'];
            const expectedValue = [
                ...initialValue.slice(0, changeIndex),
                changeValue,
                ...initialValue.slice(changeIndex + 1),
            ];
            instance.formControl.patchValue(initialValue, { emitEvent: false });
            fixture.detectChanges();
            tick();

            const listElement: HTMLDivElement = fixture.nativeElement.querySelector('.list');
            expect(listElement).not.toBeNull();
            const inputElements: NodeListOf<HTMLInputElement> = listElement.querySelectorAll('.input input');
            expect(inputElements).not.toBeNull();
            expect(inputElements.length).toEqual(initialValue.length);

            // Jank way to trigger a change from the input-field we want
            const changeInputRef = inputElements.item(changeIndex);
            changeInputRef.value = changeValue;
            const changeInput: InputComponent = fixture.debugElement.query(element => element.nativeElement === changeInputRef).componentInstance;
            changeInput.onInput({ target: changeInputRef } as any);
            fixture.detectChanges();
            tick();
            await fixture.whenRenderingDone();

            expect(instance.onFormControlChange).toHaveBeenCalledTimes(1);
            expect(instance.onFormControlChange).toHaveBeenCalledWith(expectedValue);
            // OnValueChange spy is broken for some reason
            // expect(instance.onValueChange).toHaveBeenCalledTimes(1);
            // expect(instance.onValueChange).toHaveBeenCalledWith(expectedValue);
        }));

        it('should be possible to delete an entry', componentTest(() => TestComponent, async (fixture, instance) => {
            const initialValue = ['Hello', 'foo', 'bar', 'delete', 'me', 'here'];
            const deleteIndex = 3;
            const expectedValue = ['Hello', 'foo', 'bar', 'me', 'here'];
            instance.formControl.patchValue(initialValue, { emitEvent: false });
            fixture.detectChanges();
            tick();

            const listElement: HTMLDivElement = fixture.nativeElement.querySelector('.list');
            expect(listElement).not.toBeNull();
            const deleteButtons: NodeListOf<HTMLInputElement> = listElement.querySelectorAll('.delete-button .button-event-wrapper button');
            expect(deleteButtons).not.toBeNull();
            expect(deleteButtons.length).toEqual(initialValue.length);

            deleteButtons.item(deleteIndex).click();
            fixture.detectChanges();
            tick();
            await fixture.whenRenderingDone();

            expect(instance.onFormControlChange).toHaveBeenCalledTimes(1);
            expect(instance.onFormControlChange).toHaveBeenCalledWith(expectedValue);
            // OnValueChange spy is broken for some reason
            // expect(instance.onValueChange).toHaveBeenCalledTimes(1);
            // expect(instance.onValueChange).toHaveBeenCalledWith(expectedValue);
        }));

        it('should not be possible to add values when disabled', componentTest(() => TestComponent, async (fixture, instance) => {
            const value = ['All', 'your', 'base', 'are', 'belong', 'to', 'us'];
            const label = 'sample text';
            instance.formControl.disable({ emitEvent: false });
            fixture.detectChanges();
            tick();
            instance.formControl.patchValue(value, { emitEvent: false });
            instance.label = label;
            fixture.detectChanges();
            tick();

            fixture.nativeElement.querySelector('.add-button').click();
            fixture.detectChanges();
            tick();
            expect(instance.onFormControlChange).toHaveBeenCalledTimes(0);
            expect(instance.onValueChange).toHaveBeenCalledTimes(0);
            expect(instance.onTouch).toHaveBeenCalledTimes(0);
        }));

        // Can't be tested properly, as the input will trigger changes even if it is disabled.
        // Same goes for the underlying form-control/form-array
        it('should not be possible to change values when disabled', componentTest(() => TestComponent, async (fixture, instance) => {
            const value = ['All', 'your', 'base', 'are', 'belong', 'to', 'us'];
            const label = 'sample text';
            instance.formControl.disable({ emitEvent: false });
            fixture.detectChanges();
            tick();
            instance.formControl.patchValue(value, { emitEvent: false });
            instance.label = label;
            fixture.detectChanges();
            tick();

            const inputs = fixture.nativeElement.querySelectorAll('.list .input input');
            const inputRef: HTMLInputElement = inputs.item(4);
            inputRef.value = 'change me to something else';
            const changeInput: InputComponent = fixture.debugElement.query(element => element.nativeElement === inputRef).componentInstance;
            changeInput.onInput({ target: inputRef } as any);
            fixture.detectChanges();
            tick();
            expect(instance.onFormControlChange).toHaveBeenCalledTimes(0);
            expect(instance.onValueChange).toHaveBeenCalledTimes(0);
            expect(instance.onTouch).toHaveBeenCalledTimes(0);
        }));

        it('should not be possible to remove values when disabled', componentTest(() => TestComponent, async (fixture, instance) => {
            const value = ['All', 'your', 'base', 'are', 'belong', 'to', 'us'];
            const label = 'sample text';
            instance.formControl.disable({ emitEvent: false });
            fixture.detectChanges();
            tick();
            instance.formControl.patchValue(value, { emitEvent: false });
            instance.label = label;
            fixture.detectChanges();
            tick();

            fixture.nativeElement.querySelectorAll('.list .delete-button .button-event-wrapper button').item(2).click();
            fixture.detectChanges();
            tick();
            expect(instance.onFormControlChange).toHaveBeenCalledTimes(0);
            expect(instance.onValueChange).toHaveBeenCalledTimes(0);
            expect(instance.onTouch).toHaveBeenCalledTimes(0);
        }));
    });
});

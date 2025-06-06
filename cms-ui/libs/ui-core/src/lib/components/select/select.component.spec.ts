import { Component, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, tick } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule, UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowserDynamicTestingModule } from '@angular/platform-browser-dynamic/testing';
import { BehaviorSubject } from 'rxjs';
import { KeyCode } from '../../common/keycodes';
import { DropdownTriggerDirective } from '../../directives/dropdown-trigger/dropdown-trigger.directive';
import { IconDirective } from '../../directives/icon/icon.directive';
import { SelectOptionGroupDirective } from '../../directives/select-option-group/option-group.directive';
import { SelectOptionDirective } from '../../directives/select-option/option.directive';
import { ConfigService, defaultConfig } from '../../module.config';
import { OverlayHostService } from '../../providers/overlay-host/overlay-host.service';
import { SizeTrackerService } from '../../providers/size-tracker/size-tracker.service';
import { componentTest } from '../../testing';
import { KeyboardEventConfig, crossBrowserInitKeyboardEvent } from '../../testing/keyboard-event';
import { ButtonComponent } from '../button/button.component';
import { CheckboxComponent } from '../checkbox/checkbox.component';
import { DropdownContentWrapperComponent } from '../dropdown-content-wrapper/dropdown-content-wrapper.component';
import { DropdownContentComponent } from '../dropdown-content/dropdown-content.component';
import { DropdownListComponent } from '../dropdown-list/dropdown-list.component';
import { InputComponent } from '../input/input.component';
import { OverlayHostComponent } from '../overlay-host/overlay-host.component';
import { ScrollMaskComponent } from '../scroll-mask/scroll-mask.component';
import { SelectComponent } from './select.component';

describe('SelectComponent', () => {

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [FormsModule, ReactiveFormsModule],
            declarations: [
                SelectComponent,
                SelectOptionDirective,
                SelectOptionGroupDirective,
                IconDirective,
                ButtonComponent,
                TestComponent,
                InputComponent,
                CheckboxComponent,
                DropdownListComponent,
                DropdownContentComponent,
                DropdownContentWrapperComponent,
                DropdownTriggerDirective,
                ScrollMaskComponent,
                OverlayHostComponent,
            ],
            providers: [
                OverlayHostService,
                SizeTrackerService,
                { provide: ConfigService, useValue: defaultConfig },
            ],
            teardown: { destroyAfterEach: false },
            schemas: [NO_ERRORS_SCHEMA],
        });
        TestBed.overrideModule(BrowserDynamicTestingModule, {
            set: {
                declarations: [DropdownContentWrapperComponent, ScrollMaskComponent],
            },
        });
    });

    it('emits "clear" when the clear selection button is clicked',
        componentTest(() => TestComponent, `
                <gtx-select clearable (valueChange)="onChange($event)"></gtx-select>`,
        (fixture, instance) => {
            instance.onChange = jasmine.createSpy('onChange');
            fixture.detectChanges();
            tick();

            expect(instance.onChange).not.toHaveBeenCalled();

            const clearButton = fixture.debugElement.query(By.css('gtx-button'));
            clearButton.triggerEventHandler('click', document.createEvent('Event'));
            tick();
            fixture.detectChanges();

            expect(instance.onChange).toHaveBeenCalledWith(null);
        },
        ),
    );

    it('updates the value via ngModel when the clear selection button is clicked',
        componentTest(() => TestComponent, `
                <gtx-select [(ngModel)]="ngModelValue" (valueChange)="onChange($event)" clearable>
                        <gtx-option *ngFor="let option of options" [value]="option">{{ option }}</gtx-option>
                </gtx-select>`,
        (fixture, instance) => {
            fixture.detectChanges();
            tick();
            expect(instance.ngModelValue).toEqual('Bar');

            const clearButton = fixture.debugElement.query(By.css('gtx-button'));
            clearButton.triggerEventHandler('click', document.createEvent('Event'));
            tick();
            fixture.detectChanges();

            expect(instance.ngModelValue).toBeNull();
        },
        ),
    );

    it('displays the placeholder, if it is set and if nothing is selected',
        componentTest(() => TestComponent, `
            <gtx-select [(ngModel)]="ngModelValue" (valueChange)="onChange($event)" clearable [placeholder]="placeholder">
                    <gtx-option *ngFor="let option of options" [value]="option">{{ option }}</gtx-option>
            </gtx-select>`,
        (fixture, instance) => {
            fixture.detectChanges();
            tick();
            expect(instance.ngModelValue).toEqual('Bar');

            const clearButton = fixture.debugElement.query(By.css('gtx-button'));
            clearButton.triggerEventHandler('click', document.createEvent('Event'));
            tick();
            fixture.detectChanges();

            const placeholder: HTMLElement = fixture.debugElement.query(By.css('.placeholder')).nativeElement;

            expect(placeholder).toBeDefined();
            expect(placeholder.textContent).toEqual('More...');
        },
        ),
    );

    it('binds its label to the input value',
        componentTest(() => TestComponent, `
            <gtx-select label="testLabel"></gtx-select>`,
        fixture => {
            fixture.detectChanges();
            const label: HTMLElement = fixture.nativeElement.querySelector('label');

            expect(label.textContent).toBe('testLabel');
        },
        ),
    );

    it('contains class with-label if label is present',
        componentTest(() => TestComponent, `
        <gtx-select label="testLabel"></gtx-select>`,
        fixture => {
            fixture.detectChanges();
            const dropdown: HTMLElement = fixture.nativeElement.querySelector('gtx-dropdown-list');

            expect(dropdown.classList).toContain('with-label');
        },
        ),
    );

    it('does not contain class with-label if label is not present',
        componentTest(() => TestComponent, `
        <gtx-select></gtx-select>`,
        fixture => {
            fixture.detectChanges();
            const dropdown: HTMLElement = fixture.nativeElement.querySelector('gtx-dropdown-trigger');

            expect(dropdown.classList).not.toContain('with-label');
        },
        ),
    );

    it('adds a "disabled" attribute to the view-value div if the disabled attribute is true.',
        componentTest(() => TestComponent, `
            <gtx-select label="testLabel" disabled="true"></gtx-select>`,
        fixture => {
            fixture.detectChanges();
            const viewValue: HTMLElement = fixture.debugElement.query(By.css('.view-value')).nativeElement;

            expect(viewValue.getAttribute('disabled')).toBe('true');
        },
        ),
    );

    it('when disabled, the viewValue div is not focusable.',
        componentTest(() => TestComponent, `
            <gtx-select label="testLabel" disabled="true"></gtx-select>`,
        fixture => {
            fixture.detectChanges();
            const viewValue: HTMLElement = fixture.debugElement.query(By.css('.view-value')).nativeElement;
            viewValue.focus();
            expect(document.activeElement).not.toBe(viewValue);
        },
        ),
    );

    it('accepts a string "value" and sets the viewValue to match.',
        componentTest(() => TestComponent, fixture => {
            fixture.detectChanges();
            tick();
            clickSelectAndOpen(fixture);
            const viewValue: HTMLElement = fixture.debugElement.query(By.css('.view-value')).nativeElement;
            expect(viewValue.textContent).toContain('Bar');

            tick(1000);
        }),
    );

    it('marks the initial value as selected.',
        componentTest(() => TestComponent, fixture => {
            fixture.detectChanges();
            tick();
            clickSelectAndOpen(fixture);
            const viewValue: HTMLElement = fixture.debugElement.query(By.css('li.selected')).nativeElement;
            expect(viewValue.textContent).toContain('Bar');

            tick(1000);
        }),
    );

    it('if no value is set, the viewValue is empty.',
        componentTest(() => TestComponent, `
            <gtx-select>
                <gtx-option>Foo</gtx-option>
                <gtx-option>Bar</gtx-option>
                <gtx-option>Baz</gtx-option>
            </gtx-select>
            <gtx-overlay-host></gtx-overlay-host>`,
        fixture => {
            fixture.detectChanges();
            tick();

            const viewValue: HTMLElement = fixture.debugElement.query(By.css('.view-value > div')).nativeElement;

            expect(viewValue.textContent).toBe('');

            tick(1000);
        }),
    );

    it('accept an array "value" and marks the matching options "selected" (multi select)',
        componentTest(() => TestComponent, `
            <gtx-select [value]="multiValue" multiple="true">
                <gtx-option *ngFor="let option of options" [value]="option">{{ option }}</gtx-option>
            </gtx-select>
            <gtx-overlay-host></gtx-overlay-host>`,
        fixture => {
            fixture.detectChanges();
            tick();
            clickSelectAndOpen(fixture);

            const checkboxes: CheckboxComponent[] = fixture.debugElement.queryAll(By.directive(CheckboxComponent)).map(de => de.componentInstance);

            expect(checkboxes[0].value).toBe(false);
            expect(checkboxes[1].value).toBe(true);
            expect(checkboxes[2].value).toBe(true);

            tick(1000);
        },
        ),
    );

    it('updates the "value" when a different option is clicked',
        componentTest(() => TestComponent, fixture => {
            fixture.detectChanges();
            tick();
            clickSelectAndOpen(fixture);

            const selectInstance: SelectComponent = fixture.debugElement.query(By.directive(SelectComponent)).componentInstance;
            const listItems = getListItems(fixture);

            listItems[0].click();
            tick();
            expect(selectInstance.value).toBe('Foo');

            listItems[2].click();
            tick();
            expect(selectInstance.value).toBe('Baz');
        }),
    );

    it('emits "blur" with the current value when the native input is blurred',
        componentTest(() => TestComponent, (fixture, instance) => {
            fixture.detectChanges();
            tick();
            const fakeInput: HTMLInputElement = fixture.debugElement.query(By.css('.view-value')).nativeElement;
            spyOn(instance, 'onBlur');

            triggerEvent(fakeInput, 'blur');
            tick();
            fixture.detectChanges();

            expect(instance.onBlur).toHaveBeenCalledWith('Bar');
        }),
    );

    it('emits "change" when a list item is clicked',
        componentTest(() => TestComponent, (fixture, instance) => {
            fixture.detectChanges();
            tick();
            clickSelectAndOpen(fixture);

            const listItems = getListItems(fixture);
            instance.onChange = jasmine.createSpy('onChange');

            listItems[0].click();
            tick();
            expect(instance.onChange).toHaveBeenCalledWith('Foo');

            listItems[2].click();
            tick();
            expect(instance.onChange).toHaveBeenCalledWith('Baz');
        }),
    );

    it('emits "change" when a list item is clicked (multiple select)',
        componentTest(() => TestComponent, `
            <gtx-select multiple="true" [value]="value" (valueChange)="onChange($event)">
                <gtx-option *ngFor="let option of options" [value]="option">{{ option }}</gtx-option>
            </gtx-select>
            <gtx-overlay-host></gtx-overlay-host>`,
        (fixture, instance) => {
            fixture.componentInstance.value = ['Bar'];
            fixture.detectChanges();
            tick();
            clickSelectAndOpen(fixture);

            const listItems = getListItems(fixture);
            const onChange = instance.onChange = jasmine.createSpy('onChange');

            listItems[0].click();
            tick();
            expect(onChange.calls.argsFor(0)[0]).toEqual(['Bar', 'Foo']);

            listItems[2].click();
            tick();
            expect(onChange.calls.argsFor(1)[0]).toEqual(['Foo', 'Bar', 'Baz']);
        },
        ),
    );

    it('emits "change" with an empty array when a multiselect has no selected options',
        componentTest(() => TestComponent, `
            <gtx-select multiple="true" [value]="value" (valueChange)="onChange($event)">
                <gtx-option *ngFor="let option of options" [value]="option">{{ option }}</gtx-option>
            </gtx-select>
            <gtx-overlay-host></gtx-overlay-host>`,
        (fixture, instance) => {
            fixture.detectChanges();
            tick();
            clickSelectAndOpen(fixture);

            const listItems = getListItems(fixture);
            const onChange = instance.onChange = jasmine.createSpy('onChange');

            listItems[1].click();
            tick();
            expect(onChange.calls.argsFor(0)[0]).toEqual([]);
        },
        ),
    );

    it('updates options when the gtx-options elements change',
        componentTest(() => TestComponent, async (fixture, instance) => {
            fixture.detectChanges();
            tick();
            clickSelectAndOpen(fixture);
            const getOptionText = () => getListItems(fixture).map(el => el.textContent.trim());
            expect(getOptionText()).toEqual(['Foo', 'Bar', 'Baz']);

            instance.options.push('Quux');
            fixture.detectChanges();
            tick(100);
            await fixture.whenRenderingDone();
            tick(100);
            fixture.detectChanges();
            await fixture.whenRenderingDone();
            tick(100);
            expect(getOptionText()).toEqual(['Foo', 'Bar', 'Baz', 'Quux']);
        },
        ),
    );

    it('updates options when the gtx-options elements change asynchronously',
        componentTest(() => TestComponent, async (fixture, instance) => {
            fixture.detectChanges();
            tick();
            clickSelectAndOpen(fixture);
            const getOptionText = () => getListItems(fixture).map(el => el.textContent.trim());
            expect(getOptionText()).toEqual(['Foo', 'Bar', 'Baz']);

            const tmp = new Promise<void>((r => {
                setTimeout(() => {
                    instance.options.push('Quux');
                    fixture.detectChanges();
                    fixture.whenRenderingDone().then(() => r());
                }, 500);
            }));
            tick(500);

            fixture.detectChanges();
            await fixture.whenRenderingDone();
            await tmp;
            tick(1000);

            fixture.detectChanges();
            await fixture.whenRenderingDone();
            tick(1000);

            expect(getOptionText()).toEqual(['Foo', 'Bar', 'Baz', 'Quux']);
        },
        ),
    );

    describe('keyboard controls', () => {

        it('should open when enter is pressed',
            componentTest(() => TestComponent, fixture => {
                fixture.detectChanges();
                tick();
                sendKeyDown(fixture, KeyCode.Enter);
                const optionsDropdown = fixture.debugElement.query(By.css('.select-options'));
                expect(optionsDropdown).toBeTruthy();
                tick(1000);
            }),
        );

        it('should open when space is pressed',
            componentTest(() => TestComponent, fixture => {
                fixture.detectChanges();
                tick();
                sendKeyDown(fixture, KeyCode.Space);
                const optionsDropdown = fixture.debugElement.query(By.css('.select-options'));
                expect(optionsDropdown).toBeTruthy();
                tick(1000);
            }),
        );

        it('initial value should be initially selected',
            componentTest(() => TestComponent, fixture => {
                fixture.detectChanges();
                tick();
                sendKeyDown(fixture, KeyCode.Enter);
                const selected = fixture.debugElement.nativeElement.querySelector('li.selected');
                expect(selected.innerHTML).toContain('Bar');
                tick(1000);
            }),
        );

        it('down arrow should select subsequent items',
            componentTest(() => TestComponent, fixture => {
                fixture.detectChanges();
                tick();
                sendKeyDown(fixture, KeyCode.Enter);

                sendKeyDown(fixture, KeyCode.DownArrow);
                expect(getSelectedItem(fixture).textContent).toContain('Baz');

                sendKeyDown(fixture, KeyCode.DownArrow);
                expect(getSelectedItem(fixture).textContent).toContain('Foo');

                sendKeyDown(fixture, KeyCode.DownArrow);
                expect(getSelectedItem(fixture).textContent).toContain('Bar');

                sendKeyDown(fixture, KeyCode.DownArrow);
                expect(getSelectedItem(fixture).textContent).toContain('Baz');

                tick(1000);
            }),
        );

        it('up arrow should select previous items',
            componentTest(() => TestComponent, fixture => {
                fixture.detectChanges();
                tick();
                sendKeyDown(fixture, KeyCode.Enter);

                sendKeyDown(fixture, KeyCode.UpArrow);
                expect(getSelectedItem(fixture).textContent).toContain('Foo');

                sendKeyDown(fixture, KeyCode.UpArrow);
                expect(getSelectedItem(fixture).textContent).toContain('Baz');

                sendKeyDown(fixture, KeyCode.UpArrow);
                expect(getSelectedItem(fixture).textContent).toContain('Bar');

                sendKeyDown(fixture, KeyCode.UpArrow);
                expect(getSelectedItem(fixture).textContent).toContain('Foo');

                tick(1000);
            }),
        );

        it('home and end should select first and last items',
            componentTest(() => TestComponent, fixture => {
                fixture.detectChanges();
                tick();
                sendKeyDown(fixture, KeyCode.Enter);

                sendKeyDown(fixture, KeyCode.Home);
                expect(getSelectedItem(fixture).textContent).toContain('Foo');

                sendKeyDown(fixture, KeyCode.End);
                expect(getSelectedItem(fixture).textContent).toContain('Baz');

                tick(1000);
            }),
        );

        it('page up and page down should select first and last items',
            componentTest(() => TestComponent, fixture => {
                fixture.detectChanges();
                tick();
                sendKeyDown(fixture, KeyCode.Enter);

                sendKeyDown(fixture, KeyCode.PageUp);
                expect(getSelectedItem(fixture).textContent).toContain('Foo');

                sendKeyDown(fixture, KeyCode.PageDown);
                expect(getSelectedItem(fixture).textContent).toContain('Baz');

                tick(1000);
            }),
        );

        it('characters should select subsequent matching options',
            componentTest(() => TestComponent, fixture => {
                fixture.detectChanges();
                tick();
                sendKeyDown(fixture, KeyCode.Enter);
                const F = { key: 'f', keyCode: 70 };
                const B = { key: 'b', keyCode: 66 };

                sendKeyDown(fixture, F);
                expect(getSelectedItem(fixture).textContent).toContain('Foo');

                sendKeyDown(fixture, B);
                expect(getSelectedItem(fixture).textContent).toContain('Bar');

                sendKeyDown(fixture, B);
                expect(getSelectedItem(fixture).textContent).toContain('Baz');

                sendKeyDown(fixture, B);
                expect(getSelectedItem(fixture).textContent).toContain('Bar');

                tick(1000);
            }),
        );

        it('umlauts should work as expected',
            componentTest(() => TestComponent, (fixture, testComponent) => {
                testComponent.options = [
                    'Ägypten',
                    'Äquatorialguinea',
                    'Äthiopien',
                    'Österreich',
                ];
                testComponent.value = 'Ägypten';

                fixture.detectChanges();
                tick();
                sendKeyDown(fixture, KeyCode.Enter);
                const A_UMLAUT = { key: 'ä', keyCode: 222 };
                const O_UMLAUT = { key: 'ö', keyCode: 192 };

                sendKeyDown(fixture, O_UMLAUT);
                expect(getSelectedItem(fixture).textContent).toContain('Österreich');

                sendKeyDown(fixture, A_UMLAUT);
                expect(getSelectedItem(fixture).textContent).toContain('Ägypten');

                sendKeyDown(fixture, A_UMLAUT);
                expect(getSelectedItem(fixture).textContent).toContain('Äquatorialguinea');

                sendKeyDown(fixture, A_UMLAUT);
                expect(getSelectedItem(fixture).textContent).toContain('Äthiopien');

                tick(1000);
            }),
        );

        function sendKeyDown(fixture: ComponentFixture<any>, keyToSend: number | { key: string, keyCode: number }): void {
            const eventTarget: HTMLElement = fixture.debugElement.query(By.css('.view-value')).nativeElement;
            const eventProps: KeyboardEventConfig = { bubbles: true };
            if (typeof keyToSend === 'number') {
                eventProps.key = KeyCode[keyToSend];
                eventProps.keyCode = keyToSend;
            } else {
                eventProps.key = keyToSend.key;
                eventProps.keyCode = keyToSend.keyCode;
            }
            const keydownEvent = crossBrowserInitKeyboardEvent('keydown', eventProps);
            eventTarget.dispatchEvent(keydownEvent);
            tick();
            fixture.detectChanges();
        }

        const getSelectedItem = (fixture: ComponentFixture<TestComponent>): HTMLElement =>
            fixture.debugElement.query(By.css('.select-option.selected')).nativeElement;
    });

    describe('ValueAccessor:', () => {

        it('updates a variable bound with ngModel (outbound)',
            componentTest(() => TestComponent, `
                <gtx-select [(ngModel)]="ngModelValue">
                        <gtx-option *ngFor="let option of options" [value]="option">{{ option }}</gtx-option>
                </gtx-select>
                <gtx-overlay-host></gtx-overlay-host>`,
            (fixture, instance) => {
                fixture.detectChanges();
                tick();
                clickSelectAndOpen(fixture);

                const listItems = getListItems(fixture);

                listItems[0].click();
                tick();
                tick();
                fixture.detectChanges();
                expect(instance.ngModelValue).toBe('Foo');

                listItems[2].click();
                tick();
                tick();
                fixture.detectChanges();
                expect(instance.ngModelValue).toBe('Baz');
            },
            ),
        );

        it('updates a variable bound with formControlName (outbound)',
            componentTest(() => TestComponent, `
                <form [formGroup]="testForm">
                    <gtx-select formControlName="test">
                        <gtx-option *ngFor="let option of options" [value]="option">{{ option }}</gtx-option>
                    </gtx-select>
                </form>
                <gtx-overlay-host></gtx-overlay-host>`,
            (fixture, instance) => {
                fixture.detectChanges();
                tick();
                clickSelectAndOpen(fixture);

                const listItems = getListItems(fixture);

                listItems[0].click();
                tick();
                tick();
                fixture.detectChanges();
                expect(instance.testForm.get('test')?.value).toBe('Foo');

                listItems[2].click();
                tick();
                tick();
                fixture.detectChanges();
                expect(instance.testForm.get('test')?.value).toBe('Baz');
            },
            ),
        );

        it('binds the value to a variable with formControlName (inbound)',
            componentTest(() => TestComponent, `
                <form [formGroup]="testForm">
                    <gtx-select formControlName="test">
                        <gtx-option *ngFor="let option of options" [value]="option">{{ option }}</gtx-option>
                    </gtx-select>
                </form>
                <gtx-overlay-host></gtx-overlay-host>`,
            (fixture, instance) => {
                fixture.detectChanges();
                tick();
                clickSelectAndOpen(fixture);

                const selectInstance: SelectComponent = fixture.debugElement.query(By.directive(SelectComponent)).componentInstance;

                expect(instance.testForm.get('test')?.value).toBe('Bar');
                expect(selectInstance.value).toBe('Bar');

                (instance.testForm.get('test') as UntypedFormControl).setValue('Baz');
                fixture.detectChanges();

                expect(instance.testForm.get('test')?.value).toBe('Baz');
                expect(selectInstance.value).toBe('Baz');

                tick(1000);
            },
            ),
        );

        it('marks the component as "touched" when the native input is blurred',
            componentTest(() => TestComponent, `
                <form [formGroup]="testForm">
                    <gtx-select formControlName="test">
                        <gtx-option *ngFor="let option of options" [value]="option">{{ option }}</gtx-option>
                    </gtx-select>
                </form>
                <gtx-overlay-host></gtx-overlay-host>`,
            (fixture, instance) => {
                fixture.detectChanges();
                tick();
                const fakeInput: HTMLElement = fixture.debugElement.query(By.css('.view-value')).nativeElement;

                expect(instance.testForm.get('test')?.touched).toBe(false);
                expect(instance.testForm.get('test')?.untouched).toBe(true);

                triggerEvent(fakeInput, 'focus');
                triggerEvent(fakeInput, 'blur');
                tick();
                fixture.detectChanges();

                expect(instance.testForm.get('test')?.touched).toBe(true);
                expect(instance.testForm.get('test')?.untouched).toBe(false);
            }),
        );

        it('marks the component as "disabled" if the associated FormControl is set to disabled',
            componentTest(() => TestComponent, `
                   <form [formGroup]="testForm">
                       <gtx-select formControlName="test">
                           <gtx-option *ngFor="let option of options" [value]="option">{{ option }}</gtx-option>
                       </gtx-select>
                   </form>
                   <gtx-overlay-host></gtx-overlay-host>`,
            (fixture, instance) => {
                fixture.detectChanges();
                tick();
                const fakeInput: HTMLElement = fixture.debugElement.query(By.css('.view-value')).nativeElement;

                expect(instance.testForm.get('test')?.disabled).toBe(false);
                expect(fakeInput.getAttribute('disabled')).toBe(null);

                instance.testForm.get('test')?.disable();
                fixture.detectChanges();

                expect(instance.testForm.get('test')?.disabled).toBe(true);
                expect(fakeInput.getAttribute('disabled')).toBe('true');
            }),
        );

        it('can be disabled via the form control',
            componentTest(() => TestComponent, `
                <form [formGroup]="testForm">
                    <gtx-select formControlName="test">
                        <gtx-option *ngFor="let option of options" [value]="option">{{ option }}</gtx-option>
                    </gtx-select>
                </form>`,
            (fixture, instance) => {
                fixture.detectChanges();

                const div: HTMLDivElement = fixture.nativeElement.querySelector('.view-value');
                expect(div.getAttribute('disabled')).not.toBe('true');

                instance.testForm.get('test')?.disable();
                fixture.detectChanges();
                expect(div.getAttribute('disabled')).toBe('true');
            },
            ),
        );

        it('renders the correct value as selected when used with reactive forms (string values)',
            componentTest(() => TestComponent, `
                <form [formGroup]="testForm">
                    <gtx-select formControlName="test">
                        <gtx-option *ngFor="let option of options" [value]="option">{{ option }}</gtx-option>
                    </gtx-select>
                </form>
                <gtx-overlay-host></gtx-overlay-host>`,
            async (fixture, instance) => {
                instance.testForm.reset({ test: 'Bar' }, { emitEvent: false });

                fixture.detectChanges();
                tick(100);
                await fixture.whenRenderingDone();
                fixture.detectChanges();
                tick(100);
                await fixture.whenRenderingDone();

                const displayedText: string = fixture.nativeElement.querySelector('.view-value').textContent;
                expect(displayedText).toContain('Bar');
            },
            ),
        );

        it('renders the correct value as selected when used with reactive forms (number values)',
            componentTest(() => TestComponent, `
            <form [formGroup]="testForm">
                <gtx-select formControlName="test">
                    <gtx-option [value]="1">One</gtx-option>
                    <gtx-option [value]="2">Two</gtx-option>
                    <gtx-option [value]="3">Three</gtx-option>
                    <gtx-option [value]="4">Four</gtx-option>
                </gtx-select>
            </form>
            <gtx-overlay-host></gtx-overlay-host>`,
            async (fixture, instance) => {
                instance.testForm.reset({ test: 3 }, { emitEvent: false });

                fixture.detectChanges();
                tick(100);
                await fixture.whenRenderingDone();
                fixture.detectChanges();
                tick(100);
                await fixture.whenRenderingDone();

                const displayedText: string = fixture.nativeElement.querySelector('.view-value').textContent;
                expect(displayedText).toContain('Three');
            },
            ),
        );

        it('works as expected when value is provided with a subject',
            componentTest(() => TestComponent, `
                <gtx-select [value]="valueSubject | async" (valueChange)="valueSubject.next($event)">
                    <gtx-option *ngFor="let option of optionsSubject | async" [value]="option">
                        {{ option.name }}
                    </gtx-option>
                </gtx-select>
                <gtx-overlay-host></gtx-overlay-host>`,
            (fixture, instance) => {
                let lastEmittedValue: any;
                instance.valueSubject.subscribe(v => lastEmittedValue = v);

                const options = [
                    { name: 'First option' },
                    { name: 'Second option' },
                    { name: 'Third option' },
                ];
                instance.optionsSubject.next(options);
                instance.valueSubject.next(options[1]);
                fixture.detectChanges();
                tick();

                expect(lastEmittedValue).toBe(options[1]);

                clickSelectAndOpen(fixture);
                getListItems(fixture)[2].click();
                fixture.detectChanges();

                expect(lastEmittedValue).toBe(options[2]);

                tick(1000);
            },
            ),
        );

        it('works as expected when ngModel is provided with a subject',
            componentTest(() => TestComponent, `
                <gtx-select [ngModel]="valueSubject | async" (ngModelChange)="valueSubject.next($event)">
                    <gtx-option *ngFor="let option of optionsSubject | async" [value]="option">
                        {{ option.name }}
                    </gtx-option>
                </gtx-select>
                <gtx-overlay-host></gtx-overlay-host>`,
            (fixture, instance) => {
                let lastEmittedValue: any;
                instance.valueSubject.subscribe(v => lastEmittedValue = v);

                const options = [
                    { name: 'First option' },
                    { name: 'Second option' },
                    { name: 'Third option' },
                ];
                instance.optionsSubject.next(options);
                instance.valueSubject.next(options[1]);
                fixture.detectChanges();
                tick();

                expect(lastEmittedValue).toBe(options[1]);

                clickSelectAndOpen(fixture);
                getListItems(fixture)[2].click();
                fixture.detectChanges();

                expect(lastEmittedValue).toBe(options[2]);

                tick(1000);
            },
            ),
        );
    });

});

@Component({
    template: `
        <gtx-select
            [value]="value"
            (blur)="onBlur($event)"
            (valueChange)="onChange($event)"
        >
            <gtx-option *ngFor="let option of options" [value]="option">{{ option }}</gtx-option>
        </gtx-select>
        <gtx-overlay-host></gtx-overlay-host>
`,
})
class TestComponent {

    value: string | string[] = 'Bar';
    multiValue: string[] = ['Bar', 'Baz'];
    ngModelValue = 'Bar';
    placeholder = 'More...';
    options: string[] = ['Foo', 'Bar', 'Baz'];
    optionsSubject = new BehaviorSubject<any[]>([]);
    valueSubject = new BehaviorSubject<any>('Initial value');
    testForm: UntypedFormGroup = new UntypedFormGroup({
        test: new UntypedFormControl('Bar'),
    });

    onBlur(...args: any[]): void { }
    onFocus(...args: any[]): void { }
    onChange(...args: any[]): void { }
}

function clickSelectAndOpen(fixture: ComponentFixture<TestComponent>): void {
    fixture.debugElement.query(By.directive(DropdownTriggerDirective)).nativeElement.click();
    tick(100);
    fixture.detectChanges();
}

const getListItems = (fixture: ComponentFixture<TestComponent>): HTMLLIElement[] =>
    fixture.debugElement.queryAll(By.css('.select-option')).map(de => de.nativeElement);

/**
 * Create an dispatch an 'input' event on the <input> element
 */
function triggerEvent(el: HTMLElement, eventName: string): void {
    const event: Event = document.createEvent('Event');
    event.initEvent(eventName, true, true);
    el.dispatchEvent(event);
}

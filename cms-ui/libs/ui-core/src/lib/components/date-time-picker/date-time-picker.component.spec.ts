import {
    ChangeDetectionStrategy,
    Component,
    DebugElement,
    EventEmitter,
    Injectable,
    Input,
    NO_ERRORS_SCHEMA,
    Output,
    Type,
    ViewChild,
} from '@angular/core';
import { ComponentFixture, TestBed, tick } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule, UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { componentTest } from '@gentics/ui-core/testing';
import { Subject } from 'rxjs';
import { IModalInstance, IModalOptions } from '../../common/modal';
import { IconDirective } from '../../directives/icon/icon.directive';
import { DateTimePickerFormatProvider } from '../../providers/date-time-picker-format-provider/date-time-picker-format-provider.service';
import { ModalService } from '../../providers/modal/modal.service';
import { OverlayHostService } from '../../providers/overlay-host/overlay-host.service';
import { SizeTrackerService } from '../../providers/size-tracker/size-tracker.service';
import { UserAgentProvider } from '../../providers/user-agent/user-agent-ref';
import { ButtonComponent } from '../button/button.component';
import { DateTimePickerControlsComponent } from '../date-time-picker-controls/date-time-picker-controls.component';
import { DateTimePickerModal } from '../date-time-picker-modal/date-time-picker-modal.component';
import { DynamicModal } from '../dynamic-modal/dynamic-modal.component';
import { FormElementContainerComponent } from '../form-element-container/form-element-container.component';
import { InputComponent } from '../input/input.component';
import { OverlayHostComponent } from '../overlay-host/overlay-host.component';
import { DateTimePickerComponent } from './date-time-picker.component';

const TEST_TIMESTAMP = 1457971763;

let modalService: SpyModalService;
let formatProviderToUse: DateTimePickerFormatProvider | null = null;

describe('DateTimePickerComponent', () => {

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [FormsModule, ReactiveFormsModule],
            declarations: [
                FormElementContainerComponent,
                ButtonComponent,
                DateTimePickerComponent,
                DateTimePickerModal,
                DynamicModal,
                IconDirective,
                InputComponent,
                MockDateTimePickerControls,
                OnPushTestComponent,
                OverlayHostComponent,
                TestComponent,
            ],
            providers: [
                { provide: DateTimePickerFormatProvider, useFactory: (): any => formatProviderToUse },
                { provide: ModalService, useClass: SpyModalService },
                { provide: UserAgentProvider, useClass: MockUserAgentRef },
                OverlayHostService,
                SizeTrackerService,
            ],
            schemas: [NO_ERRORS_SCHEMA],
            teardown: { destroyAfterEach: false },
        });
    });

    it('binds its label text to the label input property',
        componentTest(() => TestComponent, '<gtx-date-time-picker label="test"></gtx-date-time-picker>',
            (fixture) => {
                fixture.detectChanges();
                const label: HTMLLabelElement = fixture.nativeElement.querySelector('label');

                expect(label.innerText.trim()).toBe('test');
            },
        ),
    );

    it('shows its modal when clicked',
        componentTest(() => TestComponent, (fixture) => {
            openDatepickerModal(fixture);
            modalService = TestBed.inject(ModalService) as any;
            expect(modalService.lastModal).toBeDefined();
        }),
    );

    xit('passes displayTime=true to the DateTimePickerControls',
        componentTest(() => TestComponent, `
                <gtx-date-time-picker label="test" displayTime="true"></gtx-date-time-picker>
                <gtx-overlay-host></gtx-overlay-host>`,
        (fixture) => {
            openDatepickerModal(fixture);
            const mockControls: MockDateTimePickerControls = fixture.debugElement
                .query(By.directive(MockDateTimePickerControls)).componentInstance;
            expect(mockControls.displayTime).toBe(true);
        },
        ),
    );

    xit('passes displayTime=false to the DateTimePickerControls',
        componentTest(() => TestComponent, `
                <gtx-date-time-picker label="test" displayTime="false"></gtx-date-time-picker>
                <gtx-overlay-host></gtx-overlay-host>`,
        (fixture) => {
            openDatepickerModal(fixture);
            const mockControls: MockDateTimePickerControls = fixture.debugElement
                .query(By.directive(MockDateTimePickerControls)).componentInstance;
            expect(mockControls.displayTime).toBe(false);
        },
        ),
    );

    describe('binding value:', () => {

        it('does not send a timestamp if none is set',
            componentTest(() => TestComponent, (fixture) => {
                openDatepickerModal(fixture);
                modalService = TestBed.inject(ModalService) as any;
                expect(modalService.lastLocals).toBeDefined();
                const timestamp: number = modalService.lastLocals['timestamp'];
                expect(timestamp).toEqual(0);
            }),
        );

        it('can be bound to a string value of a timestamp',
            componentTest(() => TestComponent, `
                <gtx-date-time-picker value="${TEST_TIMESTAMP}"></gtx-date-time-picker>
                <gtx-overlay-host></gtx-overlay-host>`,
            (fixture, instance) => {
                fixture.detectChanges();
                expect(instance.pickerInstance.getUnixTimestamp()).toEqual(TEST_TIMESTAMP);
            },
            ),
        );

        it('"timestamp" can be bound to a variable',
            componentTest(() => TestComponent, `
                <gtx-date-time-picker [value]="testModel"></gtx-date-time-picker>
                <gtx-overlay-host></gtx-overlay-host>`,
            (fixture, instance) => {
                // Check if the initial value matches that of testModel.
                fixture.detectChanges();
                expect(instance.pickerInstance.getUnixTimestamp()).toEqual(TEST_TIMESTAMP);

                // Update the testModel value and check the value of the DateTimePicker again.
                const newValue = TEST_TIMESTAMP + 1000;
                fixture.componentInstance.testModel = newValue;
                fixture.detectChanges();
                expect(instance.pickerInstance.getUnixTimestamp()).toEqual(newValue);
            },
            ),
        );

    });

    describe('input display:', () => {
        it('contains an empty input if timestamp is not set',
            componentTest(() => TestComponent, async (fixture) => {
                expect(await getDisplayValue(fixture)).toBe('');
            }),
        );

        it('formats the timestamp in the input as a date when displayTime=false',
            componentTest(() => TestComponent, `
                <gtx-date-time-picker value="1457971763" displayTime="false">
                </gtx-date-time-picker>`,
            async (fixture) => {
                expect(await getDisplayValue(fixture)).toBe('03/14/2016');
            },
            ),
        );

        it('formats the timestamp in the input with a time when displayTime=true',
            componentTest(() => TestComponent, `
                <gtx-date-time-picker value="${TEST_TIMESTAMP}" displayTime="true">
                </gtx-date-time-picker>`,
            async (fixture) => {
                expect(await getDisplayValue(fixture)).toBe('03/14/2016, 5:09:23 PM');
            },
            ),
        );

        it('formats the timestamp in the input when "value" is bound to a variable',
            componentTest(() => TestComponent, `
                <gtx-date-time-picker [value]="testModel" displayTime="true">
                </gtx-date-time-picker>`,
            async (fixture) => {
                expect(await getDisplayValue(fixture)).toBe('03/14/2016, 5:09:23 PM');
            },
            ),
        );

        it('formats the timestamp with a custom format string',
            componentTest(() => TestComponent, `
                <gtx-date-time-picker value="${TEST_TIMESTAMP}" format="YY-MM-ddd">
                </gtx-date-time-picker>`,
            async (fixture) => {
                expect(await getDisplayValue(fixture)).toBe('16-03-Mon');
            },
            ),
        );

        it('does not show a clear button if clearable is false',
            componentTest(() => TestComponent, `
                <gtx-date-time-picker value="${TEST_TIMESTAMP}" clearable="false" format="YY-MM-ddd">
                </gtx-date-time-picker>`,
            (fixture, instance) => {
                fixture.detectChanges();
                const clearButton = getClearButton(fixture);
                expect(clearButton).toBeNull();
            },
            ),
        );

        it('shows a clear button if clearable is true',
            componentTest(() => TestComponent, `
                <gtx-date-time-picker value="${TEST_TIMESTAMP}" clearable="true" format="YY-MM-ddd">
                </gtx-date-time-picker>`,
            (fixture, instance) => {
                fixture.detectChanges();
                const clearButton = getClearButton(fixture);
                expect(clearButton).toBeTruthy();
            },
            ),
        );

        it('clears its value when the clear button is clicked',
            componentTest(() => TestComponent, `
                <gtx-date-time-picker
                    clearable
                    format="YY-MM-ddd"
                    [(ngModel)]="testModel"
                    (valueChange)="onChange($event)"
                ></gtx-date-time-picker>`,
            async (fixture, instance) => {
                fixture.detectChanges();
                tick();

                const clearButton = getClearButton(fixture);
                clearButton.triggerEventHandler('click', document.createEvent('Event'));
                fixture.detectChanges();

                expect(instance.testModel).toBeNull();
                expect(instance.onChange).toHaveBeenCalledWith(null);

                const displayValue = await getDisplayValue(fixture);
                expect(displayValue).toBe('');
            },
            ),
        );

        it('emits "clear" when the clear button is clicked',
            componentTest(() => TestComponent, `
                <gtx-date-time-picker clearable (clear)="onClear($event)" value="${TEST_TIMESTAMP}">
                </gtx-date-time-picker>`,
            (fixture, testComponent) => {
                fixture.detectChanges();
                tick();

                expect(testComponent.onClear).not.toHaveBeenCalled();

                const clearButton = getClearButton(fixture);
                clearButton.triggerEventHandler('click', document.createEvent('Event'));
                fixture.detectChanges();

                expect(testComponent.onClear).toHaveBeenCalled();
            },
            ),
        );

        it('does not clear its value when clicking the clear button if the date picker is disabled',
            componentTest(() => TestComponent, `
                <gtx-date-time-picker
                    clearable
                    disabled
                    format="YY-MM-ddd"
                    [(ngModel)]="testModel"
                    (change)="onChange($event)"
                ></gtx-date-time-picker>`,
            async (fixture, instance) => {
                fixture.detectChanges();
                const clearButton = getClearButton(fixture);
                clearButton.triggerEventHandler('click', document.createEvent('Event'));
                tick();
                fixture.detectChanges();
                await fixture.whenRenderingDone();

                expect(instance.testModel).not.toBeNull();
                expect(instance.onChange).not.toHaveBeenCalledWith(null);
                const displayValue = await getDisplayValue(fixture);
                expect(displayValue).not.toBe('');
            },
            ),
        );

    });

    xdescribe('confirm():', () => {

        const FIVE_DAYS = 60 * 60 * 24 * 5;

        const confirmTest = (testFn: (fixture: ComponentFixture<TestComponent>) => void): any => componentTest(() => TestComponent, `
                <gtx-date-time-picker
                    value="${TEST_TIMESTAMP}"
                    (change)="onChange($event)"
                ></gtx-date-time-picker>
                <gtx-overlay-host></gtx-overlay-host>`,
        (fixture, instance) => {
            instance.onChange = jasmine.createSpy('onChange');
            const modal = openDatepickerModal(fixture);
            const mockControls: MockDateTimePickerControls = fixture.debugElement
                .query(By.directive(MockDateTimePickerControls)).componentInstance;

            mockControls.valueChange.emit(TEST_TIMESTAMP - FIVE_DAYS);
            modal.instance.okayClicked();

            tick();
            fixture.detectChanges();

            testFn(fixture);
        },
        );

        it('changes the displayed date when a new date is selected',
            confirmTest((fixture) => {
                const nativeInput: HTMLInputElement = fixture.nativeElement.querySelector('input');
                expect(nativeInput.value.trim()).toEqual('03/09/2016, 5:09:23 PM');
            }),
        );

        it('fires the "change" event when a new date is selected',
            confirmTest((fixture) => {
                // 5 days earlier than the start timestamp
                const expected = TEST_TIMESTAMP - FIVE_DAYS;
                expect(fixture.componentRef.instance.onChange).toHaveBeenCalledWith(expected);
            }),
        );
    });

    describe('ValueAccessor:', () => {

        it('binds the timestamp to a variable with ngModel (inbound)',
            componentTest(() => TestComponent, `
                <gtx-date-time-picker [(ngModel)]="testModel">
                </gtx-date-time-picker>`,
            (fixture, instance) => {
                fixture.detectChanges();
                tick();
                expect(instance.pickerInstance.getUnixTimestamp()).toBe(TEST_TIMESTAMP);

                instance.testModel -= 10;
                fixture.detectChanges();
                tick();
                fixture.detectChanges();

                expect(instance.pickerInstance.getUnixTimestamp()).toBe(TEST_TIMESTAMP - 10);
            },
            ),
        );

        xit('binds the timestamp to a variable with ngModel (outbound)',
            componentTest(() => TestComponent, `
                <gtx-date-time-picker [(ngModel)]="testModel">
                </gtx-date-time-picker>
                <gtx-overlay-host></gtx-overlay-host>`,
            (fixture, instance) => {
                fixture.detectChanges();
                tick();
                const modal = openDatepickerModal(fixture);
                modalService = TestBed.inject(ModalService) as any;
                expect(modalService.lastLocals['timestamp']).toBe(TEST_TIMESTAMP, 'local not set');

                const mockControls: MockDateTimePickerControls = fixture.debugElement
                    .query(By.directive(MockDateTimePickerControls)).componentInstance;

                mockControls.valueChange.emit(TEST_TIMESTAMP + 1);

                // does not update the model value yet, until we click okay
                expect(instance.testModel).toBe(TEST_TIMESTAMP);

                modal.instance.okayClicked();
                tick();
                fixture.detectChanges();

                expect(instance.testModel).toBe(TEST_TIMESTAMP + 1, 'second');
            },
            ),
        );

        it('can be disabled via the form control',
            componentTest(() => TestComponent, `
                <form [formGroup]="testForm">
                    <gtx-date-time-picker formControlName="test"></gtx-date-time-picker>
                </form>`,
            async (fixture, instance) => {
                fixture.detectChanges();

                const input: HTMLDivElement = fixture.nativeElement.querySelector('.element-container');
                expect(input.classList.contains('disabled')).toBe(false);

                instance.testForm.get('test')?.disable();
                fixture.detectChanges();
                await fixture.whenRenderingDone();

                expect(input.classList.contains('disabled')).toBe(true);
            },
            ),
        );

        it('takes precedence over a bound "timestamp" input property',
            componentTest(() => TestComponent, `
                <gtx-date-time-picker [value]="testTimestamp" [(ngModel)]="testModel"></gtx-date-time-picker>
                <gtx-overlay-host></gtx-overlay-host>`,
            (fixture, instance) => {
                // Check if the initial value matches that of testModel.
                fixture.componentInstance.testTimestamp = TEST_TIMESTAMP + 1;
                fixture.detectChanges();
                tick();
                expect(instance.pickerInstance.getUnixTimestamp()).toEqual(TEST_TIMESTAMP);

                // Update both testModel and testTimestamp and check if testModel takes precendence.
                const newTestModel = TEST_TIMESTAMP + 1000;
                const newTestTimestamp = newTestModel + 1000;
                fixture.componentInstance.testModel = newTestModel;
                fixture.componentInstance.testTimestamp = newTestTimestamp;
                fixture.detectChanges();
                tick();
                expect(instance.pickerInstance.getUnixTimestamp()).toEqual(newTestModel);
            },
            ),
        );

    });

    describe('l10n/i18n support:', () => {

        let formatProvider: TestFormatProvider;
        beforeEach(() => {
            formatProviderToUse = formatProvider = new TestFormatProvider();
        });

        it('uses a custom format provider to display the date in the input field',
            componentTest(() => TestComponent, `
                <gtx-date-time-picker [(ngModel)]="testModel">
                </gtx-date-time-picker>`,
            async (fixture, instance) => {
                const format = formatProvider.format = jasmine.createSpy('format').and.returnValue('formatted date');
                fixture.detectChanges();
                tick();
                fixture.detectChanges();
                await fixture.whenRenderingDone();

                const displayValue = await getDisplayValue(fixture);

                expect(format).toHaveBeenCalledTimes(1);
                expect(displayValue).toBe('formatted date');
                expect(format).toHaveBeenCalledWith(jasmine.anything(), true, true);
                expect(format.calls.mostRecent().args[0]).toBeDefined();
                expect(format.calls.mostRecent().args[0].unix()).toEqual(instance.testModel);

                instance.testModel -= 10;
                // fixture.autoDetectChanges(true);
                // instance.changeDetector.markForCheck();
                fixture.detectChanges();
                tick();
                fixture.detectChanges();

                expect(format).toHaveBeenCalledTimes(2);
                expect(format.calls.mostRecent().args[0].unix()).toEqual(instance.testModel);
            },
            ),
        );

        it('updates the text in the input field when the format provider signals a change',
            componentTest(() => TestComponent, `
                <gtx-date-time-picker value="${TEST_TIMESTAMP}">
                </gtx-date-time-picker>`,
            async (fixture, instance) => {
                formatProvider.format = () => 'date in first format';
                const sub = new Subject<void>();
                formatProvider.changed$ = sub.asObservable();

                fixture.detectChanges();
                tick();
                await fixture.whenRenderingDone();

                expect(await getDisplayValue(fixture)).toBe('date in first format');

                formatProvider.format = () => 'date in second format';
                sub.next();
                tick();
                fixture.detectChanges();
                await fixture.whenRenderingDone();

                expect(await getDisplayValue(fixture)).toBe('date in second format');
            },
            ),
        );

    });

    describe('with OnPush components', () => {

        it('updates the text when a date was picked',
            componentTest(() => OnPushTestComponent, async (fixture, instance) => {
                fixture.autoDetectChanges(true);
                tick();

                let pretendDatepickerModalWasClosed!: (timestamp: number) => void;
                modalService = TestBed.inject(ModalService) as any;
                modalService.fromComponent = () => Promise.resolve<any>({
                    open: () => new Promise<number>((resolve) => {
                        pretendDatepickerModalWasClosed = resolve;
                    }),
                });

                getBoxElement(fixture).click();
                tick();

                expect(await getDisplayValue(fixture)).toBe('');
                pretendDatepickerModalWasClosed(1234567890123);
                tick();
                expect(await getDisplayValue(fixture)).not.toBe('');
            }),
        );

    });

});

function getBoxElement(fixture: ComponentFixture<any>): HTMLDivElement {
    return fixture.nativeElement.querySelector('.element-container .box-content');
}

function getDisplaySpan(fixture: ComponentFixture<any>): DebugElement | null {
    return fixture.debugElement.query(By.css('.display-value'));
}

function getDisplaySpanElement(fixture: ComponentFixture<any>): HTMLSpanElement | null {
    const dbg = getDisplaySpan(fixture);
    return dbg == null ? null : dbg.nativeElement as HTMLSpanElement;
}

function getClearButton(fixture: ComponentFixture<any>): DebugElement | null {
    return fixture.debugElement.query(By.css('.addon-button[data-action="clear"]'));
}

function getClearButtonElement(fixture: ComponentFixture<any>): HTMLButtonElement | null {
    const dbg = getClearButton(fixture);
    return dbg == null ? null : dbg.nativeElement as HTMLButtonElement;
}

async function getDisplayValue(fixture: ComponentFixture<any>): Promise<string> {
    fixture.detectChanges();
    await fixture.whenRenderingDone();
    const el = getDisplaySpanElement(fixture);
    return el.textContent.trim();
}

function openDatepickerModal(fixture: ComponentFixture<TestComponent>): { instance: DateTimePickerModal; query: (selector: string) => HTMLElement } {

    fixture.detectChanges();
    const nativeInput: HTMLDivElement = getBoxElement(fixture);
    nativeInput.click();
    tick();
    fixture.detectChanges();

    modalService = TestBed.inject(ModalService) as any;
    const instance = modalService.lastModal.instance as DateTimePickerModal;
    const query = (selector: string): HTMLElement => modalService.lastModal.element.querySelector(selector);
    return { instance, query };
}

@Component({
    template: `
        <gtx-date-time-picker></gtx-date-time-picker>
        <gtx-overlay-host></gtx-overlay-host>`,
    standalone: false,
})
class TestComponent {
    testModel: number = TEST_TIMESTAMP;
    testTimestamp: number = TEST_TIMESTAMP;
    testForm: UntypedFormGroup = new UntypedFormGroup({
        test: new UntypedFormControl(TEST_TIMESTAMP),
    });

    @ViewChild(DateTimePickerComponent, { static: true })
    pickerInstance: DateTimePickerComponent;

    onChange = jasmine.createSpy('onChange');
    onClear = jasmine.createSpy('onClear');
}

@Component({
    template: '<gtx-date-time-picker></gtx-date-time-picker>',
    standalone: false,
    changeDetection: ChangeDetectionStrategy.OnPush,
})
class OnPushTestComponent { }

@Component({
    selector: 'gtx-date-time-picker-controls',
    standalone: false,
    template: '',
})
class MockDateTimePickerControls implements Partial<DateTimePickerControlsComponent> {
    @Input() value: number;
    @Input() formatProvider: DateTimePickerFormatProvider = new DateTimePickerFormatProvider();
    @Input() min: Date;
    @Input() max: Date;
    @Input() selectYear: boolean;
    @Input() disabled: boolean;
    @Input() displayTime: boolean;
    @Input() displaySeconds: boolean;
    @Input() compact: boolean;
    @Output() valueChange = new EventEmitter<number>();
}

class MockUserAgentRef {
    isIE11 = false;
}

@Injectable()
class TestFormatProvider extends DateTimePickerFormatProvider { }

@Injectable()
class SpyModalService extends ModalService {
    lastOptions: IModalOptions;
    lastLocals: { [key: string]: any };
    lastModal: IModalInstance<any>;

    fromComponent(
        component: Type<any>,
        options?: IModalOptions,
        locals?: { [key: string]: any },
    ): Promise<IModalInstance<any>> {
        this.lastOptions = options!;
        this.lastLocals = locals!;

        return super.fromComponent(component, options, locals)
            .then((modal) => {
                this.lastModal = modal;
                return modal;
            });
    }
}

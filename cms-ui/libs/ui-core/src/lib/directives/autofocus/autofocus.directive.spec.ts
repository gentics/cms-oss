import { Component } from '@angular/core';
import { TestBed, tick } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { BrowserDynamicTestingModule } from '@angular/platform-browser-dynamic/testing';
import { AutosizeModule } from 'ngx-autosize';
import { IModalDialog } from '../../common/modal';
import { ButtonComponent } from '../../components/button/button.component';
import { CheckboxComponent } from '../../components/checkbox/checkbox.component';
import { DateTimePickerComponent } from '../../components/date-time-picker/date-time-picker.component';
import { DropdownContentWrapperComponent } from '../../components/dropdown-content-wrapper/dropdown-content-wrapper.component';
import { DropdownContentComponent } from '../../components/dropdown-content/dropdown-content.component';
import { DropdownListComponent } from '../../components/dropdown-list/dropdown-list.component';
import { DynamicModal } from '../../components/dynamic-modal/dynamic-modal.component';
import { FilePickerComponent } from '../../components/file-picker/file-picker.component';
import { InputComponent } from '../../components/input/input.component';
import { OverlayHostComponent } from '../../components/overlay-host/overlay-host.component';
import { RadioButtonComponent } from '../../components/radio-button/radio-button.component';
import { ScrollMaskComponent } from '../../components/scroll-mask/scroll-mask.component';
import { SearchBarComponent } from '../../components/search-bar/search-bar.component';
import { SelectComponent } from '../../components/select/select.component';
import { TextareaComponent } from '../../components/textarea/textarea.component';
import { ModalService } from '../../providers/modal/modal.service';
import { OverlayHostService } from '../../providers/overlay-host/overlay-host.service';
import { UserAgentProvider } from '../../providers/user-agent/user-agent-ref';
import { componentTest } from '../../testing';
import { DropdownTriggerDirective } from '../dropdown-trigger/dropdown-trigger.directive';
import { IconDirective } from '../icon/icon.directive';
import { AutofocusDirective } from './autofocus.directive';

describe('Autofocus Directive', () => {

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [FormsModule, ReactiveFormsModule, AutosizeModule],
            declarations: [
                AutofocusDirective,
                ButtonComponent,
                CheckboxComponent,
                DateTimePickerComponent,
                DropdownListComponent,
                DropdownTriggerDirective,
                DropdownContentComponent,
                DropdownContentWrapperComponent,
                DynamicModal,
                FilePickerComponent,
                IconDirective,
                InputComponent,
                OverlayHostComponent,
                RadioButtonComponent,
                ScrollMaskComponent,
                SearchBarComponent,
                SelectComponent,
                TestComponent,
                TestModal,
                TextareaComponent,
            ],
            providers: [
                ModalService,
                OverlayHostService,
                { provide: UserAgentProvider, useClass: MockUserAgentRef },
            ],
            teardown: { destroyAfterEach: false },
        });

        TestBed.overrideModule(BrowserDynamicTestingModule, {
            set: {
                declarations: [DropdownContentWrapperComponent, DynamicModal, TestModal, ScrollMaskComponent],
            },
        });
    });

    it('works for Button',
        componentTest(() => TestComponent, `
            <gtx-button label="first"></gtx-button>
            <gtx-button label="second" autofocus></gtx-button>`,
        fixture => {
            const [first, second] = fixture.nativeElement.querySelectorAll('button') as HTMLButtonElement[];
            fixture.detectChanges();
            tick(50);

            expect(isFocused(first)).toBe(false);
            expect(isFocused(second)).toBe(true);
            expect(second.autofocus).toBe(true, 'autofocus attribute not set');
        },
        ),
    );

    it('works for CheckBoxe',
        componentTest(() => TestComponent, `
            <gtx-checkbox label="first"></gtx-checkbox>
            <gtx-checkbox label="second" autofocus></gtx-checkbox>`,
        fixture => {
            const [first, second] = fixture.nativeElement.querySelectorAll('input') as HTMLInputElement[];
            fixture.detectChanges();
            tick(50);

            expect(isFocused(first)).toBe(false);
            expect(isFocused(second)).toBe(true);
            expect(second.autofocus).toBe(true, 'autofocus attribute not set');
        },
        ),
    );

    it('works for DateTimePicker',
        componentTest(() => TestComponent, `
            <gtx-date-time-picker label="first"></gtx-date-time-picker>
            <gtx-date-time-picker label="second" autofocus></gtx-date-time-picker>`,
        fixture => {
            const [first, second] = fixture.nativeElement.querySelectorAll('input') as HTMLInputElement[];
            fixture.detectChanges();
            tick(50);

            expect(isFocused(first)).toBe(false);
            expect(isFocused(second)).toBe(true);
            expect(second.autofocus).toBe(true, 'autofocus attribute not set');
        },
        ),
    );

    it('works for FilePicker',
        componentTest(() => TestComponent, `
            <gtx-file-picker label="first"></gtx-file-picker>
            <gtx-file-picker label="second" autofocus></gtx-file-picker>`,
        fixture => {
            const [first, second] = fixture.nativeElement.querySelectorAll('input') as HTMLInputElement[];
            fixture.detectChanges();
            tick(50);

            expect(isFocused(first)).toBe(false);
            expect(isFocused(second)).toBe(true);
            expect(second.autofocus).toBe(true, 'autofocus attribute not set');
        },
        ),
    );

    it('works for InputField',
        componentTest(() => TestComponent, `
            <gtx-input label="first"></gtx-input>
            <gtx-input label="second" autofocus></gtx-input>`,
        fixture => {
            const [first, second] = fixture.nativeElement.querySelectorAll('input') as HTMLInputElement[];
            fixture.detectChanges();
            tick(50);

            expect(isFocused(first)).toBe(false);
            expect(isFocused(second)).toBe(true);
            expect(second.autofocus).toBe(true, 'autofocus attribute not set');
        },
        ),
    );

    it('works for RadioButton',
        componentTest(() => TestComponent, `
            <gtx-radio-button label="first"></gtx-radio-button>
            <gtx-radio-button label="second" autofocus></gtx-radio-button>`,
        fixture => {
            const [first, second] = fixture.nativeElement.querySelectorAll('input') as HTMLInputElement[];
            fixture.detectChanges();
            tick(50);

            expect(isFocused(first)).toBe(false);
            expect(isFocused(second)).toBe(true);
            expect(second.autofocus).toBe(true, 'autofocus attribute not set');
        },
        ),
    );

    it('works for SearchBar',
        componentTest(() => TestComponent, `
            <gtx-search-bar label="first"></gtx-search-bar>
            <gtx-search-bar label="second" autofocus></gtx-search-bar>`,
        fixture => {
            const [first, second] = fixture.nativeElement.querySelectorAll('input') as HTMLInputElement[];
            fixture.detectChanges();
            tick(50);

            expect(isFocused(first)).toBe(false);
            expect(isFocused(second)).toBe(true);
            expect(second.autofocus).toBe(true, 'autofocus attribute not set');
        },
        ),
    );

    it('can be bound to a property',
        componentTest(() => TestComponent, `
            <gtx-input label="first" [autofocus]="!boolProp"></gtx-input>
            <gtx-input label="second" [autofocus]="boolProp"></gtx-input>`,
        (fixture, testComponent) => {
            testComponent.boolProp = true;
            const [first, second] = fixture.nativeElement.querySelectorAll('input') as HTMLInputElement[];
            fixture.detectChanges();
            tick(50);

            expect(isFocused(first)).toBe(false);
            expect(isFocused(second)).toBe(true);
            expect(second.autofocus).toBe(true, 'autofocus attribute not set');
        },
        ),
    );

    it('works for Select',
        componentTest(() => TestComponent, `
            <gtx-select label="first"></gtx-select>
            <gtx-select label="second" autofocus></gtx-select>`,
        fixture => {
            const [first, second] = fixture.nativeElement.querySelectorAll('gtx-select .select-input') as HTMLElement[];
            fixture.detectChanges();
            tick(50);

            expect(isFocused(first)).toBe(false);
            expect(isFocused(second)).toBe(true);
        },
        ),
    );

    it('works for Textarea',
        componentTest(() => TestComponent, `
            <gtx-textarea label="first"></gtx-textarea>
            <gtx-textarea label="second" autofocus></gtx-textarea>`,
        fixture => {
            const [first, second] = fixture.nativeElement.querySelectorAll('textarea') as HTMLTextAreaElement[];
            fixture.detectChanges();
            tick(50);

            expect(isFocused(first)).toBe(false);
            expect(isFocused(second)).toBe(true);
            expect(second.autofocus).toBe(true, 'autofocus attribute not set');
        },
        ),
    );

    it('works with components in ngIf',
        componentTest(() => TestComponent, `
            <gtx-input label="first"></gtx-input>
            <div *ngIf="boolProp">
                <gtx-input label="second" autofocus></gtx-input>
            </div>`,
        (fixture, testComponent) => {
            testComponent.boolProp = false;
            fixture.detectChanges();
            tick(50);

            expect(fixture.nativeElement.querySelectorAll('input').length).toBe(1);

            tick(1000);
            testComponent.boolProp = true;
            fixture.detectChanges();
            tick(50);

            const [first, second] = fixture.nativeElement.querySelectorAll('input') as HTMLInputElement[];
            expect(isFocused(first)).toBe(false);
            expect(isFocused(second)).toBe(true);
            expect(second.autofocus).toBe(true, 'autofocus attribute not set');
        },
        ),
    );

    xit('works with components inside a modal',
        componentTest(() => TestComponent, `
            <gtx-overlay-host></gtx-overlay-host>`,
        (fixture, testComponent) => {
            let opened = false;
            testComponent.modalService.fromComponent(TestModal)
                .then(modal => {
                    modal.open();
                    opened = true;

                    fixture.detectChanges();
                    tick(50);
                    const [first, second] = Array.from(modal.element.querySelectorAll('input'));

                    expect(isFocused(first)).toBe(false);
                    expect(isFocused(second)).toBe(true);
                    expect(second.autofocus).toBe(true, 'autofocus attribute not set');

                    modal.instance.closeFn(undefined);
                });

            tick(50);
            expect(opened).toBe(true, 'was not opened');
        },
        ),
    );

    it('automatically scrolls the focused element into view',
        componentTest(() => TestComponent, `
            <div style="height: 9999px"></div>
            <gtx-input autofocus></gtx-input>`,
        fixture => {
            const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;

            expect(isInView(input)).toBe(false);
            fixture.detectChanges();
            tick(50);
            expect(isInView(input)).toBe(true);
        },
        ),
    );

});

const isFocused = (element: Element): boolean => document.activeElement === element;

function isInView(element: Element): boolean {
    const rect = element.getBoundingClientRect();
    const { clientHeight, clientLeft, clientTop, clientWidth } = document.documentElement;
    return rect.top < (clientTop + clientHeight)
        && rect.bottom >= clientTop
        && rect.left < (clientLeft + clientWidth)
        && rect.right >= clientLeft;
}

@Component({
    template: '<gtx-input></gtx-input>',
})
class TestComponent {
    boolProp = false;
    constructor(public modalService: ModalService) { }
}

@Component({
    template: `
        <div class="modal-content">
            <gtx-input label="first"></gtx-input>
            <gtx-input label="second" autofocus></gtx-input>
        </div>`,
})
class TestModal implements IModalDialog {
    closeFn: any;
    cancelFn: any;
    registerCloseFn(close: any): void { this.closeFn = close; }
    registerCancelFn(cancel: any): void { this.cancelFn = cancel; }
}

class MockUserAgentRef {
    isIE11 = false;
}

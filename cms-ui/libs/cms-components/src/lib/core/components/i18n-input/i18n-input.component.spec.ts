import { CommonModule } from '@angular/common';
import { Component, NO_ERRORS_SCHEMA, OnDestroy, OnInit, Pipe, PipeTransform } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick, waitForAsync } from '@angular/core/testing';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { Subscription } from 'rxjs';
import { I18nInputComponent } from './i18n-input.component';

const DEFAULT_LANGUAGE = 'de';
const SECOND_LANGUAGE = 'en';
const AVAILABLE_LANGUAGES = [DEFAULT_LANGUAGE, SECOND_LANGUAGE];

describe('I18nInputComponent', () => {
    let component: TestComponent;
    let inputElement: HTMLInputElement;
    let fixture: ComponentFixture<TestComponent>;

    beforeEach(
        waitForAsync(() => {
            TestBed.configureTestingModule({
                declarations: [
                    I18nInputComponent,
                    TestComponent,
                    MockI18nPipe,
                ],
                imports: [
                    CommonModule,
                    FormsModule,
                    ReactiveFormsModule,
                    NoopAnimationsModule,
                    GenticsUICoreModule,
                ],
                providers: [],
                schemas: [NO_ERRORS_SCHEMA],
            }).compileComponents();
        }),
    );

    beforeEach(async () => {
        fixture = TestBed.createComponent(TestComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
        await fixture.whenStable();
        await fixture.whenRenderingDone();
        inputElement = (fixture.nativeElement as HTMLElement).querySelector('input');
    });

    it('should trigger a change when the value has been changed', fakeAsync(() => {
        // Should start without a value
        expect(component.currentValue).not.toBeDefined();

        const changeSpy = jasmine.createSpy('valueChangeHandler', component.valueChangeHandler).and.callThrough();
        component.valueChangeHandler = changeSpy;

        // --------------------

        const firstValue = 'Hello World';
        inputElement.value = firstValue;
        triggerEvent(inputElement, 'input');

        tick();
        fixture.detectChanges();

        expect(changeSpy).toHaveBeenCalledTimes(1);
        expect(component.currentValue).toEqual({ [DEFAULT_LANGUAGE]: firstValue });

        // --------------------

        component.activeLanguage = SECOND_LANGUAGE;
        tick();
        fixture.detectChanges();
        changeSpy.calls.reset();

        // --------------------

        const secondValue = 'Something else!';
        inputElement.value = secondValue;
        triggerEvent(inputElement, 'input');

        tick();
        fixture.detectChanges();

        expect(changeSpy).toHaveBeenCalledTimes(1);
        expect(component.currentValue).toEqual({
            [DEFAULT_LANGUAGE]: firstValue,
            [SECOND_LANGUAGE]: secondValue,
        });
    }));

    it('should trigger a change with a new object to prevent reference modification', fakeAsync(() => {
        const secondValue = 'im the other language value!';
        const firstValue = 'foo-bar 123';
        const initialValue = {
            [DEFAULT_LANGUAGE]: firstValue,
            [SECOND_LANGUAGE]: secondValue,
        };

        component.control.setValue(initialValue);
        tick();
        fixture.detectChanges();

        const changeSpy = jasmine.createSpy('valueChangeHandler', component.valueChangeHandler).and.callThrough();
        component.valueChangeHandler = changeSpy;

        // --------------------

        const newValue = 'Hello World';
        inputElement.value = newValue;
        triggerEvent(inputElement, 'input');

        tick();
        fixture.detectChanges();

        expect(changeSpy).toHaveBeenCalledTimes(1);
        expect(component.currentValue).toEqual({
            [DEFAULT_LANGUAGE]: newValue,
            [SECOND_LANGUAGE]: secondValue,
        });
        expect(component.currentValue).not.toEqual(initialValue);
        expect(initialValue).toEqual({
            [DEFAULT_LANGUAGE]: firstValue,
            [SECOND_LANGUAGE]: secondValue,
        });
    }));
});

@Component({
    template: `
        <gtx-i18n-input
            [formControl]="control"
            [language]="activeLanguage"
            [availableLanguages]="availableLanguages"
        ></gtx-i18n-input>`,
})
class TestComponent implements OnInit, OnDestroy {

    public control: FormControl;
    public activeLanguage = DEFAULT_LANGUAGE;
    public availableLanguages = AVAILABLE_LANGUAGES;

    public currentValue: Record<string, string>;

    private subscription: Subscription;

    ngOnInit(): void {
        this.control = new FormControl();
        this.subscription = this.control.valueChanges.subscribe(value => {
            this.valueChangeHandler(value);
        });
    }

    ngOnDestroy(): void {
        if (this.subscription) {
            this.subscription.unsubscribe();
        }
    }

    valueChangeHandler(value: Record<string, string>): void {
        this.currentValue = value;
    }
}

@Pipe({ name: 'i18n' })
class MockI18nPipe implements PipeTransform {
    transform(value: string): string {
        return value;
    }
}

/**
 * Create an dispatch an 'input' event on the <input> element
 */
function triggerEvent(el: HTMLInputElement, eventName: string): void {
    const event: Event = document.createEvent('Event');
    event.initEvent(eventName, true, true);
    el.dispatchEvent(event);
}

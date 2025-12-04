import { CommonModule } from '@angular/common';
import { Component, NO_ERRORS_SCHEMA, OnDestroy, OnInit } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick, waitForAsync } from '@angular/core/testing';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { GenticsUICoreModule, SelectComponent } from '@gentics/ui-core';
import { Subscription } from 'rxjs';
import { MockI18nPipe } from '../../../../testing';
import { I18nSelectComponent } from './i18n-select.component';

const DEFAULT_LANGUAGE = 'de';
const SECOND_LANGUAGE = 'en';
const AVAILABLE_LANGUAGES = [DEFAULT_LANGUAGE, SECOND_LANGUAGE];

describe('I18nSelectComponent', () => {
    let component: TestComponent;
    let selectComponent: SelectComponent;
    let fixture: ComponentFixture<TestComponent>;

    beforeEach(
        waitForAsync(() => {
            TestBed.configureTestingModule({
                declarations: [
                    I18nSelectComponent,
                    TestComponent,
                    MockI18nPipe,
                ],
                imports: [
                    CommonModule,
                    FormsModule,
                    ReactiveFormsModule,
                    NoopAnimationsModule,
                    GenticsUICoreModule.forRoot(),
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
        await fixture.whenRenderingDone();
        await fixture.whenRenderingDone();
        selectComponent = fixture.debugElement.children[0].children[0].componentInstance
    });

    it('should trigger a change when the value has been changed', fakeAsync(() => {
        const options = ['Hello World', 'Example 123', 'Foo Bar'];
        component.options = options;
        fixture.detectChanges();
        tick();

        const changeSpy = jasmine.createSpy('valueChangeHandler', component.valueChangeHandler).and.callThrough();
        component.valueChangeHandler = changeSpy;

        // --------------------

        selectComponent.selectItem(0, 0);
        tick();
        fixture.detectChanges();

        expect(changeSpy).toHaveBeenCalledTimes(1);
        expect(component.currentValue).toEqual({ [DEFAULT_LANGUAGE]: options[0] });

        // --------------------

        component.activeLanguage = SECOND_LANGUAGE;
        tick();
        fixture.detectChanges();
        changeSpy.calls.reset();

        // --------------------

        selectComponent.selectItem(0, 1);

        tick();
        fixture.detectChanges();

        expect(changeSpy).toHaveBeenCalledTimes(1);
        expect(component.currentValue).toEqual({
            [DEFAULT_LANGUAGE]: options[0],
            [SECOND_LANGUAGE]: options[1],
        });
    }));

    it('should trigger a change with a new object to prevent reference modification', fakeAsync(() => {
        const options = ['Hello World', 'Example 123', 'Foo Bar'];
        const initialValue = {
            [DEFAULT_LANGUAGE]: options[0],
            [SECOND_LANGUAGE]: options[1],
        };

        component.options = options;
        component.control.setValue(initialValue);
        // Needs two ticks, one for the observable, and one for the setTimeout in the subscription handler
        tick();
        fixture.detectChanges();
        tick();
        fixture.detectChanges();

        const changeSpy = jasmine.createSpy('valueChangeHandler', component.valueChangeHandler).and.callThrough();
        component.valueChangeHandler = changeSpy;

        // --------------------

        selectComponent.selectItem(0, 2);
        tick();
        fixture.detectChanges();

        expect(changeSpy).toHaveBeenCalledTimes(1);
        expect(component.currentValue).toEqual({
            [DEFAULT_LANGUAGE]: options[2],
            [SECOND_LANGUAGE]: options[1],
        });
        expect(component.currentValue).not.toEqual(initialValue);
        expect(initialValue).toEqual({
            [DEFAULT_LANGUAGE]: options[0],
            [SECOND_LANGUAGE]: options[1],
        });
    }));
});

@Component({
    template: `
        <gtx-i18n-select
            [formControl]="control"
            [language]="activeLanguage"
            [availableLanguages]="availableLanguages"
        >
            <gtx-option *ngFor="let opt of options" [value]="opt">{{ opt }}</gtx-option>
        </gtx-i18n-select>
        <gtx-overlay-host></gtx-overlay-host>
    `,
    standalone: false,
})
class TestComponent implements OnInit, OnDestroy {

    public control: FormControl;
    public activeLanguage = DEFAULT_LANGUAGE;
    public availableLanguages = AVAILABLE_LANGUAGES;

    public currentValue: Record<string, string>;
    public options: string[] = [];

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

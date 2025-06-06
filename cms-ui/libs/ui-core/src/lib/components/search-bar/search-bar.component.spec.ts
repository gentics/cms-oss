import { Component, NO_ERRORS_SCHEMA, ViewChild } from '@angular/core';
import { TestBed, tick } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { BehaviorSubject } from 'rxjs';
import { componentTest } from '../../testing';
import { ButtonComponent } from '../button/button.component';
import { InputComponent } from '../input/input.component';
import { SearchBarComponent } from './search-bar.component';

describe('SearchBarComponent', () => {

    beforeEach(() => TestBed.configureTestingModule({
        imports: [
            FormsModule,
            ReactiveFormsModule,
        ],
        declarations: [
            ButtonComponent,
            InputComponent,
            SearchBarComponent,
            TestComponent,
        ],
        teardown: { destroyAfterEach: false },
        schemas: [NO_ERRORS_SCHEMA],
    }));

    it('binds the value of its native input the the "query" property',
        componentTest(() => TestComponent, async fixture => {
            fixture.detectChanges();
            await fixture.whenRenderingDone();

            const input: HTMLInputElement = fixture.nativeElement.querySelector('input');
            expect(input.value).toBe('foo');
        }),
    );

    it('defaults to an empty search string if no query is set',
        componentTest(() => TestComponent, `
            <gtx-search-bar></gtx-search-bar>`,
        async fixture => {
            fixture.detectChanges();
            await fixture.whenRenderingDone();
            const input: HTMLInputElement = fixture.nativeElement.querySelector('input');
            expect(input.value).toBe('');
        },
        ),
    );

    it('only displays its clear button when the search query is not empty',
        componentTest(() => TestComponent, `
            <gtx-search-bar [query]="query"></gtx-search-bar>`,
        fixture => {
            fixture.detectChanges();
            const getButton = (): HTMLButtonElement => fixture.nativeElement.querySelector('.clear-button button');

            expect(getButton()).toBeNull();

            fixture.componentInstance.query = 'foo';
            fixture.detectChanges();

            expect(getButton()).not.toBeNull();
        },
        ),
    );

    it('never displays the clear button when hideClearButton is set',
        componentTest(() => TestComponent, `
            <gtx-search-bar [query]="query" hideClearButton></gtx-search-bar>`,
        fixture => {
            fixture.detectChanges();
            const getButton = (): HTMLButtonElement => fixture.nativeElement.querySelector('.clear-button button');

            expect(getButton()).toBeNull();

            fixture.componentInstance.query = 'foo';
            fixture.detectChanges();

            expect(getButton()).toBeNull();
        },
        ),
    );

    it('emits "search" when its search button is clicked',
        componentTest(() => TestComponent, (fixture, testInstance) => {
            fixture.detectChanges();
            const button: HTMLButtonElement = fixture.nativeElement.querySelector('.submit-button button');

            testInstance.onSearch = jasmine.createSpy('onSearch');
            button.click();
            tick();

            expect(testInstance.onSearch).toHaveBeenCalledWith('foo');
        }),
    );

    it('emits "clear" when its clear button is clicked',
        componentTest(() => TestComponent, (fixture, testInstance) => {
            fixture.detectChanges();
            const button: HTMLButtonElement = fixture.nativeElement.querySelector('.clear-button button');

            testInstance.onClear = jasmine.createSpy('onClear');
            button.click();
            tick();

            expect(testInstance.onClear).toHaveBeenCalledWith(true);
        }),
    );

    it('emit "search" when the enter key pressed is pressed in its input element',
        componentTest(() => TestComponent, (fixture, testInstance) => {
            fixture.detectChanges();
            const searchBar: SearchBarComponent = testInstance.searchBar;

            testInstance.onSearch = jasmine.createSpy('onSearch');

            // Cross-browser way to create a "keydown" event
            const enterKeyEvent = document.createEvent('Event');
            enterKeyEvent.initEvent('keydown', true, true);
            const props: any = {
                altKey: false,
                code: 'Enter',
                ctrlKey: false,
                key: 'Enter',
                keyCode: 13,
                keyIdentifier: 'U+000D',
                repeat: false,
                shiftKey: false,
                which: 13,
            };
            Object.keys(props).forEach(key => {
                Object.defineProperty(enterKeyEvent, key, {
                    value: props[key],
                    configurable: true,
                    enumerable: true,
                });
            });

            const nativeInput: HTMLInputElement = fixture.nativeElement.querySelector('input');
            nativeInput.dispatchEvent(enterKeyEvent);
            tick();

            expect(testInstance.onSearch).toHaveBeenCalledWith('foo');
        }),
    );

    it('emits "change" when the input value is changed with an "input" event',
        componentTest(() => TestComponent, (fixture, testInstance) => {
            fixture.detectChanges();
            const input: HTMLInputElement = fixture.nativeElement.querySelector('input');
            testInstance.onChange = jasmine.createSpy('onChange');

            input.value = 'test 123';
            const event = document.createEvent('Event');
            event.initEvent('input', true, true);
            input.dispatchEvent(event);
            tick();

            expect(testInstance.onChange).toHaveBeenCalledWith('test 123');
        }),
    );

    it('does not emit the "change" event when its input is blurred',
        componentTest(() => TestComponent, (fixture, testInstance) => {
            fixture.detectChanges();
            const input: HTMLInputElement = fixture.nativeElement.querySelector('input');
            testInstance.onChange = jasmine.createSpy('onChange');

            const event = document.createEvent('Event');
            event.initEvent('blur', true, true);
            input.dispatchEvent(event);
            tick();

            expect(testInstance.onChange).not.toHaveBeenCalled();
        }),
    );

    it('works when binding to a Subject',
        componentTest(() => TestComponent, `
            <gtx-search-bar
                [ngModel]="subject | async"
                (ngModelChange)="subject.next($event)">
            </gtx-search-bar>`,
        async (fixture, testInstance) => {
            testInstance.subject.next('A');
            fixture.detectChanges();
            tick();
            fixture.detectChanges();
            await fixture.whenRenderingDone();

            const input: HTMLInputElement = fixture.nativeElement.querySelector('input');

            expect(input.value).toBe('A');

            input.value = 'B';
            const event = document.createEvent('Event');
            event.initEvent('input', true, true);
            input.dispatchEvent(event);

            tick();

            expect(testInstance.subject.value).toBe('B');
        },
        ),
    );

    it('can be cleared when binding to a Subject',
        componentTest(() => TestComponent, `
            <gtx-search-bar
                [ngModel]="subject | async"
                (ngModelChange)="subject.next($event)"
                (clear)="subject.next('')">
            </gtx-search-bar>`,
        (fixture, testInstance) => {
            testInstance.subject.next('A');
            fixture.detectChanges();
            tick();
            fixture.detectChanges();
            const clearButton = Array.from<HTMLButtonElement>(fixture.nativeElement.querySelectorAll('button'))
                .find(button => /close/.test(button.innerText));

            expect(testInstance.subject.value).toBe('A');
            clearButton.click();
            expect(testInstance.subject.value).toBe('');

            tick();
        },
        ),
    );

});

@Component({
    template: `
        <gtx-search-bar
            query="foo"
            (search)="onSearch($event)"
            (change)="onChange($event)"
            (clear)="onClear($event)"
        ></gtx-search-bar>`,
    standalone: false,
})
class TestComponent {
    query = '';
    subject = new BehaviorSubject<string>('initial value of subject');
    @ViewChild(SearchBarComponent, { static: true }) searchBar: SearchBarComponent;
    onSearch(...args: any[]): void {}
    onChange(...args: any[]): void {}
    onClear(...args: any[]): void {}
}

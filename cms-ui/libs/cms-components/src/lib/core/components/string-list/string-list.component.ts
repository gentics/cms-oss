import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import { ControlValueAccessor, UntypedFormArray, UntypedFormControl } from '@angular/forms';
import { generateFormProvider, ISortableEvent } from '@gentics/ui-core';
import { Subscription } from 'rxjs';
import { MultiValueValidityState } from '../../../common';

@Component({
    selector: 'gtx-string-list',
    templateUrl: './string-list.component.html',
    styleUrls: ['./string-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(StringListComponent)],
})
export class StringListComponent implements OnInit, OnChanges, OnDestroy, ControlValueAccessor {

    @Input()
    public label: string;

    @Input()
    public addLabel: string;

    @Input()
    public disabled = false;

    @Input()
    public sortable = false;

    @Input()
    public value: string[] = [];

    @Input()
    public errors: MultiValueValidityState;

    @Output()
    public valueChange = new EventEmitter<string[]>();

    @Output()
    public touch = new EventEmitter<void>();

    @ViewChild('regularList')
    public regularList: ElementRef<HTMLDivElement>;

    @ViewChild('dragList', { read: ElementRef })
    public dragList: ElementRef<HTMLElement>;

    public form: UntypedFormArray = new UntypedFormArray([]);

    private cvaChange: (value: any) => any;
    private cvaTouch: () => any;

    private subscriptions: Subscription[] = [];
    private scrollTimer: number;

    constructor(protected changeDetector: ChangeDetectorRef) { }

    public ngOnInit(): void {
        this.subscriptions.push(this.form.valueChanges.subscribe(() => {
            let hasChanged = this.form.value.length !== this.value.length;
            if (!hasChanged) {
                for (let i = 0; i < this.form.value.length; i++) {
                    if (this.form.value[i] !== this.value[i]) {
                        hasChanged = true;
                        break;
                    }
                }
            }
            if (hasChanged) {
                this.value = this.form.value;
                this.triggerChange();
            }
        }));
    }

    public ngOnChanges(changes: SimpleChanges): void {
        if (changes.value) {
            this.updateValue(this.value);
        }
        if (changes.disabled) {
            this.updateDisabled(this.disabled);
        }
    }

    public ngOnDestroy(): void {
        this.clearScrollTimeout();
        this.subscriptions.forEach(sub => sub.unsubscribe());
    }

    public writeValue(value: any): void {
        this.updateValue(value);
    }

    public registerOnChange(fn: (value: any) => any): void {
        this.cvaChange = fn;
    }

    public registerOnTouched(fn: () => any): void {
        this.cvaTouch = fn;
    }

    public setDisabledState(state: boolean): void {
        this.updateDisabled(state);
    }

    protected updateValue(value: any): void {
        if (value == null) {
            this.value = [];
            return;
        }
        if (!Array.isArray(value)) {
            value = [value];
        }
        this.value = value
            .filter(element => typeof element !== 'object' && typeof element !== 'symbol')
            .map(element => typeof element !== 'string' ? '' + element : element);
        this.rebuildFormFromValue(this.value);
        this.changeDetector.markForCheck();
    }

    protected updateDisabled(value: any): void {
        if (typeof value === 'boolean') {
            this.disabled = value;
        } else if (typeof value === 'string') {
            this.disabled = value.toLowerCase() === 'true';
        } else {
            this.disabled = !!this.disabled;
        }

        if (this.disabled) {
            this.form.disable({ emitEvent: false });
        } else {
            this.form.enable({ emitEvent: false });
        }

        this.changeDetector.markForCheck();
    }

    public triggerChange(): void {
        if (this.disabled) {
            return;
        }

        if (typeof this.cvaChange === 'function') {
            this.cvaChange(this.form.value);
        }
        this.valueChange.emit(this.form.value);
        this.changeDetector.markForCheck();
    }

    public triggerTouch(): void {
        if (this.disabled) {
            return;
        }

        if (typeof this.cvaTouch === 'function') {
            this.cvaTouch();
        }
        this.touch.emit();
    }

    public addItem(): void {
        if (this.disabled) {
            return;
        }

        this.value.push('');
        this.form.push(new UntypedFormControl(''));
        this.clearScrollTimeout();
        this.scrollTimer = window.setTimeout(() => {
            let el: HTMLElement;

            if (this.regularList != null) {
                el = this.regularList.nativeElement
            } else if (this.dragList != null) {
                el = this.dragList.nativeElement;
            }

            // When a scrollbar is visible
            if (el != null && el.clientHeight !== el.scrollHeight) {
                el.scrollTop = el.scrollHeight;
            }
        }, 10);
        this.triggerChange();
        this.triggerTouch();
    }

    public removeItem(index: number): void {
        if (this.disabled) {
            return;
        }

        this.value.splice(index, 1);
        this.form.removeAt(index);
        this.triggerChange();
        this.triggerTouch();
    }

    public cancelEvent(event: Event): void {
        event.stopPropagation();
        event.preventDefault();
    }

    public sortList(event: ISortableEvent): void {
        this.rebuildFormFromValue(event.sort(this.form.controls).map(control => control.value));
        this.triggerChange();
        this.triggerTouch();
    }

    protected clearScrollTimeout(): void {
        if (this.scrollTimer != null) {
            clearTimeout(this.scrollTimer);
        }
    }

    protected rebuildFormFromValue(value: string[]): void {
        if (value.length !== this.form.controls.length) {
            this.form.clear();
            value.forEach(element => {
                this.form.push(new UntypedFormControl(element));
            });
        } else {
            this.form.patchValue(value, { emitEvent: false });
        }
    }
}

import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    Input,
    OnChanges,
    OnDestroy,
    SimpleChanges,
    ViewChild
} from '@angular/core';
import { AutosizeDirective } from 'ngx-autosize';
import { generateFormProvider } from '../../utils';
import { BaseFormElementComponent } from '../base-form-element/base-form-element.component';

function normalizeValue(value: any): string {
    return (value == null ? '' : String(value)).replace(/\r\n?/g, '\n');
}

/**
 * The Textarea wraps the native `<textarea>` form element. Textareas automatically grow to accommodate their content.
 *
 * ```html
 * <gtx-textarea label="Message" [(ngModel)]="message"></gtx-textarea>
 * ```
 */
@Component({
    selector: 'gtx-textarea',
    templateUrl: './textarea.component.html',
    styleUrls: ['./textarea.component.scss'],
    providers: [generateFormProvider(TextareaComponent)],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TextareaComponent extends BaseFormElementComponent<string> implements AfterViewInit, OnChanges, OnDestroy {

    /**
     * Sets the textarea to be auto-focused. Handled by `AutofocusDirective`.
     */
    @Input()
    public autofocus = false;

    /**
     * Sets the readonly state.
     */
    @Input()
    public readonly = false;

    /**
     * Sets the maximum number of characters permitted.
     */
    @Input()
    public maxlength: number | null = null;

    /**
     * A placeholder text to display when the control is empty.
     */
    @Input()
    public placeholder: string;

    /**
     * The name of the control.
     */
    @Input()
    public name: string;

    /**
     * Sets an id for the control.
     */
    @Input()
    public id: string;

    @ViewChild(AutosizeDirective, { static: true })
    private autosizeDir: AutosizeDirective;

    @ViewChild('textarea', { static: true })
    private textAreaEl: ElementRef<HTMLTextAreaElement>;

    private observer: IntersectionObserver;

    constructor(
        changeDetector: ChangeDetectorRef,
    ) {
        super(changeDetector);
        this.booleanInputs.push('autofocus', 'readonly');
    }

    ngAfterViewInit(): void {
        this.observer = new IntersectionObserver((entries) => {
            if (entries.length === 0) {
                return;
            }
            const entry = entries[0];
            if (!entry.isIntersecting) {
                return;
            }

            // Trigger the autosize-directive to adjust the size after a value change
            if (this.autosizeDir) {
                this.autosizeDir.adjust(true);
            }
        }, {
            root: null,
            threshold: 1.0,
        });

        this.observer.observe(this.textAreaEl.nativeElement);

        setTimeout(() => {
            if (this.autosizeDir) {
                this.autosizeDir.adjust();
            }
        });
    }

    ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.maxlength) {
            this.maxlength = Number(this.maxlength);
            if (!Number.isFinite(this.maxlength) || this.maxlength < 1) {
                this.maxlength = null;
            }
        }
    }

    ngOnDestroy(): void {
        if (this.observer) {
            this.observer.disconnect();
        }
    }

    public textAreaInputHandler(event: KeyboardEvent) {
        const elementValue = (event.target as HTMLTextAreaElement).value;
        this.triggerChange(elementValue);
    }

    protected onValueChange(): void {
        // Nothing to do
    }

    protected override getFinalValue(): string {
        return normalizeValue(this.value);
    }
}

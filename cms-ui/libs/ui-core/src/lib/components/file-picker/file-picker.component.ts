import {
    ChangeDetectorRef,
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnChanges,
    OnInit,
    Optional,
    Output,
    Self,
    ViewChild,
} from '@angular/core';
import { merge } from 'rxjs';
import { ChangesOf, IFileDropAreaOptions } from '../../common';
import { FileDropAreaDirective } from '../../directives/file-drop-area/file-drop-area.directive';
import { coerceToBoolean, coerceToTruelean, generateFormProvider } from '../../utils';
import { matchesMimeType } from '../../utils/matches-mime-type';
import { BaseFormElementComponent } from '../base-form-element/base-form-element.component';

/**
 * A file picker component.
 *
 * ```html
 * <gtx-file-picker (fileSelect)="uploadFiles($event)"></gtx-file-picker>
 * ```
 */
@Component({
    selector: 'gtx-file-picker',
    templateUrl: './file-picker.component.html',
    styleUrls: ['./file-picker.component.scss'],
    providers: [generateFormProvider(FilePickerComponent)],
    standalone: false
})
export class FilePickerComponent extends BaseFormElementComponent<File[]> implements OnInit, OnChanges {

    /**
     * Sets the file picker button to be auto-focused. Handled by `AutofocusDirective`.
     */
    @Input()
    public autofocus = false;

    /**
     * Set to a falsy value to disable picking multiple files at once. Defaults to `true` if absent.
     */
    @Input({ transform: coerceToTruelean })
    public multiple = true;

    /**
     * Provides feedback for accepted file types, if supported by the browser. Defaults to `"*"`.
     */
    @Input()
    public accept = '*';

    /**
     * Button size - "small", "regular" or "large". Forwarded to the Button component.
     */
    @Input()
    public size: 'small' | 'regular' | 'large' = 'regular';

    /**
     * Display the button as a flat button or not. Forwarded to the Button component.
     */
    @Input({ transform: coerceToBoolean })
    public flat = false;

    /**
     * Sets the type of the button. Forwarded to the Button component.
     */
    @Input()
    public type: 'primary' | 'secondary' | 'success' | 'warning' | 'alert' = 'primary';

    /**
     * Icon button without text. Forwarded to the Button component.
     */
    @Input({ transform: coerceToBoolean })
    public icon = false;

    /**
     * If it should display the names of the selected files.
     */
    @Input({ transform: coerceToBoolean })
    public displayFiles = false;

    /**
     * The label to show when `displayFiles` is `true`.
     */
    @Input()
    public selectionLabel = '';

    /**
     * Triggered when a file / files are selected via the file picker.
     * @deprecated Use `valueChange` events instead.
     */
    @Output()
    public fileSelect = new EventEmitter<File[]>();

    /**
     * Triggered when a file / files are selected but do not fit the "accept" option.
     */
    @Output()
    public fileSelectReject = new EventEmitter<File[]>();

    @ViewChild('fileInput', { static: true })
    public inputElement: ElementRef<HTMLInputElement>;

    public inputAccept = '*';

    constructor(
        @Optional()
        @Self()
        public dropArea: FileDropAreaDirective,
        changeDetector: ChangeDetectorRef,
    ) {
        super(changeDetector);
    }

    ngOnInit(): void {
        if (!this.dropArea) {
            return;
        }
        this.setDropAreaOptions();
        this.subscriptions.push(
            merge(
                this.dropArea.pageDragEnter,
                this.dropArea.pageDragLeave,
                this.dropArea.fileDragEnter,
                this.dropArea.fileDragLeave,
            ).subscribe(() => {
                this.changeDetector.markForCheck();
            }),
        );

        this.subscriptions.push(this.dropArea.fileDrop.subscribe((files: File[]) => {
            this.triggerChange(files);
            this.fileSelect.emit(files);
        }));

        this.subscriptions.push(this.dropArea.fileDropReject.subscribe((files: File[]) => {
            this.fileSelectReject.emit(files);
        }));
    }

    public ngOnChanges(changes: ChangesOf<this>): void {
        if (changes.accept) {
            this.accept = this.accept || '*';
            this.inputAccept = this.accept.replace(/,/g, ';');
        }
        if (changes.accept || changes.disabled || changes.multiple) {
            this.setDropAreaOptions();
        }
    }

    openFilePicker(): void {
        if (this.disabled) {
            return;
        }
        this.inputElement?.nativeElement?.click?.();
    }

    handleFileChange(): void {
        const files = this.inputElement?.nativeElement?.files;
        if (files && files.length) {
            const accepted: File[] = [];
            const rejected: File[] = [];

            Array.from(files).forEach(file => {
                if (matchesMimeType(file.type, this.accept)) {
                    accepted.push(file);
                } else {
                    rejected.push(file);
                }
            });

            // Remove the Files from the input
            this.inputElement.nativeElement.value = '';

            this.triggerChange(accepted);
            if (accepted.length > 0) {
                this.fileSelect.emit(accepted);
            }
            if (rejected.length > 0) {
                this.fileSelectReject.emit(rejected);
            }
        }
    }

    protected onValueChange(): void {
        // no-op
    }

    private setDropAreaOptions(): void {
        if (!this.dropArea) {
            return;
        }

        const options: IFileDropAreaOptions = Object.assign({}, this.dropArea.options || {});
        options.accept = this.accept;
        options.disabled = this.disabled;
        options.multiple = this.multiple;
        this.dropArea.options = options;
    }
}

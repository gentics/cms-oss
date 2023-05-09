import {
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Optional,
    Output,
    Self,
} from '@angular/core';
import { merge, Subscription } from 'rxjs';
import { IFileDropAreaOptions } from '../../common';
import { FileDropAreaDirective } from '../../directives/file-drop-area/file-drop-area.directive';
import { matchesMimeType } from '../../utils/matches-mime-type';

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
})
export class FilePickerComponent implements OnInit, OnDestroy {

    /**
     * Sets the file picker button to be auto-focused. Handled by `AutofocusDirective`.
     */
    @Input()
    autofocus = false;

    /**
     * Set to a non-false value to disable the file picker. Defaults to `false` if absent.
     */
    @Input()
    get disabled(): boolean {
        return this._disabled;
    }
    set disabled(value: boolean) {
        let newValue = value !== false && <any>value !== 'false';
        if (newValue != this._disabled) {
            this._disabled = newValue;
            this.setDropAreaOptions();
        }
    }

    /**
     * Set to a falsy value to disable picking multiple files at once. Defaults to `true` if absent.
     */
    @Input()
    get multiple(): boolean {
        return this._multiple;
    }
    set multiple(value: boolean) {
        let newValue = value !== false && <any>value !== 'false';
        if (newValue != this._multiple) {
            this._multiple = newValue;
            this.setDropAreaOptions();
        }
    }

    /**
     * Provides feedback for accepted file types, if supported by the browser. Defaults to `"*"`.
     */
    @Input()
    get accept(): string {
        return this._accept;
    }
    set accept(value: string) {
        let usedValue = value == undefined ? '*' : value;
        if (usedValue !== this._accept) {
            this._accept = usedValue;
            this.inputAccept = usedValue.replace(/,/g, ';');
            this.setDropAreaOptions();
        }
    }

    /**
     * Button size - "small", "regular" or "large". Forwarded to the Button component.
     */
    @Input()
    get size(): 'small' | 'regular' | 'large' {
        return this._size;
    }
    set size(val: 'small' | 'regular' | 'large') {
        this._size = val == undefined ? 'regular' : val;
    }

    /**
     * Display the button as a flat button or not. Forwarded to the Button component.
     */
    @Input()
    get flat(): boolean {
        return this._flat;
    }
    set flat(val: boolean) {
        this._flat = val != undefined && val !== false;
    }

    /**
     * Sets the type of the button. Forwarded to the Button component.
     */
    @Input()
    type: 'primary' | 'secondary' | 'success' | 'warning' | 'alert' = 'primary';

    /**
     * Icon button without text. Forwarded to the Button component.
     */
    @Input()
    get icon(): boolean {
        return this._icon;
    }
    set icon(val: boolean) {
        this._icon = val !== false && <any>val !== 'false';
    }

    /**
     * Triggered when a file / files are selected via the file picker.
     */
    @Output()
    fileSelect = new EventEmitter<File[]>();

    /**
     * Triggered when a file / files are selected but do not fit the "accept" option.
     */
    @Output()
    fileSelectReject = new EventEmitter<File[]>();


    inputAccept = '*';

    private _icon = false;
    private _size: 'small' | 'regular' | 'large' = 'regular';
    private _flat = false;
    private _accept = '*';
    private _disabled = false;
    private _multiple = true;
    private _subscriptions: Subscription[] = [];

    constructor(
        @Optional() @Self() public dropArea: FileDropAreaDirective,
        private cd: ChangeDetectorRef,
    ) { }

    ngOnInit(): void {
        if (this.dropArea) {
            this.setDropAreaOptions();
            this._subscriptions = [
                merge(
                    this.dropArea.pageDragEnter,
                    this.dropArea.pageDragLeave,
                    this.dropArea.fileDragEnter,
                    this.dropArea.fileDragLeave,
                ).subscribe(() => this.cd.markForCheck()),

                this.dropArea.fileDrop.subscribe((files: File[]) => {
                    this.fileSelect.emit(files);
                }),

                this.dropArea.fileDropReject.subscribe((files: File[]) => {
                    this.fileSelectReject.emit(files);
                }),
            ];
        }
    }

    ngOnDestroy(): void {
        this._subscriptions.forEach(s => s.unsubscribe());
    }

    onChange(event: Event, input: HTMLInputElement): void {
        let files = input && input.files;
        if (files && files.length) {
            let accepted: File[] = [];
            let rejected: File[] = [];
            Array.from(files).forEach(file => {
                if (matchesMimeType(file.type, this._accept)) {
                    accepted.push(file);
                } else {
                    rejected.push(file);
                }
            });

            // Remove the Files from the input
            input.value = '';

            if (accepted.length > 0) {
                this.fileSelect.emit(accepted);
            }
            if (rejected.length > 0) {
                this.fileSelectReject.emit(rejected);
            }
        }
    }

    private setDropAreaOptions(): void {
        if (this.dropArea) {
            let options: IFileDropAreaOptions = Object.assign({}, this.dropArea.options || {});
            options.accept = this._accept;
            options.disabled = this._disabled;
            options.multiple = this._multiple;
            this.dropArea.options = options;
        }
    }
}

import {ChangeDetectionStrategy, Component, Input, EventEmitter, Output} from '@angular/core';

/**
 * Shows a read-only input field with a clear and a browse button.
 *
 * This component does not offer item selection or selected item management functionality by itself.
 * The clear and the browse buttons fire events and the parent component needs to react to them
 * by clearing the selection or opening e.g., the repository browser. Also the value displayed
 * inside the read-only input field needs to be supplied by the parent component.
 */
@Component({
    selector: 'browse-box',
    templateUrl: './browse-box.component.html',
    styleUrls: ['./browse-box.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class BrowseBoxComponent {

    /**
     * The label that is displayed above the read-only input field.
     */
    @Input()
    label: string;

    /**
     * Determines if this component is disabled.
     */
    @Input()
    disabled = false;

    /**
     * The string that is displayed inside the read-only input field.
     */
    @Input()
    displayValue: string;

    /**
     * If true (= default), a clear button is displayed.
     */
    @Input()
    clearable = true;

    /**
     * If true (= default), a clear button is displayed.
     */
    @Input()
    canUpload = false;

    /**
     * Optional tooltip for the clear button (if this is not set, a default tooltip is used).
     */
    @Input()
    clearTooltip: string;

    /**
     * Optional tooltip for the browse button (if this is not set, a default tooltip is used).
     */
    @Input()
    browseTooltip: string;

    /**
     * Optional tooltip for the upload button (if this is not set, a default tooltip is used).
     */
    @Input()
    uploadTooltip: string;

    /**
     * Emits when the user clicks the clear button.
     */
    @Output()
    clear = new EventEmitter<void>();

    /**
     * Emits when the user clicks the browse button.
     */
    @Output()
    browse = new EventEmitter<void>();

    /**
     * Emits when the user clicks the upload button.
     */
    @Output()
    upload = new EventEmitter<void>();

    constructor() { }

}

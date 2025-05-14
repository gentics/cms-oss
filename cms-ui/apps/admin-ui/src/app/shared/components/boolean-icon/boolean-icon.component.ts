import { Component, Input } from '@angular/core';

/**
 * Displays an icon reflecting the truthiness of `value`.
 *
 * If `value` is truthy, a checkbox is displayed,
 * if it is falsy, an X is displayed.
 */
@Component({
    selector: 'gtx-boolean-icon',
    templateUrl: './boolean-icon.component.html',
    standalone: false
})
export class BooleanIconComponent {

    /** The boolean value to be displayed. */
    @Input() value: any;

}

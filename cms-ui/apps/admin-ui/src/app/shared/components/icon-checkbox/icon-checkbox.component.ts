import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

/**
 * A component which displays an icon, which turns into a checkbox on hover and can be
 * selected. If selected, it remains in the checkbox state.
 *
 * The "hover" state may alse be triggered by hovering an ancestor DOM element which has the class
 * `icon-checkbox-trigger`. This allows an implementation where hovering anywhere on a row will display the
 * checkbox, rather than only hovering over the icon.
 *
 * ```
 * <!-- Icon mode -->
 * <gtx-icon-checkbox [selected]="isSelected"
 *                (change)="toggleSelect()"
 *                icon="folder">
 * </gtx-icon-checkbox>
 * ```
 */
@Component({
    selector: 'gtx-icon-checkbox',
    templateUrl: './icon-checkbox.tpl.html',
    styleUrls: ['./icon-checkbox.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class IconCheckboxComponent {
    /**
     * Whether the checkbox is selected or not.
     */
    @Input() selected = false;
    /**
     * A Material Icon string, e.g. "folder". See https://design.google.com/icons/
     * If both icon and image are set, then the icon is displayed.
     */
    @Input() icon = '';
    /**
     * Whether the checkbox is disabled or not.
     */
    @Input() disabled = false;

    /**
     * When the checkbox is clicked, emits the opposite of the current check state.
     */
    @Output() change = new EventEmitter<boolean>();

    checkboxClicked(checkState: boolean): void {
        this.change.emit(checkState);
    }
}

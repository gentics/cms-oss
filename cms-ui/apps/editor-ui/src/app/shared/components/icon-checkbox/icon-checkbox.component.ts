import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { Image } from '@gentics/cms-models';

/**
 * A component which displays an icon or a CMS image, which turns into a checkbox on hover and can be
 * selected. If selected, it remains in the checkbox state.
 *
 * The "hover" state may alse be triggered by hovering an ancestor DOM element which has the class
 * `icon-checkbox-trigger`. This allows an implementation where hovering anywhere on a row will display the
 * checkbox, rather than only hovering over the icon.
 *
 * ```html
 * <!-- Icon mode -->
 * <icon-checkbox
 *     [selected]="isSelected"
 *     (selectedChange)="toggleSelect()"
 *     icon="folder"
 * ></icon-checkbox>
 *
 * <!-- Image mode -->
 * <icon-checkbox
 *     [selected]="isSelected"
 *     (selectedChange)="toggleSelect()"
 *     image="imageItem"
 *     [nodeId]="nodeId"
 * ></icon-checkbox>
 * ```
 */
@Component({
    selector: 'icon-checkbox',
    templateUrl: './icon-checkbox.tpl.html',
    styleUrls: ['./icon-checkbox.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class IconCheckbox {

    /**
     * Whether the checkbox is selected or not.
     */
    @Input()
    public selected = false;

    /**
     * A Material Icon string, e.g. "folder". See https://design.google.com/icons/
     * If both icon and image are set, then the icon is displayed.
     */
    @Input()
    public icon = '';

    /**
     * A CMS Image object, of which a thumbnail should be displayed.
     */
    @Input()
    public image: Image;

    /**
     * The id of the node the image is requested from. Only needed when an image is provided.
     */
    @Input()
    public nodeId?: number;

    /**
     * Whether the checkbox is disabled or not.
     */
    @Input()
    public disabled = false;

    /**
     * When the checkbox is clicked, emits the opposite of the current check state.
     */
    @Output()
    public selectedChange = new EventEmitter<boolean>();

    checkboxClicked(checkState: boolean): void {
        this.selectedChange.emit(checkState);
    }
}

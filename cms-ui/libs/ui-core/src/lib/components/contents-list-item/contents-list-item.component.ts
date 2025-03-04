import {Component, ViewEncapsulation} from '@angular/core';

/**
 * A generic list entry
 *
 * Two component-specific classes can be used:
 *
 * * `.item-avatar`: The content of this element will be styled in a circular container.
 * * `.item-primary`: The primary content of the item, which will take up all the remaining space via `flex: 1`.
 * * `.show-on-hover`: Any element with this class will appear faded out until the user hovers the list item.
 *
 *
 * ```html
 * <gtx-contents-list-item *ngFor="let item of listItems">
 *     <!-- this will be styled as a circular icon -->
 *     <div class="item-avatar"><icon>{{ item.icon }}</icon></div>
 *     <!-- this will stretch to use all available space -->
 *     <div class="item-primary"><a [routerLink]="[item.route]">{{ item.title }}</a></div>
 *     <!-- these will use remaining space to the right -->
 *     <div class="show-on-hover">
 *         <icon>edit</icon>
 *         <icon>star</icon>
 *     </div>
 * </gtx-contents-list-item>
 * ```
 * @deprecated Glorified css class, which does not need to be a component.
 * In a future release the component will simply be removed by a css class instead.
 */
@Component({
    selector: 'gtx-contents-list-item',
    templateUrl: './contents-list-item.component.html',
    styleUrls: ['./contents-list-item.component.scss'],
    // Disable view-encapsulation, as this is only a simple wrapper component
    encapsulation: ViewEncapsulation.None,
})
export class ContentsListItem {}

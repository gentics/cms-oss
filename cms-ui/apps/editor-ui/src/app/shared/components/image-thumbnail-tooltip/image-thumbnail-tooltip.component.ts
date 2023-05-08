import { ChangeDetectionStrategy, Component, ElementRef, Input, ViewChild } from '@angular/core';
import { Image } from '@gentics/cms-models';

/**
 * Displays a tooltip with a thumbnail of the specified image when
 * the user hovers over the trigger element.
 *
 * The trigger element needs to be specified as the content of the image-thumbnail-tooltip.
 *
 * Currently this element only works inside the Item List or the Repository Browser.
 *
 * ```
 * <image-thumbnail-tooltip [image]="image">
 *     <!-- This is the trigger -->
 *     <icon>zoom_in</icon>
 * </image-thumbnail-tooltip>
 * ```
 */
@Component({
    selector: 'image-thumbnail-tooltip',
    templateUrl: './image-thumbnail-tooltip.tpl.html',
    styleUrls: ['./image-thumbnail-tooltip.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ImageThumbnailTooltip {

    /** The CMS Image that should be displayed in the tooltip. */
    @Input() image: Image;

    /** The id of the node the image is requested from. */
    @Input() nodeId: number;

    /** Parent element to use its height. */
    @Input() parent: HTMLElement;

    @ViewChild('trigger', { read: ElementRef, static: true })
    trigger: ElementRef;

    @ViewChild('tooltip', { read: ElementRef, static: true })
    tooltip: ElementRef;

    mouseOver(): void {
        let repositoryBrowser = document.querySelector('repository-browser');
        let triggerTop = this.trigger.nativeElement.getBoundingClientRect().top;
        let itemParentHeight = this.parent.offsetHeight;
        let containerTop: number;

        if (repositoryBrowser) {
            containerTop = document.querySelector('.modal-content').getBoundingClientRect().top;
        } else {
            containerTop = document.querySelector('gtx-split-view-container').getBoundingClientRect().top;
        }

        if (triggerTop - containerTop < this.tooltip.nativeElement.offsetHeight) {
            this.tooltip.nativeElement.style.top = this.tooltip.nativeElement.offsetHeight + itemParentHeight + 'px';
        } else {
            this.tooltip.nativeElement.style.top = '0';
        }
    }
}

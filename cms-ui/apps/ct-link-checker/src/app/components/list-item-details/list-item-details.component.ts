import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    EventEmitter,
    Input,
    Output,
    ViewChild
} from '@angular/core';
import { Item, Template } from '@gentics/cms-models';

const MAX_HEIGHT = '80px';

/**
 * Responsible for displaying the additional item properties as specified by the DisplayFieldSelector component.
 */
@Component({
    selector: 'list-item-details',
    templateUrl: './list-item-details.component.tpl.html',
    styleUrls: ['./list-item-details.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ListItemDetailsComponent {
    @Input() fields: string[];
    @Input() item: Item;
    @Output() usageClick = new EventEmitter<Item>();

    @ViewChild('fieldsWrapper')
    fieldsWrapper: ElementRef;

    activeNodeId: number;
    constructor() { }

    /**
     * Format a template id into the template name. Accepts normalized IDs
     * or a template object to work with the repository browser.
     */
    templateName(template: number | Template): string {
        let templateObj: Template;
        templateObj = (template as Template);
        return templateObj && templateObj.name;
    }

    /**
     * Displays the total usage of an item if it is available.
     */
    totalUsage(item: Item): string {
        if (item.usage) {
            return item.usage.total.toString(10);
        } else {
            return '-';
        }
    }

    usageClicked(e: Event, item: Item): void {
        e.preventDefault();
        e.stopPropagation();
        this.usageClick.emit(item);
    }
}

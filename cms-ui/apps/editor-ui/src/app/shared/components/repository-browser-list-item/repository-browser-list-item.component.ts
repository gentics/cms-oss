import { ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { File, Folder, Image, Page } from '@gentics/cms-models';
import { iconForItemType } from '../../../common/utils/icon-for-item-type';
import { DisplayFields } from '../../../common/models';

/**
 * Component that renders a single item in the repository browser.
 * Seems to be unused!
 */
@Component({
    selector: 'repository-browser-list-item',
    templateUrl: './repository-browser-list-item.tpl.html',
    styleUrls: ['./repository-browser-list-item.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class RepositoryBrowserListItem {

    @Input() item: File | Folder | Page | Image;
    @Input() nodeId: number;
    @Input() canBeSelected: boolean;
    @Input() displayFields: DisplayFields;
    @Input() isSelected = false;
    @Input() displayNodeName = false;
    @Input() startPageId: number;
    @Input() searching = false;
    @Output() itemClick = new EventEmitter<File | Folder | Page | Image>();
    @Output() toggleSelect = new EventEmitter<boolean>();

    @ViewChild('itemPrimary', { read: ElementRef, static: true })
    itemPrimary: ElementRef;

    iconForItemType = iconForItemType;

}

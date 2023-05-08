import { ChangeDetectionStrategy, Component, EventEmitter, HostBinding, Input, Output } from '@angular/core';
import { ItemsInfo } from '@editor-ui/app/common/models';
import { Image } from '@gentics/cms-models';

/**
 * Component that renders a single image in the repository browser.
 * Non-image items are rendered as a RepositoryBrowserListItem component.
 */
@Component({
    // Attribute selector is needed since `masonry-item` can not be used inside a nested component.
    selector: 'repository-browser-list-thumbnail,[repositoryBrowserListThumbnail]',
    templateUrl: './repository-browser-list-thumbnail.tpl.html',
    styleUrls: ['./repository-browser-list-thumbnail.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class RepositoryBrowserListThumbnail {

    @Input()
    public item: Image;

    @Input()
    public itemsInfo: ItemsInfo;

    @Input()
    public nodeId: number;

    @Input()
    @HostBinding('class.selected')
    public isSelected = false;

    @Output()
    public itemClick = new EventEmitter<void>();

    @Output()
    public toggleSelect = new EventEmitter<boolean>();
}

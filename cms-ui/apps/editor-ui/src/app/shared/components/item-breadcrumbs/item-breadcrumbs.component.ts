import { ChangeDetectionStrategy, Component, Input, OnChanges, SimpleChange } from '@angular/core';
import { File, Folder, Image, Page } from '@gentics/cms-models';
import { IBreadcrumbRouterLink } from '@gentics/ui-core';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';

@Component({
    selector: 'item-breadcrumbs',
    templateUrl: './item-breadcrumbs.component.html',
    styleUrls: ['./item-breadcrumbs.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ItemBreadcrumbsComponent implements OnChanges {

    @Input()
    item: File | Folder | Image | Page;

    @Input()
    linkPaths: boolean;

    breadcrumbs: IBreadcrumbRouterLink[];

    get itemName(): string {
        if (this.item) {
            if (this.item.type === 'page') {
                return this.item.fileName;
            }
            return this.item.name;
        }
        return '';
    }

    constructor(private entityResolver: EntityResolver) { }

    ngOnChanges(changes: { [K in keyof this]: SimpleChange }): void {
        if (changes.item) {
            if (this.item) {
                let parentFolder: Folder;
                if (this.item.type === 'folder') {
                    parentFolder = this.item;
                } else {
                    const folderOrId = this.item.folder;
                    parentFolder = typeof folderOrId === 'number' ? this.entityResolver.getFolder(folderOrId) : folderOrId;
                }
                this.breadcrumbs = this.setUpBreadcrumbs(parentFolder);
            } else {
                this.breadcrumbs = null;
            }
        }
    }

    private setUpBreadcrumbs(parentFolder: Folder): IBreadcrumbRouterLink[] {
        const breadcrumbs: IBreadcrumbRouterLink[] = [];

        if (parentFolder && parentFolder.breadcrumbs) {
            // If the item is a folder it should not be included in the breadcrumbs itself.
            let breadcrumbsCount = this.item.type === 'folder' ? parentFolder.breadcrumbs.length - 1 : parentFolder.breadcrumbs.length;

            for (let i = 0; i < breadcrumbsCount; ++i) {
                const folder = parentFolder.breadcrumbs[i];
                breadcrumbs.push({
                    text: folder.name,
                    route: ['/editor', { outlets: { list: ['node', parentFolder.nodeId, 'folder', folder.id]}} ]
                });
            }
        }

        return breadcrumbs;
    }

}

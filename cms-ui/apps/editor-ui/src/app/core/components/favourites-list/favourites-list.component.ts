import { ChangeDetectionStrategy, Component, EventEmitter, Output } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { areItemsLoading } from '@editor-ui/app/common/utils/are-items-loading';
import { EditMode } from '@gentics/cms-integration-api-models';
import { Favourite, FavouriteWithDisplayDetails } from '@gentics/cms-models';
import { ISortableEvent } from '@gentics/ui-core';
import { isEqual } from 'lodash-es';
import { Observable } from 'rxjs';
import { distinctUntilChanged, filter, map, skip, take } from 'rxjs/operators';
import { ApplicationStateService, FolderActionsService } from '../../../state';
import { FavouritesService } from '../../providers/favourites/favourites.service';
import { NavigationService } from '../../providers/navigation/navigation.service';

@Component({
    selector: 'favourites-list',
    templateUrl: './favourites-list.component.html',
    styleUrls: ['./favourites-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FavouritesListComponent {

    @Output()
    public navigate = new EventEmitter<Favourite>(false);

    favourites$: Observable<FavouriteWithDisplayDetails[]>;

    constructor(
        private favourites: FavouritesService,
        private folderActions: FolderActionsService,
        private navigationService: NavigationService,
        private appState: ApplicationStateService,
        private route: ActivatedRoute,
        private router: Router,
    ) {
        this.favourites$ = this.favourites.list$;
    }

    public favouriteClicked(item: Favourite): void {
        this.navigate.emit(item);
        switch (item.type) {
            case 'folder':
                this.openFolder(item);
                break;
            case 'page':
                this.openPage(item);
                break;
            case 'image':
                this.openImage(item);
                break;
            case 'file':
                this.openFile(item);
                break;
            default:
                // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
                throw new Error(`Invalid item type ${item.type} in FavouritesList.navigateTo`);
        }
    }

    remove(oneItem: Favourite): void {
        this.favourites.remove([oneItem]);
    }

    reorder(e: ISortableEvent): void {
        const newOrder = e.sort(this.favourites.getList());
        this.favourites.reorder(newOrder);
    }

    private fetchParentFolder(item: Favourite): Promise<number> {
        return this.folderActions.getItem(item.id, item.type, { nodeId: item.nodeId })
            .then(item => (item as any).folderId);
    }

    private navigateToFolder(folderId: number, nodeId: number): Promise<boolean> {
        const newRoute = this.navigationService.list(nodeId, folderId).commands();

        const params = this.route.snapshot.firstChild.children.find(r => r.outlet === 'list').firstChild.firstChild.paramMap;
        if (params.get('nodeId') === nodeId.toString() && params.get('folderId') === folderId.toString()) {
            return Promise.resolve(true);
        }

        return this.router.navigate(newRoute)
            .then(success => {
                if (!success) {
                    return false;
                }

                // Wait until loaded
                return this.appState.select(state => state.folder).pipe(
                    map(state => areItemsLoading(state)),
                    distinctUntilChanged(isEqual),
                    skip(1),
                    filter(fetching => fetching === false),
                    take(1),
                    map(() => true),
                ).toPromise();
            });
    }

    private openFolder(folder: Favourite): void {
        this.navigateToFolder(folder.id, folder.nodeId);
    }

    private openPage(page: Favourite): void {
        this.openItem(page, 'page');
    }

    private openImage(image: Favourite): void {
        this.openItem(image, 'image');
    }

    private openFile(file: Favourite): void {
        this.openItem(file, 'file');
    }

    private openItem(item: Favourite, type: 'file' | 'image' | 'page'): void {
        this.fetchParentFolder(item)
            .then(parentFolderId => this.navigateToFolder(parentFolderId, item.nodeId))
            .then(succeeded => {
                if (succeeded) {
                    const editMode = type === 'page' ? EditMode.PREVIEW : EditMode.EDIT_PROPERTIES;
                    this.navigate.emit(item);
                    this.navigationService
                        .detailOrModal(item.nodeId, type, item.id, editMode)
                        .navigate();
                }
            });
    }
}

import { FileOrImage, Folder, Form, ItemInNode, Page, Raw } from '@gentics/cms-models';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { filter, map, take } from 'rxjs/operators';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';

/**
 * Helper class for managing a selected item in a TagPropertyEditor or FormPropertiesForm,
 * which can be determined either by an itemId & nodeId combination or
 * by an Item object. In the former case this class handles the loading of
 * the item and then publishes it on the `selectedItem$` observable and in the
 * latter case it just publishes the item.
 */
export class SelectedItemHelper<T extends ItemInNode<Page<Raw> | Folder<Raw> | Form<Raw> | FileOrImage<Raw>>> {

    /** Observable for the currently selected item. */
    readonly selectedItem$: Observable<T>;

    /** Subject for any loading errors that might occur. */
    private loadingErrorSubject = new BehaviorSubject<{ error: any, item: { itemId: number, nodeId?: number } }>(null);
    readonly loadingError$ = this.loadingErrorSubject.asObservable().pipe(
        filter((error) => error != null),
    );

    private selectedItemSubj$ = new BehaviorSubject<T>(null);

    /** The currently selected item. */
    get selectedItem(): T {
        return this.selectedItemSubj$.getValue();
    }

    /**
     * @param itemType The type of items that should be managed by this instance.
     * @param defaultNodeId The fallback nodeId to be used when an ItemInfo has no nodeId set.
     * @param client The rest client to use for loading.
     */
    constructor(
        private itemType: 'page' | 'folder' | 'file' | 'image' | 'form',
        private defaultNodeId: number,
        private client: GCMSRestClientService,
    ) {
        this.selectedItem$ = this.selectedItemSubj$.asObservable();
    }

    /**
     * Sets the selected item using the its IDs and loads that item
     *
     * @param itemId The ID of the item.
     * @param nodeId The nodeId to be used. If this is omitted the defaultNodeId is used.
     */
    setSelectedItem(itemId: number, nodeId?: number): void;
    /**
     * Sets the selected item.
     *
     * @param item The next selected item (may also be null).
     */
    setSelectedItem(item: T): void;
    setSelectedItem(item: number | T, nodeId?: number): void {
        if (typeof item === 'number') {
            const item$ = this.loadItem(item, nodeId);
            item$.pipe(
                take(1),
            ).subscribe(item => {
                this.selectedItemSubj$.next(item);
            }, error => this.loadingErrorSubject.next(error));
        } else {
            // If item is null, we also get here.
            this.selectedItemSubj$.next(item);
        }
    }

    /**
     * Loads the specified item.
     */
    private loadItem(itemId: number, nodeId?: number): Observable<T> {
        nodeId = nodeId || this.defaultNodeId;

        let obv$: Observable<any>;
        switch (this.itemType) {
            case 'file':
                obv$ = this.client.file.get(itemId, { nodeId }).pipe(
                    map(res => res.file),
                );
                break;
            case 'folder':
                obv$ = this.client.folder.get(itemId, { nodeId }).pipe(
                    map(res => res.folder),
                );
                break;
            case 'form':
                obv$ = this.client.form.get(itemId).pipe(
                    map(res => res.item),
                );
                break;
            case 'image':
                obv$ = this.client.image.get(itemId, { nodeId }).pipe(
                    map(res => res.image),
                );
                break;
            case 'page':
                obv$ = this.client.page.get(itemId, { nodeId }).pipe(
                    map(res => res.page),
                );
                break;
            default:
                // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
                obv$ = throwError(new Error(`Unknown item type "${this.itemType}"!`));
        }

        return obv$.pipe(
            map((item: T) => {
                if (!item.nodeId) {
                    item.nodeId = nodeId;
                }
                return item;
            }),
        );
    }

}

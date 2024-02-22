import { Injectable } from '@angular/core';
import { ApplicationStateService, DecreaseOverlayCountAction, IncreaseOverlayCountAction } from '@editor-ui/app/state';
import {
    AllowedSelectionType,
    AllowedSelectionTypeMap,
    ItemInNode,
    RepositoryBrowserOptions,
    TagInContainer,
} from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { RepositoryBrowser } from '../../components';

@Injectable()
export class RepositoryBrowserClient {

    constructor(
        private appState: ApplicationStateService,
        private modalService: ModalService,
    ) {}

    /**
     * Opens a repository browser window that allows selecting items / an item from
     * multichannelling nodes and their subfolders.
     *
     * It can be used for single or multiple selection
     * and limit the type of the allowed selection.
     *
     * The Promise returned by this method resolves to `ItemInNode | TagInContainer` if `options.selectMultiple` is false and
     * to `(ItemInNode | TagInContainer)[]` if `selectMultiple` is true.
     * There are multiple overloads, which allow TypeScript to recognize the return type, based on the provided `options`.
     *
     * Example:
     * ```TypeScript
     * this.repositoryBrowserClient.openRepositoryBrowser({ allowedSelection: 'page', selectMultiple: false })
     *   .then(page => useSelectedPage(page));
     * ```
     *
     * All options:
     * ```JSON
     * {
     *     allowedSelection: ['file', 'image'], // 'page' | 'folder' | 'image' | 'file' | 'template' | 'contenttag' | 'templatetag' | 'form'
     *     multiple: true, // optional, default: false
     *     startNode: 7, // optional, default: current Node
     *     startFolder: 10, // optional, default current Folder
     *     onlyInCurrentNode: true, // optional, default false
     *     title: 'Select a file or image that should be linked in the article', // optional, default text will be displayed
     *     submitLabel: 'Add to article' // optional, default text will be displayed
     * }
     * ```
     *
     * @returns A Promise, which resolves to `ItemInNode | TagInContainer` if `selectMultiple` is false and
     * to `(ItemInNode | TagInContainer)[]` if `selectMultiple` is true.
     * If user clicks on the cancel button, the promise neither resolves nor rejects.
     */
    openRepositoryBrowser<T extends AllowedSelectionType, R = AllowedSelectionTypeMap[T]>(
        options: RepositoryBrowserOptions & { allowedSelection: T, selectMultiple: false }
    ): Promise<R>;
    openRepositoryBrowser<T extends AllowedSelectionType, R = AllowedSelectionTypeMap[T]>(
        options: RepositoryBrowserOptions & { allowedSelection: T, selectMultiple: true }
    ): Promise<R[]>;
    openRepositoryBrowser<R = ItemInNode | TagInContainer>(
        options: RepositoryBrowserOptions & { allowedSelection: AllowedSelectionType[], selectMultiple: false }
    ): Promise<R>;
    openRepositoryBrowser<R = ItemInNode | TagInContainer>(
        options: RepositoryBrowserOptions & { allowedSelection: AllowedSelectionType[], selectMultiple: true }
    ): Promise<R[]>;
    openRepositoryBrowser<R = ItemInNode | TagInContainer>(options: RepositoryBrowserOptions): Promise<R | R[]>;
    async openRepositoryBrowser<R = ItemInNode | TagInContainer>(options: RepositoryBrowserOptions): Promise<R | R[]> {
        await this.appState.dispatch(new IncreaseOverlayCountAction()).toPromise();

        let didDecrease = false;
        // Because of https://jira.gentics.com/browse/GUIC-224 we cannot use the promise to determine if the RepositoryBrowser
        // was closed by clicking Cancel. To maintain compatibility with existing RepositoryBrowser usages and
        // also because there is probably no other way right now, the promise only resolves if the user clicks OK.
        // If the user clicks Cancel, nothing happens.
        const modal = await this.modalService.fromComponent(
            RepositoryBrowser,
            {
                padding: true,
                width: '1000px',
                onClose: () => {
                    this.appState.dispatch(new DecreaseOverlayCountAction());
                    didDecrease = true;
                },
            },
            { options },
        );
        const selected: R | R[] = await modal.open();

        if (!didDecrease) {
            this.appState.dispatch(new DecreaseOverlayCountAction());
        }

        return selected;
    }

}

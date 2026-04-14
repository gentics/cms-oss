import { Component, input, model, output } from '@angular/core';
import { Node, PagingSortOrder } from '@gentics/cms-models';
import { EditorPermissions, getNoPermissions, StageableItem, UIMode } from '../../../common/models';

@Component({
    template: '',
    standalone: false,
})
export abstract class BaseItemListHeaderComponent {

    public readonly UIMode = UIMode;

    /** Permissions of the editor */
    public readonly permissions = input<EditorPermissions>(getNoPermissions());
    /** The current folder id where this header is being displayed in */
    public readonly folderId = input.required<number>();
    /** The current node in where this header is being displayed in */
    public readonly node = input.required<Node>();
    /** The current items displayed in the current list page */
    public readonly currentPageItems = input.required<StageableItem[]>();
    /** All items which are currently selected */
    public readonly selectedItems = input.required<StageableItem[]>();

    /** Total count of items for this item type in the folder */
    public readonly totalCount = input.required<number>();
    /** Maxiumum amount of how many items are displayed per page */
    public readonly pageSize = input.required<number>();
    /** The current list page number (Starts with 1) */
    public readonly currentPage = input.required<number>();
    /** A set of item IDs which are marked as selected */
    public readonly selection = input.required<Set<number>>();
    /** The attribute name by which the items are to be sorted by */
    public readonly sortBy = input.required<string>();
    /** The order by which the items are to be sorted by */
    public readonly sortOrder = input.required<PagingSortOrder>();

    /** If the items should display all available languages */
    public readonly showAllLanguages = input<boolean>(false);
    /** If the language information should also contain additional language status indicators */
    public readonly showStatusIcons = input<boolean>(false);
    /** The term the list is currently filtered by */
    public readonly filterTerm = input<string>('');
    /** If a elasticsearch query is currently active */
    public readonly elasticSearchQueryActive = input<boolean>(false);
    /** If the wastebin feature is enabled */
    public readonly wastebinEnabled = input<boolean>();
    /** If the list should display deleted items */
    public readonly showDeleted = input<boolean>();
    /** Which mode the UI is currently in */
    public readonly uiMode = input.required<UIMode>();

    /** If the list is currently collapsed */
    public readonly collapsed = model.required<boolean>();

    /**
     * Event which emits the state all items should be selected to.
     * i.E. `true` if all items should be selected, and
     * `false` if all items should be de-selected.
     */
    public readonly toggleAllSelection = output<boolean>();

    public toggleSelection(state: boolean): void {
        this.toggleAllSelection.emit(state);
    }
}

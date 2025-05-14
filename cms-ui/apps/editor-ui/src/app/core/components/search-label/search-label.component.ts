import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { DropdownListComponent } from '@gentics/ui-core';

@Component({
    selector: 'search-label',
    templateUrl: './search-label.component.html',
    styleUrls: ['./search-label.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class SearchLabel {

    /** The default label when nothing is selected */
    @Input()
    label: string;

    /** A string representing the current selection */
    @Input()
    selection: string;

    /** Forwards to gtx-dropdown-list `sticky` property */
    @Input()
    sticky = false;

    @Input()
    icon: string;

    @Input()
    filterLabel = '';

    /** Emitted when the X icon is clicked, to clear the current selection */
    @Output()
    cancel = new EventEmitter<void>();

    @Output()
    close = new EventEmitter<void>();

    @ViewChild(DropdownListComponent, { static: true })
    private dropdownList: DropdownListComponent;

    cancelClicked(e: MouseEvent): void {
        e.stopPropagation();
        this.cancel.emit();
    }

    closeDropdown(): void {
        this.dropdownList.closeDropdown();
    }
}

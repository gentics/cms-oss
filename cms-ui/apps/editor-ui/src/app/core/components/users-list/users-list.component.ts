import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { User } from '@gentics/cms-models';

/**
 * A generic user list. This component is designed to be stateless - the selection must be set from the
 * parent component.
 */
@Component({
    selector: 'users-list',
    templateUrl: './users-list.tpl.html',
    styleUrls: ['./users-list.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UsersList {
    @Input() users: User[] = [];
    @Input() selected: number[] = [];
    @Output() selectedChange = new EventEmitter<number[]>();

    toggleSelectAll(): void {
        if (this.users?.length > 0 && this.users?.length === this.selected?.length) {
            this.selectedChange.emit([]);
        } else {
            this.selectedChange.emit(this.users.map(user => user.id));
        }
    }

    toggleSelect(userId: number): void {
        const index = this.selected.indexOf(userId);
        if (-1 < index) {
            const newSelection = this.selected.slice();
            newSelection.splice(index, 1);
            this.selectedChange.emit(newSelection);
        } else {
            this.selectedChange.emit(this.selected.concat([userId]));
        }
    }
}

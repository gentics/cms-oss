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
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UsersList {
    @Input() users: User[] = [];
    @Input() selected: number[] = [];
    @Input() maxHeight: string = '1000px';
    @Output() selectionChange = new EventEmitter<number[]>();

    private selectedMap: { [id: number]: boolean } = {};

    ngOnChanges(): void {
        if (this.users) {
            this.users.forEach(user => {
                this.selectedMap[user.id] = -1 < this.selected.indexOf(user.id);
            });
        }
    }

    toggleSelectall(): void {
        if (this.areAllSelected()) {
            this.selectionChange.emit([]);
        } else {
            this.selectionChange.emit(this.users.map(user => user.id));
        }
    }

    toggleSelect(userId: number): void {
        const index = this.selected.indexOf(userId);
        if (-1 < index) {
            const newSelection = this.selected.slice();
            newSelection.splice(index, 1);
            this.selectionChange.emit(newSelection);
        } else {
            this.selectionChange.emit(this.selected.concat([userId]));
        }
    }

    areAllSelected(): boolean {
        return 0 < this.users.length && this.selected.length === this.users.length;
    }
}

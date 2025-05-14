import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

// import {EntityResolver} from '../../providers/entity-resolver/entity-resolver';
import { Message } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { EntityManagerService } from '../../providers';

@Component({
    selector: 'gtx-message-list',
    templateUrl: './message-list.tpl.html',
    styleUrls: ['./message-list.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class MessageListComponent {
    @Input() waitsForDeleteConfirmation: Message;
    @Input() messages: Message[];
    @Input() nodes: { id: number, name: string }[];
    @Output() messageClick = new EventEmitter<Message>();
    @Output() messageArchive = new EventEmitter<Message>();
    @Output() messageDelete = new EventEmitter<Message>();

    @Input() selected: number[] = [];
    @Output() selectionChange = new EventEmitter<number[]>();


    constructor(private entityManager: EntityManagerService) {}

    getFullName(userId: number): Observable<string> {
        const user$ = this.entityManager.getEntity('user', userId);
        return user$.pipe(
            map(user => user ? user.firstName + ' ' + user.lastName : ''),
        );
    }

    trackUnique(message: Message): string {
        return message.id + '-' + (message.unread ? 'unread' : 'read');
    }

    isSelected(item: Message): boolean {
        return this.selected.includes(item.id);
    }

    toggleSelect(item: Message, selected: boolean): void {
        let newSelection: number[];

        if (selected === true) {
            newSelection = this.selected.concat([item.id]);
        } else {
            newSelection = this.selected.slice();
            newSelection.splice(newSelection.indexOf(item.id), 1);
        }

        this.selected = newSelection;
        this.selectionChange.emit(newSelection);
    }

    private toggleSelectAll(selected: boolean): void {
        if (selected === false) {
            this.selected = [];
            this.selectionChange.emit([]);
        } else {
            this.selected = this.getAllIds();
            this.selectionChange.emit(this.getAllIds());
        }
    }

    getAllIds(): number[] {
        return this.messages.map(message => message.id);
    }
}

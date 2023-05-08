import { ChangeDetectionStrategy, Component } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { Page, User } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { ApplicationStateService, FolderActionsService, PublishQueueActionsService } from '../../../state';
import { EntityResolver } from '../../providers/entity-resolver/entity-resolver';

@Component({
    selector: 'assign-page-modal',
    templateUrl: './assign-page-modal.component.html',
    styleUrls: ['./assign-page-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    })
export class AssignPageModal extends BaseModal<void> {

    users$: Observable<User[]>;
    loading$: Observable<boolean>;
    pages: Page[] = [];
    selected: number[] = [];
    message = '';
    filterTerm: UntypedFormControl = new UntypedFormControl('');

    constructor(
        private appState: ApplicationStateService,
        private publishQueueActions: PublishQueueActionsService,
        private entityResolver: EntityResolver,
        private folderActions: FolderActionsService,
    ) {
        super();
        publishQueueActions.getUsersForRevision();

        const filterTerm$ = this.filterTerm.valueChanges.startWith('');
        const publishQueueUsers$ = appState.select(state => state.publishQueue.users)
            .map(users => users.map(id => entityResolver.getUser(id)));

        this.users$ = publishQueueUsers$
            .combineLatest(filterTerm$)
            .map(result => this.filterUsers(result[0], result[1]));

        this.loading$ = appState.select(state => state.publishQueue.assigning);
    }

    /**
     * Filter the users by a filter term.
     */
    filterUsers(users: User[], term: string): User[] {
        const matches = (name: string) => -1 < name.toLowerCase().indexOf(term.toLowerCase());
        if (term === '') {
            return users;
        }
        return users.filter(u => matches(u.firstName) || matches(u.lastName));
    }

    /**
     * Assign the pages to the selected users for revision.
     */
    assign(): void {
        const pageIds = this.pages.map(p => p.id);
        const userIds = this.selected;
        const message = this.message;
        this.publishQueueActions.assignToUsers(pageIds, userIds, message)
            .then(() => {
                this.folderActions.refreshList('page');
            })
            .then(() => this.closeFn(null));
    }
}

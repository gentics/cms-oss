import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { Page, User } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { BaseModal } from '@gentics/ui-core';
import { Subscription } from 'rxjs';
import { FolderActionsService, PublishQueueActionsService } from '../../../state';
import { I18nNotification } from '../../providers/i18n-notification/i18n-notification.service';

/**
 * Filter the users by a filter term.
 */
function filterUsers(users: User[], term: string): User[] {
    const matches = (name: string) => -1 < name.toLowerCase().indexOf(term.toLowerCase());
    if (term.trim() === '') {
        return users;
    }
    return users.filter(u => matches(u.firstName) || matches(u.lastName));
}

@Component({
    selector: 'gtx-assign-page-modal',
    templateUrl: './assign-page-modal.component.html',
    styleUrls: ['./assign-page-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssignPageModal extends BaseModal<void> implements OnInit, OnDestroy {

    @Input()
    public pages: Page[] = [];

    public loadedUsers: User[] | null = null;
    public filteredUsers: User[] = [];

    public loading = false;
    public selected: number[] = [];

    public message = '';
    public filterTerm = '';

    public loadError = false;

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private client: GCMSRestClientService,
        private publishQueueActions: PublishQueueActionsService,
        private folderActions: FolderActionsService,
        private notification: I18nNotification,
    ) {
        super();
    }

    ngOnInit(): void {
        this.subscriptions.push(this.client.user.list().subscribe({
            next: res => {
                this.loadedUsers = res.items;
                this.filteredUsers = filterUsers(res.items, this.filterTerm);
                this.changeDetector.markForCheck();
            },
            error: error => {
                this.loadError = true;
                this.notification.show({
                    message: 'message.get_users_error',
                    type: 'alert',
                    delay: 2000,
                });
            },
        }));
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    public updateFilterTerm(term: string): void {
        this.filterTerm = term;
        this.filteredUsers = filterUsers(this.loadedUsers, this.filterTerm);
    }

    /**
     * Assign the pages to the selected users for revision.
     */
    public assign(): void {
        if (this.loading) {
            return;
        }

        this.loading = true;

        const pageIds = this.pages.map(p => p.id);
        const userIds = this.selected;
        const message = this.message;

        this.publishQueueActions.assignToUsers(pageIds, userIds, message)
            .then(assigned => {
                if (assigned) {
                    this.folderActions.refreshList('page');
                    this.closeFn(null);
                }
                this.loading = false;
            })
            .catch(err => {
                this.loading = false;
            });
    }
}

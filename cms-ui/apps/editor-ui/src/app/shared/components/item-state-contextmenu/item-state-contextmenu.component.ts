import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { ItemState } from '@editor-ui/app/common/models';
import { Item, Language, Page, TimeManagement, User } from '@gentics/cms-models';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { getFormattedTimeMgmtValue } from '../../../core/providers/i18n/i18n-utils';
import { I18nService } from '../../../core/providers/i18n/i18n.service';
import { I18nDatePipe } from '../../../shared/pipes/i18n-date/i18n-date.pipe';
import { ApplicationStateService, FolderActionsService } from '../../../state';

const SECONDS_PER_MINUTE = 60;
const SECONDS_PER_HOUR = SECONDS_PER_MINUTE * 60;
const SECONDS_PER_DAY = SECONDS_PER_HOUR * 24;

/**
 * A context menu providing all relevant state information about a page.
 */
@Component({
    selector: 'item-state-contextmenu',
    templateUrl: './item-state-contextmenu.component.html',
    styleUrls: ['./item-state-contextmenu.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [I18nDatePipe],
    standalone: false
})
export class ItemStateContextMenuComponent {

    @Input()
    public item: Item;

    @Input()
    public activeNodeId: number;

    @Input()
    public nodeLanguages: Language[] = [];

    @Input()
    public state: ItemState;

    constructor(
        private appState: ApplicationStateService,
        private folderActions: FolderActionsService,
        private i18n: I18nService,
        private i18nDate: I18nDatePipe,
    ) {}

    getUserById$(id: number): Observable<User> {
        return this.appState.select(state => state.entities.user).pipe(
            map(allUsers => allUsers[id]),
        );
    }

    getUserDisplayName$(value: number | User): Observable<string> {
        if (value == null) {
            return of(null);
        }

        if (
            typeof value === 'object'
            && typeof value.firstName === 'string'
            && typeof value.lastName === 'string'
        ) {
            return of(`${value.firstName} ${value.lastName}`);
        }

        return this.getUserById$(typeof value === 'number' ? value : value.id).pipe(
            map(user => `${user.firstName} ${user.lastName}`),
        );
    }

    getFormattedTimeMgmtValue$(page: Page, field: keyof TimeManagement): Observable<string | boolean> {
        if (!this.activeNodeId) {
            return of(false);
        }
        return getFormattedTimeMgmtValue(page, field, this.activeNodeId, this.i18n, this.i18nDate, this.folderActions);
    }
}

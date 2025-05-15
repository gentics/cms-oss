import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { ContextMenuOperationsService } from '@editor-ui/app/core/providers/context-menu-operations/context-menu-operations.service';
import { Language, Page, TimeManagement, User } from '@gentics/cms-models';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { getFormattedTimeMgmtValue } from '../../../core/providers/i18n/i18n-utils';
import { I18nService } from '../../../core/providers/i18n/i18n.service';
import { I18nDatePipe } from '../../../shared/pipes/i18n-date/i18n-date.pipe';
import { ApplicationStateService, FolderActionsService } from '../../../state';
import { PageLanguageIndicatorComponent } from '../page-language-indicator/page-language-indicator.component';

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
export class ItemStateContextMenuComponent extends PageLanguageIndicatorComponent implements OnInit {

    /** Indicating the current language version of the page */
    displayLanguage$: Observable<Language>;

    /** CONSTRUCTOR */
    constructor(
        appState: ApplicationStateService,
        folderActions: FolderActionsService,
        contextMenuOperations: ContextMenuOperationsService,
        private i18n: I18nService,
        private i18nDate: I18nDatePipe,
    ) {
        super(
            appState,
            folderActions,
            contextMenuOperations,
        );
    }

    /** On component initialization */
    ngOnInit(): void {
        super.ngOnInit();
    }

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

    /**
     * Returns a string difference between now and the "previous" timestamp.
     */
    timeAgo(previous: number): string {
        const current = Math.round(Date.now() / 1000);


        const elapsed = current - previous;
        const parts = {
            elapsed: 0,
            key: '',
        };

        if (elapsed < SECONDS_PER_MINUTE) {
            parts.elapsed = elapsed;
            parts.key = 1 < elapsed ? 'editor.time_ago_seconds' : 'editor.time_ago_second';
        } else if (elapsed < SECONDS_PER_HOUR) {
            const minutes = Math.round(elapsed / SECONDS_PER_MINUTE);
            parts.elapsed = minutes;
            parts.key = 1 < minutes ? 'editor.time_ago_minutes' : 'editor.time_ago_minute';
        } else if (elapsed < SECONDS_PER_DAY) {
            const hours = Math.round(elapsed / SECONDS_PER_HOUR);
            parts.elapsed = hours;
            parts.key = 1 < hours ? 'editor.time_ago_hours' : 'editor.time_ago_hour';
        } else {
            const days = Math.round(elapsed / SECONDS_PER_DAY);
            parts.elapsed = days;
            parts.key = 1 < days ? 'editor.time_ago_days' : 'editor.time_ago_day';
        }

        return this.i18n.translate(parts.key, { elapsed: parts.elapsed });
    }

    getFormattedTimeMgmtValue$(page: Page, field: keyof TimeManagement): Observable<string | boolean> {
        if (!this.activeNodeId) {
            return of(false);
        }
        return getFormattedTimeMgmtValue(page, field, this.activeNodeId, this.i18n, this.i18nDate, this.folderActions);
    }

}

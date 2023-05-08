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

/**
 * A context menu providing all relevant state information about a page.
 */
@Component({
    selector: 'item-state-contextmenu',
    templateUrl: './item-state-contextmenu.component.html',
    styleUrls: ['./item-state-contextmenu.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [I18nDatePipe],
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
            map(allUsers => allUsers[id])
        );
    }

    /**
     * Returns a string difference between now and the "previous" timestamp.
     */
    timeAgo(previous: number): string {
        const current = Math.round(Date.now() / 1000);
        const secondsPerMinute = 60;
        const secondsPerHour = secondsPerMinute * 60;
        const secondsPerDay = secondsPerHour * 24;

        const elapsed = current - previous;
        let parts = {
            elapsed: 0,
            key: '',
        };

        if (elapsed < secondsPerMinute) {
            parts.elapsed = elapsed;
            parts.key = 1 < elapsed ? 'editor.time_ago_seconds' : 'editor.time_ago_second';
        } else if (elapsed < secondsPerHour) {
            const minutes = Math.round(elapsed / secondsPerMinute);
            parts.elapsed = minutes;
            parts.key = 1 < minutes ? 'editor.time_ago_minutes' : 'editor.time_ago_minute';
        } else if (elapsed < secondsPerDay) {
            const hours = Math.round(elapsed / secondsPerHour);
            parts.elapsed = hours;
            parts.key = 1 < hours ? 'editor.time_ago_hours' : 'editor.time_ago_hour';
        } else {
            const days = Math.round(elapsed / secondsPerDay);
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

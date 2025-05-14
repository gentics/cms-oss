import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { ContextMenuOperationsService } from '@editor-ui/app/core/providers/context-menu-operations/context-menu-operations.service';
import { Language } from '@gentics/cms-models';
import { isEqual } from 'lodash-es';
import { Observable, combineLatest } from 'rxjs';
import { distinctUntilChanged, filter, map, mergeMap, publishReplay, refCount } from 'rxjs/operators';
import { ApplicationStateService, FolderActionsService } from '../../../state';
import { PageLanguageIndicatorComponent } from '../page-language-indicator/page-language-indicator.component';

/**
 * A component which displays the current language of a page and (optionally) a page's status icons.
 */
@Component({
    selector: 'page-language-indicator-current',
    templateUrl: './page-language-indicator-current.component.html',
    styleUrls: ['./page-language-indicator-current.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class PageLanguageIndicatorCurrentComponent extends PageLanguageIndicatorComponent implements OnInit {

    /** Is TRUE if page translation is published */
    displaySingleLanguagePublished$: Observable<boolean>;
    /** Is TRUE if page translation is not published */
    displaySingleLanguageUnpublished$: Observable<boolean>;

    /** Indicating the current language version of the page */
    displayLanguage$: Observable<Language>;

    /** CONSTRUCTOR */
    constructor(
        appState: ApplicationStateService,
        folderActions: FolderActionsService,
        contextMenuOperations: ContextMenuOperationsService,
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

        // get page translation of current language
        this.displayLanguage$ = combineLatest([
            this.item$.pipe(
                map(page => page.language),
                distinctUntilChanged(isEqual),
            ),
            this.itemLanguages$.pipe(
                distinctUntilChanged(isEqual),
            ),
            this.currentLanguage$.pipe(
                distinctUntilChanged(isEqual),
            ),
        ]).pipe(
            map(([currentPageLanguage, pageLanguages, currentLanguage]: [string, Language[], Language]) => {
                if (pageLanguages?.length > 0) {
                    const found = pageLanguages.find(lang => lang.code === currentPageLanguage);
                    if (found) {
                        return found;
                    }
                }
                return currentLanguage || {} as Language;
            }),
            publishReplay(1),
            refCount(),
        );

        const langPublished$ = this.displayLanguage$.pipe(
            filter(lang => !!lang),
            mergeMap(lang => this.statePublished$(lang.code)),
        );

        // get if page translation is published
        this.displaySingleLanguagePublished$ = combineLatest([
            this.isMultiLanguage$,
            langPublished$,
        ]).pipe(
            map(([isMultiLanguage, langPublished]) => !isMultiLanguage && langPublished),
        );

        // get if page translation is not published
        this.displaySingleLanguageUnpublished$ = combineLatest([
            this.isMultiLanguage$,
            langPublished$,
        ]).pipe(
            map(([isMultiLanguage, langPublished]) => !isMultiLanguage && !langPublished),
        );
    }
}

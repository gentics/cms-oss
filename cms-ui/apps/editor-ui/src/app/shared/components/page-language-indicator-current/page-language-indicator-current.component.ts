import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { ContextMenuOperationsService } from '@editor-ui/app/core/providers/context-menu-operations/context-menu-operations.service';
import { Language } from '@gentics/cms-models';
import { combineLatest } from 'rxjs';
import { Observable } from 'rxjs/Observable';
import { map, switchMap } from 'rxjs/operators';
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
        this.displayLanguage$ = combineLatest([this.item$, this.itemLanguages$, this.currentLanguage$]).pipe(
            map(([page, pageLanguages, currentLanguage]) => pageLanguages[page.id] || currentLanguage || {} as Language)
        );

        // get if page translation is published
        this.displaySingleLanguagePublished$ = combineLatest([this.isMultiLanguage$, this.displayLanguage$]).pipe(
            map(([isMultiLanguage, displayLanguage]) => {
                return displayLanguage && this.statePublished$(displayLanguage.code)
                    .pipe(map(statePublished => !isMultiLanguage && statePublished === true));
            }),
            switchMap(displaySingleLanguagePublished => displaySingleLanguagePublished),
        );

        // get if page translation is not published
        this.displaySingleLanguageUnpublished$ = combineLatest([this.isMultiLanguage$, this.displayLanguage$]).pipe(
            map(([isMultiLanguage, displayLanguage]) => {
                return displayLanguage && this.statePublished$(displayLanguage.code)
                    .pipe(map(statePublished => !isMultiLanguage && statePublished === false));
            }),
            switchMap(displaySingleLanguageUnpublished => displaySingleLanguageUnpublished),
        );
    }
}

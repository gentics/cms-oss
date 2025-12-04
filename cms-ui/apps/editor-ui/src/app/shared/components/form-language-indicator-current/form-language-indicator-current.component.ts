import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Language } from '@gentics/cms-models';
import { Observable, combineLatest, forkJoin, of } from 'rxjs';
import { first, map, mergeMap, switchMap, tap } from 'rxjs/operators';
import { ContextMenuOperationsService } from '../../../core/providers/context-menu-operations/context-menu-operations.service';
import { ApplicationStateService, FolderActionsService } from '../../../state';
import { FormLanguageIndicatorComponent } from '../form-language-indicator/form-language-indicator.component';

/**
 * A component which displays the current language of a form and (optionally) a form's status icons.
 */
@Component({
    selector: 'form-language-indicator-current',
    templateUrl: './form-language-indicator-current.component.html',
    styleUrls: ['./form-language-indicator-current.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class FormLanguageIndicatorCurrentComponent extends FormLanguageIndicatorComponent implements OnInit {

    /** Is TRUE if form translation is published */
    displaySingleLanguagePublished$: Observable<boolean>;
    /** Is TRUE if form translation is not published */
    displaySingleLanguageUnpublished$: Observable<boolean>;

    /** Indicating the current language version of the form */
    displayLanguage$: Observable<Language>;

    /** CONSTRUCTOR */
    constructor(
        appState: ApplicationStateService,
        folderActions: FolderActionsService,
        contextMenuOpertations: ContextMenuOperationsService,
        private changeDetectorRef: ChangeDetectorRef,
    ) {
        super(
            appState,
            folderActions,
            contextMenuOpertations,
        );
    }

    /** On component initialization */
    ngOnInit(): void {
        super.ngOnInit();

        // get form translation of current language
        this.displayLanguage$ = combineLatest([this.item$, this.activeFolderLanguage$]).pipe(
            mergeMap(([form, currentLanguage]) => {
                if (currentLanguage && form.languages.includes(currentLanguage.code)) {
                    return of(currentLanguage);
                }
                const fallbackLanguageCodes = Array.isArray(form.languages) && form.languages.length > 0 && form.languages;
                if (!fallbackLanguageCodes) {
                    throw new Error(`Form with ID ${form.id} has no translation defined in form.languages.`);
                }

                return forkJoin(form.languages
                    .filter(l => this.nodeLanguages.map(nl => nl.code).includes(l))
                    .map(formlanguageInNodeCode => {
                        return this.appState.select(state => state.entities.language).pipe(
                            first(),
                            map(statelanguages => {
                                return Object.values(statelanguages).find(stateLanguage => stateLanguage.code === formlanguageInNodeCode);
                            }),
                        )
                    }),
                ).pipe(
                    map((fallbackLanguages: Language[]) => {
                        if (Array.isArray(fallbackLanguages) && fallbackLanguages.length > 0) {
                            return fallbackLanguages[0];
                        } else {
                            throw new Error(`Form with ID ${form.id} has no translation in a language which is configured for Node with ID ${this.activeNodeId}.`);
                        }
                    }),
                );
            }),
            tap(() => this.changeDetectorRef.markForCheck()),
        );

        // get if form translation is published
        this.displaySingleLanguagePublished$ = combineLatest([this.isMultiLanguage$, this.displayLanguage$]).pipe(
            map(([isMultiLanguage, displayLanguage]) => {
                return displayLanguage && this.statePublished$(displayLanguage.code)
                    .pipe(
                        map(statePublished => !isMultiLanguage && statePublished === true),
                    );
            }),
            switchMap(displaySingleLanguagePublished => displaySingleLanguagePublished),
        );

        // get if form translation is not published
        this.displaySingleLanguageUnpublished$ = combineLatest([this.isMultiLanguage$, this.displayLanguage$]).pipe(
            map(([isMultiLanguage, displayLanguage]) => {
                return displayLanguage && this.statePublished$(displayLanguage.code)
                    .pipe(
                        map(statePublished => !isMultiLanguage && statePublished === false),
                    );
            }),
            switchMap(displaySingleLanguageUnpublished => displaySingleLanguageUnpublished),
        );
    }
}

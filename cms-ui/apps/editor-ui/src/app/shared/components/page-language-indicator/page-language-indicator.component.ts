import { animate, animateChild, query, style, transition, trigger } from '@angular/animations';
import {
    ChangeDetectionStrategy,
    Component,
    OnChanges,
    OnDestroy,
    OnInit,
    SimpleChange,
} from '@angular/core';
import { ContextMenuOperationsService } from '@editor-ui/app/core/providers/context-menu-operations/context-menu-operations.service';
import { IndexById, Language, Normalized, Page, Raw } from '@gentics/cms-models';
import { combineLatest, Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { ApplicationStateService, FolderActionsService } from '../../../state';
import { PublishableStateUtil } from '../../util/entity-states';
import { BaseLanguageIndicatorComponent } from '../base-language-indicator/base-language-indicator.component';

/**
 * A component which displays the available languages for a page and (optionally) a page's status icons.
 */
@Component({
    selector: 'page-language-indicator',
    templateUrl: './page-language-indicator.component.html',
    styleUrls: ['./page-language-indicator.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    animations: [
        trigger('animNgForParent', [
            transition(':enter, :leave', [
                query('@animNgForChild', [animateChild()], { optional: true }),
            ]),
        ]),
        trigger('animNgForChild', [
            transition('void => *', [
                style({
                    opacity: 0,
                    width: '0',
                    'padding-left': '*',
                    'padding-right': '*',
                    'margin-left': '*',
                    'margin-right': '*',
                }),
                animate('0.2s ease-in-out', style({
                    opacity: 1,
                    width: '*',
                    'padding-left': '*',
                    'padding-right': '*',
                    'margin-left': '*',
                    'margin-right': '*',
                })),
            ]),
            transition('* => void', [
                style({
                    opacity: 1,
                    width: '*',
                    'padding-left': '*',
                    'padding-right': '*',
                    'margin-left': '*',
                    'margin-right': '*',
                }),
                animate('0.2s ease-in-out', style({
                    opacity: 0,
                    width: '0',
                    'padding-left': '*',
                    'padding-right': '*',
                    'margin-left': '*',
                    'margin-right': '*',
                })),
            ]),
        ]),
    ],
    standalone: false
})
export class PageLanguageIndicatorComponent
    extends BaseLanguageIndicatorComponent<Page<Normalized>>
    implements OnInit, OnChanges, OnDestroy {

    /** CONSTRUCTOR */
    constructor(
        appState: ApplicationStateService,
        folderActions: FolderActionsService,
        protected contextMenuOperations: ContextMenuOperationsService,
    ) {
        super('page', appState, folderActions)
    }

    /** On component initialization */
    ngOnInit(): void {
        super.ngOnInit();

        // get all translations available
        this.currentLanguage$ = this.item$.pipe(
            map(page => this.nodeLanguages.filter(l => page && l.id === page.contentGroupId)[0]),
        );

        // get existing page translations
        this.itemLanguages$ = this.item$.pipe(
            map(page => this.nodeLanguages.filter(nodeLanguage => this.isPageLanguage(nodeLanguage, page))),
        );

        super.afterLanguageInit();
    }

    ngOnChanges(changes: { [K in keyof this]?: SimpleChange }): void {
        if (changes.item) {
            this.item$.next(this.item);
        }

        // check for multiple languages available for current node
        if (changes.nodeLanguages && changes.nodeLanguages.currentValue) {
            this.isMultiLanguage$.next(1 < this.nodeLanguages.length);
        }
    }

    /**
     * On component destruction
     */
    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }

    /**
     * Predicate function that returns true if the given language is one of the pages language variants.
     */
    isPageLanguage(language: Language, page: Page<Raw> | Page<Normalized>): boolean {
        if (page && page.languageVariants) {
            const pageLanguageIds = Object.keys(page.languageVariants).map(id => Number(id));
            // get IDs of page translations
            this.languageVariantsIds$.next(
                Object.keys(page.languageVariants)
                    .map(id => {
                        // if is RAW
                        if ((page.languageVariants as IndexById<Page<Raw>>)[id] instanceof Object) {
                            return (page.languageVariants as IndexById<Page<Raw>>)[id].id;
                            // else if is NORMALIZED
                        } else {
                            return Number((page.languageVariants as IndexById<number>)[id]);
                        }
                    }),
            );
            return -1 < pageLanguageIds.indexOf(language.id);
        } else if (page && page.language) {
            return page.language === language.code;
        } else {
            return false;
        }
    }

    /**
     * @param langCode of the page translations to be returned, e. g. 'en' or 'de'
     * @returns data object of page in defined language
     */
    getPageLanguageVariantOfLanguage$(langCode: string): Observable<Page> {
        return combineLatest([this.languageVariants$, this.item$]).pipe(
            map(([languageVariants, page]) => {
                // if there are existing translations this.languageVariants will contain them
                if (Object.getOwnPropertyNames(languageVariants).length > 0) {
                    const languageVariantKey = Object.keys(languageVariants)
                        .find(key => languageVariants[key].language === langCode);
                    return languageVariants[languageVariantKey];
                } else {
                    // just return the only translation existing which is the page itself
                    return page;
                }
            }),
            filter(page => !!page),
        );
    }

    /**
     * @param langCode of the page to be checked, e. g. 'en' or 'de'
     * @returns TRUE if the language variant of this page has been deleted
     * and UI shall display deleted objects
     */
    stateDeleted$(langCode: string): Observable<boolean> {
        return this.getPageLanguageVariantOfLanguage$(langCode).pipe(
            map(languageVariantOfLanguage => {
                return PublishableStateUtil.stateDeleted(languageVariantOfLanguage);
            }),
        );
    }

    /**
     * @param langCode of the page to be checked, e. g. 'en' or 'de'
     * @returns TRUE if the language variant of this page is online
     */
    statePublished$(langCode: string): Observable<boolean> {
        return this.getPageLanguageVariantOfLanguage$(langCode).pipe(
            map(languageVariantOfLanguage => {
                return PublishableStateUtil.statePublished(languageVariantOfLanguage);
            }),
        );
    }

    /**
     * @param langCode of the page to be checked, e. g. 'en' or 'de'
     * @returns TRUE if the language variant of this page has been edited by a user
     */
    stateModified$(langCode: string): Observable<boolean> {
        return this.getPageLanguageVariantOfLanguage$(langCode).pipe(
            map(languageVariantOfLanguage => {
                return PublishableStateUtil.stateModified(languageVariantOfLanguage);
            }),
        );
    }

    /**
     * @param langCode of the page to be checked, e. g. 'en' or 'de'
     * @returns TRUE if the language variant of this page has been requested for release
     */
    stateInQueue$(langCode: string): Observable<boolean> {
        return this.getPageLanguageVariantOfLanguage$(langCode).pipe(
            map(languageVariantOfLanguage => {
                return PublishableStateUtil.stateInQueue(languageVariantOfLanguage);
            }),
        );
    }

    /**
     * @param langCode of the page to be checked, e. g. 'en' or 'de'
     * @returns TRUE if the language variant of this page is scheduled for an automated action
     */
    statePlanned$(langCode: string): Observable<boolean> {
        return this.getPageLanguageVariantOfLanguage$(langCode).pipe(
            map(languageVariantOfLanguage => {
                return PublishableStateUtil.statePlanned(languageVariantOfLanguage);
            }),
        );
    }

    /**
     * @param langCode of the page to be checked, e. g. 'en' or 'de'
     * @returns TRUE if the language variant of this page is inherited from a master node page language variant
     */
    stateInherited$(langCode: string): Observable<boolean> {
        return this.getPageLanguageVariantOfLanguage$(langCode).pipe(
            map(languageVariantOfLanguage => {
                return PublishableStateUtil.stateInherited(languageVariantOfLanguage);
            }),
        );
    }

    /**
     * @param langCode of the page to be checked, e. g. 'en' or 'de'
     * @returns TRUE if the language variant of this page is inherited but has autonomous content
     */
    stateLocalized$(langCode: string): Observable<boolean> {
        return this.getPageLanguageVariantOfLanguage$(langCode).pipe(
            map(languageVariantOfLanguage => {
                if (!languageVariantOfLanguage || languageVariantOfLanguage.inherited) {
                    return false;
                }
                return PublishableStateUtil.stateLocalized(languageVariantOfLanguage);
            }),
        );
    }

    /**
     * Returns true if the page is available in the given language.
     */
    isAvailable$(language: Language): Observable<boolean> {
        return this.itemLanguages$.pipe(
            map(pageLanguages => -1 < pageLanguages.indexOf(language)),
        );
    }

    onStageLanguageClick(language: Language, page: Page): void {
        if (this.stagingMap?.[page.globalId]?.included) {
            this.contextMenuOperations.unstageItemFromCurrentPackage(page);
        } else {
            this.contextMenuOperations.stageItemToCurrentPackage(page);
        }
    }
}

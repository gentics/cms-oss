import { animate, animateChild, query, style, transition, trigger } from '@angular/animations';
import { Component, OnInit } from '@angular/core';
import { Form, Language, Normalized, Raw } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { map, withLatestFrom } from 'rxjs/operators';
import { ContextMenuOperationsService } from '../../../core/providers/context-menu-operations/context-menu-operations.service';
import { ApplicationStateService, FolderActionsService } from '../../../state';
import { PublishableStateUtil } from '../../util/entity-states';
import { BaseLanguageIndicatorComponent } from '../base-language-indicator/base-language-indicator.component';

/**
 * A component which displays the available languages for a form and (optionally) a form's status icons.
 */
@Component({
    selector: 'form-language-indicator',
    templateUrl: './form-language-indicator.component.html',
    styleUrls: ['./form-language-indicator.component.scss'],
    animations: [
        trigger('animNgForParent', [
            transition(':enter, :leave', [
                query('@animNgForChild', [
                    animateChild(),
                ]),
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
export class FormLanguageIndicatorComponent
    extends BaseLanguageIndicatorComponent<Form<Normalized>>
    implements OnInit {

    /** CONSTRUCTOR */
    constructor(
        appState: ApplicationStateService,
        folderActions: FolderActionsService,
        protected contextMenuOperations: ContextMenuOperationsService,
    ) {
        super('form', appState, folderActions)
    }

    /** On component initialization */
    ngOnInit(): void {
        super.ngOnInit();

        // get all translations available
        this.currentLanguage$ = this.item$.pipe(
            withLatestFrom(this.activeFolderLanguage$),
            map(([item, activeFolderLanguage]) => item && item.languages.includes(activeFolderLanguage.code) && activeFolderLanguage),
        );

        // get existing form translations
        this.itemLanguages$ = this.item$.pipe(
            map(item => this.nodeLanguages.filter(nodeLanguage => this.isFormLanguage(nodeLanguage, item))),
        );

        super.afterLanguageInit();
    }

    /**
     * Predicate function that returns true if the given language is one of the forms language variants.
     */
    isFormLanguage(language: Language, form: Form<Raw> | Form<Normalized>): boolean {
        return form.languages.some((l: string) => l === language.code);
    }

    /**
     * @param langCode of the form to be checked, e. g. 'en' or 'de'
     * @returns TRUE if the language variant of this form has been deleted
     * and UI shall display deleted objects
     */
    stateDeleted$(langCode: string): Observable<boolean> {
        return this.item$.pipe(
            map(item => PublishableStateUtil.stateDeleted(item)),
        );
    }

    /**
     * @param langCode of the form to be checked, e. g. 'en' or 'de'
     * @returns TRUE if the language variant of this form is online
     */
    statePublished$(langCode: string): Observable<boolean> {
        return this.item$.pipe(
            map(item => PublishableStateUtil.statePublished(item)),
        );
    }

    /**
     * @param langCode of the form to be checked, e. g. 'en' or 'de'
     * @returns TRUE if the language variant of this form has been edited by a user
     */
    stateModified$(langCode: string): Observable<boolean> {
        return this.item$.pipe(
            map(form => PublishableStateUtil.stateModified(form)),
        );
    }

    /**
     * @param langCode of the form to be checked, e. g. 'en' or 'de'
     * @returns TRUE if the language variant of this form has been requested for release
     */
    stateInQueue$(langCode: string): Observable<boolean> {
        return this.item$.pipe(
            map(form => PublishableStateUtil.stateInQueue(form)),
        );
    }

    /**
     * @param langCode of the form to be checked, e. g. 'en' or 'de'
     * @returns TRUE if the language variant of this form is scheduled for an automated action
     */
    statePlanned$(langCode: string): Observable<boolean> {
        return this.item$.pipe(
            map(form => PublishableStateUtil.statePlanned(form)),
        );
    }

    /**
     * Returns true if the form is available in the given language.
     */
    isAvailable$(language: Language): Observable<boolean> {
        return this.itemLanguages$.pipe(
            map(formLanguages => formLanguages.includes(language)),
        );
    }

    onStageLanguageClick(language: Language): void {
        if (this.stagingMap?.[this.item.globalId]?.included) {
            this.contextMenuOperations.unstageItemFromCurrentPackage(this.item);
        } else {
            this.contextMenuOperations.stageItemToCurrentPackage(this.item, false);
        }
    }
}

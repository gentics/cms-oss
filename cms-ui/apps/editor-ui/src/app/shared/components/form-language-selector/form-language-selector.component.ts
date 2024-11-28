import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChange, ViewChild } from '@angular/core';
import { Form, Language } from '@gentics/cms-models';
import { SelectComponent } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { mergeMap, tap } from 'rxjs/operators';
import { I18nService } from '../../../core/providers/i18n/i18n.service';
import { ApplicationStateService } from '../../../state';

@Component({
    selector: 'form-language-selector',
    templateUrl: './form-language-selector.component.html',
    styleUrls: ['./form-language-selector.scss'],
})
export class FormLanguageSelectorComponent implements OnInit, OnChanges {

    @Input()
    label = '';

    @Input()
    item: Form;

    @Input()
    variants: string[];

    @Input()
    selected: string[];

    @Input()
    activeLanguage: Language;

    @Output()
    selectionChange = new EventEmitter<string[]>();

    @ViewChild(SelectComponent)
    langSelector: SelectComponent;

    activeFolderLanguage$: Observable<Language>;
    private activeFolderLanguage: Language;

    constructor(
        private i18n: I18nService,
        private appState: ApplicationStateService,
    ) { }

    get toggleTitle(): string {
        if (this.allVariantsSelected()) {
            if (this.canSelectNone()) {
                return 'modal.form_select_no_language';
            } else {
                return 'modal.form_select_current_language';
            }
        } else {
            return 'modal.form_select_all_languages';
        }
    }

    ngOnChanges(changes: { [K in keyof FormLanguageSelectorComponent]: SimpleChange }): void {
        if (changes.selected) {
            this.onSelectChange(this.selected);
        }
    }

    ngOnInit(): void {
        this.showPlaceholder();

        this.activeFolderLanguage$ = this.appState.select(state => state.folder.activeLanguage).pipe(
            mergeMap((activeLanguageId: number) => this.appState.select(state => state.entities.language[activeLanguageId])),
            tap((activeFolderLanguage: Language) => {
                this.activeFolderLanguage = activeFolderLanguage;
            }),
        );
    }

    onSelectChange(e: string[]): void {
        this.showPlaceholder();
        this.selectionChange.emit(e);
    }

    /**
     * Hacky way to implement placeholder for the Select box. It receives the list of elements,
     * and if its empty then it will change the Select box viewValue.
     * setTimeout required for update after the Select box sets the displayed value itself.
     *
     * TODO: Implement placeholder function in GUIC
     *
     * @param items The selected items of the Select box
     */
    showPlaceholder(): void {
        setTimeout(() => {
            if ( this.selected.length === 0 ) {
                this.langSelector.viewValue = this.i18n.translate('common.select_placeholder');
            }
        });
    }

    /**
     * Returns true if all the language variants of the current form are selected.
     */
    allVariantsSelected(): boolean {
        return this.selected.length === this.variants.length;
    }

    /**
     * Returns true if it has only one language.
     */
    canSelectNone(): boolean {
        if (this.activeFolderLanguage) {
            return !this.variants.includes(this.activeFolderLanguage.code) || this.variants.length <= 1;
        } else {
            return this.variants.length <= 1;
        }
    }

    /**
     * If all language variants of the form are already selected, select just the current language. Else select
     * all variants or none.
     */
    toggleLanguageVariantSelection(): void {
        if (this.allVariantsSelected()) {
            if (this.canSelectNone() || !this.activeFolderLanguage) {
                this.selected = [];
            } else {
                this.selected = [this.activeFolderLanguage.code];
            }
        } else {
            this.selected = [...this.variants];
        }

        this.onSelectChange(this.selected);
    }
}

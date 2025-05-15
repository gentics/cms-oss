import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChange, ViewChild } from '@angular/core';
import { Language, Page } from '@gentics/cms-models';
import { ChangesOf, SelectComponent } from '@gentics/ui-core';
import { I18nService } from '../../../core/providers/i18n/i18n.service';

@Component({
    selector: 'page-language-selector',
    templateUrl: './page-language-selector.component.html',
    styleUrls: ['./page-language-selector.scss'],
    standalone: false
})
export class PageLanguageSelector implements OnInit, OnChanges {

    @Input()
    label = '';

    @Input()
    page: Page;

    @Input()
    variants: Page[];

    @Input()
    selected: number[];

    @Input()
    activeLanguage: Language;

    @Output()
    selectionChange = new EventEmitter<number[]>();

    @ViewChild(SelectComponent)
    langSelector: SelectComponent;

    constructor(
        private i18n: I18nService,
    ) {}

    get toggleTitle(): string {
        if (this.allVariantsSelected()) {
            if (this.canSelectNone()) {
                return 'modal.page_select_no_language';
            } else {
                return 'modal.page_select_current_language';
            }
        } else {
            return 'modal.page_select_all_languages';
        }
    }

    ngOnChanges(changes: ChangesOf<this>): void {
        if (changes.selected) {
            this.onSelectChange(this.selected);
        }
    }

    ngOnInit(): void {
        this.showPlaceholder();
    }

    onSelectChange(e: number[]): void {
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
            if (this.selected.length === 0) {
                this.langSelector.viewValue = this.i18n.translate('common.select_placeholder');
            }
        });
    }

    /**
     * Returns true if all the language variants of the current page are selected.
     */
    allVariantsSelected(): boolean {
        return this.selected.length === this.variants.length;
    }

    /**
     * Returns true if the current language is not the page language, or it has only one language.
     */
    canSelectNone(): boolean {
        if (this.activeLanguage) {
            return !(this.activeLanguage.code === this.page.language && this.variants.length > 1);
        } else {
            return false;
        }
    }

    /**
     * If all language variants of the page are already selected, select just the current language. Else select
     * all variants or none.
     */
    toggleLanguageVariantSelection(): void {
        if (this.allVariantsSelected()) {
            if (this.canSelectNone()) {
                this.selected = [];
            } else {
                this.selected = [this.page.id];
            }
        } else {
            this.selected = this.variants.map(item => item.id);
        }

        this.onSelectChange(this.selected);
    }
}

import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
} from '@angular/core';
import { Form, Language } from '@gentics/cms-models';
import { ChangesOf } from '@gentics/ui-core';
import { Subscription } from 'rxjs';
import { mergeMap } from 'rxjs/operators';
import { ApplicationStateService } from '../../../state';

@Component({
    selector: 'gtx-form-language-selector',
    templateUrl: './form-language-selector.component.html',
    styleUrls: ['./form-language-selector.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FormLanguageSelectorComponent implements OnInit, OnChanges, OnDestroy {

    @Input()
    public label = '';

    @Input()
    public item: Form;

    @Input()
    public variants: string[];

    @Input()
    public selection: string[];

    @Input()
    public activeLanguage: Language;

    @Output()
    public selectionChange = new EventEmitter<string[]>();

    public activeFolderLanguage: Language;
    public toggleTitle: string;
    public allVariantsSelected: boolean;
    public canSelectNone: boolean;

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private appState: ApplicationStateService,
    ) { }

    ngOnInit(): void {
        this.updateLocalState();

        this.subscriptions.push(this.appState.select(state => state.folder.activeLanguage).pipe(
            mergeMap((activeLanguageId: number) => this.appState.select(state => state.entities.language[activeLanguageId])),
        ).subscribe(activeLang => {
            this.activeFolderLanguage = activeLang;
            this.updateLocalState();
            this.changeDetector.markForCheck();
        }));
    }

    ngOnChanges(changes: ChangesOf<this>): void {
        if (changes.selection) {
            this.onSelectChange(this.selection);
        }
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    private updateLocalState(): void {
        this.allVariantsSelected = this.selection.length === this.variants.length;
        this.canSelectNone = !this.activeFolderLanguage || !this.variants.includes(this.activeFolderLanguage.code);

        if (this.allVariantsSelected) {
            if (this.canSelectNone) {
                this.toggleTitle = 'modal.form_select_no_language';
            } else {
                this.toggleTitle = 'modal.form_select_current_language';
            }
        } else {
            this.toggleTitle = 'modal.form_select_all_languages';
        }
    }

    onSelectChange(selection: string[]): void {
        this.selection = selection;
        this.updateLocalState();
        this.selectionChange.emit(selection);
    }

    /**
     * If all language variants of the form are already selected, select just the current language. Else select
     * all variants or none.
     */
    toggleLanguageVariantSelection(): void {
        if (this.allVariantsSelected) {
            if (this.canSelectNone || !this.activeFolderLanguage) {
                this.selection = [];
            } else {
                this.selection = [this.activeFolderLanguage.code];
            }
        } else {
            this.selection = [...this.variants];
        }

        this.updateLocalState();
        this.selectionChange.emit(this.selection);
    }
}

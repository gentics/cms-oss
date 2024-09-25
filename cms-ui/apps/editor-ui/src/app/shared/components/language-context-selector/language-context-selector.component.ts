import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { Language } from '@gentics/cms-models';

@Component({
    selector: 'language-context-selector',
    templateUrl: './language-context-selector.component.html',
    styleUrls: ['./language-context-selector.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LanguageContextSelectorComponent {

    @Input()
    availableLanguages: Language[];

    @Input()
    activeLanguage: Language;

    @Input()
    disabled = false;

    @Output()
    languageChange = new EventEmitter<Language>();


    public languageChangeHandler(language: Language): void {
        this.languageChange.emit(language);
    }

}

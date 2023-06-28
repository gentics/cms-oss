import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { Language } from '@gentics/cms-models';

@Component({
    selector: 'gtx-i18n-panel-group',
    templateUrl: './i18n-panel-group.component.html',
    styleUrls: ['./i18n-panel-group.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class I18nPanelGroupComponent {

    @Input()
    public languages: Language[] = [];

    @Input()
    public invalidLanguages: string[] = [];

    @Input()
    public activeLanguage: Language;

    @Output()
    public activeLanguageChange = new EventEmitter<Language>();

    setActiveLanguage(languageId: number): void {
        const lang = this.languages.find(lang => lang.id === languageId);
        if (!lang) {
            return;
        }
        this.activeLanguageChange.emit(lang);
    }
}

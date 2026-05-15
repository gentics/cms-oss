import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { Language } from '@gentics/cms-models';
import { ChangesOf } from '@gentics/ui-core';

@Component({
    selector: 'gtx-i18n-panel-group',
    templateUrl: './i18n-panel-group.component.html',
    styleUrls: ['./i18n-panel-group.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class I18nPanelGroupComponent implements OnChanges {

    @Input()
    public label: string;

    @Input()
    public languages: Language[] = [];

    @Input()
    public invalidLanguages: string[] = [];

    @Input()
    public activeLanguage: Language;

    @Output()
    public activeLanguageChange = new EventEmitter<Language>();

    ngOnChanges(changes: ChangesOf<this>): void {
        if (changes.languages) {
            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
            const containedBefore = (changes.languages.previousValue || []).find((lang) => lang.id === this.activeLanguage?.id) != null;
            const containsNow = (this.languages || []).find((lang) => lang.id === this.activeLanguage?.id) != null;
            if ((!this.activeLanguage || (containedBefore && !containsNow)) && this.languages?.length > 0) {
                this.activeLanguageChange.emit(this.languages[0]);
            }
        }
    }

    setActiveLanguage(languageId: number): void {
        const lang = this.languages.find((lang) => lang.id === languageId);
        if (!lang) {
            return;
        }
        this.activeLanguageChange.emit(lang);
    }
}

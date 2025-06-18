import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
    selector: 'gtx-mesh-browser-language-switcher',
    templateUrl: './mesh-browser-language-switcher.component.html',
    styleUrls: ['./mesh-browser-language-switcher.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeshBrowserLanguageSwitcherComponent {

    @Input()
    public availableLanguages: Array<string> = [];

    @Input()
    public language: string;

    @Output()
    public languageChange = new EventEmitter<string>();

    public languageChangeHandler(language: string): void {
        this.languageChange.emit(language);
    }
}

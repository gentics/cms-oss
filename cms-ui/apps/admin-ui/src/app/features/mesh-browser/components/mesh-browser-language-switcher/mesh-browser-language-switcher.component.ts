import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, Output } from '@angular/core';
import { MeshBrowserLoaderService } from '../../providers';


@Component({
    selector: 'gtx-mesh-browser-language-switcher',
    templateUrl: './mesh-browser-language-switcher.component.html',
    styleUrls: ['./mesh-browser-language-switcher.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeshBrowserLanguageSwitcherComponent {

    @Input()
    public languages: Array<string> = [];

    @Input()
    public currentLanguage: string;

    @Output()
    public languageChange = new EventEmitter<string>();


    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected loader: MeshBrowserLoaderService,
    ) { }

    public languageChangeHandler(language: string): void {
        this.languageChange.emit(language);
    }

}


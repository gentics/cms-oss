import { I18nNotificationService } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { MeshBrowserLoaderService } from '../../providers';


@Component({
    selector: 'gtx-mesh-browser-language-activation',
    templateUrl: './mesh-browser-language-activation.component.html',
    styleUrls: ['./mesh-browser-language-activation.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeshBrowserLanguageActivationComponent {

    @Input()
    public currentProject: string;

    @Input()
    public activatedLanguages: string[];

    @ViewChild('inputFilter') inputFilter;

    @Output()
    public languageChanged: EventEmitter<void> = new EventEmitter();

    public languages: string[];

    public filteredLanguages: string[];

    public currentLanguage: string;


    constructor(
        protected loader: MeshBrowserLoaderService,
        protected changeDetector: ChangeDetectorRef,
        protected notificationService: I18nNotificationService,
    ) {
        this.init();
    }

    private async init() {
        this.languages = await this.loader.getAllLanguages()
        this.languages.sort((a, b) => a.localeCompare(b))
        this.filteredLanguages = this.languages;
        this.changeDetector.markForCheck();
    }

    public languageChangeHandler(language: string): void {
        this.currentLanguage = language
    }

    public filterLanguageHandler(filterLanguage: string): void {
        this.filteredLanguages = this.languages.filter(language => language.includes(filterLanguage));
    }

    public async activateLanguageHandler(): Promise<void> {
        try {
            await this.loader.activateProjectLanguage(this.currentProject, this.currentLanguage)
            this.notificationService.show({
                type: 'success',
                message: 'mesh.language_assigned_to_project',
            })

            this.currentLanguage = null;
            this.languageChanged.emit();
        }
        catch (error) {
            this.notificationService.show({
                type: 'alert',
                message: error.message,
            })
        }
    }

    public async deactivateLanguageHandler(): Promise<void> {
        try {
            await this.loader.deactivateProjectLanguage(this.currentProject, this.currentLanguage)
            this.notificationService.show({
                type: 'success',
                message: 'mesh.language_unassigned_from_project',
            })

            this.currentLanguage = null;
            this.languageChanged.emit();
        }
        catch (error) {
            this.notificationService.show({
                type: 'alert',
                message: error.message,
            })
        }
    }

    public resetFilter(): void {
        this.inputFilter.inputElement.nativeElement.value = '';
        this.filteredLanguages = this.languages;

        setTimeout(()=> {
            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
            this.inputFilter.inputElement.nativeElement.focus();
        },10);
        this.changeDetector.markForCheck();
    }

}


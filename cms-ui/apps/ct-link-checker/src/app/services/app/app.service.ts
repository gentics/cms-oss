import { EventEmitter, Injectable } from '@angular/core';
import { I18nService } from '@gentics/cms-components';
import { BehaviorSubject, combineLatest, Observable } from 'rxjs';
import { debounceTime, first, publishReplay, refCount, switchMap } from 'rxjs/operators';
import { AppSettings } from '../../common/models/app-settings';
import { getSealedProxyObject, ObjectWithEvents } from '../../common/utils/get-sealed-proxy-object';
import { FilterService } from '../filter/filter.service';
import { UserSettingsService } from '../user-settings/user-settings.service';

@Injectable({
    providedIn: 'root',
})
export class AppService {

    private events$ = new EventEmitter<AppSettings>();
    private initialized = false;
    private updateInternal$ = new BehaviorSubject<boolean>(false);

    public update$: Observable<boolean>;

    protected readonly DEFAULT_APP_SETTINGS: AppSettings = {
        language: 'en',
        displayFields: [],
    };

    protected appSettings: AppSettings & ObjectWithEvents<AppSettings>;

    get settings(): AppSettings & ObjectWithEvents<AppSettings> {
        return this.appSettings;
    }

    constructor(
        private filterService: FilterService,
        private i18n: I18nService,
        private userSettings: UserSettingsService,
    ) {}

    init(): void {
        if (this.initialized) {
            throw new Error('The AppService.init() method must be called only once.');
        }

        this.update$ = this.updateInternal$.pipe(
            publishReplay(1),
            refCount(),
        );

        // Initialize appSettings with default data
        this.reset();
        this.initialized = true;

        // Set display fields
        combineLatest(
            this.userSettings.getUserSettings().pipe(first()),
            this.userSettings.getUserLanguage().pipe(first()),
        ).subscribe(([ct, ui]) => {
            if (!!ct.data && !!ct.data.displayFields) {
                this.settings.displayFields = ct.data.displayFields;
            }

            if (!!ct.data && !!ct.data.sortOptions) {
                this.filterService.options.sortOptions = ct.data.sortOptions;
            }

            if (ui.data) {
                this.settings.language = ui.data;
                this.i18n.setLanguage(ui.data);
            }
        });
    }

    reset(preset?: Partial<AppSettings>): void {
        this.appSettings = getSealedProxyObject({ ...this.DEFAULT_APP_SETTINGS, ...preset }, undefined, this.events$);
    }

    /**
     * Set application language
     * @param language Language to use
     */
    setLanguage(language: string): void {
        this.i18n.setLanguage(language);
    }

    /**
     * Saves the displayed fields to user settings
     * @param fields Display fields
     */
    setDisplayFields(fields: string[]): void {
        this.settings.displayFields = fields;
    }

    /**
     * Signal to update
     */
    updateData(): void {
        this.updateInternal$.next(true);
    }
}

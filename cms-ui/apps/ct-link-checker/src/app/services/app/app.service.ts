import { EventEmitter, Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { BehaviorSubject, combineLatest, Observable } from 'rxjs';
import { debounceTime, first, publishReplay, refCount, switchMap } from 'rxjs/operators';
import { AppSettings } from '../../common/models/app-settings';
import { getSealedProxyObject, ObjectWithEvents } from '../../common/utils/get-sealed-proxy-object';
import { GcmsAuthenticationService } from '../../core/services/authentication/gcms-authentication.service';
import { FilterService } from '../filter/filter.service';
import { UserSettingsService } from '../user-settings/user-settings.service';

@Injectable({
    providedIn: 'root'
})
export class AppService {

    private events$ = new EventEmitter<AppSettings>();
    private initialized = false;
    private updateInternal$ = new BehaviorSubject<boolean>(false);

    public update$: Observable<boolean>;
    public sid$: Observable<string>;

    protected readonly defaultAppSettings: AppSettings = {
        sid: null,
        language: 'en',
        displayFields: []
    };

    protected appSettings: AppSettings & ObjectWithEvents<AppSettings>;

    get settings(): AppSettings & ObjectWithEvents<AppSettings> {
        return this.appSettings;
    }

    constructor(
        private filterService: FilterService,
        private translate: TranslateService,
        private gcmsAuthenticationService: GcmsAuthenticationService,
        private userSettings: UserSettingsService
    ) {
        this.translate.setDefaultLang(this.defaultAppSettings.language);
    }

    init(): void {
        if (this.initialized) {
            throw new Error('The AppService.init() method must be called only once.');
        }

        this.update$ = this.updateInternal$.pipe(
            publishReplay(1),
            refCount()
        );

        this.sid$ = this.gcmsAuthenticationService.getSid();

        // Initialize authentication
        this.gcmsAuthenticationService.init();

        // Initialize appSettings with default data
        this.reset();
        this.initialized = true;

        // Set display fields
        this.sid$.pipe(
            debounceTime(5),
            switchMap(() => {
                return combineLatest(
                    this.userSettings.getUserSettings().pipe(first()),
                    this.userSettings.getUserLanguage().pipe(first())
                );
            })
        ).subscribe(([ct, ui]) => {
            if (!!ct.data && !!ct.data.displayFields) {
                this.settings.displayFields = ct.data.displayFields;
            }

            if (!!ct.data && !!ct.data.sortOptions) {
                this.filterService.options.sortOptions = ct.data.sortOptions;
            }

            if (!!ui.data) {
                this.settings.language = ui.data;
                this.translate.use(ui.data);
            }
        });
    }

    reset(preset?: Partial<AppSettings>): void {
        this.appSettings = getSealedProxyObject({ ...this.defaultAppSettings, ...preset }, undefined, this.events$);
        this.setSid();
    }

    /**
     * Set application SID from authentication service
     */
    private setSid(): void {
        if (this.gcmsAuthenticationService.sid) {
            this.appSettings.sid = this.gcmsAuthenticationService.sid;
        }
    }

    /**
     * Set application language
     * @param language Language to use
     */
    setLanguage(language: string): void {
        this.translate.use(language);
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

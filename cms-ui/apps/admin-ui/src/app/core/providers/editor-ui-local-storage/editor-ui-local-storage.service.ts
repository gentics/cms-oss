import { ErrorHandler } from '@admin-ui/core/providers/error-handler/error-handler.service';
import { InitializableServiceBase } from '@admin-ui/shared/providers/initializable-service-base';
import { Injectable } from '@angular/core';
import { GcmsUiLanguage } from '@gentics/cms-models';
import { fromEventPattern, Observable } from 'rxjs';
import { filter, map, publishReplay, refCount } from 'rxjs/operators';
import { LocalStorageChange } from './local-storage-change';

export const EDITOR_UI_LOCAL_STORAGE_PREFIX = 'GCMSUI_';

/**
 * A typed wrapper around HTML localStorage, for persisting data to the client's browser.
 * Note that this is only used for reading/writing data that is shared with the GCMS editor UI.
 */
@Injectable()
export class EditorUiLocalStorageService extends InitializableServiceBase {
    /**
     * The storageProvider may be substituted for testing purposes, but in real use
     * it will default to the browser's localStorage object.
     * @internal
     */
    storageProvider: Storage = window.localStorage;

    /**
     * The event provider for "storage" events. May be overwritten for testing purposes.
     * @internal
     */
    eventProvider: EventTarget = window;

    /**
     * Emits when a LocalStorage value is updated.
     */
    change$: Observable<LocalStorageChange>;


    constructor(
        private errorHandler: ErrorHandler,
    ) {
        super();
    }

    onServiceInit(): void {
        this.change$ = fromEventPattern<StorageEvent>(
            (handler: EventListener) => this.eventProvider.addEventListener('storage', handler),
            (handler: EventListener) => this.eventProvider.removeEventListener('storage', handler),
        ).pipe(
            filter(event => event.key && event.key.startsWith(EDITOR_UI_LOCAL_STORAGE_PREFIX)),
            map(event => new LocalStorageChange({
                key: event.key.substr(EDITOR_UI_LOCAL_STORAGE_PREFIX.length),
                oldValue: event.oldValue,
                newValue: event.newValue,
            })),
            publishReplay(1),
            refCount(),
        );
    }

    getForAllUsers(key: string): any {
        return this.get(key);
    }

    setForAllUsers(key: string, value: any): void {
        this.set(key, value);
    }

    getForUser(userId: number, key: string): any {
        return this.get(`USER-${userId}_${key}`);
    }

    setForUser(userId: number, key: string, value: any): void {
        this.set(`USER-${userId}_${key}`, value);
    }

    /**
     * Current user session id.
     */
    getSid(): number {
        return Number(this.get('sid'));
    }

    setSid(val: number): void {
        this.set('sid', val);
    }

    getUiLanguage(): GcmsUiLanguage {
        return this.get('uiLanguage');
    }

    setUiLanguage(language: GcmsUiLanguage): void {
        this.set('uiLanguage', language);
    }

    getVersionOfLastSave(): string {
        return this.get('lastSaveUiVersion');
    }

    /**
     * Persist the data.
     */
    private set(key: string, value: any): void {
        this.storageProvider.setItem(EDITOR_UI_LOCAL_STORAGE_PREFIX + key, value && JSON.stringify(value));
    }

    /**
     * Retrieve the data stored at the given key.
     */
    private get(key: string): any {
        const value = this.storageProvider.getItem(EDITOR_UI_LOCAL_STORAGE_PREFIX + key);
        if (value) {
            try {
                return JSON.parse(value);
            } catch (error) {
                this.errorHandler.catch(error);
                return;
            }
        }
    }
}

import { Injectable } from '@angular/core';
import { Response, UserDataResponse } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';

@Injectable({
    providedIn: 'root'
})
export class UserSettingsService {

    constructor(private api: GcmsApi) { }

    getUserLanguage(): Observable<UserDataResponse> {
        return this.api.userData.getKey('uiLanguage');
    }

    getUserSettings(): Observable<UserDataResponse> {
        return this.api.userData.getKey('ct_linkchecker');
    }

    setUserSettings(settings: any): Observable<Response> {
        return this.api.userData.setKey('ct_linkchecker', { ...settings });
    }
}

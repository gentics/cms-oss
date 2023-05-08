import { Response, UserDataResponse } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ApiBase } from '../base/api-base.service';

/**
 * API methods related to user data.
 */
export class UserDataApi {

    constructor(private apiBase: ApiBase) {}

    /**
     * Get all user data stored on the server.
     */
    getAllKeys(): Observable<UserDataResponse> {
        return this.apiBase.get('user/me/data');
    }

    /**
     * Get a specific key of the user data stored on the server.
     */
    getKey(key: string): Observable<UserDataResponse> {
        return this.apiBase.get(`user/me/data/${key}`);
    }

    /**
     * Save a specific key of user data on the server.
     */
    setKey(key: string, data: any): Observable<Response> {
        return this.apiBase.post(`user/me/data/${key}`, JSON.stringify(data));
    }
}

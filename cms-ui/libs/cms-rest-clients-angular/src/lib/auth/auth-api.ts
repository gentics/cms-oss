import { LoginResponse, Response, ValidateSidResponse } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ApiBase } from '../base/api-base.service';

/**
 * API methods related to authentication.
 *
 * Docs for the endpoints used here can be found at:
 * http://www.gentics.com/Content.Node/cmp8/guides/restapi/resource_AuthenticationResource.html
 */
export class AuthApi {

    constructor(private apiBase: ApiBase) {}

    /**
     * Log a user in.
     */
    login(username: string, password: string): Observable<LoginResponse> {
        return this.apiBase.post('auth/login', {
            login: username,
            password: password,
        });
    }

    /**
     * Log a user out.
     */
    logout(sid: number): Observable<Response> {
        return this.apiBase.post(`auth/logout/${sid}`, '');
    }

    /**
     * Validates an sid by checking against the `user/me` endpoint.
     */
    validate(sid: number): Observable<ValidateSidResponse> {
        return this.apiBase.get(`user/me?sid=${sid}`);
    }

    /**
     * Changes the user's password
     */
    changePassword(id: number, newPassword: string): Observable<Response> {
        return this.apiBase.post(`user/save/${id}`, {
            user: { password: newPassword },
        });
    }
}

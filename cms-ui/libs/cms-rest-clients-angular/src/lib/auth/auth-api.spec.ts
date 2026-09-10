import {AuthApi} from './auth-api';
import {MockApiBase} from '../base/api-base.mock';


describe('AuthApi', () => {

    let authApi: AuthApi;
    let apiBase: MockApiBase;
    beforeEach(() => {
        apiBase = new MockApiBase();
        authApi = new AuthApi(apiBase as any);
    });

    it('changePasswords() sends the correct POST request', () => {
        authApi.changePassword(1234, 'newpassword');
        expect(apiBase.post).toHaveBeenCalledWith('user/save/1234', { user: { password: 'newpassword' } });
    });

    it('login() sends the correct POST request', () => {
        authApi.login('username', 'password');
        expect(apiBase.post).toHaveBeenCalledWith('auth/login', { login: 'username', password: 'password' });
    });

    it('logout() sends the correct POST request', () => {
        authApi.logout();
        expect(apiBase.post).toHaveBeenCalledWith('auth/logout', '');
    });

    it('validate() sends the correct GET request', () => {
        authApi.validate();
        expect(apiBase.get).toHaveBeenCalledWith('user/me');
    });

});

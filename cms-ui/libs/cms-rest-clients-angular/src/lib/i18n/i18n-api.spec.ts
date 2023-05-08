import { MockApiBase } from '../base/api-base.mock';
import { I18nApi } from './i18n-api';

describe('I18nApi', () => {

    let apiBase: MockApiBase;
    let i18nApi: I18nApi;
    beforeEach(() => {
        apiBase = new MockApiBase();
        i18nApi = new I18nApi(apiBase as any);
    });

    it('getBackendLanguages() sends the correct GET request', () => {
        i18nApi.getBackendLanguages();
        expect(apiBase.post).toHaveBeenCalledWith('i18n/list');
    });

    it('getActiveBackendLanguage() sends the correct GET request', () => {
        i18nApi.getActiveBackendLanguage();
        expect(apiBase.post).toHaveBeenCalledWith('i18n/get');
    });

    it('setActiveBackendLanguage() sends the correct GET request', () => {
        i18nApi.setActiveBackendLanguage({ code: 'de' });
        expect(apiBase.post).toHaveBeenCalledWith('i18n/set', { code: 'de' });
    });

});

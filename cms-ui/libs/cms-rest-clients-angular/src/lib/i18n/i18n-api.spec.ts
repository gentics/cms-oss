import { MockApiBase } from '../base/api-base.mock';
import { I18nApi } from './i18n-api';

describe('I18nApi', () => {

    let apiBase: MockApiBase;
    let i18nApi: I18nApi;
    beforeEach(() => {
        apiBase = new MockApiBase();
        i18nApi = new I18nApi(apiBase as any);
    });

    it('getAvailableUiLanguages() sends the correct GET request', () => {
        i18nApi.getAvailableUiLanguages();
        expect(apiBase.get).toHaveBeenCalledWith('i18n/list');
    });

    it('getActiveUiLanguage() sends the correct GET request', () => {
        i18nApi.getActiveUiLanguage();
        expect(apiBase.get).toHaveBeenCalledWith('i18n/get');
    });

    it('setActiveUiLanguage() sends the correct GET request', () => {
        i18nApi.setActiveUiLanguage({ code: 'de' });
        expect(apiBase.post).toHaveBeenCalledWith('i18n/set', { code: 'de' });
    });

});

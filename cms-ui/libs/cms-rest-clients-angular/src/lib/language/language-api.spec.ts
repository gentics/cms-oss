import { MockApiBase } from '../base/api-base.mock';
import { LanguageApi } from './language-api';

describe('LanguageApi', () => {

    let apiBase: MockApiBase;
    let languageApi: LanguageApi;
    beforeEach(() => {
        apiBase = new MockApiBase();
        languageApi = new LanguageApi(apiBase as any);
    });

});

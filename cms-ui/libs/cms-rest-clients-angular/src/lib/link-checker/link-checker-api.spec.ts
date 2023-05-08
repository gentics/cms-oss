import {LinkCheckerApi} from './link-checker-api';
import {MockApiBase} from '../base/api-base.mock';

describe('LinkCheckerApi', () => {

    let linkCheckerApi: LinkCheckerApi;
    let apiBase: MockApiBase;
    beforeEach(() => {
        apiBase = new MockApiBase();
        linkCheckerApi = new LinkCheckerApi(apiBase as any);
    });

    it('getPages() sends the correct GET request', () => {
        let request = {
            editable: true,
            iscreator: false,
            iseditor: true,
            nodeId: 12,
            status: 'valid'
        };

        linkCheckerApi.getPages(request);
        expect(apiBase.get).toHaveBeenCalledWith('linkChecker/pages', request);
    });

    it('getPage() sends the correct GET request', () => {
        linkCheckerApi.getPage(123);
        expect(apiBase.get).toHaveBeenCalledWith('linkChecker/pages/123');
    });

    it('updateStatus() sends the correct POST request', () => {
        linkCheckerApi.updateStatus(123);
        expect(apiBase.post).toHaveBeenCalledWith('linkChecker/pages/123', '');
    });

    it('checkLink() sends the correct GET request', () => {
        linkCheckerApi.checkLink('someLink');
        expect(apiBase.post).toHaveBeenCalledWith('linkChecker/check', { url: 'someLink' });
    });
});

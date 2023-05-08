import {Feature} from '@gentics/cms-models';
import {MockApiBase} from '../base/api-base.mock';
import {AdminApi} from './admin-api';

describe('AdminApi', () => {

    let adminApi: AdminApi;
    let apiBase: MockApiBase;
    beforeEach(() => {
        apiBase = new MockApiBase();
        adminApi = new AdminApi(apiBase as any);
    });

    it('getAvailableEmbeddedTools sends the correct GET request', () => {
        adminApi.getAvailableEmbeddedTools();
        expect(apiBase.get).toHaveBeenCalledWith('admin/tools');
    });

    it('getFeature sends the correct GET request', () => {
        adminApi.getFeature(Feature.nice_urls);
        expect(apiBase.get).toHaveBeenCalledWith('admin/features/nice_urls');
    });

    it('getVersion sends the correct GET request', () => {
        adminApi.getVersion();
        expect(apiBase.get).toHaveBeenCalledWith('admin/version');
    });

    it('getUsersnapSettings sends the correct GET request', () => {
        adminApi.getUsersnapSettings();
        expect(apiBase.get).toHaveBeenCalledWith('usersnap');
    });

});

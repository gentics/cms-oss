import { MockApiBase } from '../util/api-base.mock';
import { AdminInfoApi } from './admin-info-api';

describe('AdminInfoApi', () => {

    let apiBase: MockApiBase;
    let adminInfoApi: AdminInfoApi;

    beforeEach(() => {
        apiBase = new MockApiBase();
        adminInfoApi = new AdminInfoApi(apiBase as any);
    });

    it('getPublishInfo() sends the correct GET request', () => {
        adminInfoApi.getPublishInfo();
        expect(apiBase.get).toHaveBeenCalledWith(`admin/publishInfo`);
    });

    it('getUpdates() sends the correct GET request', () => {
        adminInfoApi.getUpdates();
        expect(apiBase.get).toHaveBeenCalledWith(`admin/updates`);
    });

    it('getVersion() sends the correct GET request', () => {
        adminInfoApi.getVersion();
        expect(apiBase.get).toHaveBeenCalledWith(`admin/version`);
    });

    it('getFeatureInfo() sends the correct GET request', () => {
        const featureName = 'Test';

        adminInfoApi.getFeatureInfo(featureName);
        expect(apiBase.get).toHaveBeenCalledWith(`admin/features/${featureName}`);
    });
});

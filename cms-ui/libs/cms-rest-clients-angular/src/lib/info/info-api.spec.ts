import {InfoApi} from './info-api';
import {MockApiBase} from '../base/api-base.mock';

describe('InfoApi', () => {

    let infoApi: InfoApi;
    let apiBase: MockApiBase;
    beforeEach(() => {
        apiBase = new MockApiBase();
        infoApi = new InfoApi(apiBase as any);
    });

    it('getMaintenanceModeStatus sends the correct GET request', () => {
        infoApi.getMaintenanceModeStatus();
        expect(apiBase.get).toHaveBeenCalledWith('info/maintenance');
    });

});

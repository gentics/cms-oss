import { MockApiBase } from '../base/api-base.mock';
import { PortalImplementationApi } from './portal-implementation-api';

describe('PortalImplementationApi', () => {

    let portalImplementationApi: PortalImplementationApi;
    let apiBase: MockApiBase;
    beforeEach(() => {
        apiBase = new MockApiBase();
        portalImplementationApi = new PortalImplementationApi(apiBase as any);
    });

    it('getPortalImplementations sends a GET request to "portalimplementations"', () => {
        const options = { pageSize: -1 };
        portalImplementationApi.getPortalImplementations(options);

        expect(apiBase.get).toHaveBeenCalledWith('portalimplementations', options);
    });

    it('getPortalImplementation sends a GET request to "portalimplementations/id"', () => {
        const id = 1;
        portalImplementationApi.getPortalImplementation(`${id}`);

        expect(apiBase.get).toHaveBeenCalledWith(`portalimplementations/${id}`);
    });

});

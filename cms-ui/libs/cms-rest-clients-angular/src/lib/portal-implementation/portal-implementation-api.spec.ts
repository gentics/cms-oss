import { BaseListOptionsWithPaging, PagingSortOrder, PortalImplementationType } from '@gentics/cms-models';
import { MockApiBase } from '../base/api-base.mock';
import { stringifyPagingSortOptions } from '../util/sort-options/sort-options';
import { PortalImplementationApi } from './portal-implementation-api';

const SORTED_OPTIONS: BaseListOptionsWithPaging<any> = {
    page: 2,
    pageSize: 10,
    q: 'test',
    sort: [{
        sortOrder: PagingSortOrder.Desc,
        attribute: 'name',
    }],
};

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

    it('getPortalImplementations sends the sort options as string', () => {
        portalImplementationApi.getPortalImplementations(SORTED_OPTIONS);

        expect(apiBase.get).toHaveBeenCalledWith('portalimplementations', { ...SORTED_OPTIONS, sort: stringifyPagingSortOptions(SORTED_OPTIONS.sort) });
    });

    it('getPortalImplementation sends a GET request to "portalimplementations/id"', () => {
        const id = 1;
        portalImplementationApi.getPortalImplementation(`${id}`);

        expect(apiBase.get).toHaveBeenCalledWith(`portalimplementations/${id}`);
    });

    it('createPortalImplementation sends a POST request to "portalimplementations"', () => {
        const payload = { keyword: 'php', name: 'PHP Portal', type: PortalImplementationType.PHP };
        portalImplementationApi.createPortalImplementation(payload);

        expect(apiBase.post).toHaveBeenCalledWith('portalimplementations', payload);
    });

    it('updatePortalImplementation sends a PUT request to "portalimplementations/id"', () => {
        const payload = { name: 'Renamed' };
        portalImplementationApi.updatePortalImplementation(1, payload);

        expect(apiBase.put).toHaveBeenCalledWith('portalimplementations/1', payload);
    });

    it('deletePortalImplementation sends a DELETE request to "portalimplementations/id"', () => {
        portalImplementationApi.deletePortalImplementation(1);

        expect(apiBase.delete).toHaveBeenCalledWith('portalimplementations/1');
    });

    it('getPortalImplementationNodes sends a GET request to "portalimplementations/id/nodes"', () => {
        portalImplementationApi.getPortalImplementationNodes(1);

        expect(apiBase.get).toHaveBeenCalledWith('portalimplementations/1/nodes');
    });

    it('getPortalImplementationFiles sends a GET request to "portalimplementations/id/files"', () => {
        portalImplementationApi.getPortalImplementationFiles(1, SORTED_OPTIONS);

        expect(apiBase.get).toHaveBeenCalledWith('portalimplementations/1/files', { ...SORTED_OPTIONS, sort: stringifyPagingSortOptions(SORTED_OPTIONS.sort) });
    });

});

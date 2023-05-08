import { MockApiBase } from '../base/api-base.mock';
import { ObjectPropertyApi } from './object-property-api';

describe('ObjectPropertyApi', () => {

    let objectpropertyApi: ObjectPropertyApi;
    let apiBase: MockApiBase;
    beforeEach(() => {
        apiBase = new MockApiBase();
        objectpropertyApi = new ObjectPropertyApi(apiBase as any);
    });


    it('getContentrepositories sends a GET request to "objectproperty"', () => {
        const options = { pageSize: -1 };
        objectpropertyApi.getObjectProperties(options);

        expect(apiBase.get).toHaveBeenCalledWith('objectproperty', options);
    });

    it('getContentRepository sends a GET request to "objectproperty/id"', () => {
        const id = 'globalId';
        objectpropertyApi.getObjectProperty(id);

        expect(apiBase.get).toHaveBeenCalledWith(`objectproperty/${id}`);
    });

});

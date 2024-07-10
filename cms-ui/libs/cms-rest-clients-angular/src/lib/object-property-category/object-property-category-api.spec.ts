import { MockApiBase } from '../base/api-base.mock';
import { ObjectPropertyCategoryApi } from './object-property-category-api';

describe('ObjectPropertyCategoryApi', () => {

    let objectpropertycategoryApi: ObjectPropertyCategoryApi;
    let apiBase: MockApiBase;
    beforeEach(() => {
        apiBase = new MockApiBase();
        objectpropertycategoryApi = new ObjectPropertyCategoryApi(apiBase as any);
    });


    it('getObjectPropertyCategories sends a GET request to "objectpropertycategory"', () => {
        const options = { pageSize: -1 };
        objectpropertycategoryApi.getObjectPropertyCategories(options);

        expect(apiBase.get).toHaveBeenCalledWith('objectproperty/category', options);
    });

    it('getObjectPropertyCategory sends a GET request to "objectpropertycategory/id"', () => {
        const id = 'globalId';
        objectpropertycategoryApi.getObjectPropertyCategory(id);

        expect(apiBase.get).toHaveBeenCalledWith(`objectproperty/category/${id}`);
    });

});

import { MockApiBase } from '../base/api-base.mock';
import { DataSourceApi } from './datasource-api';

describe('DataSourceApi', () => {

    let contetrepositoryApi: DataSourceApi;
    let apiBase: MockApiBase;
    beforeEach(() => {
        apiBase = new MockApiBase();
        contetrepositoryApi = new DataSourceApi(apiBase as any);
    });


    it('getContentRepositoryFragments sends a GET request to "dataSource"', () => {
        const options = { pageSize: -1 };
        contetrepositoryApi.getDataSources(options);

        expect(apiBase.get).toHaveBeenCalledWith('datasource', options);
    });

    it('getContentRepositoryFragment sends a GET request to "dataSource/id"', () => {
        const id = 'globalId';
        contetrepositoryApi.getDataSource(id);

        expect(apiBase.get).toHaveBeenCalledWith(`datasource/${id}`);
    });

});

import { MockApiBase } from '../util/api-base.mock';
import { ElasticSearchIndexApi } from './elastic-search-index-api';

describe('ElasticSearchIndexApi', () => {

    let apiBase: MockApiBase;
    let entityApi: ElasticSearchIndexApi;

    beforeEach(() => {
        apiBase = new MockApiBase();
        entityApi = new ElasticSearchIndexApi(apiBase as any);
    });

    it('getItems() sends the correct GET request', () => {
        entityApi.getItems();
        expect(apiBase.get).toHaveBeenCalledWith('index', undefined);
    });

    it('rebuild() sends the correct PUT request', () => {
        const indexName = 'index_one';
        const drop = true;
        entityApi.rebuild(indexName, drop);
        const queryParams = { drop: drop || false };
        expect(apiBase.put).toHaveBeenCalledWith(`index/${indexName}/rebuild`, {}, queryParams);
    });

});

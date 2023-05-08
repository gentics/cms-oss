import { MockApiBase } from '../base/api-base.mock';
import { ContentrespositoryFragmentApi } from './cr-fragment-api';

describe('ContentRepositoryFragmentApi', () => {

    let contetrepositoryApi: ContentrespositoryFragmentApi;
    let apiBase: MockApiBase;
    beforeEach(() => {
        apiBase = new MockApiBase();
        contetrepositoryApi = new ContentrespositoryFragmentApi(apiBase as any);
    });


    it('getContentRepositoryFragments sends a GET request to "cr_fragments"', () => {
        const options = { pageSize: -1 };
        contetrepositoryApi.getContentRepositoryFragments(options);

        expect(apiBase.get).toHaveBeenCalledWith('cr_fragments', options);
    });

    it('getContentRepositoryFragment sends a GET request to "cr_fragments/id"', () => {
        const id = 1;
        contetrepositoryApi.getContentRepositoryFragment(`${id}`);

        expect(apiBase.get).toHaveBeenCalledWith(`cr_fragments/${id}`);
    });

});

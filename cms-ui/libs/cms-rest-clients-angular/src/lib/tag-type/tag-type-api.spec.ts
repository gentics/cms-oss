import {MockApiBase} from '../base/api-base.mock';
import {TagTypeApi} from './tag-type-api';

describe('TagTypeApi', () => {

    let tagTypeApi: TagTypeApi;
    let apiBase: MockApiBase;

    beforeEach(() => {
        apiBase = new MockApiBase();
        tagTypeApi = new TagTypeApi(apiBase as any);
    });

    it('getTagType sends the correct GET request', () => {
        tagTypeApi.getTagType(4711);
        expect(apiBase.get).toHaveBeenCalledWith('construct/load/4711');
    });

});

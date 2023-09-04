import { ContentRepositoryCreateRequest, ContentRepositoryType, ContentRepositoryUpdateRequest } from '@gentics/cms-models';
import { MockApiBase } from '../base/api-base.mock';
import { ContentrespositoryApi } from './content-respository-api';

describe('ContentRepositoryApi', () => {

    let contentRepositoryApi: ContentrespositoryApi;
    let apiBase: MockApiBase;
    beforeEach(() => {
        apiBase = new MockApiBase();
        contentRepositoryApi = new ContentrespositoryApi(apiBase as any);
    });


    it('getContentrepositories sends a GET request to "contentrepositories"', () => {
        const options = { pageSize: -1 };
        contentRepositoryApi.getContentrepositories(options);

        expect(apiBase.get).toHaveBeenCalledWith('contentrepositories', options);
    });

    it('getContentRepository sends a GET request to "contentrepositories/id"', () => {
        const id = 1;
        contentRepositoryApi.getContentRepository(id);

        expect(apiBase.get).toHaveBeenCalledWith(`contentrepositories/${id}`);
    });

    it('createContentRepository sends a POST request to "contentrepositories"', () => {
        const payload: ContentRepositoryCreateRequest = {
            name: 'Test-Content-Repository-01',
            crType: ContentRepositoryType.MESH,
            dbType: 'mysql',
            username: 'root',
            usePassword: false,
            url: 'jdbc:mariadb://db:3306/contentRepository',
            basepath: '',
            instantPublishing: true,
            languageInformation: true,
            permissionInformation: true,
            permissionProperty: '',
            defaultPermission: '',
            diffDelete: true,
            projectPerNode: false,
        };
        contentRepositoryApi.createContentRepository(payload);

        expect(apiBase.post).toHaveBeenCalledWith('contentrepositories', payload);
    });

    it('updateContentRepository sends a PUT request to "contentrepositories/id"', () => {
        const id = 1;
        const payload: ContentRepositoryUpdateRequest = {
            name: 'Test-Content-Repository-02',
        };
        contentRepositoryApi.updateContentRepository(id, payload);

        expect(apiBase.put).toHaveBeenCalledWith(`contentrepositories/${id}`, payload);
    });

    it('deleteContentRepository sends a DELETE request to "contentrepositories/id"', () => {
        const id = 1;
        contentRepositoryApi.deleteContentRepository(id);

        expect(apiBase.delete).toHaveBeenCalledWith(`contentrepositories/${id}`);
    });


});

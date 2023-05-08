import { TemplateLinkRequestOptions } from '@gentics/cms-models';
import { MockApiBase } from '../base/api-base.mock';
import { TemplateApi } from './template-api';

describe('TemplateApi', () => {

    let templateApi: TemplateApi;
    let apiBase: MockApiBase;
    beforeEach(() => {
        apiBase = new MockApiBase();
        templateApi = new TemplateApi(apiBase as any);
    });

    it('getTemplates sends a GET request to "template"', () => {
        const options = { maxItems: -1 };
        templateApi.getTemplates(options);

        expect(apiBase.get).toHaveBeenCalledWith('template', options);
    });

    it('getContentRepository sends a GET request to "template/id"', () => {
        const id = 'globalId';
        templateApi.getTemplate(id);

        expect(apiBase.get).toHaveBeenCalledWith(`template/${id}`);
    });

    it('linkTemplateToFolders sends a POST request to "template/link/id"', () => {
        const templateId = 42;
        const options: TemplateLinkRequestOptions = {
            folderIds: [ 1, 2, 3 ],
            nodeId: 23,
        };
        templateApi.linkTemplateToFolders(templateId, options);

        expect(apiBase.post).toHaveBeenCalledWith(`template/link/${templateId}`, options);
    });

    it('unlinkTemplateToFolders sends a POST request to "template/unlink/id"', () => {
        const templateId = 42;
        const options: TemplateLinkRequestOptions = {
            folderIds: [ 1, 2, 3 ],
            nodeId: 23,
            recursive: false,
        };
        templateApi.unlinkTemplateFromFolders(templateId, options);

        expect(apiBase.post).toHaveBeenCalledWith(`template/unlink/${templateId}`, options);
    });

});

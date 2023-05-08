import {
    Language,
    NodeCopyRequest,
    NodeCopyRequestOptions,
    NodeDeleteRequestOptions,
    NodeFeature,
    NodeFeatureListRequestOptions,
    NodeRequestOptions,
    NodeSaveRequest,
    TemplateListRequest,
    PagedConstructListRequestOptions,
} from '@gentics/cms-models';
import { GcmsTestData } from '@gentics/cms-models';
import { MockApiBase } from '../util/api-base.mock';
import { NodeApi } from './node-api';

const NODE_ID = 1;

describe('NodeApi', () => {

    let apiBase: MockApiBase;
    let nodeApi: NodeApi;

    beforeEach(() => {
        apiBase = new MockApiBase();
        nodeApi = new NodeApi(apiBase as any);
    });

    it('getNodes() sends the correct GET request', () => {
        nodeApi.getNodes();
        expect(apiBase.get).toHaveBeenCalledWith(`node`, undefined);
    });

    it('addNode() sends the correct PUT request', () => {
        const payload: NodeSaveRequest = {
            node: GcmsTestData.getExampleNodeData(),
        };

        nodeApi.addNode(payload);
        expect(apiBase.put).toHaveBeenCalledWith(`node`, payload);
    });

    it('getNodeFeatureList() sends the correct GET request', () => {
        nodeApi.getNodeFeatureList();
        expect(apiBase.get).toHaveBeenCalledWith(`node/features`, undefined);
    });

    it('getNodeFeatureList() sends the correct GET request with query parameters', () => {
        const options: NodeFeatureListRequestOptions = {
            sort: {
                attribute: 'name',
            },
        };

        nodeApi.getNodeFeatureList(options);
        expect(apiBase.get).toHaveBeenCalledWith(`node/features`, options);
    });

    it('removeNode() sends the correct DELETE request', () => {
        nodeApi.removeNode(NODE_ID);
        expect(apiBase.delete).toHaveBeenCalledWith(`node/${NODE_ID}`, undefined);
    });

    it('removeNode() sends the correct DELETE request with query parameters', () => {
        const options: NodeDeleteRequestOptions = {
            wait: 50,
        };

        nodeApi.removeNode(NODE_ID, options);
        expect(apiBase.delete).toHaveBeenCalledWith(`node/${NODE_ID}`, options);
    });

    it('getNode() sends the correct GET request', () => {
        nodeApi.getNode(NODE_ID);
        expect(apiBase.get).toHaveBeenCalledWith(`node/${NODE_ID}`, undefined);
    });

    it('getNode() sends the correct GET request with query parameters', () => {
        const options: NodeRequestOptions = {
            update: true,
        };

        nodeApi.getNode(NODE_ID, options);
        expect(apiBase.get).toHaveBeenCalledWith(`node/${NODE_ID}`, options);
    });

    it('updateNode() sends the correct POST request', () => {
        const payload: NodeSaveRequest = {
            node: GcmsTestData.getExampleNodeData(),
        };

        nodeApi.updateNode(NODE_ID, payload);
        expect(apiBase.post).toHaveBeenCalledWith(`node/${NODE_ID}`, payload);
    });

    it('getNodeFeatures() sends the correct GET request', () => {
        nodeApi.getNodeFeatures(NODE_ID);
        expect(apiBase.get).toHaveBeenCalledWith(`node/${NODE_ID}/features`, undefined);
    });

    it('getNodeFeatures() sends the correct GET request with query parameters', () => {
        const options: NodeFeatureListRequestOptions = {
            sort: {
                attribute: 'name',
            },
        };

        nodeApi.getNodeFeatures(NODE_ID, options);
        expect(apiBase.get).toHaveBeenCalledWith(`node/${NODE_ID}/features`, options);
    });

    it('updateNodeLanguages() sends the correct POST request', () => {
        const payload: Language[] = [
            {
                code: 'en',
                name: 'English',
                id: 2,
            },
            {
                code: 'de',
                name: 'Deutsch',
                id: 3,
            },
        ];

        nodeApi.updateNodeLanguages(NODE_ID, payload);
        expect(apiBase.post).toHaveBeenCalledWith(`node/${NODE_ID}/languages`, payload);
    });

    it('getNodeConstructs() sends the correct GET request', () => {
        nodeApi.getNodeConstructs(NODE_ID);
        expect(apiBase.get).toHaveBeenCalledWith(`node/${NODE_ID}/constructs`, undefined);
    });

    it('getNodeConstructs() sends the correct GET request with query parameters', () => {
        const options: PagedConstructListRequestOptions = {
            sort: {
                attribute: 'name',
            },
        };

        nodeApi.getNodeConstructs(NODE_ID, options);
        expect(apiBase.get).toHaveBeenCalledWith(`node/${NODE_ID}/constructs`, options);
    });

    it('copyNode() sends the correct POST request', () => {
        const payload: NodeCopyRequest = {
            pages: true,
            templates: true,
            files: true,
            workflows: false,
            copies: 1,
        };

        nodeApi.copyNode(NODE_ID, payload);
        expect(apiBase.post).toHaveBeenCalledWith(`node/${NODE_ID}/copy`, payload, undefined);
    });

    it('copyNode() sends the correct POST request with query parameters', () => {
        const payload: NodeCopyRequest = {
            pages: true,
            templates: true,
            files: true,
            workflows: false,
            copies: 1,
        };

        const options: NodeCopyRequestOptions = {
            wait: 50,
        };

        nodeApi.copyNode(NODE_ID, payload, options);
        expect(apiBase.post).toHaveBeenCalledWith(`node/${NODE_ID}/copy`, payload, options);
    });

    it('getNodeSettings() sends the correct GET request', () => {
        nodeApi.getNodeSettings(NODE_ID);
        expect(apiBase.get).toHaveBeenCalledWith(`node/${NODE_ID}/settings`);
    });

    it('getNodeTemplates() sends the correct GET request', () => {
        nodeApi.getNodeTemplates(NODE_ID);
        expect(apiBase.get).toHaveBeenCalledWith(`node/${NODE_ID}/templates`, undefined);
    });

    it('getNodeTemplates() sends the correct GET request with query parameters', () => {
        const options: TemplateListRequest = {
            sort: 'name',
        };

        nodeApi.getNodeTemplates(NODE_ID, options);
        expect(apiBase.get).toHaveBeenCalledWith(`node/${NODE_ID}/templates`, options);
    });

    it('deactivateNodeFeature() sends the correct DELETE request', () => {
        nodeApi.deactivateNodeFeature(NODE_ID, NodeFeature.alwaysLocalize);
        expect(apiBase.delete).toHaveBeenCalledWith(`node/${NODE_ID}/features/${NodeFeature.alwaysLocalize}`);
    });

    it('activateNodeFeature() sends the correct PUT request', () => {
        nodeApi.activateNodeFeature(NODE_ID, NodeFeature.alwaysLocalize);

        /** Second params is 'undefined' because we pass an empty body for PUT */
        expect(apiBase.put).toHaveBeenCalledWith(`node/${NODE_ID}/features/${NodeFeature.alwaysLocalize}`, undefined);
    });

    it('removeNodeLanguage() sends the correct DELETE request', () => {
        const language = 2;
        nodeApi.removeNodeLanguage(NODE_ID, language);
        expect(apiBase.delete).toHaveBeenCalledWith(`node/${NODE_ID}/languages/${language}`);
    });

    it('addNodeLanguage() sends the correct PUT request', () => {
        const language = 2;
        nodeApi.addNodeLanguage(NODE_ID, language);

        /** Second params is 'undefined' because we pass an empty body for PUT */
        expect(apiBase.put).toHaveBeenCalledWith(`node/${NODE_ID}/languages/${language}`, undefined);
    });

    it('removeNodeConstruct() sends the correct DELETE request', () => {
        const construct = 3;
        nodeApi.removeNodeConstruct(NODE_ID, construct);
        expect(apiBase.delete).toHaveBeenCalledWith(`node/${NODE_ID}/constructs/${construct}`);
    });

    it('addNodeConstruct() sends the correct PUT request', () => {
        const construct = 3;
        nodeApi.addNodeConstruct(NODE_ID, construct);

        /** Second params is 'undefined' because we pass an empty body for PUT */
        expect(apiBase.put).toHaveBeenCalledWith(`node/${NODE_ID}/constructs/${construct}`, undefined);
    });

    it('removeNodeTemplate() sends the correct DELETE request', () => {
        const template = 1;
        nodeApi.removeNodeTemplate(NODE_ID, template);
        expect(apiBase.delete).toHaveBeenCalledWith(`node/${NODE_ID}/templates/${template}`);
    });

});

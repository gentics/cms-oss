import { BaseListOptionsWithPaging, BaseListOptionsWithSkipCount, PackageCreateRequest, PagingSortOrder } from '@gentics/cms-models';
import { MockApiBase } from '../util/api-base.mock';
import { DevToolsApi } from './dev-tools-api';

const NODE_ID = 42;
const PACKAGE_NAME = 'xxxx';
const PACKAGE_ENTITY_GLOBALID = 'yyyy';
const OPTIONS: BaseListOptionsWithPaging<any> = {
    page: 42,
    pageSize: -1,
    q: 'test',
    sort: [{
        sortOrder: PagingSortOrder.Desc,
        attribute: 'name',
    }],
};
const OPTIONS_WITH_SKIPCOUNT: BaseListOptionsWithSkipCount = {
    sortorder: 'desc',
};
const PACKAGE_CREATE_PAYLOAD: PackageCreateRequest = {
    name: 'zzzz',
    subPackages: [
        {
            name: '0000',
        },
        {
            name: '1111',
        },
        {
            name: '2222',
        },
    ],
};

describe('DevToolsApi', () => {

    let apiBase: MockApiBase;
    let devtoolsApi: DevToolsApi;

    beforeEach(() => {
        apiBase = new MockApiBase();
        devtoolsApi = new DevToolsApi(apiBase as any);
    });

    // SYNC ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    it('getSyncState() sends the correct GET request', () => {
        devtoolsApi.getSyncState();
        expect(apiBase.get).toHaveBeenCalledWith('devtools/sync');
    });

    it('stopSync() sends the correct DELETE request', () => {
        devtoolsApi.stopSync();
        expect(apiBase.delete).toHaveBeenCalledWith('devtools/sync');
    });

    // PACKAGES ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    it('getPackages() sends the correct GET request', () => {
        devtoolsApi.getPackages(OPTIONS);
        expect(apiBase.get).toHaveBeenCalledWith('devtools/packages', OPTIONS);
    });

    it('getPackage() sends the correct GET request', () => {
        devtoolsApi.getPackage(PACKAGE_NAME);
        expect(apiBase.get).toHaveBeenCalledWith(`devtools/packages/${PACKAGE_NAME}`);
    });

    it('addPackage() sends the correct PUT request', () => {
        devtoolsApi.addPackage(PACKAGE_CREATE_PAYLOAD);
        expect(apiBase.put).toHaveBeenCalledWith(`devtools/packages/${PACKAGE_CREATE_PAYLOAD.name}`, null);
    });

    it('deletePackage() sends the correct DELETE request', () => {
        devtoolsApi.removePackage(PACKAGE_CREATE_PAYLOAD.name);
        expect(apiBase.delete).toHaveBeenCalledWith(`devtools/packages/${PACKAGE_CREATE_PAYLOAD.name}`);
    });

    // NODE PACKAGES ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    it('getPackagesOfNode() sends the correct GET request', () => {
        devtoolsApi.getPackagesOfNode(NODE_ID, OPTIONS);
        expect(apiBase.get).toHaveBeenCalledWith(`devtools/nodes/${NODE_ID}/packages`, OPTIONS);
    });

    it('createPackageInNode() sends the correct PUT request', () => {
        devtoolsApi.createPackageInNode(NODE_ID, PACKAGE_CREATE_PAYLOAD);
        expect(apiBase.put).toHaveBeenCalledWith(`devtools/nodes/${NODE_ID}/packages`, PACKAGE_CREATE_PAYLOAD);
    });

    it('addPackageToNode() sends the correct PUT request', () => {
        devtoolsApi.addPackageToNode(NODE_ID, PACKAGE_CREATE_PAYLOAD.name);
        expect(apiBase.put).toHaveBeenCalledWith(`devtools/nodes/${NODE_ID}/packages/${PACKAGE_CREATE_PAYLOAD.name}`, null);
    });

    it('removePackageFromNode() sends the correct DELETE request', () => {
        devtoolsApi.removePackageFromNode(NODE_ID, PACKAGE_CREATE_PAYLOAD.name);
        expect(apiBase.delete).toHaveBeenCalledWith(`devtools/nodes/${NODE_ID}/packages/${PACKAGE_CREATE_PAYLOAD.name}`);
    });

    it('syncPackageToFilesystem() sends the correct PUT request', () => {
        devtoolsApi.syncPackageToFilesystem(PACKAGE_CREATE_PAYLOAD.name);
        expect(apiBase.put).toHaveBeenCalledWith(`devtools/packages/${PACKAGE_CREATE_PAYLOAD.name}/cms2fs`, null);
    });

    it('syncPackageFromFilesystem() sends the correct PUT request', () => {
        devtoolsApi.syncPackageFromFilesystem(PACKAGE_CREATE_PAYLOAD.name);
        expect(apiBase.put).toHaveBeenCalledWith(`devtools/packages/${PACKAGE_CREATE_PAYLOAD.name}/fs2cms`, null);
    });

    // CONSTRUCTS ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    it('getConstructs() sends the correct GET request', () => {
        devtoolsApi.getConstructs(PACKAGE_NAME, OPTIONS);
        expect(apiBase.get).toHaveBeenCalledWith(`devtools/packages/${PACKAGE_NAME}/constructs`, OPTIONS);
    });

    it('getConstruct() sends the correct GET request', () => {
        devtoolsApi.getConstruct(PACKAGE_NAME, PACKAGE_ENTITY_GLOBALID);
        expect(apiBase.get).toHaveBeenCalledWith(`devtools/packages/${PACKAGE_NAME}/constructs/${PACKAGE_ENTITY_GLOBALID}`);
    });

    it('addConstructToPackage() sends the correct PUT request', () => {
        devtoolsApi.addConstructToPackage(PACKAGE_NAME, PACKAGE_ENTITY_GLOBALID);
        expect(apiBase.put).toHaveBeenCalledWith(`devtools/packages/${PACKAGE_NAME}/constructs/${PACKAGE_ENTITY_GLOBALID}`, null);
    });

    it('removeConstructFromPackage() sends the correct DELETE request', () => {
        devtoolsApi.removeConstructFromPackage(PACKAGE_NAME, PACKAGE_ENTITY_GLOBALID);
        expect(apiBase.delete).toHaveBeenCalledWith(`devtools/packages/${PACKAGE_NAME}/constructs/${PACKAGE_ENTITY_GLOBALID}`);
    });

    // CONTENTREPOSITORIES ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    it('getContentrepositories() sends the correct GET request', () => {
        devtoolsApi.getContentrepositories(PACKAGE_NAME, OPTIONS);
        expect(apiBase.get).toHaveBeenCalledWith(`devtools/packages/${PACKAGE_NAME}/contentrepositories`, OPTIONS);
    });

    it('getContentRepository() sends the correct GET request', () => {
        devtoolsApi.getContentRepository(PACKAGE_NAME, PACKAGE_ENTITY_GLOBALID);
        expect(apiBase.get).toHaveBeenCalledWith(`devtools/packages/${PACKAGE_NAME}/contentrepositories/${PACKAGE_ENTITY_GLOBALID}`);
    });

    it('addContentRepositoryToPackage() sends the correct PUT request', () => {
        devtoolsApi.addContentRepositoryToPackage(PACKAGE_NAME, PACKAGE_ENTITY_GLOBALID);
        expect(apiBase.put).toHaveBeenCalledWith(`devtools/packages/${PACKAGE_NAME}/contentrepositories/${PACKAGE_ENTITY_GLOBALID}`, null);
    });

    it('removeContentRepositoryFromPackage() sends the correct DELETE request', () => {
        devtoolsApi.removeContentRepositoryFromPackage(PACKAGE_NAME, PACKAGE_ENTITY_GLOBALID);
        expect(apiBase.delete).toHaveBeenCalledWith(`devtools/packages/${PACKAGE_NAME}/contentrepositories/${PACKAGE_ENTITY_GLOBALID}`);
    });

    // CR_FRAGMENTS ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    it('getContentRepositoryFragments() sends the correct GET request', () => {
        devtoolsApi.getContentRepositoryFragments(PACKAGE_NAME, OPTIONS);
        expect(apiBase.get).toHaveBeenCalledWith(`devtools/packages/${PACKAGE_NAME}/cr_fragments`, OPTIONS);
    });

    it('getContentRepositoryFragment() sends the correct GET request', () => {
        devtoolsApi.getContentRepositoryFragment(PACKAGE_NAME, PACKAGE_ENTITY_GLOBALID);
        expect(apiBase.get).toHaveBeenCalledWith(`devtools/packages/${PACKAGE_NAME}/cr_fragments/${PACKAGE_ENTITY_GLOBALID}`);
    });

    it('addContentRepositoryFragmentToPackage() sends the correct PUT request', () => {
        devtoolsApi.addContentRepositoryFragmentToPackage(PACKAGE_NAME, PACKAGE_ENTITY_GLOBALID);
        expect(apiBase.put).toHaveBeenCalledWith(`devtools/packages/${PACKAGE_NAME}/cr_fragments/${PACKAGE_ENTITY_GLOBALID}`, null);
    });

    it('removeContentRepositoryFragmentFromPackage() sends the correct DELETE request', () => {
        devtoolsApi.removeContentRepositoryFragmentFromPackage(PACKAGE_NAME, PACKAGE_ENTITY_GLOBALID);
        expect(apiBase.delete).toHaveBeenCalledWith(`devtools/packages/${PACKAGE_NAME}/cr_fragments/${PACKAGE_ENTITY_GLOBALID}`);
    });

    // DATASOURCE ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    it('getDataSources() sends the correct GET request', () => {
        devtoolsApi.getDataSources(PACKAGE_NAME, OPTIONS);
        expect(apiBase.get).toHaveBeenCalledWith(`devtools/packages/${PACKAGE_NAME}/datasources`, OPTIONS);
    });

    it('getDataSource() sends the correct GET request', () => {
        devtoolsApi.getDataSource(PACKAGE_NAME, PACKAGE_ENTITY_GLOBALID);
        expect(apiBase.get).toHaveBeenCalledWith(`devtools/packages/${PACKAGE_NAME}/datasources/${PACKAGE_ENTITY_GLOBALID}`);
    });

    it('addDataSourceToPackage() sends the correct PUT request', () => {
        devtoolsApi.addDataSourceToPackage(PACKAGE_NAME, PACKAGE_ENTITY_GLOBALID);
        expect(apiBase.put).toHaveBeenCalledWith(`devtools/packages/${PACKAGE_NAME}/datasources/${PACKAGE_ENTITY_GLOBALID}`, null);
    });

    it('removeDataSourceFromPackage() sends the correct DELETE request', () => {
        devtoolsApi.removeDataSourceFromPackage(PACKAGE_NAME, PACKAGE_ENTITY_GLOBALID);
        expect(apiBase.delete).toHaveBeenCalledWith(`devtools/packages/${PACKAGE_NAME}/datasources/${PACKAGE_ENTITY_GLOBALID}`);
    });

    // OBJECTPROPERTY ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    it('getObjectproperties() sends the correct GET request', () => {
        devtoolsApi.getObjectproperties(PACKAGE_NAME, OPTIONS);
        expect(apiBase.get).toHaveBeenCalledWith(`devtools/packages/${PACKAGE_NAME}/objectproperties`, OPTIONS);
    });

    it('getObjectProperty() sends the correct GET request', () => {
        devtoolsApi.getObjectProperty(PACKAGE_NAME, PACKAGE_ENTITY_GLOBALID);
        expect(apiBase.get).toHaveBeenCalledWith(`devtools/packages/${PACKAGE_NAME}/objectproperties/${PACKAGE_ENTITY_GLOBALID}`);
    });

    it('addObjectPropertyToPackage() sends the correct PUT request', () => {
        devtoolsApi.addObjectPropertyToPackage(PACKAGE_NAME, PACKAGE_ENTITY_GLOBALID);
        expect(apiBase.put).toHaveBeenCalledWith(`devtools/packages/${PACKAGE_NAME}/objectproperties/${PACKAGE_ENTITY_GLOBALID}`, null);
    });

    it('removeObjectPropertyFromPackage() sends the correct DELETE request', () => {
        devtoolsApi.removeObjectPropertyFromPackage(PACKAGE_NAME, PACKAGE_ENTITY_GLOBALID);
        expect(apiBase.delete).toHaveBeenCalledWith(`devtools/packages/${PACKAGE_NAME}/objectproperties/${PACKAGE_ENTITY_GLOBALID}`);
    });

    // TEMPLATES ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    it('getTemplates() sends the correct GET request', () => {
        devtoolsApi.getTemplates(PACKAGE_NAME, OPTIONS_WITH_SKIPCOUNT);
        expect(apiBase.get).toHaveBeenCalledWith(`devtools/packages/${PACKAGE_NAME}/templates`, OPTIONS_WITH_SKIPCOUNT);
    });

    it('getTemplate() sends the correct GET request', () => {
        devtoolsApi.getTemplate(PACKAGE_NAME, PACKAGE_ENTITY_GLOBALID);
        expect(apiBase.get).toHaveBeenCalledWith(`devtools/packages/${PACKAGE_NAME}/templates/${PACKAGE_ENTITY_GLOBALID}`);
    });

    it('addTemplateToPackage() sends the correct PUT request', () => {
        devtoolsApi.addTemplateToPackage(PACKAGE_NAME, PACKAGE_ENTITY_GLOBALID);
        expect(apiBase.put).toHaveBeenCalledWith(`devtools/packages/${PACKAGE_NAME}/templates/${PACKAGE_ENTITY_GLOBALID}`, null);
    });

    it('removeTemplateFromPackage() sends the correct DELETE request', () => {
        devtoolsApi.removeTemplateFromPackage(PACKAGE_NAME, PACKAGE_ENTITY_GLOBALID);
        expect(apiBase.delete).toHaveBeenCalledWith(`devtools/packages/${PACKAGE_NAME}/templates/${PACKAGE_ENTITY_GLOBALID}`);
    });

});

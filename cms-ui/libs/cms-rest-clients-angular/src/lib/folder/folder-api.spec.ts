// import { CropResizeParameters, FolderListOptions, FolderListRequest, RotateParameters } from '@gentics/cms-models';
// import { of as observableOf } from 'rxjs';

// import { TestApplicationState } from '../../../../../apps/editor-ui/src/app/state/test-application-state.mock';
// import { MockApiBase } from '../base/api-base.mock';
// import { FolderApi } from './folder-api';

// describe('FolderApi', () => {

//     let apiBase: MockApiBase;
//     let folderApi: FolderApi;
//     let listOptions: FolderListOptions;
//     let expectedListRequest: FolderListRequest;
//     let state: TestApplicationState;
//     beforeEach(() => {
//         apiBase = new MockApiBase();
//         folderApi = new FolderApi(apiBase as any, state);

//         listOptions = {
//             addPrivileges: true,
//             maxItems: 10,
//             privilegeMap: true,
//             recursive: false,
//             skipCount: 0,
//         };
//         expectedListRequest = apiBase.createListRequest(1234, listOptions);
//     });

//     it('cancelEditing() sends the correct POST request', () => {
//         const PAGEID = 1234;
//         folderApi.cancelEditing(PAGEID);
//         expect(apiBase.post).toHaveBeenCalledWith('page/cancel/' + PAGEID, {});
//     });

//     it('copyFile() sends the correct POST request', () => {
//         folderApi.copyFile(123, 'file-copy.jpg');
//         expect(apiBase.post).toHaveBeenCalledWith('file/copy', {
//             file: { id: 123 },
//             newFilename: 'file-copy.jpg',
//         });
//     });

//     it('copyPages() sends the correct POST request', () => {
//         const SOURCENODE = 1;
//         const TARGETNODE = 2;
//         const TARGETFOLDER = 77;

//         folderApi.copyPages([11, 22, 33], SOURCENODE, TARGETFOLDER, TARGETNODE);
//         expect(apiBase.post).toHaveBeenCalledWith('page/copy', {
//             createCopy: true,
//             nodeId: SOURCENODE,
//             sourcePageIds: [11, 22, 33],
//             targetFolders: [{
//                 id: TARGETFOLDER,
//                 channelId: TARGETNODE,
//             }],
//         });
//     });

//     describe('createFolder()', () => {

//         it('sends the correct POST request', () => {
//         const folder = {
//             name: 'NewFolder',
//             motherId: 1111,
//             nodeId: 2222,
//             publishDir: '/new-folder',
//                 description: 'A new folder',
//         };
//         folderApi.createFolder(folder);
//         expect(apiBase.post).toHaveBeenCalledWith('folder/create', folder);
//     });

//         it('sends the correct POST request with optional failOnDuplicate flag', () => {
//             const folder = {
//                 name: 'NewFolder',
//                 motherId: 1111,
//                 nodeId: 2222,
//                 publishDir: '/new-folder',
//                 description: 'A new folder',
//                 failOnDuplicate: true,
//             };
//             folderApi.createFolder(folder);
//             expect(apiBase.post).toHaveBeenCalledWith('folder/create', folder);
//         });

//     });

//     it('createPage() sends the correct POST request', () => {
//         const page = {
//             pageName: 'New page',
//             nodeId: 1111,
//             folderId: 2222,
//             language: 'en',
//             fileName: 'new-page.en.html',
//             description: 'a new page',
//             priority: 50,
//             templateId: 2,
//         };
//         folderApi.createPage(page);
//         expect(apiBase.post).toHaveBeenCalledWith('page/create', page);
//     });

//     it('createPageVariation() sends the correct POST request', () => {
//         const variation = {
//             nodeId: 1,
//             folderId: 2,
//             variantId: 3,
//             variantChannelId: 4,
//         };
//         folderApi.createPageVariation(variation);
//         expect(apiBase.post).toHaveBeenCalledWith('page/create', variation);
//     });

//     it('cropAndResizeImage() sends the correct POST request', () => {
//         const params: CropResizeParameters = {
//             image: {
//                 id: 1,
//             },
//             copyFile: true,
//             cropHeight: 600,
//             cropStartX: 100,
//             cropStartY: 50,
//             cropWidth: 800,
//             height: 900,
//             mode: 'cropandresize',
//             resizeMode: 'force',
//             targetFormat: 'jpg',
//             width: 500,
//         };
//         folderApi.cropAndResizeImage(params);
//         expect(apiBase.post).toHaveBeenCalledWith('image/resize', params);
//     });

//     it('deleteFromWastebin() sends the correct POST request', () => {
//         folderApi.deleteFromWastebin('page', [1, 2, 3]);
//         expect(apiBase.post).toHaveBeenCalledWith('page/wastebin/delete', { ids: [1, 2, 3] });
//     });

//     it('deleteItem() sends the correct POST request', () => {
//         folderApi.deleteItem('image', 123);
//         expect(apiBase.post).toHaveBeenCalledWith('image/delete/123', { id: 123 });
//     });

//     it('getBreadcrumbs() sends the correct GET request', () => {
//         folderApi.getBreadcrumbs(1234, { nodeId: 5 });
//         expect(apiBase.get).toHaveBeenCalledWith('folder/breadcrumb/1234', { nodeId: 5 });
//     });

//     it('getFiles() sends the correct GET request', () => {
//         folderApi.getFiles(1234, listOptions);
//         expect(apiBase.get).toHaveBeenCalledWith('folder/getFiles/1234', expectedListRequest);
//     });

//     it('getFolders() sends the correct GET request', () => {
//         folderApi.getFolders(1234, listOptions);
//         expect(apiBase.get).toHaveBeenCalledWith('folder/getFolders/1234', expectedListRequest);
//     });

//     it('getImages() sends the correct GET request', () => {
//         folderApi.getImages(1234, listOptions);
//         expect(apiBase.get).toHaveBeenCalledWith('folder/getImages/1234', expectedListRequest);
//     });

//     it('getInheritance() sends the correct GET request', () => {
//         folderApi.getInheritance('page', 1234, 999);
//         expect(apiBase.get).toHaveBeenCalledWith('page/disinherit/1234', { nodeId: 999 });
//     });

//     describe('getItem()', () => {
//         it('sends the correct GET request', () => {
//             const options = { maxItems: 10, search: 'searchtext', recursive: true };
//             folderApi.getItem(1234, 'folder', options);
//             expect(apiBase.get).toHaveBeenCalledWith('folder/load/1234', options);
//         });

//         it('sets a "type" for templates', () => {
//             const response: any = {
//                 responseInfo: { responseCode: 'OK' },
//                 template: { id: 1234 },
//             };
//             apiBase.get = () => observableOf(response);

//             expect(response.template.type).not.toBe('template');

//             let result: any;
//             folderApi.getItem(1234, 'template', {})
//                 .subscribe(res => result = res);

//             expect(result.template.type).toBe('template');
//         });
//     });

//     it('getItems() sends the correct GET request', () => {
//         const PARENTFOLDER = 1234;
//         folderApi.getItems(PARENTFOLDER, ['page', 'file', 'image']);
//         expect(apiBase.get).toHaveBeenCalledWith('folder/getItems/1234', jasmine.objectContaining({
//             id: 1234,
//             type: ['page', 'file', 'image'],
//         }));
//     });

//     it('getLanguagesOfNode() sends the correct GET request', () => {
//         folderApi.getLanguagesOfNode(1234);
//         expect(apiBase.get).toHaveBeenCalledWith('node/getLanguages/1234');
//     });

//     it('getNodeFeatures() sends the correct GET request', () => {
//         const NODEID = 1234;
//         folderApi.getNodeFeatures(NODEID);
//         expect(apiBase.get).toHaveBeenCalledWith('node/features/' + NODEID);
//     });

//     describe('getNodes()', () => {

//         beforeEach(() => {
//             apiBase.get = jasmine.createSpy('get').and.returnValues(
//                 observableOf({
//                     folders: [
//                         { id: 1, nodeId: 111 },
//                         { id: 2, nodeId: 222 },
//                         { id: 3, nodeId: 333 },
//                     ],
//                 }),
//                 observableOf({ node: { id: 111 } }),
//                 observableOf({ node: { id: 222 } }),
//                 observableOf({ node: { id: 333 } }),
//             );
//         });

//         it('sends the correct initial GET request', () => {
//             folderApi.getNodes();
//             expect(apiBase.get).toHaveBeenCalledWith('folder/getFolders/0', jasmine.objectContaining({
//                 id: 0,
//             }));
//         });

//         it('sends GET requests for each returned node', () => {
//             folderApi.getNodes().subscribe(() => {});

//             expect(apiBase.get).toHaveBeenCalledTimes(4);
//             expect(apiBase.get).toHaveBeenCalledWith('folder/getFolders/0', jasmine.anything());
//             expect(apiBase.get).toHaveBeenCalledWith('node/load/111');
//             expect(apiBase.get).toHaveBeenCalledWith('node/load/222');
//             expect(apiBase.get).toHaveBeenCalledWith('node/load/333');
//         });

//         it('combines the responses of all nodes with the initial request', () => {
//             let result: any;
//             folderApi.getNodes().subscribe(res => result = res);

//             expect(result).toEqual({
//                 folders: [
//                     { id: 1, nodeId: 111 },
//                     { id: 2, nodeId: 222 },
//                     { id: 3, nodeId: 333 },
//                 ],
//                 nodes: [
//                     { id: 111 },
//                     { id: 222 },
//                     { id: 333 },
//                 ],
//             });
//         });
//     });

//     it('getTemplates() sends the correct GET request', () => {
//         folderApi.getTemplates(1234, listOptions);
//         const expectedRequest = { ...expectedListRequest, ...{ checkPermission: false } };
//         expect(apiBase.get).toHaveBeenCalledWith('folder/getTemplates/1234', expectedRequest);
//     });

//     it('getAllTemplatesOfNode() sends the correct GET request', () => {
//         folderApi.getAllTemplatesOfNode(1234);
//         const expectedRequest = { };
//         expect(apiBase.get).toHaveBeenCalledWith('node/1234/templates', expectedRequest);
//     });

//     it('getTotalUsage() sends the correct GET request', () => {
//         const NODEID = 1234;
//         folderApi.getTotalUsage('page', [1, 2, 3], NODEID);
//         expect(apiBase.get).toHaveBeenCalledWith('page/usage/total?id=1&id=2&id=3', { nodeId: NODEID });
//     });

//     it('getLocalizations() sends the correct GET request', () => {
//         folderApi.getLocalizations('page', 123);
//         expect(apiBase.get).toHaveBeenCalledWith('page/localizations/123');
//     });

//     it('localizeItem() sends the correct POST request', () => {
//         const PAGEID = 1234;
//         const NODEID = 5678;
//         folderApi.localizeItem('page', PAGEID, NODEID);
//         expect(apiBase.post).toHaveBeenCalledWith('page/localize/' + PAGEID, { channelId: NODEID });
//     });

//     it('moveItems() sends the correct POST request', () => {
//         const TARGETFOLDER = 777;
//         const TARGETNODE = 99;
//         folderApi.moveItems('file', [1, 2, 3], TARGETFOLDER, TARGETNODE);
//         expect(apiBase.post).toHaveBeenCalledWith('file/move', {
//             ids: [1, 2, 3],
//             folderId: TARGETFOLDER,
//             nodeId: TARGETNODE,
//         });
//     });

//     it('publishPage() sends the correct POST request', () => {
//         folderApi.publishPage(1, 2);
//         expect(apiBase.post).toHaveBeenCalledWith('page/publish/1', { alllang: false }, { nodeId: 2 });
//     });

//     it('publishPageAt() sends the correct POST request', () => {
//         const TIME = Math.floor(Date.now() / 1000) + 10 * 24 * 60;
//         folderApi.publishPageAt(1, 2, TIME, true);
//         expect(apiBase.post).toHaveBeenCalledWith('page/publish/1', { alllang: false, at: TIME, keepVersion: true }, { nodeId: 2 });
//     });

//     describe('publishPages()', () => {
//         it('sends the correct POST request for one page', () => {
//             folderApi.publishPages([1], 777);
//             expect(apiBase.post).toHaveBeenCalledWith('page/publish/1', { alllang: false }, { nodeId: 777 });
//         });

//         it('sends the correct POST request for multiple pages', () => {
//             folderApi.publishPages([1, 2, 3], 777);
//             expect(apiBase.post).toHaveBeenCalledWith('page/publish', { alllang: false, ids: [1, 2, 3] }, { nodeId: 777 });
//         });
//     });

//     it('pushToMaster() sends the correct POST request', () => {
//         folderApi.pushToMaster('folder', 11, {
//             channelId: 33,
//             masterId: 22,
//             recursive: true,
//             types: ['page', 'file', 'image'],
//         });
//         expect(apiBase.post).toHaveBeenCalledWith('folder/push2master/11', {
//             channelId: 33,
//             masterId: 22,
//             recursive: true,
//             types: ['page', 'file', 'image'],
//         });
//     });

//     it('replaceFile() sends the correct file upload POST request', () => {
//         const testFile = { name: 'file.jpg' } as any;
//         folderApi.replaceFile('image', 1, testFile, { folderId: 99, nodeId: 777 });
//         expect(apiBase.upload).toHaveBeenCalledWith([testFile], 'file/save/1', {
//             fileField: 'fileBinaryData',
//             fileNameField: 'fileName',
//             params: {
//                 folderId: 99,
//                 nodeId: 777,
//                 name: jasmine.stringMatching(/^tmpfile/),
//             },
//         },
//         undefined);
//         });

//     it('replaceFile() sends the correct file upload POST request with existing file name', () => {
//         const testFile = { name: 'file.jpg' } as any;
//         const fileName = 'existingFileName';
//         folderApi.replaceFile('image', 1, testFile, { folderId: 99, nodeId: 777 }, fileName);
//         expect(apiBase.upload).toHaveBeenCalledWith([testFile], 'file/save/1', {
//             fileField: 'fileBinaryData',
//             fileNameField: 'fileName',
//             params: {
//                 folderId: 99,
//                 nodeId: 777,
//                 name: jasmine.stringMatching(/^tmpfile/),
//             },
//         },
//         fileName);
//     });

//     it('restoreFromWastebin() sends the correct POST request', () => {
//         folderApi.restoreFromWastebin('page', [1, 2, 3]);
//         expect(apiBase.post).toHaveBeenCalledWith('page/wastebin/restore', { ids: [1, 2, 3] });
//     });

//     it('restorePageVersion() sends the correct POST request', () => {
//         const VERSION_TIMESTAMP = Math.floor(Date.now() / 1000) - 120;
//         folderApi.restorePageVersion(1, VERSION_TIMESTAMP);
//         expect(apiBase.post).toHaveBeenCalledWith('page/restore/1', {}, { version: VERSION_TIMESTAMP });
//     });

//     it('rotateImage() sends the correct POST request', () => {
//         const params: RotateParameters = {
//             image: {
//                 id: 1,
//             },
//             copyFile: false,
//             targetFormat: 'jpg',
//             rotate: 'ccw',
//         };
//         folderApi.rotateImage(params);
//         expect(apiBase.post).toHaveBeenCalledWith('image/rotate', params);
//     });

//     it('searchPages() sends the correct GET request', () => {
//         const NODEID = 777;
//         folderApi.searchPages(NODEID, {
//             folder: true,
//             langvars: true,
//             liveUrl: 'my.host.com/page.html',
//             template: true,
//             update: false,
//         });

//         expect(apiBase.get).toHaveBeenCalledWith('page/search', {
//             folder: true,
//             langvars: true,
//             liveUrl: 'my.host.com/page.html',
//             nodeId: NODEID,
//             template: true,
//             update: false,
//         });
//     });

//     it('setFolderStartpage() sends the correct POST request', () => {
//         const FOLDERID = 1;
//         const PAGEID = 2;
//         folderApi.setFolderStartpage(FOLDERID, PAGEID);
//         expect(apiBase.post).toHaveBeenCalledWith('folder/startpage/' + FOLDERID, { pageId: PAGEID });
//     });

//     it('setInheritance() sends the correct POST request', () => {
//         const FOLDERID = 1234;
//         folderApi.setInheritance('folder', FOLDERID, {
//             disinherit: [1, 2, 3],
//             disinheritDefault: false,
//             reinherit: [],
//         });

//         expect(apiBase.post).toHaveBeenCalledWith('folder/disinherit/' + FOLDERID, {
//             disinherit: [1, 2, 3],
//             disinheritDefault: false,
//             reinherit: [],
//         });
//     });

//     it('sanitizeFolderPath() sends the correct POST request', () => {
//         const folder = {
//             nodeId: 1,
//             publishDir: 'new folder',
//         };
//         folderApi.sanitizeFolderPath(folder);
//         expect(apiBase.post).toHaveBeenCalledWith('folder/sanitize/publishDir', folder);
//     });

//     it('takePageOffline() sends the correct POST request', () => {
//         folderApi.takePageOffline(1234);
//         expect(apiBase.post).toHaveBeenCalledWith('page/takeOffline/1234', {});
//     });

//     it('translatePage() sends the correct POST request', () => {
//         const NODEID = 1;
//         const PAGEID = 2;
//         folderApi.translatePage(NODEID, PAGEID, 'de');
//         expect(apiBase.post).toHaveBeenCalledWith('page/translate/' + PAGEID, {}, {
//             channelId: NODEID,
//             language: 'de',
//         });
//     });

//     it('unlocalizeItem() sends the correct POST request', () => {
//         folderApi.unlocalizeItem('folder', 1234, 777);
//         expect(apiBase.post).toHaveBeenCalledWith('folder/unlocalize/1234', {
//             channelId: 777,
//             recursive: false,
//         });
//     });

//     it('updateItem() sends the correct POST request', () => {
//         folderApi.updateItem('page', 1234, {
//             description: 'New description',
//             fileName: 'new-filename.html',
//         });
//         expect(apiBase.post).toHaveBeenCalledWith('page/save/1234', {
//             page: {
//                 description: 'New description',
//                 fileName: 'new-filename.html',
//             },
//         });
//     });

//     it('updateItem() sends the correct POST request with deriveFileName parameter if fileName is empty', () => {
//         folderApi.updateItem('page', 1234, {
//             description: 'New description',
//             fileName: '',
//         });
//         expect(apiBase.post).toHaveBeenCalledWith('page/save/1234', {
//             page: {
//                 description: 'New description',
//                 fileName: '',
//             },
//             deriveFileName: true,
//         });
//     });

//     it('updateItem() sends the correct POST request if options are specified', () => {
//         folderApi.updateItem('page', 1234, {
//             description: 'New description',
//             fileName: 'new-filename.html',
//         }, {
//             unlock: true,
//         });
//         expect(apiBase.post).toHaveBeenCalledWith('page/save/1234', {
//             page: {
//                 description: 'New description',
//                 fileName: 'new-filename.html',
//             },
//             unlock: true,
//         });
//     });

//     it('upload() sends the correct file upload POST request', () => {
//         const testFile: any = { name: 'test-file.csv' };
//         folderApi.upload('file', [testFile], {
//             folderId: 1234,
//             nodeId: 777,
//         });

//         expect(apiBase.upload).toHaveBeenCalledWith([testFile], 'file/create', {
//             fileField: 'fileBinaryData',
//             fileNameField: 'fileName',
//             params: {
//                 folderId: 1234,
//                 nodeId: 777,
//             },
//         });
//     });

//     describe('searchItems()', () => {

//         let baseQuery: any;
//         const testTimestamp = 1508244565;
//         const testDateString = '2017-10-17';

//         beforeEach(() => {
//             baseQuery = {
//                 query: {
//                     bool: {
//                         must: [],
//                     },
//                 },
//                 from: 0,
//                 size: 10,
//                 _source: false,
//             };

//             apiBase.get = jasmine.createSpy('get').and.returnValue(observableOf({ folders: [{ id: 1 }, { id: 2 }]}));
//         });

//         it('makes a POST request with base query object and creates query parameters for a global search', () => {
//             folderApi.searchItems('page', 1).subscribe(() => {});

//             expect(apiBase.post).toHaveBeenCalledWith(`elastic/page/_search`, baseQuery, { recursive: true, folder: true });
//         });

//         it('creates correct query parameters for node filter', () => {
//             folderApi.searchItems('page', 1, { node: 42 })
//                 .subscribe(() => {});

//             expect(apiBase.post).toHaveBeenCalledWith(`elastic/page/_search`, baseQuery, { folderId: 1, nodeId: 42, recursive: true, folder: true });
//         });

//         it('always sets the query parameter recursive=true', () => {
//             folderApi.searchItems('page', 1, { node: 4711 }, { recursive: false }).subscribe(() => {});

//             expect(apiBase.post).toHaveBeenCalledWith(`elastic/page/_search`, baseQuery, { folderId: 1, nodeId: 4711, recursive: true, folder: true });
//         });

//         it('creates correct Elastic query for template filter', () => {
//             folderApi.searchItems('page', 1, { template: 5 })
//                 .subscribe(() => {});

//             const expectedQuery = [{ term: { templateId: 5 } }];
//             expect(apiBase.post.calls.argsFor(0)[1].query.bool.must).toEqual(expectedQuery);
//         });

//         it('creates correct Elastic query for editor filter', () => {
//             folderApi.searchItems('page', 1, { editor: 7 })
//                 .subscribe(() => {});

//             const expectedQuery = [{ term: { editorId: 7 } }];
//             expect(apiBase.post.calls.argsFor(0)[1].query.bool.must).toEqual(expectedQuery);
//         });

//         it('creates correct Elastic query for creator filter', () => {
//             folderApi.searchItems('page', 1, { creator: 7 })
//                 .subscribe(() => {});

//             const expectedQuery = [{ term: { creatorId: 7 } }];
//             expect(apiBase.post.calls.argsFor(0)[1].query.bool.must).toEqual(expectedQuery);
//         });

//         it('creates correct Elastic query for publisher filter', () => {
//             folderApi.searchItems('page', 1, { publisher: 7 })
//                 .subscribe(() => {});

//             const expectedQuery = [{ term: { publisherId: 7 } }];
//             expect(apiBase.post.calls.argsFor(0)[1].query.bool.must).toEqual(expectedQuery);
//         });

//         it('creates correct Elastic query for edited date since', () => {
//             folderApi.searchItems('page', 1, { edited: { timestamp: testTimestamp, since: true } })
//                 .subscribe(() => {});

//             const expectedQuery = [{
//                 range: {
//                     edited: {
//                         format: 'yyyy-MM-dd',
//                         gte: testDateString,
//                     },
//                 },
//             }];
//             expect(apiBase.post.calls.argsFor(0)[1].query.bool.must).toEqual(expectedQuery);
//         });

//         it('creates correct Elastic query for edited date on', () => {
//             folderApi.searchItems('page', 1, { edited: { timestamp: testTimestamp, since: false } })
//                 .subscribe(() => {});

//             const expectedQuery = [{
//                 range: {
//                     edited: {
//                         format: 'yyyy-MM-dd',
//                         gte: testDateString,
//                         lte: testDateString,
//                     },
//                 },
//             }];
//             expect(apiBase.post.calls.argsFor(0)[1].query.bool.must).toEqual(expectedQuery);
//         });

//         describe('query_string query', () => {

//             it('includes "name", "path", and "description" fields for folders', () => {
//                 const QUERY = 'foo';
//                 folderApi.searchItems('folder', 1, {}, { search: QUERY }).subscribe(() => {});

//                 const expectedQuery = [{
//                     query_string: {
//                         fields: ['name', 'path', 'description'],
//                         query: QUERY,
//                     },
//                 }];
//                 expect(apiBase.post.calls.argsFor(0)[1].query.bool.must).toEqual(expectedQuery);
//             });

//             it('includes "name", "path", "description", and "niceUrl" fields for pages', () => {
//                 const QUERY = 'foo';
//                 folderApi.searchItems('page', 1, {}, { search: QUERY }).subscribe(() => {});

//                 const expectedQuery = [{
//                     query_string: {
//                         fields: ['name', 'path', 'description', 'niceUrl'],
//                         query: QUERY,
//                     },
//                 }];
//                 expect(apiBase.post.calls.argsFor(0)[1].query.bool.must).toEqual(expectedQuery);
//             });

//             it('includes "name", "path", and "description" fields for files', () => {
//                 const QUERY = 'foo';
//                 folderApi.searchItems('file', 1, {}, { search: QUERY }).subscribe(() => {});

//                 const expectedQuery = [{
//                     query_string: {
//                         fields: ['name', 'path', 'description'],
//                         query: QUERY,
//                     },
//                 }];
//                 expect(apiBase.post.calls.argsFor(0)[1].query.bool.must).toEqual(expectedQuery);
//             });

//             it('includes "name", "path", and "description" fields for images', () => {
//                 const QUERY = 'foo';
//                 folderApi.searchItems('image', 1, {}, { search: QUERY }).subscribe(() => {});

//                 const expectedQuery = [{
//                     query_string: {
//                         fields: ['name', 'path', 'description'],
//                         query: QUERY,
//                     },
//                 }];
//                 expect(apiBase.post.calls.argsFor(0)[1].query.bool.must).toEqual(expectedQuery);
//             });

//             it('adds the language code for page searches', () => {
//                 const QUERY = 'foo';
//                 const LANGUAGE_CODE = 'en';
//                 folderApi.searchItems('page', 1, {}, { search: QUERY, language: LANGUAGE_CODE }).subscribe(() => {});

//                 const expectedQuery = [{
//                     query_string: {
//                         fields: ['name', 'path', 'description', 'niceUrl'],
//                         query: QUERY,
//                     },
//                 }, {
//                     term: { languageCode: LANGUAGE_CODE },
//                 }];
//                 expect(apiBase.post.calls.argsFor(0)[1].query.bool.must).toEqual(expectedQuery);
//             });

//             describe('id searches', () => {

//                 it('includes "name", "path", "description", and "id" fields for folders when the query is numeric', () => {
//                     const QUERY = '123';
//                     folderApi.searchItems('image', 1, {}, { search: QUERY }).subscribe(() => {});

//                     const expectedQuery = [{
//                         query_string: {
//                             fields: ['name', 'path', 'description', 'id'],
//                             query: QUERY,
//                         },
//                     }];
//                     expect(apiBase.post.calls.argsFor(0)[1].query.bool.must).toEqual(expectedQuery);
//                 });

//                 function assertQueryGivenSearchTerm(term: string, expectedQueryString: string): void {
//                     folderApi.searchItems('folder', 1, {}, { search: term }).subscribe(() => {});
//                     const expectedQuery = [{
//                         query_string: {
//                             fields: ['name', 'path', 'description'],
//                             query: expectedQueryString,
//                         },
//                     }];
//                     expect(apiBase.post.calls.mostRecent().args[1].query.bool.must).toEqual(expectedQuery);
//                 }

//                 it('allows integer id searches', () => {
//                     assertQueryGivenSearchTerm('id:2', 'id:2');
//                     assertQueryGivenSearchTerm('id: 4', 'id: 4');
//                     assertQueryGivenSearchTerm('id: 4 id:32', 'id: 4 id:32');
//                     assertQueryGivenSearchTerm('id:2 name:bar*', 'id:2 name:bar*');
//                     assertQueryGivenSearchTerm('id: 665 +name:bar*', 'id: 665 +name:bar*');
//                     assertQueryGivenSearchTerm('+name:bar* id:44', '+name:bar* id:44');
//                     assertQueryGivenSearchTerm('id:  4 +name:bar* id:44', 'id:  4 +name:bar* id:44');
//                 });

//                 it('removes non-numeric id searches', () => {
//                     assertQueryGivenSearchTerm('id:foo', '');
//                     assertQueryGivenSearchTerm('id: foo', '');
//                     assertQueryGivenSearchTerm('id:   foo', '');
//                     assertQueryGivenSearchTerm('id: foo bar*', 'bar*');
//                     assertQueryGivenSearchTerm('id: foo +name:bar*', '+name:bar*');
//                     assertQueryGivenSearchTerm('id: 2 id: foo +name:bar*', 'id: 2 +name:bar*');
//                 });
//             });

//         });

//     });

// });

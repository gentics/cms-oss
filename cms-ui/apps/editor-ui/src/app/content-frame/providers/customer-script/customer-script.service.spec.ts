import { HttpClient } from '@angular/common/http';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ApiBase } from '@editor-ui/app/core/providers/api';
import { EntityResolver } from '@editor-ui/app/core/providers/entity-resolver/entity-resolver';
import { ErrorHandler } from '@editor-ui/app/core/providers/error-handler/error-handler.service';
import { EditorOverlayService } from '@editor-ui/app/editor-overlay/providers/editor-overlay.service';
import { RepositoryBrowserClient } from '@editor-ui/app/shared/providers';
import { ApplicationStateService, STATE_MODULES } from '@editor-ui/app/state';
import { MockAppState, TestApplicationState } from '@editor-ui/app/state/test-application-state.mock';
import { TagEditorService } from '@editor-ui/app/tag-editor';
import { EditMode } from '@gentics/cms-integration-api-models';
import { ItemInNode, Tag } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { GCMSTestRestClientService } from '@gentics/cms-rest-client-angular/testing';
import { GenticsUICoreModule, ModalService } from '@gentics/ui-core';
import { NgxsModule } from '@ngxs/store';
import { of } from 'rxjs';
import { PostLoadScript } from '../../components/content-frame/custom-scripts/post-load';
import { PreLoadScript } from '../../components/content-frame/custom-scripts/pre-load';
import { AlohaGlobal, CNParentWindow, CNWindow } from '../../models/content-frame';
import { AlohaIntegrationService } from '../aloha-integration/aloha-integration.service';
import { DynamicOverlayService } from '../dynamic-overlay/dynamic-overlay.service';
import { CustomerScriptService } from './customer-script.service';

let mockCustomerScript = ' module.exports = function(GCMSUI) {}; ';

describe('CustomerScriptService', () => {

    const mockDocument = {} as any;
    const mockScriptHost = {} as any;
    const mockPage = { contentGroupId: 5, type: 'page' } as any;
    const mockLanguage = { id: 5, code: 'en' } as any;
    const mockAppState: MockAppState = {
        auth: {
            sid: 123,
            user: {
                id: 99,
            } as any,
        },
        entities: {
            page: { 1: mockPage },
            language: { 5: mockLanguage },
        },
        editor: {
            itemType: 'page',
            itemId: 1,
            editMode: EditMode.EDIT,
        },
        ui: {
            language: 'en',
            uiVersion: '1.2.3',
        },
    };

    let mockWindow: CNWindow;
    let customerScriptService: CustomerScriptService;
    let state: TestApplicationState;
    let apiBase: MockApiBase;
    let originalConsoleWarn: any;
    let originalPreLoadScriptRun: any;
    let originalPostLoadScriptRun: any;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                NgxsModule.forRoot(STATE_MODULES),
                GenticsUICoreModule.forRoot(),
            ],
            providers: [
                CustomerScriptService,
                EntityResolver,
                AlohaIntegrationService,
                { provide: HttpClient, useClass: MockHttpClient },
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: ApiBase, useClass: MockApiBase },
                { provide: TagEditorService, useClass: MockTagEditorService },
                { provide: EditorOverlayService, useClass: MockEditorOverlayService },
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: RepositoryBrowserClient, useClass: MockRepositoryBrowserClientService },
                { provide: GCMSRestClientService, useClass: GCMSTestRestClientService },
                { provide: ModalService, useClass: MockModalService },
                DynamicOverlayService,
            ],
        });

        mockWindow = new MockWindow() as any;
        customerScriptService = TestBed.get(CustomerScriptService);
        state = TestBed.get(ApplicationStateService);
        apiBase = TestBed.get(ApiBase);

        state.mockState(mockAppState);

        originalConsoleWarn = console.warn;
        console.warn = jasmine.createSpy('console.warn');
        mockScriptHost.setContentModified = jasmine.createSpy('setContentModified');

        originalPreLoadScriptRun = PreLoadScript.prototype.run;
        originalPostLoadScriptRun = PostLoadScript.prototype.run;
    });

    afterEach(() => {
        console.warn = originalConsoleWarn;
        PreLoadScript.prototype.run = originalPreLoadScriptRun;
        PostLoadScript.prototype.run = originalPostLoadScriptRun;
    });

    it('loadCustomerScript() throws if the script cannot be parsed', fakeAsync(() => {
        mockCustomerScript = ' @@ parse error!! @@ ';

        expect(() => {
            customerScriptService.loadCustomerScript();
            tick();
        }).toThrow();
    }));

    it('loadCustomerScript() does not throw if the script can be parsed', fakeAsync(() => {
        mockCustomerScript = 'module.exports = function(GCMSUI) {};';

        expect(() => {
            customerScriptService.loadCustomerScript();
            tick();
        }).not.toThrow();
    }));

    describe('invokeCustomerScript()', () => {

        function run(): void {
            customerScriptService.invokeCustomerScript(mockWindow, mockDocument, mockScriptHost);
        }

        it('throws correct error if module.exports is not a function', fakeAsync(() => {
            mockCustomerScript = 'module.exports = "not a function";';
            customerScriptService.loadCustomerScript();
            tick();

            expect(run).toThrowError('Customer script should export a function, got "string"');
        }));

        it('throws correct error if module.exports.default is not a function', fakeAsync(() => {
            mockCustomerScript = 'module.exports = { default: "not a function" }; ';
            customerScriptService.loadCustomerScript();
            tick();

            expect(run).toThrowError('Customer script should export a function, got "string"');
        }));

        it('logs a warning when the script throws an error', fakeAsync(() => {
            mockCustomerScript = 'module.exports = function() { a.b.c; }; ';
            customerScriptService.loadCustomerScript();
            tick();

            expect(run).toThrow();
            expect(console.warn).toHaveBeenCalledWith('Customer script threw an error:');
        }));

        it('actually executes the code in the customer script', fakeAsync(() => {
            mockCustomerScript = 'module.exports = function() { window.foo = \'bar\' }; ';
            customerScriptService.loadCustomerScript();
            tick();

            customerScriptService.invokeCustomerScript(mockWindow, mockDocument, mockScriptHost);

            expect((mockWindow as any).foo).toBe('bar');
        }));
    });

    describe('createGCMSUIObject()', () => {

        it('creates the GCMSUI object, sets it on the mockWindow, and sets GCMSUI_childIFrameInit', () => {
            const initFn = (mockWindow.parent as CNParentWindow).GCMSUI_childIFrameInit;
            const result = customerScriptService.createGCMSUIObject(mockScriptHost, mockWindow, mockDocument);

            expect(result).toBeTruthy();
            expect(mockWindow.GCMSUI).toBe(result);
            expect(mockWindow.GCMSUI_childIFrameInit).toBe(initFn);
        });

        it('runPreLoadScript() runs the pre-load script', () => {
            const result = customerScriptService.createGCMSUIObject(mockScriptHost, mockWindow, mockDocument);
            const preLoadScriptSpy = spyOn(PreLoadScript.prototype, 'run').and.stub();

            result.runPreLoadScript();
            expect(preLoadScriptSpy).toHaveBeenCalledTimes(1);
        });

        it('runPreLoadScript() catches errors in the pre-load script', () => {
            const errorMsg = 'Error in pre-load script';
            PreLoadScript.prototype.run = () => { throw new Error(errorMsg); }
            const errorHandler = TestBed.get(ErrorHandler);
            const catchErrorSpy = spyOn(errorHandler, 'catch');

            const result = customerScriptService.createGCMSUIObject(mockScriptHost, mockWindow, mockDocument);
            expect(() => result.runPreLoadScript()).not.toThrow();
            expect(catchErrorSpy).toHaveBeenCalledTimes(1);
            expect(catchErrorSpy).toHaveBeenCalledWith(jasmine.objectContaining({ message: errorMsg }), { notification: false });
        });

        it('runPostLoadScript() runs the post-load script and the customer script', () => {
            const result = customerScriptService.createGCMSUIObject(mockScriptHost, mockWindow, mockDocument);
            const postLoadRunSpy = spyOn(PostLoadScript.prototype, 'run').and.stub();
            const customerScriptSpy = spyOn(customerScriptService, 'invokeCustomerScript').and.stub();

            result.runPostLoadScript();
            expect(postLoadRunSpy).toHaveBeenCalledTimes(1);
            expect(customerScriptSpy).toHaveBeenCalledTimes(1);
            expect(customerScriptSpy).toHaveBeenCalledWith(mockWindow, mockDocument, mockScriptHost)
        });

        it('runPostLoadScript() catches errors in the pre-load script and the customer script', () => {
            const errorMsgPostLoadScript = 'Error in post-load script';
            const errorMsgCustomerScript = 'Error in customer script';
            PostLoadScript.prototype.run = () => { throw new Error(errorMsgPostLoadScript); }
            spyOn(customerScriptService, 'invokeCustomerScript').and.throwError(errorMsgCustomerScript);

            const errorHandler = TestBed.get(ErrorHandler);
            const catchErrorSpy = spyOn(errorHandler, 'catch');

            const result = customerScriptService.createGCMSUIObject(mockScriptHost, mockWindow, mockDocument);
            expect(() => result.runPostLoadScript()).not.toThrow();

            expect(catchErrorSpy).toHaveBeenCalledTimes(2);
            expect(catchErrorSpy.calls.argsFor(0)).toEqual([ jasmine.objectContaining({ message: errorMsgPostLoadScript }), { notification: false } ]);
            expect(catchErrorSpy.calls.argsFor(1)).toEqual([ jasmine.objectContaining({ message: errorMsgCustomerScript }), { notification: false } ]);
        });

        it('appState contains correct data', () => {
            const result = customerScriptService.createGCMSUIObject(mockScriptHost, mockWindow, mockDocument);

            expect(result.appState).toEqual({
                currentItem: mockPage,
                editMode: EditMode.EDIT,
                pageLanguage: mockLanguage,
                sid: 123,
                uiLanguage: 'en',
                uiVersion: '1.2.3',
                userId: 99,
            });
        });

        it('onStateChange handler is called when state changes', () => {
            const result = customerScriptService.createGCMSUIObject(mockScriptHost, mockWindow, mockDocument);
            const spy = jasmine.createSpy('handler');
            result.onStateChange(spy);

            state.mockState({ ...mockAppState, ...{ ui: { language: 'de' } } });

            expect(spy).toHaveBeenCalled();
            expect(spy.calls.argsFor(0)[0].uiLanguage).toBe('de');
        });

        it('restRequestGET() delegates to ApiBase.get()', () => {
            const result = customerScriptService.createGCMSUIObject(mockScriptHost, mockWindow, mockDocument);
            const url = 'some/url';
            const params = { foo: 'bar' };
            result.restRequestGET(url, params);

            expect(apiBase.get).toHaveBeenCalledWith(url, params);
        });

        it('restRequestGET() strips leading / before delegating', () => {
            const result = customerScriptService.createGCMSUIObject(mockScriptHost, mockWindow, mockDocument);
            const url = '/some/url';
            result.restRequestGET(url);

            expect(apiBase.get.calls.argsFor(0)[0]).toBe('some/url');
        });

        it('restRequestPOST() delegates to ApiBase.post()', () => {
            const result = customerScriptService.createGCMSUIObject(mockScriptHost, mockWindow, mockDocument);
            const url = 'some/url';
            const data = { quux: 'buux' };
            const params = { foo: 'bar' };
            result.restRequestPOST(url, data, params);

            expect(apiBase.post).toHaveBeenCalledWith(url, data, params);
        });

        it('restRequestPOST() strips leading / before delegating', () => {
            const result = customerScriptService.createGCMSUIObject(mockScriptHost, mockWindow, mockDocument);
            const url = '/some/url';
            result.restRequestPOST(url, {});

            expect(apiBase.post.calls.argsFor(0)[0]).toBe('some/url');
        });

        it('setContentModified() correctly delegates to ContentFrame', () => {
            const result = customerScriptService.createGCMSUIObject(mockScriptHost, mockWindow, mockDocument);
            result.setContentModified(true);

            expect(mockScriptHost.setContentModified).toHaveBeenCalledWith(true);
        });

        it('setContentModified() correctly delegates to ContentFrame', () => {
            const result = customerScriptService.createGCMSUIObject(mockScriptHost, mockWindow, mockDocument);
            result.setContentModified('not boolean' as any);

            expect(mockScriptHost.setContentModified).not.toHaveBeenCalled();
            expect(console.warn).toHaveBeenCalledWith('setContentModified expects a boolean value as its argument');
        });

        it('openTagEditor() correctly delegates to TagEditorService', () => {
            const mockTag = { tag: 'tag' };
            const mockTagType = { tagType: 'tagType' };
            const mockPage = { page: 'page' };
            const expectedResult = Promise.resolve(<Tag> null);

            const mockTagEditorService = TestBed.get(TagEditorService);
            spyOn(mockTagEditorService, 'openTagEditor').and.returnValue(expectedResult);

            const gcmsUi = customerScriptService.createGCMSUIObject(mockScriptHost, mockWindow, mockDocument);
            const actualResult = gcmsUi.openTagEditor(<any> mockTag, <any> mockTagType, <any> mockPage, { withDelete: false });

            expect(mockTagEditorService.openTagEditor).toHaveBeenCalledWith(mockTag, mockTagType, mockPage, { withDelete: false });
            expect(actualResult).toBe(expectedResult);
        });

        it('openRepositoryBrowser() correctly delegates to RepositoryBrowserClientService', () => {
            const mockOptions = { allowedSelection: 'folder', selectMultiple: false };
            const selected: ItemInNode | ItemInNode[] = [];
            const expectedResult = Promise.resolve(selected);

            const mockRepositoryBrowserClientService = TestBed.get(RepositoryBrowserClient);
            spyOn(mockRepositoryBrowserClientService, 'openRepositoryBrowser').and.returnValue(expectedResult);

            const gcmsUi = customerScriptService.createGCMSUIObject(mockScriptHost, mockWindow, mockDocument);
            const actualResult = gcmsUi.openRepositoryBrowser(<any> mockOptions);

            expect(mockRepositoryBrowserClientService.openRepositoryBrowser).toHaveBeenCalledWith(mockOptions);
            expect(actualResult).toBe(expectedResult);
        });

        it('editImage() correctly delegates to EditorOverlayService', () => {
            const result = customerScriptService.createGCMSUIObject(mockScriptHost, mockWindow, mockDocument);
            const editorOverlayService = TestBed.get(EditorOverlayService) as MockEditorOverlayService;

            const promise = result.editImage(1, 4711);
            expect(promise instanceof Promise).toBeTruthy();
            expect(editorOverlayService.editImage).toHaveBeenCalledWith({ nodeId: 1, itemId: 4711 });
        });

        it('unsubscribes the onStateChange handler and destroys the GCMSUI object', () => {
            let fireUnloadEvent: () => void;
            const addEventListenerSpy = spyOn(mockWindow, 'addEventListener').and.callFake((type, eventHandler) => fireUnloadEvent = eventHandler);
            const removeEventListenerSpy = spyOn(mockWindow, 'removeEventListener').and.stub();

            const result = customerScriptService.createGCMSUIObject(mockScriptHost, mockWindow, mockDocument);
            expect(addEventListenerSpy).toHaveBeenCalledTimes(1);
            expect(addEventListenerSpy.calls.argsFor(0)[0]).toEqual('unload');
            expect(fireUnloadEvent instanceof Function).toBeTruthy();
            expect(mockWindow.GCMSUI).toBeTruthy();

            const handlerSpy = jasmine.createSpy('handler');
            result.onStateChange(handlerSpy);
            state.mockState({ ...mockAppState, ...{ ui: { language: 'de' } } });
            expect(handlerSpy).toHaveBeenCalledTimes(1);

            fireUnloadEvent();
            state.mockState({ ...mockAppState, ...{ ui: { language: 'en' } } });

            expect(handlerSpy).toHaveBeenCalledTimes(1);
            expect(removeEventListenerSpy).toHaveBeenCalledTimes(1);
            expect(mockWindow.GCMSUI).toBeFalsy();
        });
    });
});

class MockHttpClient {
    get(): any {
        return of(mockCustomerScript);
    }
}

class MockApiBase {
    get = jasmine.createSpy('get').and.returnValue( { toPromise(): void {} });
    post = jasmine.createSpy('post').and.returnValue( { toPromise(): void {} });
}
class MockEntityResolver {}

class MockTagEditorService {
    openTagEditor(): void { }
}

class MockEditorOverlayService {
    editImage = jasmine.createSpy('editImage').and.returnValue(Promise.resolve(undefined));
}

class MockRepositoryBrowserClientService {
    openRepositoryBrowser(): void { }
}

class MockWindow {
    parent: Partial<CNParentWindow> = {
        GCMSUI_childIFrameInit: (iFrameWindow, iFrameDocument) => null,
    };

    // eslint-disable-next-line @typescript-eslint/naming-convention
    Aloha: Partial<AlohaGlobal> = {
        settings: {

        } as any,
        trigger: function() {},
    };
    addEventListener(): void { }
    removeEventListener(): void { }
}

class MockErrorHandler {
    catch(): void { }
}

class MockModalService {
    fromComponent(): Promise<void> {
        return Promise.resolve();
    }
}

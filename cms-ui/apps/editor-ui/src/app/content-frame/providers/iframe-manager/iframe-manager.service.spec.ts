import { fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { EditMode } from '@gentics/cms-integration-api-models';
import { NgxsModule } from '@ngxs/store';
import { take } from 'rxjs/operators';
import { getTestPageUrl, MockIFrame, MockManagedIFrame } from '../../../../testing/iframe-helpers';
import { EditorState, ITEM_PROPERTIES_TAB } from '../../../common/models';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { ResourceUrlBuilder } from '../../../core/providers/resource-url-builder/resource-url-builder';
import { ApplicationStateService, STATE_MODULES } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { BLANK_PROPERTIES_PAGE } from '../../models/content-frame';
import { ManagedIFrameCollection } from '../../models/managed-iframe-collection';
import { CustomScriptHostService } from '../custom-script-host/custom-script-host.service';
import { CustomerScriptService } from '../customer-script/customer-script.service';
import { IFrameCollectionService } from '../iframe-collection/iframe-collection.service';
import { IFrameManager } from './iframe-manager.service';
import createSpy = jasmine.createSpy;

describe('IFrameManager', () => {

    let iframeManager: IFrameManager;
    let iframeCollectionService: MockIFrameCollectionService;
    let resourceUrlBuilder: ResourceUrlBuilder;

    let appState: TestApplicationState;
    let mockMasterIFrame: MockIFrame;
    let mockMasterFrame: MockManagedIFrame;
    let mockHostComponent: MockHostComponent;
    const mockEditorState: EditorState = {
        editMode: EditMode.PREVIEW,
        itemType: 'page',
        contentModified: false,
        editorIsFocused: true,
        editorIsOpen: true,
        fetching: false,
        saving: false,
        lastError: '',
        objectPropertiesModified: false,
        openObjectPropertyGroups: [],
        modifiedObjectPropertiesValid: false,
        openTab: 'properties',
        openPropertiesTab: ITEM_PROPERTIES_TAB,
        uploadInProgress: false,
    };

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                ResourceUrlBuilder,
                { provide: IFrameCollectionService, useClass: MockIFrameCollectionService },
                { provide: CustomerScriptService, useClass: MockCustomerScriptService },
                { provide: CustomScriptHostService, useClass: MockCustomScriptHostService },
                IFrameManager,
                { provide: ErrorHandler, useClass: MockErrorHandler },
            ],
        });

        iframeManager = TestBed.get(IFrameManager);
        iframeCollectionService = TestBed.get(IFrameCollectionService);
        resourceUrlBuilder = TestBed.get(ResourceUrlBuilder);
        appState = TestBed.get(ApplicationStateService);

        mockMasterIFrame = new MockIFrame();
        mockHostComponent = new MockHostComponent();
    });

    function initializeIframeManager(): void {
        iframeManager.initialize(<any> mockMasterIFrame, <any> mockHostComponent);
        mockMasterFrame = iframeManager.collection.masterFrame as any;
    }

    describe('master frame lifecycle', () => {

        beforeEach(() => {
            initializeIframeManager();
        });

        it('initialize() should create a master frame with the given iframe element', () => {
            const spy = spyOn(iframeCollectionService, 'create').and.callThrough();
            const mockIFrame = {};
            iframeManager.initialize(<any> mockIFrame, <any> new MockHostComponent());

            expect(spy).toHaveBeenCalledWith(mockIFrame);
        });

        it('should call setMasterFrameLoaded(true) on host component on load of master frame', waitForAsync(() => {
            mockMasterFrame.load$.next({ iframe: mockMasterIFrame });

            expect(mockHostComponent.setMasterFrameLoaded).toHaveBeenCalledWith(true);
        }));

        it('should emit requesting$ false on load of master frame', (done: DoneFn) => {
            iframeManager.requesting$.pipe(take(1)).subscribe(value => {
                expect(value).toBe(false);
                done();
            });
            mockMasterFrame.load$.next({ iframe: mockMasterIFrame });
        });

        it('should invoke hostComponent.setContentModified(false) when closing', waitForAsync(() => {
            const spy = createSpy('closed');
            iframeManager.onMasterFrameClosed(spy);
            iframeManager.initiateUserClose();
            mockMasterFrame.unload$.next({ iframe: mockMasterIFrame });

            expect(mockHostComponent.setContentModified).toHaveBeenCalledWith(false);
        }));

        it('should invoke masterFrameClosedCallback when closing', fakeAsync(() => {
            const spy = createSpy('closed');
            iframeManager.onMasterFrameClosed(spy);
            iframeManager.initiateUserClose();
            mockMasterFrame.unload$.next({ iframe: mockMasterIFrame });
            tick();
            expect(spy).toHaveBeenCalled();
        }));

        it('should not invoke masterFrameClosedCallback when unloading without closing', fakeAsync(() => {
            const spy = createSpy('closed');
            iframeManager.onMasterFrameClosed(spy);
            mockMasterFrame.unload$.next({ iframe: mockMasterIFrame });
            tick();
            expect(spy).not.toHaveBeenCalled();
        }));

    });

    describe('editor state change', () => {

        beforeEach(() => {
            initializeIframeManager();
        });

        function setMockState(): void {
            appState.mockState({
                editor: { ...mockEditorState, ...{ itemId: 42 } },
                entities: {
                    page: {
                        42: {
                            currentVersion: {} as any,
                        },
                    },
                } });
        }

        it('should call setMasterFrameLoaded(false) on host component', fakeAsync(() => {
            setMockState();
            tick();
            expect(mockHostComponent.setMasterFrameLoaded).toHaveBeenCalledWith(false);
        }));

        it('should emit requesting$ true', (done: DoneFn) => {
            iframeManager.requesting$.pipe(take(1)).subscribe(value => {
                expect(value).toBe(true);
                done();
            });
            setMockState();
        });

        it('should call setUrl() on the master frame', fakeAsync(() => {
            const spy = spyOn(iframeManager.collection.masterFrame, 'setUrl').and.callThrough();
            setMockState();
            tick();
            expect(spy).toHaveBeenCalled();
        }));
    });

    describe('managing child iframes', () => {

        let childIFrame1: HTMLIFrameElement;
        let childIFrame2: HTMLIFrameElement;

        beforeEach(() => {
            childIFrame1 = document.createElement('iframe');
            childIFrame2 = document.createElement('iframe');
            childIFrame1.src = getTestPageUrl(1);
            childIFrame2.src = getTestPageUrl(2);

            initializeIframeManager();
        });

        it('should wrap any child iframes in a ManagedIFrame on DOMContentLoaded', () => {
            const qsaSpy: any = spyOn(mockMasterIFrame.contentWindow.document, 'querySelectorAll')
                .and.callFake(((selector: string) => {
                    return (selector === 'iframe') ? [childIFrame1, childIFrame2] : [];
                }) as any);
            const collectionAddSpy = spyOn(iframeManager.collection, 'add').and.callThrough();

            mockMasterFrame.domContentLoaded$.next({ iframe: mockMasterIFrame });

            expect(qsaSpy).toHaveBeenCalledWith('iframe');
            expect(collectionAddSpy.calls.count()).toBe(2);
            expect(collectionAddSpy).toHaveBeenCalledWith(childIFrame1);
            expect(collectionAddSpy).toHaveBeenCalledWith(childIFrame2);
        });

        it('should wrap any child iframes added after DOMContentLoaded in a ManagedIFrame', (done: DoneFn) => {
            const mockDocumentElement = mockMasterIFrame.contentWindow.document.documentElement;
            let calls = 0;
            const addSpy = spyOn(iframeManager.collection, 'add').and.callFake((iframe: any) => {
                calls++;

                if (calls === 1) {
                    expect(addSpy.calls.count()).toBe(1);
                    expect(addSpy).toHaveBeenCalledWith(childIFrame1);
                }

                if (calls === 2) {
                    expect(addSpy.calls.count()).toBe(2);
                    expect(addSpy).toHaveBeenCalledWith(childIFrame2);
                    done();
                }

                return new MockManagedIFrame(iframe);
            });

            mockMasterFrame.domContentLoaded$.next({ iframe: mockMasterIFrame });

            mockDocumentElement.appendChild(childIFrame1);
            mockDocumentElement.appendChild(childIFrame2);
        });

        it('should ignore iframes that have data-gcms-ui-skip-injection set', (done: DoneFn) => {
            childIFrame1.dataset['gcmsUiSkipInjection'] = 'true';
            const mockDocumentElement = mockMasterIFrame.contentWindow.document.documentElement;
            const addSpy = spyOn(iframeManager.collection, 'add').and.callFake((iframe: any) => {
                expect(addSpy.calls.count()).toBe(1);
                expect(addSpy).toHaveBeenCalledWith(childIFrame2);
                done();
            });
            mockMasterFrame.domContentLoaded$.next({ iframe: mockMasterIFrame });

            mockDocumentElement.appendChild(childIFrame1);
            mockDocumentElement.appendChild(childIFrame2);
        });

        it('should remove iframes from the ManagedIFrameCollection when directly removed from DOM', (done: DoneFn) => {
            const mockDocumentElement = mockMasterIFrame.contentWindow.document.documentElement;
            mockDocumentElement.appendChild(childIFrame1);

            const removeSpy = spyOn(iframeManager.collection, 'removeByNativeElement').and.callFake(() => {
                expect(removeSpy.calls.count()).toBe(1);
                expect(removeSpy).toHaveBeenCalledWith(childIFrame1);
                done();
            });
            mockMasterFrame.domContentLoaded$.next({ iframe: mockMasterIFrame });

            childIFrame1.parentElement.removeChild(childIFrame1);
        });

        it('should remove iframes from the ManagedIFrameCollection when ancestor removed from DOM', (done: DoneFn) => {
            const mockDocumentElement = mockMasterIFrame.contentWindow.document.documentElement;
            const outerDiv = document.createElement('div');
            const innerDiv = document.createElement('div');
            outerDiv.appendChild(innerDiv);
            innerDiv.appendChild(childIFrame1);

            mockDocumentElement.appendChild(outerDiv);

            const removeSpy = spyOn(iframeManager.collection, 'removeByNativeElement').and.callFake(() => {
                expect(removeSpy.calls.count()).toBe(1);
                expect(removeSpy).toHaveBeenCalledWith(childIFrame1);
                done();
            });
            mockMasterFrame.domContentLoaded$.next({ iframe: mockMasterIFrame });

            outerDiv.parentElement.removeChild(outerDiv);
        });

        it('should invoke ManagedIFrameCollection.removeAllChildren() when master frame unloads', waitForAsync(() => {
            const removeAllChildrenSpy = spyOn(iframeManager.collection, 'removeAllChildren');
            mockMasterFrame.unload$.next({ iframe: mockMasterIFrame });
            expect(removeAllChildrenSpy).toHaveBeenCalled();
        }));

    });

    describe('stateToUrl()', () => {

        let editorState: EditorState;

        beforeEach(() => {
            editorState = {
                contentModified: false,
                editorIsFocused: true,
                editorIsOpen: true,
                fetching: false,
                saving: false,
                lastError: '',
                objectPropertiesModified: false,
                openObjectPropertyGroups: [],
                modifiedObjectPropertiesValid: false,
                openTab: 'properties',
                openPropertiesTab: ITEM_PROPERTIES_TAB,
                uploadInProgress: false,
            };

            initializeIframeManager();
        });

        it('preview page', waitForAsync(() => {
            const spy = spyOn(resourceUrlBuilder, 'pagePreview');
            editorState.nodeId = 1;
            editorState.itemType = 'page';
            editorState.editMode = EditMode.PREVIEW;
            iframeManager.stateToUrl(editorState).then(() => {
                expect(spy).toHaveBeenCalledWith(CURRENT_ITEM.id, editorState.nodeId);
            });
        }));

        it('edit page', waitForAsync(() => {
            const spy = spyOn(resourceUrlBuilder, 'pageEditor');
            editorState.nodeId = 1;
            editorState.itemType = 'page';
            editorState.editMode = EditMode.EDIT;
            iframeManager.stateToUrl(editorState).then(() => {
                expect(spy).toHaveBeenCalledWith(CURRENT_ITEM.id, editorState.nodeId);
            });
        }));

        it('compare page languages', waitForAsync(() => {
            const spy = spyOn(resourceUrlBuilder, 'pagePreview');
            editorState.nodeId = 1;
            editorState.itemType = 'page';
            editorState.editMode = EditMode.PREVIEW;
            editorState.compareWithId = 4;
            iframeManager.stateToUrl(editorState).then(() => {
                expect(spy).toHaveBeenCalledWith(CURRENT_ITEM.id, editorState.nodeId);
            });
        }));

        it('edit item-properties', waitForAsync(() => {
            editorState.nodeId = 1;
            editorState.editMode = EditMode.EDIT_PROPERTIES;
            editorState.openTab = 'properties';
            iframeManager.stateToUrl(editorState).then(result => {
                expect(result).toEqual(BLANK_PROPERTIES_PAGE);
            });
        }));

        it('preview version', waitForAsync(() => {
            const spy = spyOn(resourceUrlBuilder, 'previewPageVersion');
            editorState.nodeId = 1;
            editorState.editMode = EditMode.PREVIEW_VERSION;
            editorState.version = { timestamp: 1234 } as any;
            iframeManager.stateToUrl(editorState).then(() => {
                expect(spy).toHaveBeenCalledWith(editorState.nodeId, CURRENT_ITEM.id, editorState.version.timestamp);
            });
        }));

        it('compare version contents', waitForAsync(() => {
            const spy = spyOn(resourceUrlBuilder, 'comparePageVersions');
            editorState.nodeId = 1;
            editorState.editMode = EditMode.COMPARE_VERSION_CONTENTS;
            editorState.version = { timestamp: 5678 } as any;
            editorState.oldVersion = { timestamp: 1234 } as any;
            iframeManager.stateToUrl(editorState).then(() => {
                expect(spy).toHaveBeenCalledWith(editorState.nodeId, CURRENT_ITEM.id,
                    editorState.oldVersion.timestamp, editorState.version.timestamp);
            });
        }));

        it('compare version sources', waitForAsync(() => {
            const spy = spyOn(resourceUrlBuilder, 'comparePageVersionSources');
            editorState.nodeId = 1;
            editorState.editMode = EditMode.COMPARE_VERSION_SOURCES;
            editorState.version = { timestamp: 5678 } as any;
            editorState.oldVersion = { timestamp: 1234 } as any;
            iframeManager.stateToUrl(editorState).then(() => {
                expect(spy).toHaveBeenCalledWith(editorState.nodeId, CURRENT_ITEM.id,
                    editorState.oldVersion.timestamp, editorState.version.timestamp);
            });
        }));
    });

    describe('error states', () => {

        it('destroy() does not throw if initialize() has not yet been called', () => {
            expect(() => iframeManager.destroy()).not.toThrow();
        });

        it('does not throw when loading an iframe with no contentWindow', () => {
            const badIFrame: any = {};
            iframeManager.initialize(badIFrame, <any> mockHostComponent);
            const badMasterFrame = iframeManager.collection.masterFrame as any;
            expect(() => badMasterFrame.load$.next({ iframe: badIFrame })).not.toThrow();
        });

    });

});

class MockIFrameCollectionService {
    create(iframe: any): ManagedIFrameCollection {
        const mif = new MockManagedIFrame(iframe);
        const collection = new ManagedIFrameCollection(mif as any);
        collection.managedIFrameCtor = MockManagedIFrame as any;
        return collection;
    }
}

class MockCustomerScriptService {
    createGCMSUIObject = jasmine.createSpy('createGCMSUIObject');
    invokeCustomerScript = jasmine.createSpy('createGCMSUIObject');
}

const CURRENT_ITEM = {
    folderId: 4,
    id: 42,
    type: 'page',
};

class MockHostComponent {
    setMasterFrameLoaded = createSpy('setMasterFrameLoaded');
    setContentModified = createSpy('setContentModified');
    get currentItem(): any {
        return CURRENT_ITEM;
    }
    getCurrentItem(): Promise<any> {
        return Promise.resolve(CURRENT_ITEM);
    }
}

class MockCustomScriptHostService {}

class MockErrorHandler {
    catch(): void {}
}

import { NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed, tick } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Api, ApiBase } from '@editor-ui/app/core/providers/api';
import { EntityResolver } from '@editor-ui/app/core/providers/entity-resolver/entity-resolver';
import { ErrorHandler } from '@editor-ui/app/core/providers/error-handler/error-handler.service';
import { I18nService } from '@editor-ui/app/core/providers/i18n/i18n.service';
import { NavigationService } from '@editor-ui/app/core/providers/navigation/navigation.service';
import { PermissionService } from '@editor-ui/app/core/providers/permissions/permission.service';
import { SharedModule } from '@editor-ui/app/shared/shared.module';
import { ApplicationStateService, FolderActionsService, PostUpdateBehavior, STATE_MODULES } from '@editor-ui/app/state';
import { TestApplicationState } from '@editor-ui/app/state/test-application-state.mock';
import { TagEditorService } from '@editor-ui/app/tag-editor';
import { componentTest } from '@editor-ui/testing';
import { mockPipes } from '@editor-ui/testing/mock-pipe';
import { getExampleEditableTag } from '@editor-ui/testing/test-tag-editor-data.mock';
import {
    File as CmsFile,
    EditableFileProps,
    Folder,
    FolderItemSaveOptionsMap,
    FolderItemType,
    FolderItemTypeMap,
    ModelType,
    Node,
    Normalized,
    Tags,
} from '@gentics/cms-models';
import {
    getExampleEditableObjectTag,
    getExampleFileData,
    getExampleFolderDataNormalized,
    getExampleNodeDataNormalized,
} from '@gentics/cms-models/testing/test-data.mock';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { GCMSTestRestClientService } from '@gentics/cms-rest-client-angular/testing';
import { MockApiBase } from '@gentics/cms-rest-clients-angular/base/api-base.mock';
import { GenticsUICoreModule, ModalService } from '@gentics/ui-core';
import { NgxsModule } from '@ngxs/store';
import { of } from 'rxjs';
import { CombinedPropertiesEditorComponent } from '../combined-properties-editor/combined-properties-editor.component';
import { ImagePropertiesModalComponent } from './image-properties-modal.component';

class MockApi extends MockApiBase {}
class MockEntityResolver implements Partial<EntityResolver> {
    getNode(id: number): Node<Normalized> {
        return getExampleNodeDataNormalized();
    }
    getFolder(id: number): Folder<Normalized> {
        return getExampleFolderDataNormalized();
    }
}
class MockFolderActionsService implements Partial<FolderActionsService> {
    updateFileProperties(fileId: number, properties: EditableFileProps, postUpdateBehavior?: PostUpdateBehavior): Promise<void | CmsFile<ModelType.Raw>> {
        return Promise.resolve();
    }
    updateItemObjectProperties<T extends FolderItemType, U extends FolderItemTypeMap<ModelType.Raw>[T], R extends FolderItemSaveOptionsMap[T]>(
        type: T,
        itemId: number,
        updatedObjProps: Partial<Tags>,
        postUpdateBehavior: PostUpdateBehavior & Required<Pick<PostUpdateBehavior, 'fetchForUpdate'>>,
        requestOptions?: Partial<R>,
    ): Promise<U> {
        return Promise.resolve(getExampleFileData()) as any;
    }
}
class MockNavigationService {}
class MockPermissionService {}
class MockTagEditorService {}
class MockModalService {}
class MockErrorHandler {}
class MockI18nService {
    translate(key: string): string {
        return key;
    }
}

const NODE_ID = 1;

describe('ImagePropertiesModal', () => {

    let actions: MockFolderActionsService;
    let client: GCMSTestRestClientService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [
                ImagePropertiesModalComponent,
                CombinedPropertiesEditorComponent,
                mockPipes('i18n', 'i18nDate', 'filesize'),
            ],
            imports: [
                NgxsModule.forRoot(STATE_MODULES),
                GenticsUICoreModule.forRoot(),
                FormsModule,
                ReactiveFormsModule,
                SharedModule,
            ],
            providers: [
                { provide: ApiBase, useClass: MockApi },
                { provide: GCMSRestClientService, useClass: GCMSTestRestClientService },
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: EntityResolver, useClass: MockEntityResolver },
                { provide: FolderActionsService, useClass: MockFolderActionsService },
                { provide: NavigationService, useClass: MockNavigationService },
                { provide: PermissionService, useClass: MockPermissionService },
                { provide: TagEditorService, useClass: MockTagEditorService },
                { provide: ModalService, useClass: MockModalService },
                { provide: I18nService, useClass: MockI18nService },
                { provide: ErrorHandler, useClass: MockErrorHandler },
                Api,
            ],
            schemas: [NO_ERRORS_SCHEMA],
        });

        actions = TestBed.inject(FolderActionsService) as any;
        client = TestBed.inject(GCMSRestClientService) as any;
    });

    it('should save properties if edited', componentTest(() => ImagePropertiesModalComponent, (fixture, instance) => {
        // Setup spies
        spyOn(actions, 'updateFileProperties').and.callThrough();
        spyOn(actions, 'updateItemObjectProperties').and.callThrough();
        spyOn(client.node, 'listLanguages').and.returnValue(of({
            items: [],
            hasMoreItems: false,
            messages: [],
            numItems: 0,
            responseInfo: null,
        }));
        spyOn(client.folder, 'templates').and.returnValue(of({
            templates: [],
            hasMoreItems: false,
            messages: [],
            numItems: 0,
            responseInfo: null,
        }));

        // Testing constants
        const ITEM = getExampleFileData();
        const CHANGES = { foo: 'bar', name: 'hello_world.txt' } as any;

        // Components Inputs
        instance.file = ITEM;
        instance.nodeId = NODE_ID;

        // Trigger lifecycle-hooks
        fixture.detectChanges();
        tick(1000);

        // Apply changes
        const editor = instance.combinedPropertiesEditor;
        editor.handlePropChanges(CHANGES);
        instance.saveAndClose();

        // Wait for promises/observables
        tick();

        expect(actions.updateFileProperties).toHaveBeenCalledWith(ITEM.id, CHANGES, jasmine.anything());
        expect(actions.updateItemObjectProperties).toHaveBeenCalledWith(ITEM.type, ITEM.id, ITEM.tags, jasmine.anything());
    }));

    it('should save object-properties if edited', componentTest(() => ImagePropertiesModalComponent, (fixture, instance) => {
        // Setup spies
        spyOn(actions, 'updateFileProperties').and.callThrough();
        spyOn(actions, 'updateItemObjectProperties').and.callThrough();
        spyOn(client.node, 'listLanguages').and.returnValue(of({
            items: [],
            hasMoreItems: false,
            messages: [],
            numItems: 0,
            responseInfo: null,
        }));
        spyOn(client.folder, 'templates').and.returnValue(of({
            templates: [],
            hasMoreItems: false,
            messages: [],
            numItems: 0,
            responseInfo: null,
        }));

        // Testing constants
        const ITEM = getExampleFileData();
        const TAG = getExampleEditableTag();
        ITEM.tags[TAG.name] = TAG;
        const OBJ_PROP = getExampleEditableObjectTag();
        const EXPECTED = structuredClone(ITEM);
        EXPECTED.tags[OBJ_PROP.name] = OBJ_PROP;

        // Components Inputs
        instance.file = structuredClone(ITEM);
        instance.nodeId = NODE_ID;

        // Trigger lifecycle-hooks
        fixture.detectChanges();
        tick(1000);

        // Apply changes
        const editor = instance.combinedPropertiesEditor;
        editor.editedObjectProperty = OBJ_PROP;
        editor.handleObjectPropertyChange(OBJ_PROP.properties);
        instance.saveAndClose();

        // Wait for promises/observables
        tick();

        expect(actions.updateFileProperties).not.toHaveBeenCalled();
        expect(actions.updateItemObjectProperties).toHaveBeenCalledWith(EXPECTED.type, EXPECTED.id, EXPECTED.tags, jasmine.anything());
    }));
});

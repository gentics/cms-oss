import { Component, NO_ERRORS_SCHEMA, Pipe, PipeTransform, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick, waitForAsync } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { EditMode } from '@gentics/cms-integration-api-models';
import { GenticsUIImageEditorModule } from '@gentics/image-editor';
import { GenticsUICoreModule, IModalInstance } from '@gentics/ui-core';
import { NgxsModule } from '@ngxs/store';
import { BehaviorSubject, Subject } from 'rxjs';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { NavigationService } from '../../../core/providers/navigation/navigation.service';
import { ApplicationStateService, EditorStateUrlParams, NodeSettingsActionsService, STATE_MODULES } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { EditorOverlayService } from '../../providers/editor-overlay.service';
import { ImageEditorModalComponent } from '../image-editor-modal/image-editor-modal.component';
import { EditorOverlay } from './editor-overlay.component';

@Component({
    template: `
        <editor-overlay #editor></editor-overlay>
        <gtx-overlay-host></gtx-overlay-host>
    `,
    standalone: false,
})
class TestComponent {
    @ViewChild('editor', { static: true })
    editor: EditorOverlay;
}

let appState: TestApplicationState;

const ITEM_ID = 1;
const PARENTFOLDER_ID = 2;
const SUBFOLDER_ID = 3;
const ITEM_NODE = 11;
const DIFFERENT_NODE = 22;
const MOCK_NODE_NAME = 'MockNode';

describe('EditorOverlayComponent', () => {
    let component: TestComponent;
    let fixture: ComponentFixture<TestComponent>;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [
                NgxsModule.forRoot(STATE_MODULES),
                GenticsUICoreModule.forRoot(),
                GenticsUIImageEditorModule,
            ],
            declarations: [
                EditorOverlay,
                ImageEditorModalComponent,
                TestComponent,
                MockI18nPipe,
            ],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: ActivatedRoute, useClass: MockActivatedRoute },
                { provide: NavigationService, useClass: MockNavigationService },
                { provide: EditorOverlayService, useClass: MockEditorOverlayService },
                { provide: EntityResolver, useClass: MockEntityResolver },
                { provide: NodeSettingsActionsService, useClass: MockNodeSettingsActions },
            ],
            schemas: [NO_ERRORS_SCHEMA],
        });

        TestBed.compileComponents();

        appState = TestBed.inject(ApplicationStateService) as any;
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(TestComponent);
        component = fixture.componentInstance;
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    describe('route calls', () => {
        it('should call editImage', fakeAsync(() => {
            const editorOverlayService = TestBed.inject(EditorOverlayService);

            editImageState(fixture);

            expect((component.editor as any).subscriptions.length).toEqual(1);

            tick();
            fixture.detectChanges();

            expect(editorOverlayService.editImage).toHaveBeenCalled();
        }));
    });

});

function editImageState(fixture: ComponentFixture<any>): void {
    appState.mockState({
        auth: {
            changingPassword: false,
            currentUserId: 1,
            isLoggedIn: true,
            lastError: '',
            loggingIn: false,
            loggingOut: false,
            sid: 1234,
        },
        entities: {
            image: {
                [ITEM_ID]: {
                    type: 'image',
                    id: ITEM_ID,
                    masterNodeId: ITEM_NODE,
                    name: 'mockpic.jpg',
                    path: '/GCN5 Demo/[Media]/[Images]/',
                },
            },
        },
    });
    fixture.detectChanges();
}

class MockActivatedRoute {
    params = new BehaviorSubject<EditorStateUrlParams>({
        editMode: EditMode.EDIT,
        itemId: ITEM_ID,
        nodeId: ITEM_NODE,
        options: 'e30=',
        type: 'image',
    });
}
class MockResourceUrlBuilder {
    // eslint-disable-next-line max-len
    imageFullsize(): string { return 'data:image/png;base64,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'; }
}
class MockNavigationService {
    deserializeOptions(): any { }
    instruction(): any {
        return {
            navigate: () => { },
        };
    }
}

class MockEntityResolver {
    getNode(): any {
        return {name: MOCK_NODE_NAME};
    }
    getEntity(): any {
        return {
            id: ITEM_ID,
            globalId: 'A123.123456',
            name: 'mockpic.jpg',
            cdate: 1288366546,
            edate: 1530802426,
            type: 'image',
            typeId: 10011,
            fileType: 'image/jpeg',
            folderId: SUBFOLDER_ID,
            folderName: '[Images]',
            fileSize: 33004,
            channelId: 0,
            inherited: false,
            liveUrl: '',
            inheritedFrom: MOCK_NODE_NAME,
            inheritedFromId: ITEM_NODE,
            masterNode: MOCK_NODE_NAME,
            masterNodeId: ITEM_NODE,
            path: `/${MOCK_NODE_NAME}/[Media]/[Images]/`,
            forceOnline: false,
            online: true,
            broken: false,
            excluded: false,
            disinheritDefault: false,
            disinherited: false,
            sizeX: 618,
            sizeY: 367,
            dpiX: 0,
            dpiY: 0,
            fpX: 0.5,
            fpY: 0.5,
            gisResizable: true,
            iconCls: 'gtx_image',
            leaf: true,
            text: 'mockpic.jpg',
            cls: 'file',
            '@class': 'com.gentics.contentnode.rest.model.Image',
        };
    }
}

@Pipe({
    name: 'i18n',
    standalone: false,
})
class MockI18nPipe implements PipeTransform {
    transform(key: string, params: any): string {
        return key + (params ? ':' + JSON.stringify(params) : '');
    }
}

class MockNodeSettingsActions {
    loadNodeSettings(): void {}
}

class MockErrorHandler {}
class MockEditorOverlayService {
    editorOverlayOnOpen$ = new Subject<IModalInstance<any>>();
    editorOverlayOnClose$ = new Subject<IModalInstance<any>>();
    editImage = jasmine.createSpy('editImage').and.returnValue(Promise.resolve(undefined));
}

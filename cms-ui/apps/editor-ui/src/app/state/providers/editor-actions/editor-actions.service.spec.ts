/* eslint-disable id-blacklist */
import { TestBed } from '@angular/core/testing';
import { EditMode } from '@gentics/cms-integration-api-models';
import { PageVersion } from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';
import { Api } from '../../../core/providers/api/api.service';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { MockErrorHandler } from '../../../core/providers/error-handler/error-handler.mock';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { ComparePageVersionSourcesAction, ComparePageVersionsAction } from '../../modules/editor/editor.actions';
import { STATE_MODULES } from '../../modules/state-modules';
import { TestApplicationState } from '../../test-application-state.mock';
import { ApplicationStateService } from '../application-state/application-state.service';
import { EditorActionsService } from './editor-actions.service';

describe('EditorActionsService', () => {

    let editorActions: EditorActionsService;
    let state: TestApplicationState;

    const version10 = { number: '1' } as PageVersion;
    const version11 = { number: '1.1' } as PageVersion;
    const version12 = { number: '1.2' } as PageVersion;
    const version20 = { number: '2.0' } as PageVersion;
    const version40 = { number: '4' } as PageVersion;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                {provide: ApplicationStateService, useClass: TestApplicationState },
                {provide: Api, useClass: MockApi },
                {provide: ErrorHandler, useClass: MockErrorHandler},
                EditorActionsService,
                EntityResolver,
            ],
        });

        state = TestBed.inject(ApplicationStateService) as any;
        expect(state instanceof TestApplicationState).toBe(true, 'state is not a TestApplicationState');
        editorActions = TestBed.inject(EditorActionsService);
    });

    describe('previewPage()', () => {

        it('correctly calls editor.editItem (starting from empty state)', () => {
            state.mockState({
                editor: { },
            });

            editorActions.previewPage(10, 1);
            const currentState = state.now.editor;
            expect(currentState).toEqual(jasmine.objectContaining({
                itemType: 'page',
                itemId: 10,
                nodeId: 1,
                editMode: 'preview',
            }));
        });
    });

    describe('editPage()', () => {

        beforeEach(() => {
            state.mockState({
                auth: {
                    user: {
                        id: 1,
                    } as any,
                },
                editor: {
                    itemId: 42,
                    nodeId: 2,
                    itemType: 'page',
                    editMode: EditMode.EDIT,
                },
            });
        });

        it('correctly calls editor.editItem', () => {
            editorActions.editPage(10, 1);
            const currentState = state.now.editor;
            expect(currentState).toEqual(jasmine.objectContaining({
                compareWithId: undefined,
                itemType: 'page',
                itemId: 10,
                nodeId: 1,
                editMode: EditMode.EDIT,
            }));
        });
    });

    describe('previewPageVersion()', () => {

        beforeEach(() => {
            state.mockState({
                editor: {
                    itemType: 'page',
                    itemId: 42,
                    nodeId: 2,
                    editMode: EditMode.PREVIEW_VERSION,
                    version: version12,
                },
            });
        });

        it('correctly calls editor.previewPageVersion', () => {
            editorActions.previewPageVersion(10, 1, version12);
            expect(state.now.editor).toEqual(jasmine.objectContaining({
                editMode: EditMode.PREVIEW_VERSION,
                editorIsOpen: true,
                editorIsFocused: true,
                itemType: 'page',
                itemId: 10,
                nodeId: 1,
                version: { number: '1.2' },
            }));
        });
    });

    describe('comparePageVersions()', () => {

        beforeEach(() => {
            state.mockState({
                editor: {
                    itemType: 'page',
                    itemId: 42,
                    nodeId: 2,
                    editMode: EditMode.PREVIEW_VERSION,
                    version: version12,
                    oldVersion: version10,
                },
            });
        });

        it('correctly calls editor.comparePageVersions', () => {
            state.dispatch(new ComparePageVersionsAction(10, 1, version12, version40));
            const currentState = state.now.editor;

            expect(currentState.itemId).toEqual(10);
            expect(currentState.nodeId).toEqual(1);
            expect(currentState.oldVersion).toEqual(version12);
            expect(currentState.version).toEqual(version40);
        });
    });

    describe('comparePageVersionSources()', () => {

        beforeEach(() => {
            state.mockState({
                editor: {
                    itemType: 'page',
                    itemId: 42,
                    nodeId: 2,
                    editMode: EditMode.PREVIEW_VERSION,
                    version: version12,
                    oldVersion: version10,
                },
            });
        });

        it('correctly calls editor.comparePageVersionSources', () => {
            state.dispatch(new ComparePageVersionSourcesAction(10, 1, version20, version40));
            const currentState = state.now.editor;

            expect(currentState.itemId).toEqual(10);
            expect(currentState.nodeId).toEqual(1);
            expect(currentState.oldVersion).toEqual(version20);
            expect(currentState.version).toEqual(version40);
        });
    });

});

class MockApi { }

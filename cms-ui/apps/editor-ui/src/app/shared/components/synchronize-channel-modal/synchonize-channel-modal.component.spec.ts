import { Component, EventEmitter, Injectable, Input, NO_ERRORS_SCHEMA, Output } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ChannelSyncRequest, Feature, File, Folder, Image, Node, Normalized, Page } from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';
import { componentTest } from '../../../../testing/component-test';
import { mockPipes } from '../../../../testing/mock-pipe';
import { ApiBase } from '../../../core/providers/api';
import { MockApiBase } from '../../../core/providers/api/api-base.mock';
import { Api } from '../../../core/providers/api/api.service';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { ApplicationStateService, FolderActionsService, STATE_MODULES } from '../../../state';
import { replaceInState, TestApplicationState } from '../../../state/test-application-state.mock';
import { SynchronizeChannelModal } from './synchonize-channel-modal.component';

describe('SynchronizeChannelModal', () => {

    let apiBase: MockApiBase;
    let appState: TestApplicationState;
    let folderActions: SpyFolderActionsService;
    let masterNode: Node<Normalized>;
    let channelOneLevelDeep: Node<Normalized>;
    let channelTwoLevelsDeep: Node<Normalized>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: FolderActionsService, useClass: SpyFolderActionsService },
                { provide: ApiBase, useClass: MockApiBase },
                Api,
                EntityResolver,
            ],
            declarations: [
                SynchronizeChannelModal,
                MockButton,
                MockCheckbox,
                MockNodeSelector,
                MockProgressBar,
                mockPipes('capitalize', 'i18n', 'truncatePath', 'itemIsLocal', 'itemIsLocalized'),
            ],
            schemas: [NO_ERRORS_SCHEMA],
        });

        apiBase = TestBed.get(ApiBase);
        expect(apiBase instanceof MockApiBase).toBe(true);

        appState = TestBed.get(ApplicationStateService);
        expect(appState instanceof TestApplicationState).toBe(true);

        appState.mockState({
            folder: {
                channelSyncReport: {
                    folders: [],
                    pages: [],
                    images: [],
                    files: [],
                    templates: [],
                    fetching: false,
                },
            },
            entities: {
                //
                // Master Node #1
                //  |- Channel #2
                //  |   '- Channel #3
                //  '- Channel #4
                //
                node: {
                    1: {
                        id: 1,
                        name: 'Master Node #1',
                        inheritedFromId: 1,
                        type: 'node',
                    },
                    2: {
                        id: 2,
                        name: 'Channel #2',
                        inheritedFromId: 1,
                        type: 'channel',
                    },
                    3: {
                        id: 3,
                        name: 'Channel #3',
                        inheritedFromId: 2,
                        type: 'channel',
                    },
                    4: {
                        id: 4,
                        name: 'Channel #4',
                        inheritedFromId: 1,
                        type: 'channel',
                    },
                },
            },
        });

        masterNode = appState.now.entities.node[1];
        channelOneLevelDeep = appState.now.entities.node[2];
        channelTwoLevelsDeep = appState.now.entities.node[3];

        folderActions = TestBed.get(FolderActionsService);
    });

    function getObjectTypeSection(fixture: ComponentFixture<SynchronizeChannelModal>):
    { labels: string[], checkboxes: MockCheckbox[], checked: string[] } {
        const rows = fixture.debugElement.queryAll(By.css('.affected-objects-row'));
        const checkboxes = rows.map(row => row.query(By.directive(MockCheckbox)).componentInstance);
        const checked = checkboxes.filter(cb => cb.checked).map(cb => cb.label);
        const labels = checkboxes.map(cb => cb.label);
        expect(labels).toEqual(['folder', 'page', 'file', 'image', 'template']);
        return { checkboxes, checked, labels };
    }

    const getConfirmButton = (fixture: ComponentFixture<SynchronizeChannelModal>): MockButton => fixture.debugElement
        .query(By.css('.modal-footer'))
        .queryAll(By.directive(MockButton))
        .map(debugElement => debugElement.componentInstance as MockButton)
        .filter(btn => btn.type === 'default')[0];

    it('can be created',
        componentTest(() => SynchronizeChannelModal, (fixture, modal) => {
            modal.channel = channelOneLevelDeep;
            modal.item = { type: 'folder', id: 1234 } as Folder;
            fixture.detectChanges();
            expect(modal == null).toBe(false);
        }),
    );

    it('shows a list of affected objects when opened for a folder',
        componentTest(() => SynchronizeChannelModal, (fixture, modal) => {
            modal.channel = channelOneLevelDeep;
            modal.item = { type: 'folder', id: 1234 } as Folder;

            fixture.detectChanges();
            const affectedObjects = fixture.debugElement.query(By.css('.affected-objects-wrapper'));
            expect(affectedObjects == null).toBe(false);
        }),
    );

    it('"recursive" is not checked initially when opened for a folder',
        componentTest(() => SynchronizeChannelModal, (fixture, modal) => {
            modal.channel = channelOneLevelDeep;
            modal.item = { type: 'folder', id: 1234 } as Folder;

            fixture.detectChanges();

            const recursive: MockCheckbox = fixture.debugElement.query(By.css('.recursive-checkbox')).componentInstance;
            expect(recursive.checked).toBe(false);
        }),
    );

    it('checks the correct item types when opened for a folder',
        componentTest(() => SynchronizeChannelModal, (fixture, modal) => {
            modal.channel = channelOneLevelDeep;
            modal.item = { type: 'folder', id: 1234 } as Folder;

            fixture.detectChanges();

            const {checked} = getObjectTypeSection(fixture);
            expect(checked).toEqual(['folder', 'page', 'file', 'image', 'template']);
        }),
    );

    it('checks the correct item types when opened for a nodes root folder',
        componentTest(() => SynchronizeChannelModal, (fixture, modal) => {
            modal.channel = channelOneLevelDeep;
            modal.item = channelOneLevelDeep as any;

            fixture.detectChanges();

            const {checked} = getObjectTypeSection(fixture);
            expect(checked).toEqual(['folder', 'page', 'file', 'image', 'template']);
        }),
    );

    it('does not show the list of affected items when opened for a page',
        componentTest(() => SynchronizeChannelModal, (fixture, modal) => {
            modal.channel = channelOneLevelDeep;
            modal.item = { type: 'page', id: 1234 } as Page;

            fixture.detectChanges();

            const checkboxes = fixture.debugElement.queryAll(By.directive(MockCheckbox));
            expect(checkboxes.length).toEqual(0);
        }),
    );

    it('does not show the list of affected items when opened for a file',
        componentTest(() => SynchronizeChannelModal, (fixture, modal) => {
            modal.channel = channelOneLevelDeep;
            modal.item = { type: 'file', id: 1234 } as File;

            fixture.detectChanges();

            const checkboxes = fixture.debugElement.queryAll(By.directive(MockCheckbox));
            expect(checkboxes.length).toEqual(0);
        }),
    );

    it('does not show the list of affected items when opened for an image',
        componentTest(() => SynchronizeChannelModal, (fixture, modal) => {
            modal.channel = channelOneLevelDeep;
            modal.item = { type: 'image', id: 1234 } as Image;

            fixture.detectChanges();

            const checkboxes = fixture.debugElement.queryAll(By.directive(MockCheckbox));
            expect(checkboxes.length).toEqual(0);
        }),
    );

    it('does not fetch a list of sync nodes when opened for a local folder',
        componentTest(() => SynchronizeChannelModal, (fixture, modal) => {
            modal.channel = channelOneLevelDeep;
            modal.item = { type: 'folder', id: 1234 } as Folder;

            fixture.detectChanges();

            expect(apiBase.get).not.toHaveBeenCalledWith('folder/localizations/1234');
        }),
    );

    it('fetches a list of sync nodes from the server when opened for an inherited folder',
        componentTest(() => SynchronizeChannelModal, (fixture, modal) => {
            appState.mockState({
                features: {
                    [Feature.MULTICHANNELLING]: true,
                },
            });
            modal.channel = channelOneLevelDeep;
            modal.item = {
                type: 'folder',
                id: 1234,
                inherited: true,
                inheritedFromId: masterNode.id,
            } as Folder;

            fixture.detectChanges();
            expect(apiBase.get).toHaveBeenCalledWith('folder/localizations/1234');
        }),
    );

    it('fetches the child elements from the server when opened for a folder',
        componentTest(() => SynchronizeChannelModal, (fixture, modal) => {
            appState.mockState({
                features: {
                    [Feature.MULTICHANNELLING]: true,
                },
            });
            modal.channel = channelOneLevelDeep;
            modal.item = { type: 'folder', id: 1234 } as Folder;

            fixture.detectChanges();
            expect(folderActions.getChannelSyncReport).toHaveBeenCalledTimes(1);
        }),
    );

    it('re-fetches the child elements from the server when "recursive" is checked',
        componentTest(() => SynchronizeChannelModal, (fixture, modal) => {
            appState.mockState({
                features: {
                    [Feature.MULTICHANNELLING]: true,
                },
            });
            modal.channel = channelOneLevelDeep;
            modal.item = { type: 'folder', id: 1234 } as Folder;

            fixture.detectChanges();
            expect(folderActions.getChannelSyncReport).toHaveBeenCalledTimes(1);

            const recursive: MockCheckbox = fixture.debugElement.query(By.css('.recursive-checkbox')).componentInstance;
            recursive.change.emit();
            expect(folderActions.getChannelSyncReport).toHaveBeenCalledTimes(2);
        }),
    );

    it('automatically chooses the sync target node when the channel has only one parent node',
        componentTest(() => SynchronizeChannelModal, (fixture, modal) => {
            // Master #1
            //  '- Channel #2 - target automatically set to #1
            //      '- Channel #3 - user can select to sync to #2 or #1
            modal.channel = channelOneLevelDeep;
            modal.item = { type: 'folder', id: 1234 } as Folder;

            fixture.detectChanges();

            const nodeSelector = fixture.debugElement.query(By.directive(MockNodeSelector));
            expect(nodeSelector).toBeNull();
        }),
    );

    it('lets the user choose the sync target node when the channel has more than one parent node',
        componentTest(() => SynchronizeChannelModal, (fixture, modal) => {
            // Master #1
            //  '- Channel #2 - target automatically set to #1
            //      '- Channel #3 - user can select to sync to #2 or #1
            modal.channel = channelTwoLevelsDeep;
            modal.item = { type: 'folder', id: 1234 } as Folder;

            fixture.detectChanges();

            const nodeSelector = fixture.debugElement.query(By.directive(MockNodeSelector));
            expect(nodeSelector).not.toBeNull();
        }),
    );

    it('enables the confirm button when a parent node is visible to the user',
        componentTest(() => SynchronizeChannelModal, (fixture, modal) => {
            modal.channel = channelTwoLevelsDeep;
            modal.item = { type: 'folder', id: 1234 } as Folder;

            fixture.detectChanges();

            const confirmButton = getConfirmButton(fixture);
            expect(confirmButton.disabled).toBe(false);
        }),
    );

    it('disables the confirm button when no parent node is found or permissable to the user',
        componentTest(() => SynchronizeChannelModal, (fixture, modal) => {
            appState.mockState({
                entities: {
                    node: replaceInState({
                        [channelTwoLevelsDeep.id]: channelTwoLevelsDeep,
                    }),
                },
            });

            modal.channel = channelTwoLevelsDeep;
            modal.item = { type: 'folder', id: 1234 } as Folder;

            fixture.detectChanges();

            const confirmButton = getConfirmButton(fixture);
            expect(confirmButton.disabled).toBe(true);
        }),
    );

    it('defaults to "not recursive, all item types" when submitted for a folder',
        componentTest(() => SynchronizeChannelModal, (fixture, modal) => {
            modal.channel = channelOneLevelDeep;
            modal.item = { type: 'folder', id: 1234 } as Folder;

            let modalResult: ChannelSyncRequest;
            modal.registerCloseFn(result => modalResult = result);

            fixture.detectChanges();

            getConfirmButton(fixture).click.emit();

            const expected: ChannelSyncRequest = {
                channelId: channelOneLevelDeep.id,
                masterId: masterNode.id,
                recursive: false,
                types: ['folder', 'page', 'file', 'image', 'template'],
            };

            expect(modalResult).toEqual(expected);
        }),
    );

    it('defaults to "not recursive, no item types" when submitted for a page',
        componentTest(() => SynchronizeChannelModal, (fixture, modal) => {
            modal.channel = channelOneLevelDeep;
            modal.item = { type: 'page', id: 1234 } as Page;

            let modalResult: ChannelSyncRequest;
            modal.registerCloseFn(result => modalResult = result);

            fixture.detectChanges();
            getConfirmButton(fixture).click.emit();

            const expected: ChannelSyncRequest = {
                channelId: channelOneLevelDeep.id,
                masterId: masterNode.id,
                recursive: false,
                types: [],
            };

            expect(modalResult).toEqual(expected);
        }),
    );

    it('clicking a checkbox of an object type toggles it in the value returned by the modal',
        componentTest(() => SynchronizeChannelModal, (fixture, modal) => {
            modal.channel = channelOneLevelDeep;
            modal.item = { type: 'folder', id: 1234 } as Folder;

            let lastModalResult: ChannelSyncRequest;
            modal.registerCloseFn(result => lastModalResult = result);
            fixture.detectChanges();

            const { checkboxes } = getObjectTypeSection(fixture);
            checkboxes[0].change.emit(); // toggle folders to "off"
            checkboxes[2].change.emit(); // toggle files to "off"
            checkboxes[4].change.emit(); // toggle templates to "off"

            expect(modal.selectedTypes).toEqual({
                folder: false,
                page: true,
                file: false,
                image: true,
                template: false,
            });

            getConfirmButton(fixture).click.emit();

            const expected: ChannelSyncRequest = {
                channelId: channelOneLevelDeep.id,
                masterId: masterNode.id,
                recursive: false,
                types: ['page', 'image'],
            };

            expect(lastModalResult).toEqual(expected);

            checkboxes[4].change.emit(); // toggle templates to "on"
            getConfirmButton(fixture).click.emit();
            expected.types.push('template');

            expect(lastModalResult).toEqual(expected);
        }),
    );

});

@Component({ selector: 'gtx-button', template: '' })
class MockButton {
    @Input() disabled: boolean;
    @Input() label: string;
    @Input() type: string;
    @Output() click = new EventEmitter();
}

@Component({ selector: 'gtx-progress-bar', template: '' })
class MockProgressBar {
    @Input() active: boolean;
}

@Component({ selector: 'gtx-checkbox', template: '' })
class MockCheckbox {
    @Input() checked: boolean;
    @Input() label: string;
    @Output() change = new EventEmitter();
}

@Component({ selector: 'node-selector', template: '' })
class MockNodeSelector {
    @Input() activeNodeId: number;
    @Input() nodes: Node[];
    @Input() showName: boolean;
    @Input() useLinks: boolean;
    @Output() nodeSelected = new EventEmitter();
}

@Injectable()
class SpyFolderActionsService {
    constructor() {
        spyOn(this as any, 'getChannelSyncReport').and.callThrough();
    }
    getChannelSyncReport(folderId: number, channelId: number, recursive: boolean): void { }
}

import { TestBed } from '@angular/core/testing';
import { Folder, Page } from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';
import { ApplicationStateService, STATE_MODULES } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { NodeNameOfItemPipe } from './node-name-of-item.pipe';

const NODE_ID = 123;

describe('NodeNameOfItemPipe', () => {
    let pipe: NodeNameOfItemPipe;
    let appState: TestApplicationState;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
            ],
        });
        appState = TestBed.inject(ApplicationStateService) as any;

        appState.mockState({
            entities: {
                node: {
                    123: {
                        name: 'Node Name',
                    },
                },
            },
        });
        pipe = new NodeNameOfItemPipe(appState);
    });

    it('can be created', () => {
        expect(pipe).toBeDefined();
    });

    it('uses the node id of the passed item if available', () => {
        const folderWithNodeId: Partial<Folder> = {
            type: 'folder',
            nodeId: NODE_ID,
        };

        const result = pipe.transform(folderWithNodeId as Folder);
        expect(result).toBe('Node Name');
    });

    it('uses the path of the item object if no node id is available', () => {
        const pageWithPath: Partial<Page> = {
            type: 'page',
            name: 'Page Name',
            path: '/Node Name/Subfolder 1/Subfolder 2/',
        };
        const result = pipe.transform(pageWithPath as Page);
        expect(result).toBe('Node Name');
    });

    it('returns an empty string if everything else fails', () => {
        const pageWithNoPath: Partial<Page> = {
            type: 'page',
            name: 'Page Name',
        };
        const result = pipe.transform(pageWithNoPath as Page);
        expect(result).toBe('');
    })

});

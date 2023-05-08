import { FolderState } from '../../common/models';
import { areItemsLoading } from './are-items-loading';

describe('areItemsLoading()', () => {

    let mockItemsInfo: any;
    let mockFolderState: any;

    const entities: Array<keyof FolderState> = ['nodes', 'folders', 'pages', 'files', 'images', 'breadcrumbs'];

    beforeEach(() => {
        mockItemsInfo = {
            creating: false,
            fetching: false,
            saving: false,
            deleting: [] as number[]
        };

        mockFolderState = {
            folders: mockItemsInfo,
            pages: mockItemsInfo,
            images: mockItemsInfo,
            files: mockItemsInfo,
            templates: mockItemsInfo,
            nodes: mockItemsInfo,
            activeNodeLanguages: mockItemsInfo,
            breadcrumbs: mockItemsInfo
        };
    });

    it('should return false when no async activity', () => {
        expect(areItemsLoading(mockFolderState)).toBe(false);
    });

    it('should not throw if an expected key does not exist', () => {
        delete mockFolderState.folders;
        const run = () => areItemsLoading(mockFolderState);
        expect(run).not.toThrow();
    });

    entities.forEach(entity => {

        it(`should return true when ${entity} is fetching`, () => {
            mockFolderState[entity].fetching = true;
            expect(areItemsLoading(mockFolderState)).toBe(true);
        });

        it(`should return true when ${entity} is creating`, () => {
            mockFolderState[entity].creating = true;
            expect(areItemsLoading(mockFolderState)).toBe(true);
        });

        it(`should return true when ${entity} is deleting`, () => {
            mockFolderState[entity].deleting = [1];
            expect(areItemsLoading(mockFolderState)).toBe(true);
        });

    });
});

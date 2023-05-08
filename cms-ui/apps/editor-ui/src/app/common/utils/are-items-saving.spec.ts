import { FolderState } from '../../common/models';
import { areItemsSaving } from './are-items-saving';

describe('areItemsSaving()', () => {

    let mockAppState: any;
    let mockItemsInfo: any;

    const entities: Array<keyof FolderState> = ['folders', 'pages', 'files', 'images'];

    beforeEach(() => {
        mockItemsInfo = {
            saving: false,
        };

        mockAppState = {
            editor: mockItemsInfo,
            folder: {
                folders: mockItemsInfo,
                pages: mockItemsInfo,
                files: mockItemsInfo,
                images: mockItemsInfo,
            },
        };
    });

    it('should return false when no async activity', () => {
        expect(areItemsSaving(mockAppState)).toBe(false);
    });

    it('should return true when editor is saving', () => {
        mockAppState.editor.saving = true;
        expect(areItemsSaving(mockAppState)).toBe(true);
    });

    entities.forEach(entity => {
        it(`should return true when ${entity} are saving`, () => {
            mockAppState.folder[entity].saving = true;
            expect(areItemsSaving(mockAppState)).toBe(true);
        });
    });
});

import { I18nService } from '@gentics/cms-components';
import { DependenciesCountPipe } from './dependencies-count.pipe';

describe('DependenciesCountPipe', () => {
    let pipe: DependenciesCountPipe;
    let mockI18nService: MockI18nService;
    const pageDependency = createPageDependency();

    beforeEach(() => {
        mockI18nService = new MockI18nService();
        pipe = new DependenciesCountPipe(mockI18nService as any);
    });

    it('can be created', () => {
        expect(pipe).toBeDefined();
    });

    it('shows items count with translated singular/plural texts', () => {
        const actual = pipe.transform(pageDependency);
        expect(mockI18nService.instant).toHaveBeenCalledWith('common.type_pages');
        expect(mockI18nService.instant).toHaveBeenCalledWith('common.type_file');
        expect(actual).toBe('3 common.type_pages_translated, 1 common.type_file_translated');
    });

});

function createPageDependency(): any {
    const pageDependency: any = {
        file: [{ 186: 3 }],
        page: [{ 125: 4 }, { 124: 3 }, { 126: 4 }],
    };
    return pageDependency;
}

class MockI18nService implements Partial<I18nService> {
    instant = jasmine.createSpy('instant').and.callFake((key: string) => {
        return `${key}_translated`;
    });
}

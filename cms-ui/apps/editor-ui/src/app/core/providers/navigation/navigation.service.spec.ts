import { Location } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { EditMode } from '@gentics/cms-models';
import { InstructionActions, NavigationService } from './navigation.service';

describe('NavigationService', () => {

    let navigationService: NavigationService;
    let router: MockRouter;
    let encodeOptions: (val: any) => any;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                NavigationService,
                { provide: Router, useClass: MockRouter },
                { provide: Location, useClass: MockLocation },
            ],
        });
        navigationService = TestBed.get(NavigationService);
        router = TestBed.get(Router);
        encodeOptions = navigationService.serializeOptions.bind(navigationService);
    });

    describe('commands()', () => {

        it('returns correct value for list', () => {
            const commands = navigationService.instruction({
                list: {
                    nodeId: 3,
                    folderId: 42,
                },
            }).commands();
            expect(commands).toEqual(['/editor', {
                outlets: {
                    list: ['node', 3, 'folder', 42],
                },
            }]);
        });

        it('returns correct value for detail', () => {
            const commands = navigationService.instruction({
                detail: {
                    nodeId: 1,
                    itemType: 'page',
                    itemId: 6,
                    editMode: EditMode.PREVIEW,
                },
            }).commands();
            expect(commands).toEqual(['/editor', {
                outlets: {
                    detail: ['node', 1, 'page', 6, EditMode.PREVIEW, encodeOptions({})],
                },
            }]);
        });

        it('returns correct value for detail with options', () => {
            const options = { foo: 'bar', baz: 123 } as any;
            const commands = navigationService.instruction({
                detail: {
                    nodeId: 1,
                    itemType: 'page',
                    itemId: 6,
                    editMode: EditMode.PREVIEW,
                    options: options,
                },
            }).commands();
            expect(commands).toEqual(['/editor', {
                outlets: {
                    detail: ['node', 1, 'page', 6, EditMode.PREVIEW, encodeOptions(options)],
                },
            }]);
        });

        it('returns correct value for null detail value', () => {
            const commands = navigationService.instruction({ detail: null }).commands();
            expect(commands).toEqual(['/editor', {
                outlets: {
                    detail: null,
                },
            }]);
        });

        it('returns correct value for list and detail together', () => {
            const commands = navigationService.instruction({
                list: {
                    nodeId: 3,
                    folderId: 42,
                },
                detail: {
                    nodeId: 1,
                    itemType: 'page',
                    itemId: 6,
                    editMode: EditMode.PREVIEW,
                },
            }).commands();
            expect(commands).toEqual(['/editor', {
                outlets: {
                    list: ['node', 3, 'folder', 42],
                    detail: ['node', 1, 'page', 6, EditMode.PREVIEW, encodeOptions({})],
                },
            }]);
        });
    });

    describe('navigate()', () => {

        it('returns the value of Router.navigate()', () => {
            router.navigate = jasmine.createSpy('navigate').and.returnValue('mocked return value');
            const result = navigationService.instruction({
                list: {
                    nodeId: 3,
                    folderId: 42,
                },
            }).navigate();
            expect(result).toBe('mocked return value' as any);
        });

        it('invokes Router.navigate() with correct commands', () => {
            navigationService.instruction({
                list: {
                    nodeId: 3,
                    folderId: 42,
                },
            }).navigate();
            expect(router.navigate).toHaveBeenCalledWith(['/editor', {
                outlets: {
                    list: ['node', 3, 'folder', 42],
                },
            }], undefined);
        });

        it('invokes Router.navigate() with correct commands and extras', () => {
            navigationService.instruction({
                list: {
                    nodeId: 3,
                    folderId: 42,
                },
            }).navigate({ preserveFragment: true });
            expect(router.navigate).toHaveBeenCalledWith(['/editor', {
                outlets: {
                    list: ['node', 3, 'folder', 42],
                },
            }], { preserveFragment: true });
        });
    });

    describe('navigateIfNotSet()', () => {

        let location: Location;
        let instruction: InstructionActions;

        beforeEach(() => {
            location = TestBed.get(Location);
            instruction = navigationService.instruction({
                list: {
                    nodeId: 3,
                    folderId: 42,
                },
            });
        });

        it('navigates if the url is not an editor path', () => {
            location.path = () => '/login';

            instruction.navigateIfNotSet();

            expect(router.navigate).toHaveBeenCalled();
        });

        it('does not navigate if the url is an editor path', () => {
            location.path = () => '/editor/(list:node/3/folder/56)';

            instruction.navigateIfNotSet();

            expect(router.navigate).not.toHaveBeenCalled();
        });

    });

    describe('shortcut methods', () => {

        it('list()', () => {
            const commands = navigationService.list(3, 5).commands();
            expect(commands).toEqual(['/editor', {
                outlets: {
                    list: ['node', 3, 'folder', 5],
                },
            }]);
        });

        it('detail()', () => {
            const commands = navigationService.detail(1, 'page', 2, EditMode.EDIT_PROPERTIES).commands();
            expect(commands).toEqual(['/editor', {
                outlets: {
                    detail: ['node', 1, 'page', 2, EditMode.EDIT_PROPERTIES, encodeOptions({})],
                },
            }]);
        });

        it('modal()', () => {
            const commands = navigationService.modal(1, 'image', 2, EditMode.EDIT).commands();
            expect(commands).toEqual(['/editor', {
                outlets: {
                    modal: ['node', 1, 'image', 2, EditMode.EDIT, encodeOptions({})],
                },
            }]);
        });

        it('detail() called by detailOrModal()', () => {
            const commands = navigationService.detailOrModal(1, 'page', 2, EditMode.EDIT_PROPERTIES).commands();
            expect(commands).toEqual(['/editor', {
                outlets: {
                    detail: ['node', 1, 'page', 2, EditMode.EDIT_PROPERTIES, encodeOptions({})],
                },
            }]);
        });

        it('modal() called by detailOrModal()', () => {
            const commands = navigationService.detailOrModal(1, 'image', 2, EditMode.EDIT).commands();
            expect(commands).toEqual(['/editor', {
                outlets: {
                    modal: ['node', 1, 'image', 2, EditMode.EDIT, encodeOptions({})],
                },
            }]);
        });

    });

});

class MockRouter {
    navigate = jasmine.createSpy('navigate');
}

class MockLocation {
    path: any;
}

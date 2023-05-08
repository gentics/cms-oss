import { TestBed } from '@angular/core/testing';
import { ItemInNode, RepositoryBrowserOptions } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { RepositoryBrowser } from '../../components';
import { RepositoryBrowserClient } from './repository-browser-client.service';

let service: RepositoryBrowserClient;

describe('RepositoryBrowserClientService', () => {
    let modalService: MockModalService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                RepositoryBrowserClient,
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: ModalService, useClass: MockModalService },
            ],
        });

        modalService = TestBed.get(ModalService);
        service = TestBed.get(RepositoryBrowserClient);
    });

    it('can be created ', () => {
        expect(service).toBeDefined();
    });

    describe('openRepositoryBrowser()', () => {

        const selected: ItemInNode | ItemInNode[] = [];

        beforeEach(() => {
            modalService.fromComponent = jasmine.createSpy('fromComponent')
                .and.returnValue(Promise.resolve({
                    open: (): any => Promise.resolve(selected),
                }));
        });

        it('works fine for single folder', () => {
            const options: RepositoryBrowserOptions = { allowedSelection: 'folder', selectMultiple: false };

            serviceSetupForSingleFolder();

            expect(modalService.fromComponent).toHaveBeenCalledWith(RepositoryBrowser, { padding: true, width: '1000px' }, { options });
        });

        it('works fine for multiple folders', () => {
            const options: RepositoryBrowserOptions = { allowedSelection: 'folder', selectMultiple: true };

            serviceSetupForMultipleFolders();

            expect(modalService.fromComponent).toHaveBeenCalledWith(RepositoryBrowser, { padding: true, width: '1000px' }, { options });
        });

        it('works fine for single page', () => {
            const options: RepositoryBrowserOptions = { allowedSelection: 'page', selectMultiple: false };

            serviceSetupForSinglePage();

            expect(modalService.fromComponent).toHaveBeenCalledWith(RepositoryBrowser, { padding: true, width: '1000px' }, { options });
        });

        it('works fine for multiple pages', () => {
            const options: RepositoryBrowserOptions = { allowedSelection: 'page', selectMultiple: true};

            serviceSetupForMultiplePages();

            expect(modalService.fromComponent).toHaveBeenCalledWith(RepositoryBrowser, { padding: true, width: '1000px' }, { options });
        });
    });
});

class MockErrorHandler {}

class MockModalService {
    fromComponent = jasmine.createSpy('fromComponent');
}

function serviceSetupForSingleFolder(): void {
    service.openRepositoryBrowser({
        allowedSelection: 'folder',
        selectMultiple: false,
    });
}

function serviceSetupForMultipleFolders(): void {
    service.openRepositoryBrowser({
        allowedSelection: 'folder',
        selectMultiple: true,
    });
}

function serviceSetupForSinglePage(): void {
    service.openRepositoryBrowser({
        allowedSelection: 'page',
        selectMultiple: false,
    });
}

function serviceSetupForMultiplePages(): void {
    service.openRepositoryBrowser({
        allowedSelection: 'page',
        selectMultiple: true,
    });
}

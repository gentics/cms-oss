import { TestBed } from '@angular/core/testing';
import { ApplicationStateService, StateModule } from '@editor-ui/app/state';
import { TestApplicationState } from '@editor-ui/app/state/test-application-state.mock';
import { RepositoryBrowserOptions } from '@gentics/cms-integration-api-models';
import { ItemInNode } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { RepositoryBrowser } from '../../components';
import { RepositoryBrowserClient } from './repository-browser-client.service';

let service: RepositoryBrowserClient;

describe('RepositoryBrowserClientService', () => {

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                RepositoryBrowserClient,
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: ModalService, useClass: MockModalService },
                { provide: ApplicationStateService, useClass: TestApplicationState },
            ],
            imports: [
                StateModule,
            ],
        });

        service = TestBed.inject(RepositoryBrowserClient);
    });

    it('can be created ', () => {
        expect(service).toBeDefined();
    });

    describe('openRepositoryBrowser()', () => {

        let modalService: MockModalService;
        const selected: ItemInNode | ItemInNode[] = [];

        beforeEach(() => {
            modalService = TestBed.inject(ModalService) as any;
            modalService.fromComponent = jasmine.createSpy('fromComponent')
                .and.returnValue(Promise.resolve({
                    open: (): any => Promise.resolve(selected),
                }));
        });

        it('works fine for single folder', (async () => {
            const options: RepositoryBrowserOptions = { allowedSelection: 'folder', selectMultiple: false };
            await service.openRepositoryBrowser(options);
            expect(modalService.fromComponent).toHaveBeenCalledWith(
                RepositoryBrowser,
                jasmine.objectContaining({ padding: true, width: '1000px' }),
                { options },
            );
        }));

        it('works fine for multiple folders', (async () => {
            const options: RepositoryBrowserOptions = { allowedSelection: 'folder', selectMultiple: true };
            await service.openRepositoryBrowser(options);
            expect(modalService.fromComponent).toHaveBeenCalledWith(
                RepositoryBrowser,
                jasmine.objectContaining({ padding: true, width: '1000px' }),
                { options },
            );
        }));

        it('works fine for single page', (async () => {
            const options: RepositoryBrowserOptions = { allowedSelection: 'page', selectMultiple: false };
            await service.openRepositoryBrowser(options);
            expect(modalService.fromComponent).toHaveBeenCalledWith(
                RepositoryBrowser,
                jasmine.objectContaining({ padding: true, width: '1000px' }),
                { options },
            );
        }));

        it('works fine for multiple pages', (async () => {
            const options: RepositoryBrowserOptions = { allowedSelection: 'page', selectMultiple: true};
            await service.openRepositoryBrowser(options);
            expect(modalService.fromComponent).toHaveBeenCalledWith(
                RepositoryBrowser,
                jasmine.objectContaining({ padding: true, width: '1000px' }),
                { options },
            );
        }));
    });
});

class MockErrorHandler {}

class MockModalService {
    fromComponent = (...args: any[]) => Promise.resolve({});
}

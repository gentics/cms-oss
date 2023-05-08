import { InterfaceOf, ObservableStopper } from '@admin-ui/common';
import { createDelayedError, createDelayedObservable } from '@admin-ui/testing';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { Folder, GcmsTestData, Raw, RecursivePartial } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { takeUntil } from 'rxjs/operators';
import { FolderOperations } from '.';
import { EntityManagerService } from '../../entity-manager';
import { MockEntityManagerService } from '../../entity-manager/entity-manager.service.mock';
import { ErrorHandler } from '../../error-handler';
import { MockErrorHandler } from '../../error-handler/error-handler.mock';
import { I18nNotificationService } from '../../i18n-notification';
import { MockI18nNotificationService } from '../../i18n-notification/i18n-notification.service.mock';

class MockApi implements RecursivePartial<InterfaceOf<GcmsApi>> {
    folders = {
        getItem: jasmine.createSpy('getItem').and.stub(),
    };
}

describe('FolderOperations', () => {

    let api: MockApi;
    let entityManager: MockEntityManagerService;
    let errorHandler: MockErrorHandler;
    let folderOps: FolderOperations;
    let stopper: ObservableStopper;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                FolderOperations,
                { provide: EntityManagerService, useClass: MockEntityManagerService },
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: GcmsApi, useClass: MockApi },
                { provide: I18nNotificationService, useClass: MockI18nNotificationService },
            ],
        });

        api = TestBed.get(GcmsApi);
        entityManager = TestBed.get(EntityManagerService);
        errorHandler = TestBed.get(ErrorHandler);
        folderOps = TestBed.get(FolderOperations);
        stopper = new ObservableStopper();
    });

    afterEach(() => {
        stopper.stop();
    });

    describe('get()', () => {

        it('fetches a folder and adds it to the EntityState', fakeAsync(() => {
            const mockFolder = GcmsTestData.getExampleFolderData({ id: 1 });
            api.folders.getItem.and.returnValue(
                createDelayedObservable({ folder: mockFolder }),
            );

            let result: Folder<Raw>;
            folderOps.get(1).pipe(
                takeUntil(stopper.stopper$),
            ).subscribe(folder => result = folder);

            tick();
            expect(result).toBe(mockFolder);
            expect(entityManager.addEntity).toHaveBeenCalledTimes(1);
            expect(entityManager.addEntity).toHaveBeenCalledWith('folder', mockFolder);
        }));

        it('notifies the user about errors and rethrows them', fakeAsync(() => {
            const error = new Error('Test Error');
            api.folders.getItem.and.returnValue(createDelayedError(error));
            errorHandler.assertNotifyAndRethrowIsCalled(folderOps.get(1), error);
        }));

    });

});

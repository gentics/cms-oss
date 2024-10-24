import { Type } from '@angular/core';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { I18nService } from '@editor-ui/app/core/providers/i18n/i18n.service';
import { IModalDialog, IModalInstance, IModalOptions, ModalService } from '@gentics/ui-core';
import { CoreModule } from '../../core/core.module';
import { DebugToolService } from './debug-tool.service';

class MockModalService implements Partial<ModalService> {
    public fromComponent<T extends IModalDialog>(component: Type<T>, options?: IModalOptions, locals?: { [K in keyof T]?: T[K]; }): Promise<IModalInstance<T>> {
        return Promise.resolve(null);
    }
}
class MockI18nService implements Partial<I18nService> {}

describe('DebugToolService', () => {

    // ToDo: If we implement integration tests, we should implement one for the debug tools as well,
    // because the punycode workaround `runWithPatchedPunycode()` is not needed in the unit tests,
    // since they somehow load the old version of the punycode library. So, an automated test for
    // this workaround is only possible as an integration test.

    let debugToolService: DebugToolService;
    let modalService: ModalService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                DebugToolService,
                { provide: ModalService, useClass: MockModalService },
                { provide: I18nService, useClass: MockI18nService },
            ],
            imports: [CoreModule],
        });

        modalService = TestBed.inject(ModalService);
        /// ???
        spyOn(DebugToolService.prototype, 'initialize').and.stub();
        debugToolService = TestBed.inject(DebugToolService);

        // Fake AppState
        const debugAppStateSpy = jasmine.createSpy('debug_appState').and.returnValue({});
        Object.defineProperty(debugToolService, 'debug_appState', { get: debugAppStateSpy });
    });

    it('gets created ok', () => {
        expect(debugToolService.initialize).toHaveBeenCalled();
    });

    describe('modal actions', () => {
        it('should download report on Generate Report', fakeAsync(() => {
            spyOn(debugToolService, 'requestApiData').and.callFake((): Promise<any> => {
                return Promise.resolve({
                    user: { success: true },
                    userData: { success: true },
                    node: { success: true },
                });
            });

            spyOn(modalService, 'fromComponent').and.callFake((() => {
                return Promise.resolve({
                    open: (): any => {
                        return debugToolService.generateReport();
                    },
                });
            }) as any);

            spyOn(debugToolService, 'downloadReport').and.stub();

            debugToolService.runDebugTool();
            tick(10);

            expect(debugToolService.requestApiData).toHaveBeenCalled();
            expect(debugToolService.downloadReport).toHaveBeenCalled();
        }));

        it('should call clearBrowserData on Clear Local Data', fakeAsync(() => {
            spyOn(modalService, 'fromComponent').and.callFake((() => {
                return Promise.resolve({
                    open: (): any => {
                        return debugToolService.clearSiteData();
                    },
                });
            }) as any);

            spyOn(debugToolService, 'clearSiteData').and.callFake(() => {
                return Promise.resolve('clear');
            });

            debugToolService.runDebugTool();
            tick(10);

            expect(debugToolService.clearSiteData).toHaveBeenCalled();
        }));
    });

    describe('generateReport()', () => {

        it('returns all available data', fakeAsync(() => {
            spyOn(debugToolService, 'requestApiData').and.callFake((): Promise<any> => {
                return Promise.resolve({
                    user: { success: true },
                    userData: { success: true },
                    node: { success: true },
                });
            });

            let result: any;
            debugToolService.generateReport().then((res) => {
                result = res;
            });

            tick();

            expect(result).not.toBeNull();
        }));

        it('returns available data even if requestApiData fails', fakeAsync(() => {
            spyOn(debugToolService, 'requestApiData').and.callFake((): Promise<any> => {
                return Promise.resolve(null);
            });

            let result: any;
            debugToolService.generateReport().then((res) => {
                result = res;
            });

            tick();

            expect(result).not.toBeNull();
            expect(result.apiData).toBeNull();
        }));

    });

});

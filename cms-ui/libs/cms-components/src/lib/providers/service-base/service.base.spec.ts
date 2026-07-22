import { ServiceBase } from './service.base';

class MockService extends ServiceBase {

    onServiceDestroySpy = jasmine.createSpy('onServiceInitSpy').and.stub();

    protected onServiceDestroy(): void {
        this.onServiceDestroySpy();
    }

}

describe('ServiceBase', () => {

    let service: MockService;

    beforeEach(() => {
        service = new MockService();
    });

    it('onServiceDestroy() is called in ngOnDestroy()', () => {
        service.ngOnDestroy();
        expect(service.onServiceDestroySpy).toHaveBeenCalledTimes(1);
    });

});

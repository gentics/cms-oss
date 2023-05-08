import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { Feature } from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';
import { of } from 'rxjs';
import { Api } from '../../../core/providers/api/api.service';
import { STATE_MODULES } from '../../modules';
import { TestApplicationState } from '../../test-application-state.mock';
import { ApplicationStateService } from '../application-state/application-state.service';
import { FeaturesActionsService } from './features-actions.service';

describe('FeaturesActionsService', () => {

    let state: TestApplicationState;
    let api: Api;
    let featuresActions: FeaturesActionsService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: Api, useClass: MockApi },
                FeaturesActionsService,
            ],
        });
        state = TestBed.get(ApplicationStateService);
        api = TestBed.get(Api);
        featuresActions = TestBed.get(FeaturesActionsService);
    });

    describe('checkFeature()', () => {

        it('sets feature to true, if enabled', fakeAsync(() => {
            let result: boolean;
            spyOn(api.admin, 'getFeature').and.callFake(((key: Feature) => of({ name: key, activated: true })) as any);

            featuresActions.checkFeature(Feature.nice_urls).then((active: boolean) => {
                result = active;
            });
            tick();
            expect(state.now.features[Feature.nice_urls]).toBeTrue();
            expect(result).toEqual(true);
        }));

        it('sets feature to false, if disabled', fakeAsync(() => {
            let result: boolean;
            spyOn(api.admin, 'getFeature').and.callFake(((key: Feature) => of({ name: key, activated: false })) as any);

            featuresActions.checkFeature(Feature.nice_urls).then((active: boolean) => {
                result = active;
            });
            tick();
            expect(state.now.features[Feature.nice_urls]).toBeFalse();
            expect(result).toEqual(false);
        }));
    });

    class MockApi {
        admin = {
            getFeature: () => {
                throw new Error('called unmocked function api.admin.getFeature()');
            },
        };
    }
});

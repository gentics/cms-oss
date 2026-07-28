import { TestBed, waitForAsync } from '@angular/core/testing';
import { NgxsModule } from '@ngxs/store';

import { AppStateService } from '../providers/app-state/app-state.service';
import { TestAppState, TEST_APP_STATE } from '../utils/test-app-state';
import {
    DecrementDetailLoading,
    DecrementMasterLoading,
    IncrementDetailLoading,
    IncrementMasterLoading,
} from './loading.actions';
import { INITIAL_LOADING_STATE, LoadingStateModel, LoadingStateModule } from './loading.state';

describe('LoadingStateModule', () => {

    let appState: TestAppState;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot([LoadingStateModule])],
            providers: [TEST_APP_STATE],
        }).compileComponents();
        appState = TestBed.inject(AppStateService) as any;
    }));

    it('sets the correct initial state', () => {
        appState.selectOnce(state => state.loading).subscribe(loadingState => {
            expect(loadingState).toEqual(INITIAL_LOADING_STATE);
        });
    });

    describe('detailLoading', () => {

        it('IncrementDetailLoading works', () => {
            appState.dispatch(new IncrementDetailLoading());
            appState.selectOnce(state => state.loading).subscribe(loadingState => {
                expect(loadingState).toEqual(jasmine.objectContaining<LoadingStateModel>({
                    detailLoading: 1,
                    masterLoading: 0,
                }));
            });
        });

        it('DecrementDetailLoading works', () => {
            appState.mockState({
                loading: {
                    detailLoading: 1,
                },
            });
            appState.dispatch(new DecrementDetailLoading());
            appState.selectOnce(state => state.loading).subscribe(loadingState => {
                expect(loadingState).toEqual(jasmine.objectContaining<LoadingStateModel>({
                    detailLoading: 0,
                    masterLoading: 0,
                }));
            });
        });
    });

    describe('masterLoading', () => {

        it('IncrementMasterLoading works', () => {
            appState.dispatch(new IncrementMasterLoading());
            appState.selectOnce(state => state.loading).subscribe(loadingState => {
                expect(loadingState).toEqual(jasmine.objectContaining<LoadingStateModel>({
                    detailLoading: 0,
                    masterLoading: 1,
                }));
            });
        });

        it('DecrementMasterLoading works', () => {
            appState.mockState({
                loading: {
                    masterLoading: 1,
                },
            });
            appState.dispatch(new DecrementMasterLoading());
            appState.selectOnce(state => state.loading).subscribe(loadingState => {
                expect(loadingState).toEqual(jasmine.objectContaining<LoadingStateModel>({
                    detailLoading: 0,
                    masterLoading: 0,
                }));
            });
        });
    });
});
